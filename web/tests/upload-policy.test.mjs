import test from 'node:test';
import assert from 'node:assert/strict';
import {validatePhoto, validateDecodablePhoto, MAX_PHOTO_BYTES} from '../lib/upload-policy.ts';
const png = new Uint8Array([137,80,78,71,13,10,26,10]);
test('recognizes permitted raster signatures', async()=>{
 for(const [bytes,name,type,format] of [[png,'a.png','image/png','png'],[[255,216,255],'a.jpeg','image/jpeg','jpg'],[new TextEncoder().encode('RIFF0000WEBP'),'a.webp','image/webp','webp']]) assert.equal(await validatePhoto(new File([new Uint8Array(bytes)],name,{type})),format);
});
test('rejects disguised SVG, mismatched types and unsupported extensions', async()=>{
 for(const file of [new File(['<svg/>'],'a.jpg',{type:'image/jpeg'}),new File([png],'a.svg',{type:'image/png'}),new File([png],'a.jpg',{type:'image/jpeg'}),new File([png],'a.png',{type:'image/svg+xml'})]) await assert.rejects(validatePhoto(file));
});
test('rejects empty and oversized uploads',async()=>{
 await assert.rejects(validatePhoto(new Blob([])));
 await assert.rejects(validatePhoto(new Blob([new Uint8Array(MAX_PHOTO_BYTES+1)])));
});
test('requires successful decoding and caps pixel count',async()=>{
 const old=globalThis.createImageBitmap;
 try {
  globalThis.createImageBitmap=async()=>{throw new Error('bad image')};
  await assert.rejects(validateDecodablePhoto(new Blob([png],{type:'image/png'})),/damaged/);
  let closed=false;
  globalThis.createImageBitmap=async()=>({width:10000,height:10000,close(){closed=true}});
  await assert.rejects(validateDecodablePhoto(new Blob([png],{type:'image/png'})),/megapixels/);
  assert.equal(closed,true);
 } finally {globalThis.createImageBitmap=old;}
});
