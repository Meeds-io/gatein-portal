package org.exoplatform.portal.mop.navigation;

import java.io.Serializable;
import java.util.Objects;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.jdbc.dao.NavigationDAO;
import org.exoplatform.portal.mop.jdbc.dao.NodeDAO;
import org.exoplatform.portal.mop.jdbc.dao.PageDAO;
import org.exoplatform.portal.mop.jdbc.dao.SiteDAO;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class CachedNavigationStore extends NavigationStoreImpl {

  private static final Log                                            LOG                   =
                                                                          ExoLogger.getExoLogger(CachedNavigationStore.class);

  public static final String                                          NAVIGATION_CACHE_NAME = "portal.NavigationService";

  public static final String                                          NODE_CACHE_NAME       = "portal.NavigationNode";

  private final FutureExoCache<NavigationKey, NavigationData, Object> navigationFutureCache;

  private final ExoCache<NavigationKey, NavigationData>               navigationCache;

  private final FutureExoCache<Long, NodeData, Object>                nodeFutureCache;

  private final ExoCache<Long, NodeData>                              nodeCache;

  public CachedNavigationStore(CacheService cacheService,
                               NavigationDAO navigationDAO,
                               SiteDAO siteDAO,
                               NodeDAO nodeDAO,
                               PageDAO pageDAO,
                               DataStorage dataStorage) {
    super(navigationDAO, siteDAO, nodeDAO, pageDAO, dataStorage);
    this.navigationCache = cacheService.getCacheInstance(NAVIGATION_CACHE_NAME);
    this.navigationFutureCache = new FutureExoCache<>(new Loader<NavigationKey, NavigationData, Object>() {
      @Override
      public NavigationData retrieve(Object context, NavigationKey navigationKey) throws Exception {
        if (navigationKey.key == null) {
          return CachedNavigationStore.super.loadNavigationData(navigationKey.nodeId);
        } else {
          return CachedNavigationStore.super.loadNavigationData(navigationKey.key);
        }
      }
    }, navigationCache);

    this.nodeCache = cacheService.getCacheInstance(NODE_CACHE_NAME);
    this.nodeFutureCache = new FutureExoCache<>(new Loader<Long, NodeData, Object>() {
      @Override
      public NodeData retrieve(Object context, Long nodeId) throws Exception {
        return CachedNavigationStore.super.loadNode(nodeId);
      }
    }, nodeCache);
  }

  @Override
  public NodeData[] createNode(Long parentId, Long previousId, String name, NodeState state) {
    try {
      return super.createNode(parentId, previousId, name, state);
    } finally {
      this.nodeCache.remove(parentId);
    }
  }

  @Override
  public NodeData destroyNode(Long targetId) {
    NodeData nodeData = loadNode(targetId);
    try {
      return super.destroyNode(targetId);
    } finally {
      clearNodeCache(nodeData);
    }
  }

  @Override
  public NodeData loadNode(Long nodeId) {
    return nodeFutureCache.get(null, nodeId);
  }

  @Override
  public NodeData[] moveNode(Long targetId, Long fromId, Long toId, Long previousId) {
    NodeData nodeData = loadNode(targetId);
    NodeData fromNodeData = loadNode(fromId);
    NodeData toNodeData = loadNode(toId);
    try {
      return super.moveNode(targetId, fromId, toId, previousId);
    } finally {
      clearNodeCache(nodeData);
      clearNodeCache(fromNodeData);
      clearNodeCache(toNodeData);
    }
  }

  @Override
  public NodeData[] renameNode(Long targetId, Long parentId, String name) {
    NodeData nodeData = loadNode(targetId);
    NodeData parentNodeData = loadNode(parentId);
    try {
      return super.renameNode(targetId, parentId, name);
    } finally {
      clearNodeCache(nodeData);
      clearNodeCache(parentNodeData);
    }
  }

  @Override
  public NodeData updateNode(Long targetId, NodeState state) {
    NodeData nodeData = loadNode(targetId);
    try {
      return super.updateNode(targetId, state);
    } finally {
      clearNodeCache(nodeData);
    }
  }

  @Override
  public NavigationData loadNavigationData(Long nodeId) {
    return navigationFutureCache.get(null, new NavigationKey(null, nodeId));
  }

  @Override
  public NavigationData loadNavigationData(SiteKey key) {
    return navigationFutureCache.get(null, new NavigationKey(key, null));
  }

  @Override
  public void saveNavigation(SiteKey key, NavigationState state) {
    try {
      super.saveNavigation(key, state);
    } finally {
      clearNavigationByKey(key);
      clearNodeCache(key);
    }
  }

  @Override
  public boolean destroyNavigation(NavigationData data) {
    try {
      return super.destroyNavigation(data);
    } finally {
      SiteKey siteKey = data.getSiteKey();
      clearNavigationByKey(siteKey);
      clearNodeCache(siteKey);
      clearNodeCache(Long.parseLong(data.rootId));
    }
  }

  public void clearNavigationByKey(SiteKey siteKey) {
    if (siteKey != null) {
      try {
        this.navigationCache.select(new NavigationCacheSelector(siteKey));
        this.nodeCache.select(new NavigationDataCacheSelector(siteKey, null));
      } catch (Exception e) {
        LOG.error("Error clearing cache of navigation having site key {}", siteKey, e);
      }
    }
  }

  public void clearNodeCache(SiteKey siteKey) {
    try {
      this.nodeCache.select(new NavigationDataCacheSelector(siteKey, null));
      this.navigationCache.select(new NavigationCacheSelector(siteKey));
    } catch (Exception e) {
      LOG.error("Error clearing cache of nodes of site {}", siteKey, e);
    }
  }

  public void clearNodeCache(Long nodeId) {
    try {
      this.nodeCache.select(new NavigationDataCacheSelector(null, nodeId));
    } catch (Exception e) {
      LOG.error("Error clearing cache of node data with id", nodeId, e);
    }
  }

  public void clearNodeCache(NodeData nodeData) {
    if (nodeData != null) {
      try {
        SiteKey siteKey = nodeData.getState().getSiteKey();
        this.nodeCache.select(new NavigationDataCacheSelector(siteKey,
                                                              Long.parseLong(nodeData.getId())));
        if (nodeData.getParentId() != null) {
          this.nodeCache.select(new NavigationDataCacheSelector(null, Long.parseLong(nodeData.getParentId())));
        }
        this.navigationCache.select(new NavigationCacheSelector(siteKey));
      } catch (Exception e) {
        LOG.error("Error clearing cache of node data with id {} of page {}", nodeData.getId(), nodeData.getName(), e);
      }
    }
  }

  public static class NavigationCacheSelector implements CachedObjectSelector<NavigationKey, NavigationData> {

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

  public static class NavigationDataCacheSelector implements CachedObjectSelector<Long, NodeData> {

    private SiteKey key;

    private Long    nodeId;

    public NavigationDataCacheSelector(SiteKey key, Long nodeId) {
      this.key = key;
      this.nodeId = nodeId;
    }

    @Override
    public boolean select(final Long nodeKey, final ObjectCacheInfo<? extends NodeData> ocinfo) {
      return Objects.equals(nodeId, nodeKey) || Objects.equals(key, ocinfo.get().getParentId())
          || Objects.equals(key, ocinfo.get().getState().getSiteKey());
    }

    @Override
    public void onSelect(ExoCache<? extends Long, ? extends NodeData> cache,
                         Long key,
                         ObjectCacheInfo<? extends NodeData> ocinfo) throws Exception {
      cache.remove(key);
    }
  }

  public static class NavigationKey implements Serializable {

    private static final long serialVersionUID = 186446668859416892L;

    private SiteKey           key;

    private Long              nodeId;

    public NavigationKey(SiteKey key, Long nodeId) {
      this.key = key;
      this.nodeId = nodeId;
    }

    public SiteKey getKey() {
      return key;
    }

    public void setKey(SiteKey key) {
      this.key = key;
    }

    public Long getNodeId() {
      return nodeId;
    }

    public void setNodeId(Long nodeId) {
      this.nodeId = nodeId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, nodeId);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof NavigationKey)) {
        return false;
      }
      return Objects.equals(key, ((NavigationKey) obj).key)
          && Objects.equals(nodeId, ((NavigationKey) obj).nodeId);
    }
  }
}
