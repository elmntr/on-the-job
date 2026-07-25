import { importX509, jwtVerify, type JWTVerifyGetKey, type KeyLike } from "jose";

interface Env {
  GEMINI_API_KEY: string;
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
const importedKeyCache = new Map<string, KeyLike>();

async function getFirebaseCerts(): Promise<Record<string, string>> {
  const now = Date.now();
  if (certCache && now < certCache.expiresAt) {
    return certCache.certs;
  }

  const res = await fetch(FIREBASE_CERT_URL);
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

  const cached = importedKeyCache.get(kid);
  if (cached) return cached;

  const certs = await getFirebaseCerts();
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
  if (!match) return null;

  const token = match[1];
  try {
    const { payload } = await jwtVerify(token, getKey, {
      issuer: FIREBASE_ISSUER,
      audience: FIREBASE_PROJECT_ID,
      algorithms: ["RS256"],
    });
    if (!payload.sub) return null;
    return { uid: payload.sub };
  } catch (err) {
    console.error("Firebase ID token verification failed:", err);
    return null;
  }
}

// -------------------------------------------------------------------------

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
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
      const body = (await request.json()) as { rawText?: string };
      rawText = body.rawText ?? "";
      if (!rawText.trim()) {
        return Response.json({ success: false, reason: "failed_other" }, { status: 400 });
      }
    } catch (err) {
      console.error("Gemini proxy error:", err);
      return Response.json({ success: false, reason: "failed_other", debug: String(err) });
    }
    try {
      const geminiRes = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=${env.GEMINI_API_KEY}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
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
      // Rate limit or quota exhaustion
      if (geminiRes.status === 429) {
        const errBody = await geminiRes.json().catch(() => ({}));
        const isQuotaExhausted = JSON.stringify(errBody).includes("RESOURCE_EXHAUSTED");
        return Response.json({
          success: false,
          reason: isQuotaExhausted ? "quota_exhausted" : "rate_limited",
        });
      }
      if (!geminiRes.ok) {
        const errText = await geminiRes.text();
        console.error("Gemini API error:", geminiRes.status, errText);
        return Response.json({ success: false, reason: "failed_other", debug: errText });
      }
      const data = (await geminiRes.json()) as any;
      const formattedText: string = data?.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
      if (!formattedText.trim()) {
        return Response.json({ success: false, reason: "failed_other" });
      }
      return Response.json({ success: true, formattedText });
    } catch (err) {
      return Response.json({ success: false, reason: "failed_other" });
    }
  },
} satisfies ExportedHandler<Env>;
