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

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

public class JDBCNavigationServiceImpl implements NavigationService {

    private final NodeManager manager;

    final NavigationStore store;

    public JDBCNavigationServiceImpl(NavigationStore store) throws NullPointerException {
        if (store == null) {
            throw new NullPointerException("No null persistence factory allowed");
        }

        //
        this.store = store;
        this.manager = new NodeManager(store);
    }

    public NavigationContext loadNavigation(SiteKey key) {
        if (key == null) {
            throw new NullPointerException();
        }

        //
        NavigationData data = store.loadNavigationData(key);
        return data != null && data != NavigationData.EMPTY ? new NavigationContext(data) : null;
    }

    @Override
    public List<NavigationContext> loadNavigations(SiteType type) throws NullPointerException, NavigationServiceException {
        if (type == null) {
            throw new NullPointerException();
        }
        List<NavigationContext> navigations = new LinkedList<NavigationContext>();
        for (NavigationData data : store.loadNavigations(type)) {
            navigations.add(new NavigationContext(data));
        }
        return navigations;
    }

    public void saveNavigation(NavigationContext navigation) throws NullPointerException, NavigationServiceException {
        if (navigation == null) {
            throw new NullPointerException();
        }

        //
        try {
            // Save
            store.saveNavigation(navigation.key, navigation.state);

            // Update state
            navigation.data = store.loadNavigationData(navigation.key);
            navigation.state = null;
        } finally {
            store.flush();
        }
    }

    public boolean destroyNavigation(NavigationContext navigation) throws NullPointerException, NavigationServiceException {
        if (navigation == null) {
            throw new NullPointerException("No null navigation argument");
        }
        if (navigation.data == null) {
            throw new IllegalArgumentException("Already removed");
        }

        //
        try {
            if (store.destroyNavigation(navigation.data)) {
                navigation.data = null;
                return true;
            } else {
                return false;
            }
        } finally {
            store.flush();
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

    public <N> void updateNode(NodeContext<N> root, Scope scope, NodeChangeListener<NodeContext<N>> listener)
            throws NullPointerException, IllegalArgumentException, NavigationServiceException {
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

    public void clearCache() {
        store.clear();
    }
}
