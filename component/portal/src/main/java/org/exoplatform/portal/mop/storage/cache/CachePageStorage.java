package org.exoplatform.portal.mop.storage.cache;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.portal.mop.dao.PageDAO;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.storage.LayoutStorage;
import org.exoplatform.portal.mop.storage.PageStorageImpl;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.listener.ListenerService;

public class CachePageStorage extends PageStorageImpl {

  public static final String                              PAGE_CACHE_NAME = "portal.PageService";

  private final FutureExoCache<PageKey, PageData, Object> pageFutureCache;

  public CachePageStorage(CacheService cacheService,
                          ListenerService listenerService,
                          LayoutStorage layoutStorage,
                          SiteDAO siteDAO,
                          PageDAO pageDAO) {
    super(listenerService, layoutStorage, siteDAO, pageDAO);
    this.pageFutureCache = new FutureExoCache<>(new Loader<PageKey, PageData, Object>() {
      @Override
      public PageData retrieve(Object context, PageKey pageKey) throws Exception {
        return CachePageStorage.super.getPage(pageKey.toPomPageKey());
      }
    }, cacheService.getCacheInstance(PAGE_CACHE_NAME));
  }

  @Override
  public boolean savePage(PageContext page) {
    try {
      return super.savePage(page);
    } finally {
      pageFutureCache.remove(page.getKey());
    }
  }

  @Override
  public boolean destroyPage(PageKey key) {
    try {
      return super.destroyPage(key);
    } finally {
      pageFutureCache.remove(key);
    }
  }

  @Override
  public void save(PageData page) {
    try {
      super.save(page);
    } finally {
      pageFutureCache.remove(page.getKey().toMopPageKey());
    }
  }

  @Override
  public PageData getPage(org.exoplatform.portal.pom.data.PageKey key) {
    return pageFutureCache.get(null, key.toMopPageKey());
  }

}
