package org.exoplatform.portal.mop.storage.cache.model;

import java.util.Objects;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;

import lombok.Data;

@Data
public class NavigationCacheSelector implements CachedObjectSelector<SiteKey, NavigationData> {

  private SiteKey key;

  public NavigationCacheSelector(SiteKey key) {
    this.key = key;
  }

  @Override
  public boolean select(final SiteKey siteKey, final ObjectCacheInfo<? extends NavigationData> ocinfo) {
    return Objects.equals(key, siteKey) || Objects.equals(key, ocinfo.get().getSiteKey());
  }

  @Override
  public void onSelect(ExoCache<? extends SiteKey, ? extends NavigationData> cache,
                       SiteKey key,
                       ObjectCacheInfo<? extends NavigationData> ocinfo) throws Exception {
    cache.remove(key);
  }

}
