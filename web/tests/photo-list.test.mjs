import test from 'node:test';
import assert from 'node:assert/strict';
import {visiblePhotoUrls} from '../lib/photo-list.ts';
test('photos arriving from Android appear even while an older draft is open',()=>{
 assert.deepEqual(visiblePhotoUrls(['web.jpg','android.jpg']),['web.jpg','android.jpg']);
});
test('only explicit removals are hidden; new uploads are preserved',()=>{
 assert.deepEqual(visiblePhotoUrls(['old.jpg','new.jpg'],['old.jpg']),['new.jpg']);
});
test('remote deletion cannot be resurrected by an older photo draft',()=>{
 assert.deepEqual(visiblePhotoUrls([],[]),[]);
});
