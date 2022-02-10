/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.portal.mop.service;

import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeChangeNotifier;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeManager;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.storage.NavigationStorage;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NavigationServiceImpl implements NavigationService {

  private static final Log  LOG = ExoLogger.getLogger(NavigationServiceImpl.class);

  private final NodeManager nodeManager;

  final ListenerService     listenerService;

  final NavigationStorage   navigationStorage;

  public NavigationServiceImpl(ListenerService listenerService,
                               NavigationStorage navigationStorage) {
    this.listenerService = listenerService;
    this.navigationStorage = navigationStorage;
    this.nodeManager = new NodeManager(navigationStorage);
  }

  public NavigationContext loadNavigation(SiteKey key) {
    NavigationData navigationData = navigationStorage.loadNavigationData(key);
    return navigationData == null ? null : new NavigationContext(navigationData);
  }

  public void saveNavigation(NavigationContext navigation) {
    boolean created = loadNavigation(navigation.getKey()) == null;

    navigationStorage.saveNavigation(navigation.getKey(), navigation.getState());
    navigation.setData(navigationStorage.loadNavigationData(navigation.getKey()));
    navigation.setState(null);

    if (created) {
      notify(EventType.NAVIGATION_CREATED, navigation.getKey());
    } else {
      notify(EventType.NAVIGATION_UPDATED, navigation.getKey());
    }
  }

  public boolean destroyNavigation(NavigationContext navigation) {
    if (navigation == null) {
      throw new IllegalArgumentException("NavigationContext is mandatory");
    }
    if (navigation.getData() == null) {
      throw new IllegalArgumentException("NavigationContext.data removed");
    }

    notify(EventType.NAVIGATION_DESTROY, navigation.getKey());
    if (navigationStorage.destroyNavigation(navigation.getData())) {
      navigation.setData(null);
      notify(EventType.NAVIGATION_DESTROYED, navigation.getKey());
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean destroyNavigation(SiteKey siteKey) {
    NavigationContext navigationContext = loadNavigation(siteKey);
    if (navigationContext == null) {
      return false;
    } else {
      return destroyNavigation(navigationContext);
    }
  }

  public <N> NodeContext<N> loadNode(NodeModel<N> model, NavigationContext navigation, Scope scope,
                                     NodeChangeListener<NodeContext<N>> listener) {
    if (model == null) {
      throw new NullPointerException("No null model accepted");
    }
    if (navigation == null) {
      throw new NullPointerException("No null navigation accepted");
    }
    if (scope == null) {
      throw new NullPointerException("No null scope accepted");
    }
    String nodeId = navigation.getData().getRootId();
    if (nodeId != null) {
      return nodeManager.loadNode(model, nodeId, scope, new NodeChangeNotifier<>(listener, this, listenerService));
    } else {
      return null;
    }
  }

  @Override
  public <N> NodeContext<N> loadNodeById(NodeModel<N> model, String nodeId, Scope scope,
                                         NodeChangeListener<NodeContext<N>> listener) {
    if (model == null) {
      throw new NullPointerException("No null model accepted");
    }
    if (nodeId == null) {
      throw new NullPointerException("No null node id accepted");
    }
    if (scope == null) {
      throw new NullPointerException("No null scope accepted");
    }
    return nodeManager.loadNode(model, nodeId, scope, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  public <N> void updateNode(NodeContext<N> root, Scope scope, NodeChangeListener<NodeContext<N>> listener) {
    nodeManager.updateNode(root, scope, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  public <N> void saveNode(NodeContext<N> context, NodeChangeListener<NodeContext<N>> listener) {
    nodeManager.saveNode(context, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  public <N> void rebaseNode(NodeContext<N> context, Scope scope, NodeChangeListener<NodeContext<N>> listener) {
    nodeManager.rebaseNode(context, scope, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  private void notify(String name, SiteKey key) {
    try {
      listenerService.broadcast(name, this, key);
    } catch (Exception e) {
      LOG.error("Error when delivering notification " + name + " for navigation " + key, e);
    }
  }

}