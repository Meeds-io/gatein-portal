package org.exoplatform.portal.mop.page;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.portal.pom.config.POMSession;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ExoDataCache extends DataCache {

    private final static String CACHE_NAME = "portal.PageService";

    /** . */
    protected ExoCache<PageKey, PageData> cache;

    /** . */
    protected FutureExoCache<PageKey, PageData, POMSession> objects;

    /** . */
    private Loader<PageKey, PageData, POMSession> pageLoader = new Loader<PageKey, PageData, POMSession>() {
        public PageData retrieve(POMSession session, PageKey key) throws Exception {
            PageData data = loadPage(session, key);
            return data == PageData.EMPTY ? null : data;
        }
    };

    public ExoDataCache(CacheService cacheService) {
        this.cache = cacheService.getCacheInstance(CACHE_NAME);
        this.objects = new FutureExoCache<PageKey, PageData, POMSession>(pageLoader, cache);
    }

    @Override
    protected PageData getPage(POMSession session, PageKey key) {
        return objects.get(session, key);
    }

    @Override
    protected void removePage(POMSession session, PageKey key) {
        cache.remove(key);
    }

    @Override
    protected void putPage(PageData data) {
        cache.put(data.key, data);
    }

    @Override
    protected void clear() {
        cache.clearCache();
    }
}
