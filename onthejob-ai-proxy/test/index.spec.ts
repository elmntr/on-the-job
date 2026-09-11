import {SELF} from 'cloudflare:test';
import {describe,it,expect} from 'vitest';
describe('Browser access and authentication',()=>{
 it('allows authenticated browser preflight',async()=>{const response=await SELF.fetch('https://example.com',{method:'OPTIONS',headers:{Origin:'https://student.example'}});expect(response.status).toBe(200);expect(response.headers.get('Access-Control-Allow-Origin')).toBe('*');expect(response.headers.get('Access-Control-Allow-Headers')).toContain('Authorization');});
 it('returns browser-readable unauthorized errors',async()=>{const response=await SELF.fetch('https://example.com',{method:'POST',body:JSON.stringify({rawText:'Daily notes'})});expect(response.status).toBe(401);expect(response.headers.get('Access-Control-Allow-Origin')).toBe('*');expect(await response.json()).toEqual({success:false,reason:'unauthorized'});});
 it('rejects unsupported methods with CORS intact',async()=>{const response=await SELF.fetch('https://example.com');expect(response.status).toBe(405);expect(response.headers.get('Access-Control-Allow-Origin')).toBe('*');});
});
