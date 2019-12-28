/*
 * Copyright (C) 2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.mop.navigation;

import java.util.*;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.jdbc.dao.*;
import org.exoplatform.portal.mop.jdbc.entity.*;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NavigationStoreImpl implements NavigationStore {

  private NavigationDAO navigationDAO;

  private SiteDAO       siteDAO;

  private NodeDAO       nodeDAO;

  private PageDAO       pageDAO;

  private DataStorage   dataStorage;

  private static Log    log = ExoLogger.getExoLogger(NavigationStoreImpl.class);

  public NavigationStoreImpl(NavigationDAO navigationDAO, SiteDAO siteDAO, NodeDAO nodeDAO, PageDAO pageDAO, DataStorage dataStorage) {
    this.navigationDAO = navigationDAO;
    this.siteDAO = siteDAO;
    this.nodeDAO = nodeDAO;
    this.pageDAO = pageDAO;
    this.dataStorage = dataStorage;
  }

  @Override
  public NodeData loadNode(Long nodeId) {
    NodeEntity node = nodeDAO.find(nodeId);
    return buildNodeData(node);
  }

  @Override
  public NodeData[] loadNodes(String pageRef) {
    PageEntity page = this.pageDAO.findByKey(PageKey.parse(pageRef));
    if (page == null) {
      return new NodeData[0];
    }
    List<NodeEntity> nodes = nodeDAO.findAllByPage(page.getId());
    NodeData[] result = new NodeData[nodes.size()];
    for (int i = 0; i < nodes.size(); i++) {
      result[i] = buildNodeData(nodes.get(i));
    }
    return result;
  }

  @Override
  public NodeData[] createNode(Long parentId, Long previousId, String name, NodeState state) {
    NodeEntity parent = null;
    if (parentId != null) {
      parent = nodeDAO.find(parentId);
    }
    Long prev = null;
    if (previousId != null) {
      prev = previousId;
    }

    NodeEntity target = buildNodeEntity(null, state);
    target.setName(name);
    target.setParent(parent);

    if (parent != null) {
      List<NodeEntity> children = parent.getChildren();
      int i;
      for (i = 0; i < children.size(); i++) {
        if (children.get(i).getId().equals(prev)) {
          i += 1;
          break;
        }
      }
      children.add(i, target);
      parent.setChildren(children);
      nodeDAO.create(target);
      nodeDAO.update(parent);
    } else {
      nodeDAO.create(target);
    }
    return new NodeData[] { buildNodeData(parent), buildNodeData(target) };
  }

  @Override
  public NodeData destroyNode(Long targetId) {
    NodeEntity node = nodeDAO.find(targetId);
    if (node != null) {
      NodeEntity parent = node.getParent();
      if (parent != null) {
        Iterator<NodeEntity> children = parent.getChildren().iterator();
        while (children.hasNext()) {
          if (children.next().getId().equals(node.getId())) {
            children.remove();
            break;
          }
        }
      }
      nodeDAO.delete(node);
      return buildNodeData(parent);
    } else {
      return null;
    }
  }

  @Override
  public NodeData updateNode(Long targetId, NodeState state) {
    NodeEntity node = nodeDAO.find(targetId);
    if (node != null) {
      node = buildNodeEntity(node, state);
      nodeDAO.update(node);
      return buildNodeData(node);
    } else {
      return null;
    }
  }

  @Override
  public NodeData[] moveNode(Long targetId, Long fromId, Long toId, Long previousId) {
    NodeEntity target = nodeDAO.find(targetId);
    if (target == null) {
      return null;
    }

    NodeEntity from = null;
    if (fromId != null) {
      from = nodeDAO.find(fromId);
      if (from != null) {
        List<NodeEntity> children = from.getChildren();
        children.remove(target);
      }
    }

    int index = -1;
    NodeEntity to = null;
    if (toId != null) {
      to = nodeDAO.find(toId);

      List<NodeEntity> children = to.getChildren();
      if (children != null && previousId != null) {
        Long prev = previousId;
        for (index = 0; index < children.size(); index++) {
          if (children.get(index).getId().equals(prev)) {
            break;
          }
        }
      }
    }

    target.setParent(to);
    if (to != null) {
      List<NodeEntity> children = to.getChildren();
      children.add(index + 1, target);
      to.setChildren(children);
      to = nodeDAO.update(to);
    }
    target = nodeDAO.update(target);

    if (from != null && fromId != toId) {
      from = nodeDAO.update(from);
    }
    return new NodeData[] { buildNodeData(target), buildNodeData(from), buildNodeData(to) };
  }

  @Override
  public NodeData[] renameNode(Long targetId, Long parentId, String name) {
    NodeEntity target = nodeDAO.find(targetId);
    if (target == null) {
      return null;
    }
    NodeEntity parent = null;
    if (parentId != null) {
      parent = nodeDAO.find(parentId);
    }

    target.setName(name);
    nodeDAO.update(target);

    return new NodeData[] { buildNodeData(target), buildNodeData(parent) };
  }

  @Override
  public void flush() {

  }

  @Override
  public List<NavigationData> loadNavigations(SiteType type) {
    List<NavigationData> results = new LinkedList<NavigationData>();

    Query<PortalConfig> q = new Query<PortalConfig>(type.getName(), null, PortalConfig.class);
    try {
      LazyPageList<PortalConfig> configs = dataStorage.find(q);
      for (PortalConfig config : configs.getAll()) {
        SiteKey siteKey = new SiteKey(config.getType(), config.getName());
        NavigationData navData = loadNavigationData(siteKey);
        if (navData != NavigationData.EMPTY) {
          results.add(navData);
        }
      }
    } catch (Exception e) {
      log.error(e);
    }

    return results;
  }

  @Override
  public NavigationData loadNavigationData(SiteKey key) {
    NavigationEntity navEntity = navigationDAO.findByOwner(key.getType(), key.getName());
    if (navEntity != null) {
      NavigationState navigationState = new NavigationState(navEntity.getPriority());
      return new NavigationData(key, navigationState, String.valueOf(navEntity.getRootNode().getId()));
    } else {
      return NavigationData.EMPTY;
    }
  }

  @Override
  public NavigationData loadNavigationData(Long nodeId) {
    NodeData root = this.buildNodeData(this.getRootNode(nodeId));
    NavigationEntity navEntity = navigationDAO.findByRootNode(Safe.parseLong(root.getId()));
    if (navEntity != null) {
      SiteEntity siteEntity = navEntity.getOwner();
      SiteKey key = siteEntity.getSiteType().key(siteEntity.getName());
      NavigationState navigationState = new NavigationState(navEntity.getPriority());
      return new NavigationData(key, navigationState, String.valueOf(navEntity.getRootNode().getId()));
    }

    return NavigationData.EMPTY;
  }

  private NodeEntity getRootNode(Long nodeId) {
    NodeEntity entity = this.nodeDAO.find(nodeId);
    if (entity.getParent() != null) {
      return this.getRootNode(entity.getParent().getId());
    }
    return entity;
  }

  @Override
  public void saveNavigation(SiteKey key, NavigationState state) {
    SiteEntity owner = siteDAO.findByKey(key);
    if (owner == null) {
      throw new NavigationServiceException(NavigationError.NAVIGATION_NO_SITE);
    }
    
    NavigationEntity navEntity = navigationDAO.findByOwner(key.getType(), key.getName());
    navEntity = buildNavEntity(navEntity, key, state.getPriority());
    if (navEntity.getId() == null) {
      nodeDAO.create(navEntity.getRootNode());
      navigationDAO.create(navEntity);
    } else {
      navigationDAO.update(navEntity);
    }
  }

  @Override
  public boolean destroyNavigation(NavigationData data) {
    SiteKey siteKey = data.key;
    SiteEntity owner = siteDAO.findByKey(siteKey);
    if (owner == null) {
      throw new NavigationServiceException(NavigationError.NAVIGATION_NO_SITE);
    }
    
    NavigationEntity navEntity = navigationDAO.findByOwner(siteKey.getType(), siteKey.getName());
    if (navEntity != null) {
      navigationDAO.delete(navEntity);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void clear() {

  }

  private NavigationEntity buildNavEntity(NavigationEntity entity, SiteKey key, Integer priority) {
    if (entity == null) {
      entity = new NavigationEntity();
      NodeEntity rootNode = new NodeEntity();
      //rootNode.setId(UUID.randomUUID().toString());
      rootNode.setName("default");
      entity.setRootNode(rootNode);

    }
    entity.setPriority(priority == null ? 0 : priority);
    entity.setOwner(siteDAO.findByKey(key));
    return entity;
  }

  private NodeEntity buildNodeEntity(NodeEntity entity, NodeState state) {
    if (entity == null) {
      entity = new NodeEntity();
      //entity.setId(UUID.randomUUID().toString());
    }
    if (state == null) {
      return entity;
    }
    entity.setEndTime(state.getEndPublicationTime());
    entity.setIcon(state.getIcon());
    entity.setLabel(state.getLabel());
    if (state.getPageRef() != null) {
      PageEntity page = pageDAO.findByKey(state.getPageRef());
      if (page != null) {
        entity.setPage(page);
      }
    } else {
      entity.setPage(null);
    }
    entity.setStartTime(state.getStartPublicationTime());
    entity.setVisibility(state.getVisibility());
    return entity;
  }

  private NodeData buildNodeData(NodeEntity node) {
    if (node == null) {
      return null;
    }

    String parentId = null;
    if (node.getParent() != null) {
      parentId = node.getParent().getId().toString();
    }

    List<String> children = new ArrayList<>();
    if (node.getChildren() != null) {
      for (NodeEntity child : node.getChildren()) {
        children.add(child.getId().toString());
      }
    }

    NodeState.Builder builder = new NodeState.Builder();
    builder.endPublicationTime(node.getEndTime())
           .icon(node.getIcon())
           .label(node.getLabel())
           .startPublicationTime(node.getStartTime())
           .visibility(node.getVisibility());
    PageEntity page = node.getPage();
    if (page != null) {
      SiteKey siteKey = new SiteKey(page.getOwnerType(), page.getOwnerId());
      PageKey pageKey = new PageKey(siteKey, page.getName());
      builder.pageRef(pageKey);
    }

    NodeState state = builder.build();

    return new NodeData(parentId, node.getId().toString(), node.getName(), state, children.toArray(new String[children.size()]));
  }
}
