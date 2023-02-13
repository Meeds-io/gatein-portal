package org.exoplatform.portal.mop.cache.model;

import java.util.Objects;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;

import lombok.Data;

@Data
public class NavigationDataCacheSelector implements CachedObjectSelector<Long, NodeData> {

  private SiteKey key;

  private Long    nodeId;

  public NavigationDataCacheSelector(SiteKey key, Long nodeId) {
    this.key = key;
    this.nodeId = nodeId;
  }

  @Override
  public boolean select(final Long nodeKey, final ObjectCacheInfo<? extends NodeData> ocinfo) {
    return Objects.equals(nodeId, nodeKey)
        || Objects.equals(key, ocinfo.get().getState().getSiteKey())
        || Objects.equals(String.valueOf(nodeId), ocinfo.get().getParentId());
  }

  @Override
  public void onSelect(ExoCache<? extends Long, ? extends NodeData> cache,
                       Long key,
                       ObjectCacheInfo<? extends NodeData> ocinfo) throws Exception {
    cache.remove(key);
  }
}
