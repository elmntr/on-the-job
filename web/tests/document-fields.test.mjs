import test from 'node:test';
import assert from 'node:assert/strict';
import { documentFields } from '../lib/document-fields.ts';
test('entry write excludes Android-conflicting ID and client sync metadata', () => {
 const original={id:'document-id',pending:true,userId:'student',text:'My notes',rawText:'Original',hours:7.5,entryDate:'2026-09-09',ojtInstanceId:'placement',imageUrls:['https://example.com/attachment'],formattingStatus:'done',createdAt:{sentinel:true}};
 const stored=documentFields(original);
 assert.equal(Object.hasOwn(stored,'id'),false);
 assert.equal(Object.hasOwn(stored,'pending'),false);
 for(const key of ['userId','text','rawText','hours','entryDate','ojtInstanceId','imageUrls','formattingStatus','createdAt'])assert.deepEqual(stored[key],original[key]);
 assert.equal(original.id,'document-id');
});
test('placement writes omit document ID while preserving the hours goal', () => {
 assert.deepEqual(documentFields({id:'placement',name:'OJT 1',hoursRequired:486}),{name:'OJT 1',hoursRequired:486});
});
