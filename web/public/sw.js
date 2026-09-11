const CACHE='onthejob-shell-v2';
const SHELL=['/','/manifest.webmanifest','/icon-192.png','/icon-512.png'];
self.addEventListener('install',event=>{event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(SHELL)));});
self.addEventListener('activate',event=>{event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('onthejob-shell-')&&key!==CACHE).map(key=>caches.delete(key)))).then(()=>self.clients.claim()));});
self.addEventListener('fetch',event=>{
 const url=new URL(event.request.url);
 if(event.request.method!=='GET'||url.origin!==self.location.origin)return;
 // Never cache identity endpoints, personal data, RSC responses or development modules.
 const asset= !url.search && (SHELL.includes(url.pathname) || /^\/(?:_next\/static|assets|fonts)\/.+\.(?:js|css|ttf|woff2?|png)$/.test(url.pathname));
 if(event.request.mode==='navigate'&&url.pathname==='/'){
  event.respondWith(fetch(event.request).then(response=>{if(response.ok&&!response.redirected){const copy=response.clone();event.waitUntil(caches.open(CACHE).then(cache=>cache.put('/',copy)));}return response;}).catch(()=>caches.match('/')));return;
 }
 if(asset){event.respondWith(caches.match(event.request).then(cached=>cached||fetch(event.request).then(response=>{if(response.ok&&!response.redirected){const copy=response.clone();event.waitUntil(caches.open(CACHE).then(cache=>cache.put(event.request,copy)));}return response;})));}
});

self.addEventListener('message', event => {
 if(event.data?.type !== 'CACHE_SHELL' || !Array.isArray(event.data.urls)) return;
 const urls = event.data.urls.slice(0, 64).filter(value => {
  try {const url = new URL(value);return !url.search && !url.hash && url.origin === self.location.origin && (url.pathname.startsWith('/_next/static/') || url.pathname.startsWith('/assets/') || url.pathname.startsWith('/fonts/'));} catch {return false;}
 });
 event.waitUntil(caches.open(CACHE).then(cache => Promise.allSettled(urls.map(url => cache.add(url)))));
});
