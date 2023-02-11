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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class JDBCNavigationServiceImpl implements NavigationService {

  private static final Log  LOG = ExoLogger.getLogger(JDBCNavigationServiceImpl.class);

  private final NodeManager manager;

  final NavigationStore     store;

  public JDBCNavigationServiceImpl(NavigationStore store) throws NullPointerException {
    if (store == null) {
      throw new NullPointerException("No null persistence factory allowed");
    }

    //
    this.store = store;
    this.manager = new NodeManager(store);
  }

  public NavigationContext loadNavigation(SiteKey key) {
    //
    NavigationData data = store.loadNavigationData(key);
    return data != null && data != NavigationData.EMPTY ? new NavigationContext(data) : null;
  }

  @Override
  public List<NavigationContext> loadNavigations(SiteType type, int offset, int limit) {
    if (type == null) {
      throw new IllegalArgumentException("Site type is mandatory");
    }
    return store.loadNavigations(type, offset, limit)
                .stream()
                .map(NavigationContext::new)
                .toList();
  }

  @Override
  @Deprecated(forRemoval = true, since = "6.4.0")
  public List<NavigationContext> loadNavigations(SiteType type) {
    LOG.warn("Using a heavy method that will consume a lot of Memory, please take time to change it using pagination!");
    return loadNavigations(type, 0, 0);
  }

  public void saveNavigation(NavigationContext navigation) throws NullPointerException, NavigationServiceException {
    if (navigation == null) {
      throw new NullPointerException();
    }

    //
    // Save
    store.saveNavigation(navigation.key, navigation.getState());

    // Update state
    navigation.data = store.loadNavigationData(navigation.key);
    navigation.state = null;
  }

  public boolean destroyNavigation(NavigationContext navigation) throws NullPointerException, NavigationServiceException {
    if (navigation == null) {
      throw new NullPointerException("No null navigation argument");
    }
    if (navigation.data == null) {
      throw new IllegalArgumentException("Already removed");
    }

    if (store.destroyNavigation(navigation.data)) {
      navigation.data = null;
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
    String nodeId = navigation.data.rootId;
    if (navigation.data.rootId != null) {
      return manager.loadNode(model, nodeId, scope, listener);
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
    return manager.loadNode(model, nodeId, scope, listener);
  }

  public <N> void updateNode(NodeContext<N> root, Scope scope, NodeChangeListener<NodeContext<N>> listener)
                                                                                                            throws NullPointerException,
                                                                                                            IllegalArgumentException,
                                                                                                            NavigationServiceException {
    manager.updateNode(root, scope, listener);
  }

  public <N> void saveNode(NodeContext<N> context, NodeChangeListener<NodeContext<N>> listener) throws NullPointerException,
                                                                                                NavigationServiceException {
    manager.saveNode(context, listener);
  }

  public <N> void rebaseNode(NodeContext<N> context, Scope scope, NodeChangeListener<NodeContext<N>> listener)
                                                                                                               throws NavigationServiceException {
    manager.rebaseNode(context, scope, listener);
  }

}
