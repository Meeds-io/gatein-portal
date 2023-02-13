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

import java.util.List;

import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NavigationStorageWrapper implements NavigationService {

  private static final Log  LOG = ExoLogger.getLogger(NavigationStorageWrapper.class);

  private final NodeManager nodeManager;

  final ListenerService     listenerService;

  final NavigationStore     navigationStore;

  public NavigationStorageWrapper(ListenerService listenerService,
                                  NavigationStore navigationStore) {
    this.listenerService = listenerService;
    this.navigationStore = navigationStore;
    this.nodeManager = new NodeManager(navigationStore);
  }

  public NavigationContext loadNavigation(SiteKey key) {
    //
    NavigationData navigationData = navigationStore.loadNavigationData(key);
    return navigationData != null && navigationData != NavigationData.EMPTY ? new NavigationContext(navigationData) : null;
  }

  @Override
  public List<NavigationContext> loadNavigations(SiteType type, int offset, int limit) {
    if (type == null) {
      throw new IllegalArgumentException("Site type is mandatory");
    }
    return navigationStore.loadNavigations(type, offset, limit)
                          .stream()
                          .map(NavigationContext::new)
                          .toList();
  }

  public void saveNavigation(NavigationContext navigation) throws NullPointerException, NavigationServiceException {
    boolean created = loadNavigation(navigation.getKey()) == null;

    navigationStore.saveNavigation(navigation.getKey(), navigation.getState());
    navigation.setData(navigationStore.loadNavigationData(navigation.getKey()));
    navigation.setState(null);

    if (created) {
      notify(EventType.NAVIGATION_CREATED, navigation.getKey());
    } else {
      notify(EventType.NAVIGATION_UPDATED, navigation.getKey());
    }
  }

  public boolean destroyNavigation(NavigationContext navigation) throws NullPointerException, NavigationServiceException {
    if (navigation == null) {
      throw new IllegalArgumentException("NavigationContext is mandatory");
    }
    if (navigation.getData() == null) {
      throw new IllegalArgumentException("NavigationContext.data removed");
    }

    notify(EventType.NAVIGATION_DESTROY, navigation.getKey());
    if (navigationStore.destroyNavigation(navigation.getData())) {
      navigation.setData(null);
      notify(EventType.NAVIGATION_DESTROYED, navigation.getKey());
      return true;
    } else {
      return false;
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

  public <N> void updateNode(NodeContext<N> root, Scope scope, NodeChangeListener<NodeContext<N>> listener)
                                                                                                            throws NullPointerException,
                                                                                                            IllegalArgumentException,
                                                                                                            NavigationServiceException {
    nodeManager.updateNode(root, scope, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  public <N> void saveNode(NodeContext<N> context, NodeChangeListener<NodeContext<N>> listener) throws NullPointerException,
                                                                                                NavigationServiceException {
    nodeManager.saveNode(context, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  public <N> void rebaseNode(NodeContext<N> context, Scope scope, NodeChangeListener<NodeContext<N>> listener)
                                                                                                               throws NavigationServiceException {
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
