package org.exoplatform.portal.mop.storage.cache.model;

import java.util.Objects;

import org.exoplatform.portal.mop.State;
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;

import lombok.Data;

@Data
public class DescriptionCacheSelector implements CachedObjectSelector<DescriptionCacheKey, State> {

  private String id;

  public DescriptionCacheSelector(String id) {
    this.id = id;
  }

  @Override
  public boolean select(final DescriptionCacheKey cacheKey, final ObjectCacheInfo<? extends State> ocinfo) {
    return Objects.equals(id, cacheKey.getId());
  }

  @Override
  public void onSelect(ExoCache<? extends DescriptionCacheKey, ? extends State> cache,
                       DescriptionCacheKey key,
                       ObjectCacheInfo<? extends State> ocinfo) throws Exception {
    cache.remove(key);
  }

}
