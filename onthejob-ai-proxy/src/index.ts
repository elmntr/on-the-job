import { upstreamCategory, exceptionCategory } from './ai-diagnostics';
import { importX509, jwtVerify, type JWTVerifyGetKey } from "jose";

interface Env {
  GEMINI_API_KEY: string;
  AI_RATE_LIMITER: {limit(options: {key: string}): Promise<{success: boolean}>};
}

// ---- Firebase Auth ID token verification -------------------------------
// Firebase ID tokens are RS256-signed JWTs. Google does NOT publish these
// as a standard JWKS — the endpoint below returns a plain map of
// { [kid]: PEM_X509_CERT }, so we can't use jose's createRemoteJWKSet()
// helper directly. We fetch the cert map ourselves, respect the
// Cache-Control max-age Google sends back, and lazily import+cache each
// PEM cert as a CryptoKey via jose.importX509().
//
// Cache lives in module scope, so it's reused across requests handled by
// the same warm isolate. It is NOT persistent across cold starts/isolate
// recycling (no KV involved, per the "no new infra" decision) — that's
// fine, it just means occasional refetches, never a correctness issue.

const FIREBASE_PROJECT_ID = "on-the-job-19c0f";
const FIREBASE_ISSUER = `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`;
const FIREBASE_CERT_URL =
  "https://www.googleapis.com/service_accounts/v1/metadata/x509/securetoken@system.gserviceaccount.com";

let certCache: { certs: Record<string, string>; expiresAt: number } | null = null;
const importedKeyCache = new Map<string, CryptoKey>();

async function getFirebaseCerts(): Promise<Record<string, string>> {
  const now = Date.now();
  if (certCache && now < certCache.expiresAt) {
    return certCache.certs;
  }

  const res = await fetch(FIREBASE_CERT_URL, {signal: AbortSignal.timeout(10000)});
  if (!res.ok) {
    throw new Error(`Failed to fetch Firebase certs: ${res.status}`);
  }
  const certs = (await res.json()) as Record<string, string>;

  // Respect Cache-Control max-age; fall back to 1 hour if missing/unparseable.
  const cacheControl = res.headers.get("cache-control") ?? "";
  const maxAgeMatch = cacheControl.match(/max-age=(\d+)/);
  const maxAgeSeconds = maxAgeMatch ? parseInt(maxAgeMatch[1], 10) : 3600;

  certCache = { certs, expiresAt: now + maxAgeSeconds * 1000 };
  // New cert set fetched — drop any imported keys tied to old certs so
  // rotated kids get re-imported fresh.
  importedKeyCache.clear();
  return certs;
}

const getKey: JWTVerifyGetKey = async (header) => {
  const kid = header.kid;
  if (!kid) throw new Error("Token header missing kid");

  const certs = await getFirebaseCerts();
  const cached = importedKeyCache.get(kid);
  if (cached) return cached;

  const pem = certs[kid];
  if (!pem) throw new Error(`No matching Firebase cert for kid: ${kid}`);

  const key = await importX509(pem, "RS256");
  importedKeyCache.set(kid, key);
  return key;
};

/**
 * Verifies the Authorization: Bearer <token> header against Firebase Auth.
 * Returns the decoded payload (including `sub` = Firebase uid) on success,
 * or null if verification fails for any reason. Never throws.
 */
async function verifyFirebaseAuth(request: Request): Promise<{ uid: string } | null> {
  const authHeader = request.headers.get("Authorization") ?? "";
  const match = authHeader.match(/^Bearer (.+)$/);
  if (!match || match[1].length > 8192) return null;

  const token = match[1];
  try {
    const { payload } = await jwtVerify(token, getKey, {
      issuer: FIREBASE_ISSUER,
      audience: FIREBASE_PROJECT_ID,
      algorithms: ["RS256"],
    });
    if (!payload.sub || payload.sub.length > 128 || typeof payload.exp !== "number" || typeof payload.iat !== "number" || payload.iat > Date.now()/1000 + 60) return null;
    return { uid: payload.sub };
  } catch {
    return null;
  }
}

// -------------------------------------------------------------------------

