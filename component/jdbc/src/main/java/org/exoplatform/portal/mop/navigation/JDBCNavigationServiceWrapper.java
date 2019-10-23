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
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class JDBCNavigationServiceWrapper implements NavigationService {

  private static final Logger log = LoggerFactory.getLogger(JDBCNavigationServiceWrapper.class);

  private final JDBCNavigationServiceImpl service;

  private final ListenerService listenerService;

  public JDBCNavigationServiceWrapper(NavigationStore store,
                                      ListenerService listenerService) {
    this.service = new JDBCNavigationServiceImpl(store);
    this.listenerService = listenerService;
  }

  public NavigationContext loadNavigation(SiteKey key) {
    return service.loadNavigation(key);
  }

  @Override
  public List<NavigationContext> loadNavigations(SiteType type) throws NullPointerException, NavigationServiceException {
    return service.loadNavigations(type);
  }

  public void saveNavigation(NavigationContext navigation) throws NullPointerException, NavigationServiceException {
    boolean created = service.loadNavigation(navigation.key) == null;
    service.saveNavigation(navigation);

    //
    if (created) {
      notify(EventType.NAVIGATION_CREATED, navigation.getKey());
    } else {
      notify(EventType.NAVIGATION_UPDATED, navigation.getKey());
    }
  }

  public boolean destroyNavigation(NavigationContext navigation) throws NullPointerException, NavigationServiceException {
    notify(EventType.NAVIGATION_DESTROY, navigation.getKey());
    boolean destroyed = service.destroyNavigation(navigation);

    //
    if (destroyed) {
      notify(EventType.NAVIGATION_DESTROYED, navigation.key);
    }

    //
    return destroyed;
  }

  public <N> NodeContext<N> loadNode(NodeModel<N> model,
                                     NavigationContext navigation,
                                     Scope scope,
                                     NodeChangeListener<NodeContext<N>> listener) {
    return service.loadNode(model, navigation, scope, listener);
  }

  public <N> void saveNode(NodeContext<N> context,
                           NodeChangeListener<NodeContext<N>> listener) throws NavigationServiceException {
    service.saveNode(context, new NodeChangeNotifier<>(listener, this, listenerService));
  }

  public <N> void updateNode(NodeContext<N> context,
                             Scope scope,
                             NodeChangeListener<NodeContext<N>> listener) throws NullPointerException,
          NavigationServiceException {
    service.updateNode(context, scope, listener);
  }

  public <N> void rebaseNode(NodeContext<N> context,
                             Scope scope,
                             NodeChangeListener<NodeContext<N>> listener) throws NullPointerException,
          NavigationServiceException {
    service.rebaseNode(context, scope, listener);
  }

  private void notify(String name, SiteKey key) {
    try {
      listenerService.broadcast(name, this, key);
    } catch (Exception e) {
      log.error("Error when delivering notification " + name + " for navigation " + key, e);
    }
  }

  public static class NodeChangeNotifier<N> implements NodeChangeListener<NodeContext<N>> {

    private NodeChangeListener<NodeContext<N>> listener;

    private ListenerService listenerService;

    private NavigationService navigationService;

    public NodeChangeNotifier(NodeChangeListener<NodeContext<N>> listener, NavigationService navigationService, ListenerService listenerService) {
      this.listener = listener;
      this.listenerService = listenerService;
      this.navigationService = navigationService;
    }

    @Override
    public void onAdd(NodeContext<N> target, NodeContext<N> parent, NodeContext<N> previous) {
      notifyNodeChange(EventType.NAVIGATION_NODE_ADD, target);
      if (listener != null) {
        listener.onAdd(target, parent, previous);
      }
    }

    @Override
    public void onCreate(NodeContext<N> target, NodeContext<N> parent, NodeContext<N> previous, String name) {
      notifyNodeChange(EventType.NAVIGATION_NODE_CREATE, target);
      if (listener != null) {
        listener.onCreate(target, parent, previous, name);
      }
    }

    @Override
    public void onRemove(NodeContext<N> target, NodeContext<N> parent) {
      notifyNodeChange(EventType.NAVIGATION_NODE_REMOVE, target);
      if (listener != null) {
        listener.onRemove(target, parent);
      }
    }

    @Override
    public void onDestroy(NodeContext<N> target, NodeContext<N> parent) {
      notifyNodeChange(EventType.NAVIGATION_NODE_DESTROY, target);
      if (listener != null) {
        listener.onDestroy(target, parent);
      }
    }

    @Override
    public void onRename(NodeContext<N> target, NodeContext<N> parent, String name) {
      notifyNodeChange(EventType.NAVIGATION_NODE_RENAME, target);
      if (listener != null) {
        listener.onRename(target, parent, name);
      }
    }

    @Override
    public void onUpdate(NodeContext<N> target, NodeState state) {
      notifyNodeChange(EventType.NAVIGATION_NODE_UPDATE, target);
      if (listener != null) {
        listener.onUpdate(target, state);
      }
    }

    @Override
    public void onMove(NodeContext<N> target, NodeContext<N> from, NodeContext<N> to, NodeContext<N> previous) {
      notifyNodeChange(EventType.NAVIGATION_NODE_MOVE, target);
      if (listener != null) {
        listener.onMove(target, from, to, previous);
      }
    }

    private void notifyNodeChange(String eventName, NodeContext<N> target) {
      try {
        log.debug("Broadcasting change type " + eventName + " notification for node " + target.getId()
                + " name " + target.getName());
        listenerService.broadcast(eventName, navigationService, target);
      } catch (Exception e) {
        log.error("Error when delivering notification " + eventName + " for node " + target.getId(), e);
      }
    }
  }
}
