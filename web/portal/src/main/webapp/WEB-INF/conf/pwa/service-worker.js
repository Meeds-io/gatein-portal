importScripts('/eXoResources/javascript/workbox-5.1.4/workbox-sw.js');

const cachePrefix = 'pwa-resources';

workbox.setConfig({
  debug: true,
  modulePathPrefix: '/eXoResources/javascript/workbox-5.1.4/'
});

workbox.core.setCacheNameDetails({prefix: cachePrefix});

workbox.loadModule('workbox-strategies');

workbox.routing.registerRoute(
  new RegExp('.*manifest.json($|\\?.*|\\#.*)'),
  new workbox.strategies.CacheFirst({
    cacheName: `${cachePrefix}-settings`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*\\.(ttf|woff|woff2|otf|otf|ttc)($|\\?.*|\\#.*)'),
  new workbox.strategies.CacheFirst({
    cacheName: `${cachePrefix}-font`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*\\.css($|\\?.*|\\#.*)'),
  new workbox.strategies.CacheFirst({
    cacheName: `${cachePrefix}-css`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*\\.js($|\\?.*|\\#.*)'),
  new workbox.strategies.CacheFirst({
    cacheName: `${cachePrefix}-js`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*\\.(?:png|jpg|jpeg|svg|gif|ico)'),
  new workbox.strategies.CacheFirst({
    cacheName: `${cachePrefix}-image`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*/i18n/bundle/.*\\.json'),
  new workbox.strategies.CacheFirst({
    cacheName: `${cachePrefix}-bundle`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*/rest/.*/social/.*/avatar.*'),
  new workbox.strategies.CacheFirst({
    cacheName: `${cachePrefix}-image`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*/rest/.*/social/.*/banner.*'),
  new workbox.strategies.CacheFirst({
    cacheName: `${cachePrefix}-image`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*/rest/v1/platform/branding/css.*'),
  new workbox.strategies.CacheFirst({
    cacheName: `${cachePrefix}-css`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*/dom-cache.*'),
  new workbox.strategies.CacheOnly({
    cacheName: `${cachePrefix}-dom`,
  }),
);

const cacheableDOM = new workbox.cacheableResponse.CacheableResponse({
  statuses: [200],
  headers: {
    'Content-Type': ['text/html;charset=UTF-8', 'text/html'],
  },
});

const handleDOMResponse = (event) => {
  return fetch(event.request)
    .then((response) => {
      if (cacheable.isResponseCacheable(response)) {
        return response.text()
          .then(html => {
          });
      }
      return response;
    });
};

const domMatcher = ({url, request, event}) => {
  const pathname = url.pathname;
  return (pathname.indexOf('/dw') > 0 || pathname.indexOf('/g:') > 0)
         && pathname.indexOf('/rest/') < 0
         && pathname.indexOf('.js') < 0
         && pathname.indexOf('.css') < 0;
};

const domHandler = async ({url, request, event, params}) => {
  const response = await fetch(request);
  let html = await response.text();

  const cacheableDOMs = [...html.matchAll(/<v-cacheable-dom-app([ \t\r\n]*)cache-id="(.*)"([ \t\r\n]*)(\/>|>[ \t\r\n]*<\/v-cacheable-dom-app>)/g)];
  if(cacheableDOMs.length) {
    const domCache = await self.caches.open(`${cachePrefix}-dom`);
    for (let index in cacheableDOMs) {
      const cacheableDOM = cacheableDOMs[index];
      const domToReplace = cacheableDOM[0];
      const appId = cacheableDOM[2];
      const domCacheEntry = await domCache.match(`/dom-cache?id=${appId}`);
      if (domCacheEntry) {
        const htmlAppPart = await domCacheEntry.text();
        html = html.replace(domToReplace, htmlAppPart);
      }
    }
  }
  return new Response(html, {
    headers: {'content-type': 'text/html'},
  });
};

workbox.routing.registerRoute(domMatcher, domHandler);

workbox.core.skipWaiting();
workbox.core.clientsClaim();
