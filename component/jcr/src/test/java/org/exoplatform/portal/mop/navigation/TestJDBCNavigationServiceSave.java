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

package org.exoplatform.portal.mop.navigation;

import java.util.*;

import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.gatein.mop.api.workspace.*;
import org.gatein.mop.core.api.MOPService;

import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.mop.*;
import org.exoplatform.portal.mop.jdbc.service.JDBCModelStorageImpl;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PortalData;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/mop/navigation/configuration.xml")})
public class TestJDBCNavigationServiceSave extends AbstractTestNavigationService {

  /** . */
  protected NavigationService  service;

  private JDBCModelStorageImpl modelStorage;

  protected void setUp() throws Exception {
      super.setUp();
      PortalContainer container = PortalContainer.getInstance();
      this.service = (NavigationService) container.getComponentInstanceOfType(NavigationService.class);
      this.modelStorage = container.getComponentInstanceOfType(JDBCModelStorageImpl.class);
  }

  protected void createSite(SiteType type, String siteName) throws Exception {
      ContainerData container = new ContainerData(null, "testcontainer_" + siteName, "", "", "", "", "",
              "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
      PortalData portal = new PortalData(null, siteName, type.getName(), null, null,
              null, new ArrayList<>(), null, null, null, container, null);
      this.modelStorage.create(portal);

      NavigationContext nav = new NavigationContext(type.key(siteName), new NavigationState(1));
      this.service.saveNavigation(nav);

      end();
  }

  protected void createNavigation(SiteType siteType, String siteName) throws Exception {
      createSite(siteType, siteName);
      service.saveNavigation(new NavigationContext(new SiteKey(siteType, siteName), new NavigationState(1)));
  }

  public void testNonExistingSite() throws Exception {
    assertNull(service.loadNavigation(SiteKey.portal("non_existing")));
  }

  public void testSaveNavigation() throws Exception {
    NavigationContext nav = service.loadNavigation(SiteKey.portal("save_navigation"));
    assertNull(nav);

    //
    createSite(SiteType.PORTAL, "save_navigation");

    //
    end();

    //
    nav = service.loadNavigation(SiteKey.portal("save_navigation"));
    assertNull(nav);

    //
    nav = new NavigationContext(SiteKey.portal("save_navigation"), new NavigationState(5));
    assertNull(nav.getData());
    assertNotNull(nav.getState());
    service.saveNavigation(nav);
    assertNotNull(nav.getData());

    //
    nav.setState(new NavigationState(5));
    service.saveNavigation(nav);
    nav = service.loadNavigation(SiteKey.portal("save_navigation"));
    assertNotNull(nav.getData().getState());
    assertEquals(5, nav.getData().getState().getPriority().intValue());

    //
    end();

    //
    nav = service.loadNavigation(SiteKey.portal("save_navigation"));
    assertNotNull(nav);
    assertEquals(SiteKey.portal("save_navigation"), nav.getKey());
    NavigationState state = nav.getData().getState();
    Integer p = state.getPriority();
    assertEquals(5, (int) p);
  }

  public void testDestroyNavigation() throws Exception {
    NavigationContext nav = service.loadNavigation(SiteKey.portal("destroy_navigation"));
    assertNull(nav);

    //
    createNavigation(SiteType.PORTAL, "destroy_navigation");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("destroy_navigation"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    end();
    if (service instanceof NavigationServiceImpl) {
      ((NavigationServiceImpl) service).clearCache();
    }

    //
    nav = service.loadNavigation(SiteKey.portal("destroy_navigation"));
    assertNotNull(nav);

    //
    Node root = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();

    //
    assertTrue(service.destroyNavigation(nav));
    assertNull(nav.getState());
    assertNull(nav.getData());

    //
    try {
      service.destroyNavigation(nav);
    } catch (Exception e) {
    }

    //
    nav = service.loadNavigation(SiteKey.portal("destroy_navigation"));
    assertNull(nav);

    //
    end();

    //
    nav = service.loadNavigation(SiteKey.portal("destroy_navigation"));
    assertNull(nav);
  }

  public void testAddChild() throws Exception {
    createNavigation(SiteType.PORTAL, "add_child");

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("add_child"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    assertEquals(0, root1.getNodeCount());

    // Test what happens when null is added
    try {
      root1.addChild((String) null);
      fail();
    } catch (NullPointerException ignore) {
    }

    // Test what happens when an illegal index is added
    try {
      root1.addChild(-1, "foo");
      fail();
    } catch (IndexOutOfBoundsException ignore) {
    }
    try {
      root1.addChild(1, "foo");
      fail();
    } catch (IndexOutOfBoundsException ignore) {
    }

    //
    Node foo = root1.addChild("foo");
    assertNull(foo.getId());
    assertEquals("foo", foo.getName());
    assertSame(foo, root1.getChild("foo"));
    assertEquals(1, root1.getNodeCount());
    service.saveNode(root1.getContext(), null);

    //
    root1.assertConsistent();

    //
    end();

    //
    Node root2 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    Node foo2 = root2.getChild("foo");
    assertNotNull(foo2);
    assertEquals(1, root2.getNodeCount());
    assertEquals("foo", foo2.getName());

    //
    root1.assertEquals(root2);
  }

  public void testRemoveChild() throws Exception {
    createNavigation(SiteType.PORTAL, "remove_child");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("remove_child"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("remove_child"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();

    //
    try {
      root1.removeChild(null);
      fail();
    } catch (NullPointerException e) {
    }
    try {
      root1.removeChild("bar");
      fail();
    } catch (IllegalArgumentException e) {
    }

    //
    Node foo1 = root1.getChild("foo");
    assertNotNull(foo1.getId());
    assertEquals("foo", foo1.getName());
    assertSame(foo1, root1.getChild("foo"));

    //
    assertTrue(root1.removeChild("foo"));
    assertNull(root1.getChild("foo"));
    service.saveNode(root1.getContext(), null);

    //
    root1.assertConsistent();

    //
    end();

    //
    Node root2 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    Node foo2 = root2.getChild("foo");
    assertNull(foo2);

    //
    root1.assertEquals(root2);
  }

  public void testRemoveTransientChild() throws Exception {
    createNavigation(SiteType.PORTAL, "remove_transient_child");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("remove_transient_child"));

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("remove_transient_child"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    Node foo1 = root1.addChild("foo");
    assertNull(foo1.getId());
    assertEquals("foo", foo1.getName());
    assertSame(foo1, root1.getChild("foo"));

    //
    assertTrue(root1.removeChild("foo"));
    assertNull(root1.getChild("foo"));
    service.saveNode(root1.getContext(), null);

    //
    root1.assertConsistent();

    //
    end();

    //
    Node root2 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    Node foo2 = root2.getChild("foo");
    assertNull(foo2);

    //
    root1.assertEquals(root2);
  }

  public void testRename() throws Exception {
    createNavigation(SiteType.PORTAL, "rename");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rename"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("rename"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.GRANDCHILDREN, null).getNode();
    try {
      root1.setName("something");
      fail();
    } catch (IllegalStateException e) {
    }

    //
    Node a1 = root1.getChild("a");
    assertEquals(0, a1.getContext().getIndex());
    try {
      a1.setName(null);
      fail();
    } catch (NullPointerException e) {
    }
    try {
      a1.setName("b");
      fail();
    } catch (IllegalArgumentException e) {
    }

    //
    a1.setName("c");
    assertEquals("c", a1.getName());
    assertEquals(0, a1.getContext().getIndex());
    service.saveNode(a1.getContext(), null);

    //
    root1.assertConsistent();

    //
    end();

    //
    nav = service.loadNavigation(SiteKey.portal("rename"));
    Node root2 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    Node a2 = root2.getChild("c");
    assertNotNull(a2);
    // assertEquals(0, a2.getContext().getIndex());

    // Does not pass randomly because of JCR bugs
    // root1.assertEquals(root2);
  }

  public void testReorderChild() throws Exception {
    createNavigation(SiteType.PORTAL, "reorder_child");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("reorder_child"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo");
    rootContext1.add(null, "bar");
    rootContext1.add(null, "juu");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("reorder_child"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    Iterator<Node> i = root1.getChildren().iterator();
    Node foo1 = i.next();
    assertEquals("foo", foo1.getName());
    Node bar1 = i.next();
    assertEquals("bar", bar1.getName());
    Node juu1 = i.next();
    assertEquals("juu", juu1.getName());
    assertFalse(i.hasNext());

    // Test what happens when null is added
    try {
      root1.addChild(1, (Node) null);
      fail();
    } catch (NullPointerException expected) {
    }

    // Test what happens when an illegal index is added
    try {
      root1.addChild(-1, juu1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }
    try {
      root1.addChild(4, juu1);
      fail();
    } catch (IndexOutOfBoundsException expected) {
    }

    //
    root1.addChild(1, juu1);
    service.saveNode(root1.getContext(), null);

    //
    end();
    root1 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    root1.assertConsistent();
//
//    //
//    end();
//
//    //
//    Node root2 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//    i = root2.getChildren().iterator();
//    Node foo2 = i.next();
//    assertEquals("foo", foo2.getName());
//    Node juu2 = i.next();
//    assertEquals("bar", juu2.getName());
//    Node bar2 = i.next();
//    assertEquals("juu", bar2.getName());
//    assertFalse(i.hasNext());
//
//    //
//    root1.assertEquals(root2);
//
//    //
//    root2.addChild(0, bar2);
//
//    //
//    service.saveNode(root2.getContext(), null);
//
//    //
//    root2.assertConsistent();
//
//    //
//    end();
//
//    //
//    Node root3 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
//    i = root3.getChildren().iterator();
//    Node bar3 = i.next();
//    assertEquals("foo", bar3.getName());
//    Node foo3 = i.next();
//    assertEquals("bar", foo3.getName());
//    Node juu3 = i.next();
//    assertEquals("juu", juu3.getName());
//    assertFalse(i.hasNext());
//
//    //
//    root2.assertEquals(root3);
  }

  public void _testReorderChild2() {
    MOPService mop = mgr.getPOMService();
    Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "reorder_child_2");
    Navigation rootNavigation = portal.getRootNavigation().addChild("default");
    rootNavigation.addChild("foo");
    rootNavigation.addChild("bar");
    rootNavigation.addChild("juu");

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("reorder_child_2"));
    Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    assertEquals("bar", root.getChild(1).getName());
    assertTrue(root.removeChild("bar"));
    service.saveNode(root.getContext(), null);

    //
    end();

    //
    root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    root.addChild("daa");
    Node tab3 = root.getChild(2);
    assertEquals("daa", tab3.getName());
    service.saveNode(root.getContext(), null);

    //
    end();

    //
    root = new NavigationServiceImpl(mgr).loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    for (Node child : root.getChildren()) {
      System.out.println("child : " + child.getId());
    }
    tab3 = root.getChild(2);
    assertEquals("daa", tab3.getName());

    root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    for (Node child : root.getChildren()) {
      System.out.println("child : " + child.getId());
    }
    tab3 = root.getChild(2);
    assertEquals("daa", tab3.getName());
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

  public void testMoveChild() throws Exception {
    createNavigation(SiteType.PORTAL, "move_child");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("move_child"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo").add(null, "juu");
    rootContext1.add(null, "bar");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("move_child"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node foo1 = root1.getChild("foo");
    Node bar1 = root1.getChild("bar");
    Node juu1 = foo1.getChild("juu");
    bar1.addChild(juu1);
    service.saveNode(root1.getContext(), null);

    //
    end();

    //
    Node root2 = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node foo2 = root2.getChild("foo");
    Node juu2 = foo2.getChild("juu");
    assertNull(juu2);
    Node bar2 = root2.getChild("bar");
    juu2 = bar2.getChild("juu");
    assertNotNull(juu2);

    //
    root1.assertEquals(root2);
  }

  public void testMoveAfter1() throws Exception {
    createNavigation(SiteType.PORTAL, "save_move_after_1");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("save_move_after_1"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    rootContext1.add(null, "c");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("save_move_after_1"));
    Node root = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node a = root.getChild("a");
    Node b = root.getChild("b");
    Node c = root.getChild("c");
    root.addChild(1, a);
    assertSame(a, root.getChild(0));
    assertSame(b, root.getChild(1));
    assertSame(c, root.getChild(2));
    service.saveNode(root.getContext(), null);

    //
    assertSame(a, root.getChild(0));
    assertSame(b, root.getChild(1));
    assertSame(c, root.getChild(2));

    //
    end();

    //
    root = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    a = root.getChild("a");
    b = root.getChild("b");
    c = root.getChild("c");
    assertSame(a, root.getChild(0));
    assertSame(b, root.getChild(1));
    assertSame(c, root.getChild(2));
  }

  public void testMoveAfter2() throws Exception {
    createNavigation(SiteType.PORTAL, "save_move_after_2");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("save_move_after_2"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    rootContext1.add(null, "c");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("save_move_after_2"));
    Node root = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node a = root.getChild("a");
    Node b = root.getChild("b");
    Node c = root.getChild("c");
    root.addChild(2, a);
    assertSame(b, root.getChild(0));
    assertSame(a, root.getChild(1));
    assertSame(c, root.getChild(2));
    service.saveNode(root.getContext(), null);

    //
    assertSame(b, root.getChild(0));
    assertSame(a, root.getChild(1));
    assertSame(c, root.getChild(2));

    //
    end();

    //
    root = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    a = root.getChild("a");
    b = root.getChild("b");
    c = root.getChild("c");
    assertSame(b, root.getChild(0));
    assertSame(a, root.getChild(1));
    assertSame(c, root.getChild(2));
  }

  public void testRenameNode() throws Exception {
    createNavigation(SiteType.PORTAL, "rename_node");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rename_node"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("rename_node"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node foo1 = root1.getChild("foo");
    foo1.setName("foo");
    service.saveNode(root1.getContext(), null);

    //
    root1.assertConsistent();

    //
    end();

    //
    nav = service.loadNavigation(SiteKey.portal("rename_node"));
    Node root2 = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();

    //
    root1.assertEquals(root2);

    //
    Node foo2 = root2.getChild("foo");
    foo2.setName("bar");
    assertEquals("bar", foo2.getName());
    assertSame(foo2, root2.getChild("bar"));
    service.saveNode(root2.getContext(), null);
    assertEquals("bar", foo2.getName());
    assertSame(foo2, root2.getChild("bar"));

    //
    root2.assertConsistent();

    //
    end();

    //
    Node root3 = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node bar3 = root3.getChild("bar");
    assertNotNull(bar3);
    assertSame(bar3, root3.getChild("bar"));

    //
    root2.assertEquals(root3);

    //
    root3.addChild("foo");
    try {
      bar3.setName("foo");
      fail();
    } catch (IllegalArgumentException ignore) {
    }
  }

  public void testSaveChildren() throws Exception {
    createNavigation(SiteType.PORTAL, "save_children");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("save_children"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "1");
    rootContext1.add(null, "2");
    rootContext1.add(null, "3");
    rootContext1.add(null, "4");
    rootContext1.add(null, "5");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("save_children"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    root1.removeChild("5");
    root1.removeChild("2");
    root1.addChild(0, root1.getChild("3"));
    root1.addChild(1, root1.addChild("."));
    service.saveNode(root1.getContext(), null);
    Iterator<Node> i = root1.getChildren().iterator();
    assertEquals("3", i.next().getName());
    assertEquals(".", i.next().getName());
    assertEquals("1", i.next().getName());
    assertEquals("4", i.next().getName());
    assertFalse(i.hasNext());
  }

  public void testSaveRecursive() throws Exception {
    createNavigation(SiteType.PORTAL, "save_recursive");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("save_recursive"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("save_recursive"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node foo1 = root1.getChild("foo");
    Node bar1 = foo1.addChild("bar");
    bar1.addChild("juu");
    service.saveNode(root1.getContext(), null);

    //
    root1.assertConsistent();

    //
    end();

    //
    Node root2 = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node foo2 = root2.getChild("foo");
    Node bar2 = foo2.getChild("bar");
    assertNotNull(bar2.getId());
    Node juu2 = bar2.getChild("juu");
    assertNotNull(juu2.getId());

    //
    root1.assertEquals(root2);
  }

  public void testSaveState() throws Exception {
    createNavigation(SiteType.PORTAL, "save_state");

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("save_state"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.SINGLE, null).getNode();
    NodeState state = root1.getState();
    assertNull(state.getLabel());
    assertEquals(0, state.getStartPublicationTime());
    assertEquals(0, state.getEndPublicationTime());
    long now = System.currentTimeMillis();
    root1.setState(new NodeState.Builder().endPublicationTime(now).label("bar").build());
    service.saveNode(root1.getContext(), null);

    //
    root1.assertConsistent();

    //
    end();

    //
    Node root2 = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    state = root2.getState();
    assertEquals("bar", state.getLabel());
    assertEquals(-1, state.getStartPublicationTime());
    assertEquals(now, state.getEndPublicationTime());
    assertEquals(Visibility.DISPLAYED, state.getVisibility());

    //
    root1.assertEquals(root2);
  }

  public void _testSaveStateOverwrite() {
    MOPService mop = mgr.getPOMService();
    Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "save_state_overwrite");
    portal.getRootNavigation().addChild("default");

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("save_state_overwrite"));
    Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    root.addChild("foo");
    service.saveNode(root.getContext(), null);

    //
    end();

    //
    root.addChild("bar");
    service.saveNode(root.getContext(), null);

    //
    end();

    //
    nav = service.loadNavigation(SiteKey.portal("save_state_overwrite"));
    root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    assertEquals(2, root.getChildren().size());
  }

  public void testRecreateNode() throws Exception {
    createNavigation(SiteType.PORTAL, "recreate_node");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("recreate_node"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "foo");
    service.saveNode(rootContext1, null);

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("recreate_node"));
    Node root1 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    String fooId = root1.getChild("foo").getId();
    assertTrue(root1.removeChild("foo"));
    assertNull(root1.addChild("foo").getId());
    service.saveNode(root1.getContext(), null);

    //
    root1.assertConsistent();

    //
    end();

    //
    Node root2 = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    assertNotNull(root2.getChild("foo").getId());
    assertNotSame(fooId, root2.getChild("foo").getId());

    //
    root1.assertEquals(root2);
  }

  public void testMoveToAdded() throws Exception {
    createNavigation(SiteType.PORTAL, "move_to_added");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("move_to_added"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();
    Node a1 = root1.getChild("a");
    Node b1 = a1.getChild("b");
    Node c1 = root1.addChild("c");
    c1.addChild(b1);
    service.saveNode(root1.getContext(), null);

    //
//    root1.assertConsistent();

    //
    end();

    //
    navigation = service.loadNavigation(SiteKey.portal("move_to_added"));
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();
    Node a2 = root2.getChild("a");
    assertNotNull(a2);
    Node c2 = root2.getChild("c");
    assertNotNull(c2);
    Node b2 = c2.getChild("b");
    assertNotNull(b2);

    //
    root1.assertEquals(root2);
  }

  public void testMoveFromRemoved() throws Exception {
    createNavigation(SiteType.PORTAL, "moved_from_removed");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("moved_from_removed"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "c");
    rootContext1.add(null, "b");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();
    Node a1 = root1.getChild("a");
    Node b1 = root1.getChild("b");
    Node c1 = a1.getChild("c");
    b1.addChild(c1);
    root1.removeChild("a");
    service.saveNode(root1.getContext(), null);

    //
    root1.assertConsistent();

    //
    end();

    //
    navigation = service.loadNavigation(SiteKey.portal("moved_from_removed"));
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();
    assertNull(root2.getChild("a"));
    Node b2 = root2.getChild("b");
    assertNotNull(b2);
    Node c2 = b2.getChild("c");
    assertNotNull(c2);

    //
    root1.assertEquals(root2);
  }

  public void testRemoveAdded() throws Exception {
    createNavigation(SiteType.PORTAL, "remove_added");

    //
    end();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("remove_added"));
    Node root = service.loadNode(Node.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();
    root.addChild("foo");
    root.removeChild("foo");
    service.saveNode(root.getContext(), null);

    //
    root.assertConsistent();

    //
    end();

    //
    root = service.loadNode(Node.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();
    assertEquals(0, root.getChildren().size());
  }

  public void testTransitiveRemoveTransient() throws Exception {
    createNavigation(SiteType.PORTAL, "transitive_remove_transient");

    //
    end();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("transitive_remove_transient"));
    Node root = service.loadNode(Node.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();
    root.addChild("foo").addChild("bar");
    root.removeChild("foo");
    service.saveNode(root.getContext(), null);

    //
    root.assertConsistent();

    //
    end();

    //
    root = service.loadNode(Node.MODEL, navigation, Scope.GRANDCHILDREN, null).getNode();
    assertEquals(0, root.getChildren().size());
  }

  public void testRenameCreatedNode() throws Exception {
    createNavigation(SiteType.PORTAL, "save_rename_created");

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("save_rename_created"));
    Node root = service.loadNode(Node.MODEL, nav, Scope.ALL, null).getNode();
    Node temp = root.addChild("temp");
    temp.setName("bar");
    Iterator<NodeChange<Node>> changes = root.save(service);
    assertTrue(changes.hasNext());
    NodeChange.Created<Node> created = (NodeChange.Created<Node>)changes.next();
    Node n = created.getTarget();
    assertEquals("bar", n.getName());
    assertSame("bar", root.getChild(0).getName());
  }

  public void testConcurrentAddToRemoved() throws Exception {
    createNavigation(SiteType.PORTAL, "add_to_removed");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("add_to_removed"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root.getChild("a").addChild("b");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("a");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    try {
      service.saveNode(root.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.ADD_CONCURRENTLY_REMOVED_PARENT_NODE, e.getError());
    }
  }

  public void testConcurrentMerge() throws Exception {
    createNavigation(SiteType.PORTAL, "save_merge");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("save_merge"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    rootContext1.add(null, "c");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();

    //
    sync();

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    root2.addChild(1, root2.addChild("2"));
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    service.saveNode(root1.getContext(), null);

    //
    root1.addChild(1, root1.addChild("1"));
    service.saveNode(root1.getContext(), null);
  }

  public void testConcurrentRemoveRemoved() throws Exception {
    createNavigation(SiteType.PORTAL, "remove_removed");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("remove_removed"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.removeChild("a");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("a");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    service.saveNode(root1.getContext(), null);

    //
    root1.assertEquals(root2);
  }

  public void testConcurrentMoveRemoved() throws Exception {
    createNavigation(SiteType.PORTAL, "move_removed");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("move_removed"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.addChild(root1.getChild("a").getChild("b"));

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.getChild("a").removeChild("b");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.MOVE_CONCURRENTLY_REMOVED_MOVED_NODE, e.getError());
    }
  }

  public void testConcurrentMoveToRemoved() throws Exception {
    createNavigation(SiteType.PORTAL, "move_to_removed");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("move_to_removed"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("b").addChild(root1.getChild("a"));

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("b");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.MOVE_CONCURRENTLY_REMOVED_DST_NODE, e.getError());
    }
  }

  public void testConcurrentMoveMoved() throws Exception {
    createNavigation(SiteType.PORTAL, "move_moved");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("move_moved"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    rootContext1.add(null, "c");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("b").addChild(root1.getChild("a"));

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.getChild("c").addChild(root2.getChild("a"));
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.MOVE_CONCURRENTLY_CHANGED_SRC_NODE, e.getError());
    }
  }

  public void testConcurrentAddDuplicate() throws Exception {
    createNavigation(SiteType.PORTAL, "concurrent_add_duplicate");

    //
    end();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("concurrent_add_duplicate"));
    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.addChild("a");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    root1.addChild("a");
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.ADD_CONCURRENTLY_ADDED_NODE, e.getError());
    }
  }

  public void testConcurrentAddAfterRemoved() throws Exception {
    createNavigation(SiteType.PORTAL, "concurrent_add_after_removed");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("concurrent_add_after_removed"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.addChild(1, "b");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("a");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.ADD_CONCURRENTLY_REMOVED_PREVIOUS_NODE, e.getError());
    }
  }

  public void testConcurrentMoveAfterRemoved() throws Exception {
    createNavigation(SiteType.PORTAL, "concurrent_move_after_removed");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("concurrent_move_after_removed"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    rootContext1.add(null, "c");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.addChild(2, root1.getChild("a").getChild("b"));

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("c");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.MOVE_CONCURRENTLY_REMOVED_PREVIOUS_NODE, e.getError());
    }
  }

  public void testConcurrentMoveFromRemoved() throws Exception {
    createNavigation(SiteType.PORTAL, "concurrent_move_from_removed");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("concurrent_move_from_removed"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    rootContext1.add(null, "c");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("c").addChild(root1.getChild("a").getChild("b"));

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("a");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.MOVE_CONCURRENTLY_REMOVED_SRC_NODE, e.getError());
    }
  }

  public void testConcurrentRenameRemoved() throws Exception {
    createNavigation(SiteType.PORTAL, "concurrent_rename_removed");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("concurrent_rename_removed"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("a").setName("b");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("a");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.RENAME_CONCURRENTLY_REMOVED_NODE, e.getError());
    }
  }

  public void testConcurrentDuplicateRename() throws Exception {
    createNavigation(SiteType.PORTAL, "concurrent_duplicate_rename");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("concurrent_duplicate_rename"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("a").setName("b");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.addChild("b");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.RENAME_CONCURRENTLY_DUPLICATE_NAME, e.getError());
    }
  }

  public void testSavePhantomNode() throws Exception {
    createNavigation(SiteType.PORTAL, "concurrent_save");

    //
    end();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("concurrent_save"));
    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.addChild("a");
    service.saveNode(root1.getContext(), null);

    //
    end();

    // Reload the root node and modify it
    root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("a").setState(root1.getState().builder().label("foo").build());

    //
    end();

    // Edit navigation in another browser
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("a");
    service.saveNode(root2.getContext(), null);

    //
    end();

    // Now click Save button in the first browser
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.UPDATE_CONCURRENTLY_REMOVED_NODE, e.getError());
    }
  }

  public void testConcurrentRemovalDoesNotPreventSave() throws Exception {
    createNavigation(SiteType.PORTAL, "removal_does_not_prevent_save");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("removal_does_not_prevent_save"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("a");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    service.saveNode(root1.getContext(), null);
  }

  public void testConcurrentRename() throws Exception {
    createNavigation(SiteType.PORTAL, "save_concurrent_rename");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("save_concurrent_rename"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node a = root1.getChild("a");
    a.setName("b");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node a2 = root2.getChild("a");
    a2.setName("c");
    service.saveNode(root2.getContext(), null);

    //
    Iterator<NodeChange<Node>> changes = root1.save(service);
    assertTrue(changes.hasNext());
    NodeChange.Renamed<Node> renamed = (NodeChange.Renamed<Node>)changes.next();
    Node n = renamed.getTarget();
    assertEquals("b", n.getName());
  }

  public void testRemovedNavigation() throws Exception {
    createNavigation(SiteType.PORTAL, "save_removed_navigation");

    //
    end();

    //
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("save_removed_navigation"));
    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    service.destroyNavigation(navigation);

    //
    end();

    //
    try {
      service.saveNode(root.getContext(), null);
      fail();
    } catch (HierarchyException e) {
      assertSame(HierarchyError.UPDATE_CONCURRENTLY_REMOVED_NODE, e.getError());
    }
  }

  public void testPendingChangesBypassCache() throws Exception {
    createNavigation(SiteType.PORTAL, "pending_changes_bypass_cache");

    //
    end();

    //
    NavigationContext nav = service.loadNavigation(SiteKey.portal("pending_changes_bypass_cache"));
    Node root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    root.addChild("foo");
    service.saveNode(root.getContext(), null);

    //
    root = service.loadNode(Node.MODEL, nav, Scope.CHILDREN, null).getNode();
    assertNotNull(root.getChild("foo"));
  }

  public void testAtomic() throws Exception {
    createNavigation(SiteType.PORTAL, "save_atomic");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("save_atomic"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root1.getChild("a").addChild("c");
    root1.getChild("b").addChild("d");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("b");
    service.saveNode(root2.getContext(), null);

    //
    end();

    //
    assertFalse(mgr.getSession().isModified());

    //
    try {
      service.saveNode(root1.getContext(), null);
      fail();
    } catch (NavigationServiceException e) {
      assertSame(NavigationError.ADD_CONCURRENTLY_REMOVED_PARENT_NODE, e.getError());
    }

    //
    assertFalse(mgr.getSession().isModified());
  }

  public void testSaveRebase() throws Exception {
    createNavigation(SiteType.PORTAL, "save_rebase");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("save_rebase"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    end();

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node a = root1.getChild("a");
    Node b = root1.addChild("b");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node c = root2.addChild("c");
    service.saveNode(root2.getContext(), null);

    //
    Iterator<NodeChange<Node>> changes = root1.save(service);
    NodeChange.Created<Node> added = (NodeChange.Created<Node>) changes.next();
    Node n = added.getTarget();
    assertEquals("b", n.getName());
    assertFalse(changes.hasNext());
    assertSame(a.getName(), root1.getChild(0).getName());
    assertSame(b.getName(), root1.getChild(1).getName());
    assertSame(c.getName(), root1.getChild(2).getName());
  }

  @Override
  protected NavigationService getNavigationService() {
    PortalContainer container = PortalContainer.getInstance();
    return (NavigationService) container.getComponentInstanceOfType(NavigationService.class);
  }
}
