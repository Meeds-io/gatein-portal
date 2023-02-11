package org.exoplatform.portal.mop.navigation.cached;

import java.util.List;
import java.util.Objects;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.jdbc.dao.NavigationDAO;
import org.exoplatform.portal.mop.jdbc.dao.NodeDAO;
import org.exoplatform.portal.mop.jdbc.dao.PageDAO;
import org.exoplatform.portal.mop.jdbc.dao.SiteDAO;
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NavigationStoreImpl;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
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

  private final DataStorage                                           dataStorage;

  public CachedNavigationStore(CacheService cacheService,
                               DataStorage dataStorage,
                               NavigationDAO navigationDAO,
                               SiteDAO siteDAO,
                               NodeDAO nodeDAO,
                               PageDAO pageDAO) {
    super(navigationDAO, siteDAO, nodeDAO, pageDAO);
    this.dataStorage = dataStorage;
    this.navigationCache = cacheService.getCacheInstance(NAVIGATION_CACHE_NAME);
    this.navigationFutureCache = new FutureExoCache<>(new Loader<NavigationKey, NavigationData, Object>() {
      @Override
      public NavigationData retrieve(Object context, NavigationKey navigationKey) throws Exception {
        if (navigationKey.getKey() == null) {
          return CachedNavigationStore.super.loadNavigationData(navigationKey.getNodeId());
        } else {
          return CachedNavigationStore.super.loadNavigationData(navigationKey.getKey());
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
    NavigationData navigationData = navigationFutureCache.get(null, new NavigationKey(null, nodeId));
    return navigationData == null || navigationData.getSiteKey() == null ? null : navigationData;
  }

  @Override
  public NavigationData loadNavigationData(SiteKey key) {
    NavigationData navigationData = navigationFutureCache.get(null, new NavigationKey(key, null));
    return navigationData == null || navigationData.getSiteKey() == null ? null : navigationData;
  }

  @Override
  public List<NavigationData> loadNavigations(SiteType type, int offset, int limit) {
    List<String> portalNames = dataStorage.getSiteNames(type, offset, limit);
    return portalNames.stream()
                      .map(name -> loadNavigationData(type.key(name)))
                      .filter(Objects::nonNull)
                      .toList();
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
      clearNodeCache(Long.parseLong(data.getRootId()));
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

}
