import test from 'node:test';
import assert from 'node:assert/strict';
import {photoPreviewUrl} from '../lib/photo-url.ts';
const root='https://res.cloudinary.com/dskoyv2oe/image/upload/';
test('shared SVG gets a PNG preview while the original URL remains intact',()=>{
 const original=root+'v1789003786/fsacqimotz7ehmpepvrw.svg';
 assert.equal(photoPreviewUrl(original),root+'f_png/v1789003786/fsacqimotz7ehmpepvrw.svg');
 assert.equal(original,root+'v1789003786/fsacqimotz7ehmpepvrw.svg');
});
test('camera formats get compatible previews and existing previews stay stable',()=>{
 for(const format of ['heic','heif','tif','tiff','avif'])assert.equal(photoPreviewUrl(root+'v1/photo.'+format),root+'f_png/v1/photo.'+format);
 assert.equal(photoPreviewUrl(root+'f_png/v1/photo.svg'),root+'f_png/v1/photo.svg');
});
test('ordinary photos remain unchanged',()=>{
 for(const url of [root+'v1/photo.jpg',root+'v1/photo.png'])assert.equal(photoPreviewUrl(url),url);
});

test('unsafe URLs cannot be rendered or opened',()=>{
 for(const url of ['javascript:alert(1)','data:image/svg+xml,<svg/>','https://example.com/image.svg','https://res.cloudinary.com/other/image/upload/a.jpg','https://res.cloudinary.com.evil.test/dskoyv2oe/image/upload/a.jpg','https://user:pass@res.cloudinary.com/dskoyv2oe/image/upload/a.jpg','invalid']) assert.equal(photoPreviewUrl(url),'');
});
