package org.exoplatform.portal.mop.navigation.cached;

import java.util.Objects;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;

import lombok.Data;

@Data
public class NavigationCacheSelector implements CachedObjectSelector<NavigationKey, NavigationData> {

  private SiteKey key;

  public NavigationCacheSelector(SiteKey key) {
    this.key = key;
  }

  @Override
  public boolean select(final NavigationKey navigationKey, final ObjectCacheInfo<? extends NavigationData> ocinfo) {
    return Objects.equals(key, navigationKey.getKey()) || Objects.equals(key, ocinfo.get().getSiteKey());
  }

  @Override
  public void onSelect(ExoCache<? extends NavigationKey, ? extends NavigationData> cache,
                       NavigationKey key,
                       ObjectCacheInfo<? extends NavigationData> ocinfo) throws Exception {
    cache.remove(key);
  }

}
