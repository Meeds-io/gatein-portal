/*
 * Copyright (C) 2019 eXo Platform SAS.
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

package org.exoplatform.portal.jdbc.service;

import java.util.Iterator;
import java.util.List;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.mop.Described;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.AbstractTestNavigationService;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.Node;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.navigation.TestNavigationService;
import org.exoplatform.portal.mop.navigation.VisitMode;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.gatein.mop.api.workspace.Navigation;
import org.gatein.mop.api.workspace.ObjectType;
import org.gatein.mop.api.workspace.Site;
import org.gatein.mop.core.api.MOPService;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test.navigation.configuration.xml") })
public class TestJDBCNavigationService extends TestNavigationService {

    protected void setUp() throws Exception {
        super.setUp();
        PortalContainer container = PortalContainer.getInstance();
        this.service = (NavigationService) container.getComponentInstanceOfType(NavigationService.class);
    }

    public void testHiddenNode() throws Exception {
//        MOPService mop = mgr.getPOMService();
//        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "hidden_node");
//        Navigation defaultNav = portal.getRootNavigation().addChild("default");
//        defaultNav.addChild("a");
//        defaultNav.addChild("b");
//        defaultNav.addChild("c");
//
//        //
//        sync(true);
//
//        //
//        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_node"));
//
//        //
//        Node root;
//        Node a;
//        Node b;
//        Node c;
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.GRANDCHILDREN, null).getNode();
//        a = root.getChild("a");
//        b = root.getChild("b");
//        a.setHidden(true);
//        assertEquals(2, root.getChildren().size());
//        assertNull(root.getChild("a"));
//        assertEquals("b", root.getChild(0).getName());
//        try {
//            root.getChild(2);
//            fail();
//        } catch (IndexOutOfBoundsException ignore) {
//        }
//        assertFalse(root.removeChild("a"));
//        try {
//            b.setName("a");
//            fail();
//        } catch (IllegalArgumentException ignore) {
//        }
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        a = root.getChild("a");
//        b = root.getChild("b");
//        c = root.getChild("c");
//        b.setHidden(true);
//        assertSame(a, root.getChild(0));
//        assertSame(c, root.getChild(1));
//        try {
//            root.getChild(2);
//            fail();
//        } catch (IndexOutOfBoundsException e) {
//        }
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        a = root.getChild("a");
//        b = root.getChild("b");
//        c = root.getChild("c");
//        a.setHidden(true);
//        c.setHidden(true);
//        assertSame(b, root.getChild(0));
//        try {
//            root.getChild(1);
//            fail();
//        } catch (IndexOutOfBoundsException e) {
//        }
    }

    public void testHiddenInsert1() throws Exception {
//        MOPService mop = mgr.getPOMService();
//        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "hidden_insert_1");
//        Navigation defaultNav = portal.getRootNavigation().addChild("default");
//        defaultNav.addChild("a");
//
//        //
//        sync(true);
//
//        //
//        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_insert_1"));
//
//        //
//        Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        Node a = root.getChild("a");
//        a.setHidden(true);
//        Node b = root.addChild("b");
//        assertEquals(1, root.getChildren().size());
//        assertSame(b, root.getChildren().iterator().next());
//        a.setHidden(false);
//        assertEquals(2, root.getChildren().size());
//        Iterator<Node> it = root.getChildren().iterator();
//        assertSame(b, it.next());
//        assertSame(a, it.next());
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        a = root.getChild("a");
//        a.setHidden(true);
//        b = root.addChild(0, "b");
//        assertEquals(1, root.getChildren().size());
//        assertSame(b, root.getChildren().iterator().next());
//        a.setHidden(false);
//        assertEquals(2, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(b, it.next());
//        assertSame(a, it.next());
    }

    public void testHiddenInsert2() throws Exception {
//        MOPService mop = mgr.getPOMService();
//        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "hidden_insert_2");
//        Navigation defaultNav = portal.getRootNavigation().addChild("default");
//        defaultNav.addChild("a");
//        defaultNav.addChild("b");
//
//        //
//        sync(true);
//
//        //
//        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_insert_2"));
//
//        //
//        Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        Node a = root.getChild("a");
//        Node b = root.getChild("b");
//        b.setHidden(true);
//        Node c = root.addChild(0, "c");
//        assertEquals(2, root.getChildren().size());
//        Iterator<Node> it = root.getChildren().iterator();
//        assertSame(c, it.next());
//        assertSame(a, it.next());
//        b.setHidden(false);
//        assertEquals(3, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(c, it.next());
//        assertSame(a, it.next());
//        assertSame(b, it.next());
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        a = root.getChild("a");
//        b = root.getChild("b");
//        b.setHidden(true);
//        c = root.addChild(1, "c");
//        assertEquals(2, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(c, it.next());
//        b.setHidden(false);
//        assertEquals(3, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(c, it.next());
//        assertSame(b, it.next());
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        a = root.getChild("a");
//        b = root.getChild("b");
//        b.setHidden(true);
//        c = root.addChild("c");
//        assertEquals(2, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(c, it.next());
//        b.setHidden(false);
//        assertEquals(3, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(c, it.next());
//        assertSame(b, it.next());
    }

    public void testHiddenInsert3() throws Exception {
//        MOPService mop = mgr.getPOMService();
//        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "hidden_insert_3");
//        Navigation defaultNav = portal.getRootNavigation().addChild("default");
//        defaultNav.addChild("a");
//        defaultNav.addChild("b");
//        defaultNav.addChild("c");
//
//        //
//        sync(true);
//
//        //
//        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_insert_3"));
//
//        //
//        Node root, a, b, c, d;
//        Iterator<Node> it;
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        a = root.getChild("a");
//        b = root.getChild("b");
//        c = root.getChild("c");
//        b.setHidden(true);
//        d = root.addChild(0, "d");
//        assertEquals(3, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(d, it.next());
//        assertSame(a, it.next());
//        assertSame(c, it.next());
//        b.setHidden(false);
//        assertEquals(4, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(d, it.next());
//        assertSame(a, it.next());
//        assertSame(b, it.next());
//        assertSame(c, it.next());
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        a = root.getChild("a");
//        b = root.getChild("b");
//        c = root.getChild("c");
//        b.setHidden(true);
//        d = root.addChild(1, "d");
//        assertEquals(3, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(d, it.next());
//        assertSame(c, it.next());
//        b.setHidden(false);
//        assertEquals(4, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(d, it.next());
//        assertSame(b, it.next());
//        assertSame(c, it.next());
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        a = root.getChild("a");
//        b = root.getChild("b");
//        c = root.getChild("c");
//        b.setHidden(true);
//        d = root.addChild(2, "d");
//        assertEquals(3, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(c, it.next());
//        assertSame(d, it.next());
//        b.setHidden(false);
//        assertEquals(4, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(b, it.next());
//        assertSame(c, it.next());
//        assertSame(d, it.next());
//
//        //
//        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//        a = root.getChild("a");
//        b = root.getChild("b");
//        c = root.getChild("c");
//        b.setHidden(true);
//        d = root.addChild("d");
//        assertEquals(3, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(c, it.next());
//        assertSame(d, it.next());
//        b.setHidden(false);
//        assertEquals(4, root.getChildren().size());
//        it = root.getChildren().iterator();
//        assertSame(a, it.next());
//        assertSame(b, it.next());
//        assertSame(c, it.next());
//        assertSame(d, it.next());
    }

    public void testCount() {
//        MOPService mop = mgr.getPOMService();
//        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "count");
//        portal.getRootNavigation().addChild("default");
//
//        //
//        sync(true);
//
//        //
//        NavigationContext navigation = service.loadNavigation(SiteKey.portal("count"));
//        Node root;
//
//        //
//        root = service.loadNode(Node.MODEL, navigation, Scope.SINGLE, null).getNode();
//        assertEquals(0, root.getNodeCount());
//        // assertEquals(-1, root.getSize());
//
//        //
//        root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
//        assertEquals(0, root.getNodeCount());
//        assertEquals(0, root.getSize());
//        Node a = root.addChild("a");
//        assertEquals(1, root.getNodeCount());
//        assertEquals(1, root.getSize());
//        a.setHidden(true);
//        assertEquals(0, root.getNodeCount());
//        assertEquals(1, root.getSize());
    }

    public void testInsertDuplicate() {
//        MOPService mop = mgr.getPOMService();
//        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "insert_duplicate");
//        portal.getRootNavigation().addChild("default").addChild("a");
//
//        //
//        sync(true);
//
//        //
//        NavigationContext navigation = service.loadNavigation(SiteKey.portal("insert_duplicate"));
//        Node root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
//        try {
//            root.addChild("a");
//            fail();
//        } catch (IllegalArgumentException e) {
//        }
    }
}
