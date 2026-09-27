export interface UploadEnv {
  CLOUDINARY_API_KEY?: string;
  CLOUDINARY_API_SECRET?: string;
  UPLOAD_RATE_LIMITER?: { limit(options: {key: string}): Promise<{success: boolean}> };
}
const MAX_BYTES = 10 * 1024 * 1024;
const CLOUD = 'dskoyv2oe';

export async function handleUpload(request: Request, uid: string, env: UploadEnv): Promise<Response> {
  if (!env.CLOUDINARY_API_KEY || !env.CLOUDINARY_API_SECRET || !env.UPLOAD_RATE_LIMITER)
    return Response.json({reason: 'uploads_unavailable'}, {status: 503});
  if (!(await env.UPLOAD_RATE_LIMITER.limit({key: uid})).success)
    return Response.json({reason: 'rate_limited'}, {status: 429, headers: {'Retry-After': '60'}});
  // Count the actual stream: Content-Length and client validation are untrusted.
  if (Number(request.headers.get('content-length')) > MAX_BYTES)
    return Response.json({reason: 'photo_too_large'}, {status: 413});
  const reader = request.body?.getReader();
  if (!reader) return Response.json({reason: 'invalid_photo'}, {status: 400});
  const chunks: Uint8Array[] = [];
  let length = 0;
  try {
    while (true) {
      const {done, value} = await reader.read();
      if (done) break;
      length += value.byteLength;
      if (length > MAX_BYTES) {
        await reader.cancel();
        return Response.json({reason: 'photo_too_large'}, {status: 413});
      }
      chunks.push(value);
    }
  } finally { reader.releaseLock(); }
  const bytes = new Uint8Array(length);
  let offset = 0;
  for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
  const starts = (...signature: number[]) => signature.every((b, i) => bytes[i] === b);
  const ascii = (start: number, end: number) => String.fromCharCode(...bytes.slice(start, end));
  const format = starts(0xff, 0xd8, 0xff) ? 'jpg'
    : starts(137, 80, 78, 71, 13, 10, 26, 10) ? 'png'
    : ascii(0, 4) === 'RIFF' && ascii(8, 12) === 'WEBP' ? 'webp' : null;
  if (!format) return Response.json({reason: 'invalid_photo'}, {status: 415});
  // Hash UID so even unusual valid Firebase UIDs cannot inject paths/parameters.
  const hex = (buffer: ArrayBuffer) => Array.from(new Uint8Array(buffer), b => b.toString(16).padStart(2, '0')).join('');
  const owner = hex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(uid)));
  const params: Record<string, string> = {
    allowed_formats: 'jpg,png,webp',
    overwrite: 'false',
    public_id: `onthejob/${owner}/${crypto.randomUUID()}`,
    timestamp: String(Math.floor(Date.now() / 1000)),
  };
  const signingText = Object.keys(params).sort().map(key => `${key}=${params[key]}`).join('&');
  const signature = hex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(signingText + env.CLOUDINARY_API_SECRET)));
  const body = new FormData();
  for (const [key, value] of Object.entries(params)) body.set(key, value);
  body.set('api_key', env.CLOUDINARY_API_KEY);
  body.set('signature', signature);
  body.set('file', new Blob([bytes], {type: `image/${format === 'jpg' ? 'jpeg' : format}`}), `photo.${format}`);
  // The image endpoint decodes the file; allowed_formats rejects non-raster input.
  const upstream = await fetch(`https://api.cloudinary.com/v1_1/${CLOUD}/image/upload`, {
    method: 'POST', body, signal: AbortSignal.timeout(30000),
  });
  if (!upstream.ok) return Response.json({reason: 'upload_failed'}, {status: 502});
  const result = await upstream.json() as {secure_url?: string};
  if (!result.secure_url?.startsWith(`https://res.cloudinary.com/${CLOUD}/image/upload/`))
    return Response.json({reason: 'invalid_upload_response'}, {status: 502});
  return Response.json({secure_url: result.secure_url});
}
