import {readFile,writeFile,readdir} from 'node:fs/promises';
import {createHash} from 'node:crypto';
const root=new URL('../dist/client/',import.meta.url);
const hashes=new Set();
const pages=[];
async function scan(directory){
 for(const item of await readdir(directory,{withFileTypes:true})){
  const url=new URL(item.name+(item.isDirectory()?'/':''),directory);
  if(item.isDirectory())await scan(url);
  else if(item.name.endsWith('.html')){
   const html=await readFile(url,'utf8');
   pages.push({url,html});
   for(const match of html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)){
    if(!/\bsrc\s*=/.test(match[1])&&match[2])hashes.add(`'sha256-${createHash('sha256').update(match[2]).digest('base64')}'`);
   }
  }
 }
}
await scan(root);
const csp=["default-src 'self'",`script-src 'self' https://apis.google.com ${[...hashes].join(' ')}`,"style-src 'self' 'unsafe-inline'","img-src 'self' https://res.cloudinary.com/dskoyv2oe/","font-src 'self'","connect-src 'self' https://*.googleapis.com https://on-the-job-19c0f.firebaseapp.com https://api.cloudinary.com https://onthejob-ai-proxy.elmntr.workers.dev","frame-src https://on-the-job-19c0f.firebaseapp.com https://accounts.google.com","object-src 'none'","base-uri 'self'","form-action 'self'","frame-ancestors 'self' https://chatgpt.com https://*.chatgpt.com","worker-src 'self'"].join('; ');
// A meta policy also protects cached/offline pages. Keep frame-ancestors in HTTP
// headers because browsers do not support that directive in meta policies.
const metaPolicy=csp.replace(/; frame-ancestors[^;]*/, '');
for(const {url,html} of pages) await writeFile(url,html.replace('<head>', '<head><meta http-equiv="Content-Security-Policy" content="'+metaPolicy.replaceAll('&','&amp;').replaceAll('"','&quot;')+'">'));
const headers={'Content-Security-Policy':"frame-ancestors 'self' https://chatgpt.com https://*.chatgpt.com; object-src 'none'; base-uri 'self'",'X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer','Permissions-Policy':'camera=(), microphone=(), geolocation=()'};
const existing=await readFile(new URL('_headers',root),'utf8').catch(()=>'');
await writeFile(new URL('_headers',root),existing+'\n/*\n'+Object.entries(headers).map(([k,v])=>`  ${k}: ${v}`).join('\n')+'\n/sw.js\n  Cache-Control: no-cache\n/index.html\n  Cache-Control: no-cache\n');
const configUrl=new URL('../firebase.json',import.meta.url);
const config=JSON.parse(await readFile(configUrl,'utf8'));
config.hosting.headers=config.hosting.headers.filter(rule=>rule.source!=='**');
config.hosting.headers.push({source:'**',headers:Object.entries(headers).map(([key,value])=>({key,value}))});
await writeFile(configUrl,JSON.stringify(config,null,2)+'\n');
console.log('Security headers generated with hashes for '+hashes.size+' inline scripts.');
