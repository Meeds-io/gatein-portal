importScripts('/eXoResources/javascript/workbox-5.1.4/workbox-sw.js');

const cachePrefix = 'meeds-pwa-resources';

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
    new RegExp('.*/$'),
    new workbox.strategies.CacheFirst({
      cacheName: `${cachePrefix}-dom`,
    }),
);

workbox.routing.registerRoute(
    new RegExp('.*/portal$'),
    new workbox.strategies.CacheFirst({
      cacheName: `${cachePrefix}-dom`,
    }),
);

workbox.routing.registerRoute(
    new RegExp('.*/portal/dw.*'),
    new workbox.strategies.NetworkFirst({
      cacheName: `${cachePrefix}-dom`,
    }),
);

workbox.routing.registerRoute(
  new RegExp('.*/rest/v1/platform/branding/css'),
  new workbox.strategies.NetworkFirst({
    cacheName: `${cachePrefix}-css`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*/rest/v1/navigations/.*'),
  new workbox.strategies.NetworkFirst({
    cacheName: `${cachePrefix}-navigations`,
  }),
);

workbox.routing.registerRoute(
  new RegExp('.*/rest/.*'),
  new workbox.strategies.NetworkFirst({
    cacheName: `${cachePrefix}-rest`,
  }),
);

workbox.core.skipWaiting();
workbox.core.clientsClaim();
