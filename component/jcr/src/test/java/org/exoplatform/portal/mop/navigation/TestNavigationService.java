/*
 * Copyright (C) 2010 eXo Platform SAS.
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

import java.util.Iterator;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.portal.mop.Described;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.gatein.mop.api.workspace.Navigation;
import org.gatein.mop.api.workspace.ObjectType;
import org.gatein.mop.api.workspace.Site;
import org.gatein.mop.core.api.MOPService;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TestNavigationService extends AbstractTestNavigationService {

    public void testNonExistingSite() throws Exception {
        assertNull(service.loadNavigation(SiteKey.portal("non_existing")));
    }

    // Do not execute this test case for the moment as it depends on
    // execution order of test cases, that explains failure in JDK 1.7
    public void _testLoadNavigations() {
        List<NavigationContext> navCtxs = service.loadNavigations(SiteType.PORTAL);
        assertEquals(3, navCtxs.size());
    }

    public void testLoadSingleScope() throws Exception {
        NavigationContext nav = service.loadNavigation(SiteKey.portal("classic"));
        Node root = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();
        assertNull(root.getChildren());
        assertEquals("default", root.getName());
        try {
            root.getChild(0);
            fail();
        } catch (IllegalStateException ignore) {
        }
        try {
            root.addChild("a");
            fail();
        } catch (IllegalStateException e) {
        }
        try {
            root.addChild(0, "a");
            fail();
        } catch (IllegalStateException e) {
        }
        try {
            root.removeChild("a");
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testLoadChildrenScope() throws Exception {
        NavigationContext nav = service.loadNavigation(SiteKey.portal("classic"));
        Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        assertEquals("default", root.getName());
        Iterator<? extends Node> i = root.getChildren().iterator();
        assertTrue(i.hasNext());
        Node home = i.next();
        assertSame(home, root.getChild(0));
        assertNull(home.getChildren());
        assertEquals("home", home.getName());
        assertTrue(i.hasNext());
        Node webexplorer = i.next();

        assertNull(webexplorer.getChildren());

        assertSame(webexplorer, root.getChild(1));
        assertEquals("webexplorer", webexplorer.getName());
        assertFalse(i.hasNext());
    }

    public void testLoadCustomScope() throws Exception {
        NavigationContext nav = service.loadNavigation(SiteKey.portal("large"));
        Node root = service.loadNode(Node.MODEL, nav, new Scope() {
            public Visitor get() {
                return new Visitor() {
                    public VisitMode enter(int depth, String id, String name, NodeState state) {
                        boolean use = false;
                        switch (depth) {
                            case 0:
                                use = "default".equals(name);
                                break;
                            case 1:
                                use = "b".equals(name);
                                break;
                            case 2:
                                use = "d".equals(name);
                                break;
                        }
                        return use ? VisitMode.ALL_CHILDREN : VisitMode.NO_CHILDREN;
                    }

                    public void leave(int depth, String id, String name, NodeState state) {
                    }
                };
            }
        }, null).getNode();
        assertNull(root.getChild("a").getChildren());
        Node b = root.getChild("b");
        Node d = b.getChild("d");
        assertNull(d.getChild("e").getChildren());
    }

    public void testUpdateNode() throws Exception {
        NavigationContext nav = service.loadNavigation(SiteKey.portal("large"));
        Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        Node a = root.getChild("a");
        assertNotNull(a);
        assertNull(a.getChildren());
        a.update(service, Scope.CHILDREN);
        assertNotNull(a.getChildren());
        assertEquals(1, a.getChildren().size());
        Node c = a.getChild("c");
        assertEquals("c", c.getName());
        assertSame(a, c.getParent());
        service.updateNode(a.context, Scope.SINGLE, null);
        assertNotNull(a.getChildren());
        assertEquals(1, a.getChildren().size());
        assertSame(c, a.getChild("c"));
        assertNotNull(c.getParent());
    }

    public void testState() throws Exception {
        NavigationContext nav = service.loadNavigation(SiteKey.portal("test"));
        Node root = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
        assertEquals(5, root.getNodeCount());
        Node child1 = root.getChild("node_name");
        Node child2 = root.getChild("node_name4");
        assertEquals("node_name", child1.getName());
        assertEquals("node_label", child1.getContext().getState().getLabel());
        assertEquals(SiteKey.portal("test").page("test1"), child1.getContext().getState().getPageRef());
        assertEquals(Visibility.TEMPORAL, child1.getContext().getState().getVisibility());
        assertEquals(953602380000L, child1.getContext().getState().getStartPublicationTime());
        assertEquals(1237599180000L, child1.getContext().getState().getEndPublicationTime());
        assertEquals("node_name4", child2.getName());
        assertEquals("node_label4", child2.getContext().getState().getLabel());
        assertEquals(SiteKey.portal("test").page("test1"), child2.getContext().getState().getPageRef());
        assertEquals(Visibility.DISPLAYED, child2.getContext().getState().getVisibility());
    }

    public void testDepth() throws Exception {
        NavigationContext nav = service.loadNavigation(SiteKey.portal("test"));
        Node root = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
        Node child1 = root.getChild("node_name");
        assertEquals(0, child1.context.getDepth(child1.context));
        assertEquals(1, child1.context.getDepth(root.context));
        try {
            root.context.getDepth(child1.context);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    public void testHiddenNode() throws Exception {
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "hidden_node");
        Navigation defaultNav = portal.getRootNavigation().addChild("default");
        defaultNav.addChild("a");
        defaultNav.addChild("b");
        defaultNav.addChild("c");

        //
        end();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_node"));

        //
        Node root;
        Node a;
        Node b;
        Node c;

        //
        root = service.loadNode(Node.MODEL, nav, Scope.GRANDCHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        a.setHidden(true);
        assertEquals(2, root.getChildren().size());
        assertNull(root.getChild("a"));
        assertEquals("b", root.getChild(0).getName());
        try {
            root.getChild(2);
            fail();
        } catch (IndexOutOfBoundsException ignore) {
        }
        assertFalse(root.removeChild("a"));
        try {
            b.setName("a");
            fail();
        } catch (IllegalArgumentException ignore) {
        }

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        assertSame(a, root.getChild(0));
        assertSame(c, root.getChild(1));
        try {
            root.getChild(2);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        a.setHidden(true);
        c.setHidden(true);
        assertSame(b, root.getChild(0));
        try {
            root.getChild(1);
            fail();
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testHiddenInsert1() throws Exception {
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "hidden_insert_1");
        Navigation defaultNav = portal.getRootNavigation().addChild("default");
        defaultNav.addChild("a");

        //
        end();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_insert_1"));

        //
        Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        Node a = root.getChild("a");
        a.setHidden(true);
        Node b = root.addChild("b");
        assertEquals(1, root.getChildren().size());
        assertSame(b, root.getChildren().iterator().next());
        a.setHidden(false);
        assertEquals(2, root.getChildren().size());
        Iterator<Node> it = root.getChildren().iterator();
        assertSame(b, it.next());
        assertSame(a, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        a.setHidden(true);
        b = root.addChild(0, "b");
        assertEquals(1, root.getChildren().size());
        assertSame(b, root.getChildren().iterator().next());
        a.setHidden(false);
        assertEquals(2, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(b, it.next());
        assertSame(a, it.next());
    }

    public void testHiddenInsert2() throws Exception {
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "hidden_insert_2");
        Navigation defaultNav = portal.getRootNavigation().addChild("default");
        defaultNav.addChild("a");
        defaultNav.addChild("b");

        //
        end();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_insert_2"));

        //
        Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        Node a = root.getChild("a");
        Node b = root.getChild("b");
        b.setHidden(true);
        Node c = root.addChild(0, "c");
        assertEquals(2, root.getChildren().size());
        Iterator<Node> it = root.getChildren().iterator();
        assertSame(c, it.next());
        assertSame(a, it.next());
        b.setHidden(false);
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(c, it.next());
        assertSame(a, it.next());
        assertSame(b, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        b.setHidden(true);
        c = root.addChild(1, "c");
        assertEquals(2, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        b.setHidden(false);
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        assertSame(b, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        b.setHidden(true);
        c = root.addChild("c");
        assertEquals(2, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        b.setHidden(false);
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        assertSame(b, it.next());
    }

    public void testHiddenInsert3() throws Exception {
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "hidden_insert_3");
        Navigation defaultNav = portal.getRootNavigation().addChild("default");
        defaultNav.addChild("a");
        defaultNav.addChild("b");
        defaultNav.addChild("c");

        //
        end();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("hidden_insert_3"));

        //
        Node root, a, b, c, d;
        Iterator<Node> it;

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        d = root.addChild(0, "d");
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(d, it.next());
        assertSame(a, it.next());
        assertSame(c, it.next());
        b.setHidden(false);
        assertEquals(4, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(d, it.next());
        assertSame(a, it.next());
        assertSame(b, it.next());
        assertSame(c, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        d = root.addChild(1, "d");
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(d, it.next());
        assertSame(c, it.next());
        b.setHidden(false);
        assertEquals(4, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(d, it.next());
        assertSame(b, it.next());
        assertSame(c, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        d = root.addChild(2, "d");
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        assertSame(d, it.next());
        b.setHidden(false);
        assertEquals(4, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(b, it.next());
        assertSame(c, it.next());
        assertSame(d, it.next());

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        a = root.getChild("a");
        b = root.getChild("b");
        c = root.getChild("c");
        b.setHidden(true);
        d = root.addChild("d");
        assertEquals(3, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(c, it.next());
        assertSame(d, it.next());
        b.setHidden(false);
        assertEquals(4, root.getChildren().size());
        it = root.getChildren().iterator();
        assertSame(a, it.next());
        assertSame(b, it.next());
        assertSame(c, it.next());
        assertSame(d, it.next());
    }

    public void _testNodeInvalidationByRemoval() {
        //
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "invalidation_by_removal");
        portal.getRootNavigation().addChild("default");

        //
        end();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("invalidation_by_removal"));
        Node root = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();
        assertNotNull(root);

        //
        end();

        //
        mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "invalidation_by_removal").getRootNavigation()
                .getChild("default").destroy();

        //
        end();

        //
        NodeContext<Node> rootCtx = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null);
        assertNull(rootCtx);
    }

    public void _testNodeInvalidationByChild() {
        // Create a navigation
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "invalidation_by_child");
        portal.getRootNavigation().addChild("default");

        //
        end();

        // Put the navigation in the cache
        NavigationContext nav = service.loadNavigation(SiteKey.portal("invalidation_by_child"));
        Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        Iterator<? extends Node> iterator = root.getChildren().iterator();
        assertFalse(iterator.hasNext());

        //
        end();

        //
        mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "invalidation_by_child").getRootNavigation()
                .getChild("default").addChild("new");

        //
        end();

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        iterator = root.getChildren().iterator();
        iterator.next();
        assertFalse(iterator.hasNext());

        //
        end();

        //
        mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "invalidation_by_child").getRootNavigation()
                .getChild("default").getChild("new").destroy();

        //
        end();

        //
        root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
        iterator = root.getChildren().iterator();
        assertFalse(iterator.hasNext());
    }

    public void _testNodeInvalidationByProperty() {
        // Create a navigation
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "invalidation_by_propertychange");
        portal.getRootNavigation().addChild("default");

        //
        end();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("invalidation_by_propertychange"));
        Node defaultNode = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();
        assertNull(defaultNode.getContext().getState().getLabel());

        //
        end();

        //
        Described defaultDescribed = mop.getModel().getWorkspace()
                .getSite(ObjectType.PORTAL_SITE, "invalidation_by_propertychange").getRootNavigation().getChild("default")
                .adapt(Described.class);
        defaultDescribed.setName("bilto");

        //
        end();

        //
        defaultNode = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();
        assertEquals("bilto", defaultNode.getContext().getState().getLabel());

        //
        end();

        //
        defaultDescribed = mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "invalidation_by_propertychange")
                .getRootNavigation().getChild("default").adapt(Described.class);
        defaultDescribed.setName("bilta");

        //
        end();

        //
        defaultNode = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();
        assertEquals("bilta", defaultNode.getContext().getState().getLabel());

        //
        end();

        //
        defaultDescribed = mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "invalidation_by_propertychange")
                .getRootNavigation().getChild("default").adapt(Described.class);
        defaultDescribed.setName(null);

        //
        end();

        //
        defaultNode = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();
        assertNull(defaultNode.getContext().getState().getLabel());
    }

    public void _testNodeInvalidationByAttribute() {
        //
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "invalidation_by_attribute");
        portal.getRootNavigation().addChild("default");

        //
        end();

        //
        NavigationContext nav = service.loadNavigation(SiteKey.portal("invalidation_by_attribute"));
        Node defaultNode = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();

        //
        end();

        //
        mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "invalidation_by_attribute").getRootNavigation()
                .getChild("default").getAttributes().setValue(MappedAttributes.URI, "foo_uri");

        //
        end();

        //
        defaultNode = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();

        //
        end();

        //
        mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "invalidation_by_attribute").getRootNavigation()
                .getChild("default").getAttributes().setValue(MappedAttributes.URI, "bar_uri");

        //
        end();

        //
        defaultNode = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();

        //
        end();

        //
        mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "invalidation_by_attribute").getRootNavigation()
                .getChild("default").getAttributes().setValue(MappedAttributes.URI, null);

        //
        end();

        //
        defaultNode = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();
    }

    public void _testWeirdBug() {
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "reorder_child_2");
        Navigation rootNavigation = portal.getRootNavigation().addChild("default");
        rootNavigation.addChild("foo");
        rootNavigation.addChild("bar");
        rootNavigation.addChild("juu");

        //
        end();

        //
        portal = mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "reorder_child_2");
        rootNavigation = portal.getRootNavigation().getChild("default");
        rootNavigation.getChild("bar").destroy();

        //
        end();

        //
        portal = mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "reorder_child_2");
        rootNavigation = portal.getRootNavigation().getChild("default");
        rootNavigation.addChild("daa");

        //
        end();

        //
        portal = mop.getModel().getWorkspace().getSite(ObjectType.PORTAL_SITE, "reorder_child_2");
        rootNavigation = portal.getRootNavigation().getChild("default");
        Navigation daa = rootNavigation.getChildren().get(2);
        assertEquals("daa", daa.getName());
    }

    public void _testWeirdBug2() throws Exception {
        MOPService mop = mgr.getPOMService();
        Session session = mop.getModel().getSession().getJCRSession();
        javax.jcr.Node container = session.getRootNode().getNode("mop:workspace/mop:portalsites")
                .addNode("mop:reorder_child_2").getNode("mop:rootnavigation/mop:children").addNode("mop:default")
                .getNode("mop:children");
        container.addNode("mop:foo");
        container.addNode("mop:bar");
        container.addNode("mop:juu");

        //
        end();

        //
        session = mop.getModel().getSession().getJCRSession();
        container = session.getRootNode().getNode(
                "mop:workspace/mop:portalsites/mop:reorder_child_2/mop:rootnavigation/mop:children/mop:default/mop:children");
        container.getNode("mop:bar").remove();

        //
        end();

        //
        session = mop.getModel().getSession().getJCRSession();
        container = session.getRootNode().getNode(
                "mop:workspace/mop:portalsites/mop:reorder_child_2/mop:rootnavigation/mop:children/mop:default/mop:children");
        container.addNode("mop:daa");
        container.orderBefore("mop:daa", null);

        //
        end();

        //
        container = session.getRootNode().getNode(
                "mop:workspace/mop:portalsites/mop:reorder_child_2/mop:rootnavigation/mop:children/mop:default/mop:children");
        NodeIterator it = container.getNodes();
        assertEquals("mop:foo", it.nextNode().getName());
        assertEquals("mop:juu", it.nextNode().getName());
        assertEquals("mop:daa", it.nextNode().getName());
        assertFalse(it.hasNext());
    }

    public void testCount() {
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "count");
        portal.getRootNavigation().addChild("default");

        //
        end();

        //
        NavigationContext navigation = service.loadNavigation(SiteKey.portal("count"));
        Node root;

        //
        root = service.loadNode(Node.MODEL, navigation, Scope.SINGLE, null).getNode();
        assertEquals(0, root.getNodeCount());
        // assertEquals(-1, root.getSize());

        //
        root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
        assertEquals(0, root.getNodeCount());
        assertEquals(0, root.getSize());
        Node a = root.addChild("a");
        assertEquals(1, root.getNodeCount());
        assertEquals(1, root.getSize());
        a.setHidden(true);
        assertEquals(0, root.getNodeCount());
        assertEquals(1, root.getSize());
    }

    public void testInsertDuplicate() {
        MOPService mop = mgr.getPOMService();
        Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "insert_duplicate");
        portal.getRootNavigation().addChild("default").addChild("a");

        //
        end();

        //
        NavigationContext navigation = service.loadNavigation(SiteKey.portal("insert_duplicate"));
        Node root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
        try {
            root.addChild("a");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }
}
