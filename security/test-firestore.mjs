import assert from 'node:assert/strict';
const project = 'demo-onthejob';
const base = `http://127.0.0.1:8080/v1/projects/${project}/databases/(default)/documents`;
function token(uid) {
 const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
 return `${encode({alg:'none',typ:'JWT'})}.${encode({sub:uid,user_id:uid,aud:project,iss:`https://securetoken.google.com/${project}`,iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,firebase:{sign_in_provider:'custom'}})}.`;
}
async function call(path, uid, method='GET') {
 return fetch(`${base}/${path}`, {method, headers:{'Content-Type':'application/json',...(uid?{Authorization:`Bearer ${token(uid)}`}:{})}, ...(method==='PATCH'?{body:JSON.stringify({fields:{name:{stringValue:'Test'}}})}:{})});
}
for (const collection of ['entries','ojtInstances']) {
 const path=`users/alice/${collection}/test`;
 assert.equal((await call(path,'alice','PATCH')).status,200,'owner write');
 assert.equal((await call(path,'alice')).status,200,'owner read');
 for (const uid of [null,'bob']) {
  for (const method of ['GET','PATCH','DELETE']) assert.equal((await call(path,uid,method)).status,403,`${uid} ${method} denied`);
 }
 assert.equal((await call(path,'alice','DELETE')).status,200,'owner delete');
}
for (const path of ['users/alice','users/alice/private/test','unexpected/test'])
 assert.equal((await call(path,'alice','PATCH')).status,403,'unspecified path denied');
console.log('Firestore ownership checks passed');