// Count actual streamed bytes; Content-Length is optional and cannot be trusted.
export async function readRawText(request: Request): Promise<string> {
  if (request.headers.get('content-type')?.split(';')[0].trim().toLowerCase() !== 'application/json') throw new Error('Expected JSON');
  if (Number(request.headers.get('content-length')) > 131072) throw new RangeError('Too large');
  const reader = request.body?.getReader();
  if (!reader) throw new Error('Empty body');
  const chunks: Uint8Array[] = [];
  let size = 0;
  try {
    while (true) {
      const {done, value} = await reader.read();
      if (done) break;
      size += value.byteLength;
      if (size > 131072) {await reader.cancel(); throw new RangeError('Too large');}
      chunks.push(value);
    }
  } finally {reader.releaseLock();}
  const bytes = new Uint8Array(size);
  let offset = 0;
  for (const chunk of chunks) {bytes.set(chunk, offset); offset += chunk.length;}
  const body: unknown = JSON.parse(new TextDecoder('utf-8', {fatal: true, ignoreBOM: false}).decode(bytes));
  if (!body || typeof body !== 'object' || !('rawText' in body) || typeof body.rawText !== 'string' || !body.rawText.trim()) throw new Error('Invalid text');
  if (body.rawText.length > 20000) throw new RangeError('Too much text');
  return body.rawText;
}

async function handleRequest(request: Request, env: Env): Promise<Response> {
    // CORS preflight (Android app calls this directly, but keep it safe if you ever test from a browser)
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, Authorization",
        },
      });
    }

    // Only accept POST requests
    if (request.method !== "POST") {
      return new Response("Method not allowed", { status: 405 });
    }

    // Require a valid Firebase Auth ID token before doing anything else,
    // so only signed-in app users can invoke this and burn Gemini quota.
    const auth = await verifyFirebaseAuth(request);
    if (!auth) {
      return Response.json({ success: false, reason: "unauthorized" }, { status: 401 });
    }

    let rawText: string;
    try {
      rawText = await readRawText(request);
    } catch (error) {
      return Response.json({ success: false, reason: "failed_other" }, {status: error instanceof RangeError ? 413 : 400});
    }
    const allowed = await env.AI_RATE_LIMITER.limit({key: auth.uid});
    if (!allowed.success) return Response.json({success: false, reason: "rate_limited"}, {status: 429, headers: {"Retry-After": "60"}});
    try {
      const geminiRes = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
          signal: AbortSignal.timeout(25000),
          body: JSON.stringify({
            generationConfig: {maxOutputTokens: 8192},
            contents: [
              {
                parts: [
                  {
                    text: `Rewrite this OJT (On-the-Job Training) daily log into exactly ONE polished, professional narrative entry, written in first person past tense. Output ONLY the rewritten entry itself — no options, no headers, no markdown formatting, no explanations, no preamble. Do not invent details that weren't mentioned in the raw description.\n\nRaw description: ${rawText}`,
                  },
                ],
              },
            ],
          }),
        }
      );
      if (!geminiRes.ok) {
        const errorBody: unknown = await geminiRes.json().catch(() => null);
        const category = upstreamCategory(errorBody);
        console.warn('ai_upstream_failure', { status: geminiRes.status, category });
        if (geminiRes.status === 429) {
          return Response.json({
            success: false,
            reason: category === 'RESOURCE_EXHAUSTED' ? 'quota_exhausted' : 'rate_limited',
          });
        }
        return Response.json({ success: false, reason: "failed_other" }, {status: 502});
      }
      const data = (await geminiRes.json()) as any;
      const formattedText: string = data?.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
      if (typeof formattedText !== "string" || !formattedText.trim() || formattedText.length > 40000) {
        console.warn('ai_invalid_output', { category: 'missing_or_invalid_text' });
        return Response.json({ success: false, reason: "failed_other" });
      }
      return Response.json({ success: true, formattedText });
    } catch (err) {
      console.warn('ai_request_failure', { category: exceptionCategory(err) });
      return Response.json({ success: false, reason: "failed_other" });
    }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const response = await handleRequest(request, env).catch(() => Response.json({success: false, reason: "failed_other"}, {status: 503}));
    // Bearer tokens authorize requests; CORS must also cover errors and success.
    const headers = new Headers(response.headers);
    headers.set("Cache-Control", "no-store");
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("Access-Control-Allow-Origin", "*");
    headers.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    return new Response(response.body, { status: response.status, headers });
  },
} satisfies ExportedHandler<Env>;
