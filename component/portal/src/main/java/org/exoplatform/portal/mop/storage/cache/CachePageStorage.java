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
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.ListenerService;

public class CachePageStorage extends PageStorageImpl {

  public static final String                              PAGE_CACHE_NAME     = "portal.PageService";

  public static final String                              PAGE_KEY_CACHE_NAME = "portal.PageKeyById";

  private final FutureExoCache<PageKey, PageData, Object> pageFutureCache;

  private final ExoCache<Long, PageKey>                   pageKeyByIdCache;

  public CachePageStorage(CacheService cacheService,
                          ListenerService listenerService,
                          LayoutStorage layoutStorage,
                          SiteDAO siteDAO,
                          PageDAO pageDAO) {
    super(listenerService, layoutStorage, siteDAO, pageDAO);
    this.pageFutureCache = new FutureExoCache<>(new Loader<PageKey, PageData, Object>() {
      @Override
      public PageData retrieve(Object context, PageKey pageKey) throws Exception {
        PageData pageData = CachePageStorage.super.getPage(pageKey.toPomPageKey());
        return pageData == null ? PageData.NULL_OBJECT : pageData;
      }
    }, cacheService.getCacheInstance(PAGE_CACHE_NAME));
    pageKeyByIdCache = cacheService.getCacheInstance(PAGE_KEY_CACHE_NAME);
  }

  @Override
  public PageContext clone(PageKey srcPageKey, PageKey dstPageKey) {
    try {
      return super.clone(srcPageKey, dstPageKey);
    } finally {
      pageFutureCache.remove(srcPageKey);
      pageFutureCache.remove(dstPageKey);
    }
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
  public void save(PageData page) {
    try {
      super.save(page);
    } finally {
      pageFutureCache.remove(page.getKey().toMopPageKey());
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
  public PageData getPage(org.exoplatform.portal.pom.data.PageKey key) {
    PageData pageData = pageFutureCache.get(null, key.toMopPageKey());
    return pageData == null || pageData.isNull() ? null : pageData;
  }

  @Override
  protected PageKey getPageKey(long id) {
    PageKey pageKey = pageKeyByIdCache.get(id);
    if (pageKey == null) {
      pageKey = super.getPageKey(id);
      pageKeyByIdCache.put(id, pageKey);
    }
    return pageKey;
  }

}
