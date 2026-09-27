import {describe, it, expect, vi, afterEach} from 'vitest';
import worker from '../src/index';
import {handleUpload} from '../src/upload';
const env = {CLOUDINARY_API_KEY: 'key', CLOUDINARY_API_SECRET: 'secret', UPLOAD_RATE_LIMITER: {limit: async () => ({success: true})}};
const request = (bytes: Uint8Array) => new Request('https://example.com/upload', {method: 'POST', body: bytes});
afterEach(() => vi.restoreAllMocks());
describe('photo upload security', () => {
 it('rejects anonymous uploads before contacting Cloudinary', async () => {
  const fetcher = vi.spyOn(globalThis, 'fetch');
  const result = await worker.fetch(request(new Uint8Array([255,216,255])), {...env, GEMINI_API_KEY:'', AI_RATE_LIMITER:env.UPLOAD_RATE_LIMITER});
  expect(result.status).toBe(401); expect(fetcher).not.toHaveBeenCalled();
 });
 it('fails closed without secrets or rate limiter', async () => {
  expect((await handleUpload(request(new Uint8Array()), 'owner', {})).status).toBe(503);
 });
 it('rejects rate-limited users and unsupported content without upstream uploads', async () => {
  const fetcher = vi.spyOn(globalThis, 'fetch');
  expect((await handleUpload(request(new Uint8Array()), 'owner', {...env, UPLOAD_RATE_LIMITER:{limit:async()=>({success:false})}})).status).toBe(429);
  expect((await handleUpload(request(new TextEncoder().encode('<svg/>')), 'owner', env)).status).toBe(415);
  expect(fetcher).not.toHaveBeenCalled();
 });
 it('enforces the actual byte limit without Content-Length', async () => {
  expect((await handleUpload(request(new Uint8Array(10*1024*1024+1)), 'owner', env)).status).toBe(413);
 });
 it('signs server-controlled parameters and does not disclose credentials', async () => {
  const fetcher = vi.spyOn(globalThis, 'fetch').mockResolvedValue(Response.json({secure_url:'https://res.cloudinary.com/dskoyv2oe/image/upload/photo.jpg'}));
  const result = await handleUpload(request(new Uint8Array([255,216,255])), '../someone&overwrite=true', env);
  expect(result.status).toBe(200);
  const body = fetcher.mock.calls[0][1]!.body as FormData;
  expect(body.get('upload_preset')).toBeNull();
  expect(body.get('public_id')).toMatch(/^onthejob\/[a-f0-9]{64}\/[a-f0-9-]+$/);
  expect(body.get('overwrite')).toBe('false');
  const params = ['allowed_formats','overwrite','public_id','timestamp'].map(key=>`${key}=${body.get(key)}`).join('&');
  const digest = await crypto.subtle.digest('SHA-256',new TextEncoder().encode(params+'secret'));
  expect(body.get('signature')).toBe(Array.from(new Uint8Array(digest),b=>b.toString(16).padStart(2,'0')).join(''));
  expect(await result.text()).not.toContain('secret');
 });
 it('does not expose provider errors', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('secret diagnostics',{status:400}));
  const result=await handleUpload(request(new Uint8Array([255,216,255])), 'owner', env);
  expect(result.status).toBe(502); expect(await result.text()).not.toContain('secret');
 });
});
