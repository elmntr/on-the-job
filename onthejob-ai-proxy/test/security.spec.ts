import {describe,it,expect,vi,afterEach} from 'vitest';
import worker,{readRawText} from '../src/index';
import {jwtVerify} from 'jose';
vi.mock('jose',async(importOriginal)=>({...await importOriginal<typeof import('jose')>(),jwtVerify:vi.fn()}));
const request=(body:unknown)=>new Request('https://example.com',{method:'POST',headers:{'content-type':'application/json',Authorization:'Bearer fake'},body:JSON.stringify(body)});
afterEach(()=>vi.restoreAllMocks());
describe('AI request security',()=>{
 it('accepts normal text but rejects invalid types and oversized bodies',async()=>{
  expect(await readRawText(request({rawText:'Work notes'}))).toBe('Work notes');
  for(const body of [null,[],{rawText:3},{rawText:' '},{rawText:'a'.repeat(20001)},{padding:'a'.repeat(131073)}])await expect(readRawText(request(body))).rejects.toThrow();
 });
 it('rejects a streamed body above the byte limit without Content-Length',async()=>{
  const stream=new ReadableStream({start(controller){controller.enqueue(new Uint8Array(131073));controller.close();}});
  const req=new Request('https://example.com',{method:'POST',headers:{'content-type':'application/json'},body:stream});
  await expect(readRawText(req)).rejects.toThrow();
 });
 it('returns the formatted entry for an authenticated valid request',async()=>{
  vi.mocked(jwtVerify).mockResolvedValue({payload:{sub:'student',exp:Date.now()/1000+100,iat:Date.now()/1000},protectedHeader:{alg:'RS256'}} as never);
  vi.spyOn(globalThis,'fetch').mockResolvedValue(Response.json({candidates:[{content:{parts:[{text:'I completed my work.'}]}}]}));
  const response=await worker.fetch(request({rawText:'Work notes'}),{GEMINI_API_KEY:'test-secret',AI_RATE_LIMITER:{limit:async()=>({success:true})}});
  expect(await response.json()).toEqual({success:true,formattedText:'I completed my work.'});
 });
 it('rejects excessive requests before calling Gemini',async()=>{
  vi.mocked(jwtVerify).mockResolvedValue({payload:{sub:'student',exp:Date.now()/1000+100,iat:Date.now()/1000},protectedHeader:{alg:'RS256'}} as never);
  const fetcher=vi.spyOn(globalThis,'fetch');
  const response=await worker.fetch(request({rawText:'notes'}),{GEMINI_API_KEY:'test-secret',AI_RATE_LIMITER:{limit:async()=>({success:false})}});
  expect(response.status).toBe(429);expect(fetcher).not.toHaveBeenCalled();
 });
 it('keeps API keys out of URLs and upstream errors out of responses',async()=>{
  vi.mocked(jwtVerify).mockResolvedValue({payload:{sub:'student',exp:Date.now()/1000+100,iat:Date.now()/1000},protectedHeader:{alg:'RS256'}} as never);
  const fetcher=vi.spyOn(globalThis,'fetch').mockResolvedValue(new Response('private diagnostic test-secret',{status:500}));
  const response=await worker.fetch(request({rawText:'notes'}),{GEMINI_API_KEY:'test-secret',AI_RATE_LIMITER:{limit:async()=>({success:true})}});
  expect(response.status).toBe(502);expect(await response.text()).not.toContain('private');
  expect(String(fetcher.mock.calls[0][0])).not.toContain('test-secret');
  expect(response.headers.get('cache-control')).toBe('no-store');
 });
});
