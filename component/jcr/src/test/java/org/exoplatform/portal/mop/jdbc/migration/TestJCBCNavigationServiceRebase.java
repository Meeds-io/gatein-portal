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

package org.exoplatform.portal.mop.jdbc.migration;

import java.util.Iterator;

import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.*;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/mop/navigation/configuration.xml")})
public class TestJCBCNavigationServiceRebase extends AbstractTestNavigationService {

  public void testRebase1() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase1");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase1"));
    NodeContext root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    root1.add(null, "a");
    root1.add(null, "d");
    service.saveNode(root1, null);
    //
    sync(true);

    //
    root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    NodeContext a = root1.get("a");
    NodeContext d = root1.get("d");
    NodeContext b = root1.add(1, "b");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node c2 = root2.addChild(1, "c");
    service.saveNode(root2.getContext(), null);
    sync(true);

    //
    service.rebaseNode(root1, null, null);
    assertEquals(4, root1.getNodeCount());
    assertSame(a.getNode(), root1.getNode(0));
    assertSame(b.getNode(), root1.getNode(1));
    Node c1 = (Node) root1.getNode(2);
    assertEquals("c", c1.getName());
    assertEquals(c2.getId(), c1.getId());
    assertSame(d.getNode(), root1.getNode(3));

  }

  public void testRebase2() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase2");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase2"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    service.saveNode(rootContext1, null);

    //
    sync(true);

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    Node a = root1.getChild("a");
    Node b = root1.getChild("b");
    Node c = a.addChild("c");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.getChild("b").addChild(root2.getChild("a"));
    service.saveNode(root2.getContext(), null);
    sync(true);

    //
    service.rebaseNode(root1.getContext(), null, null);
    assertEquals(null, root1.getChild("a"));
    assertSame(b, root1.getChild("b"));
    assertEquals(root1, b.getParent());
    assertSame(a, b.getChild("a"));
    assertEquals(b, a.getParent());
    assertSame(c, a.getChild("c"));
    assertEquals(a, c.getParent());
  }

  public void testRebase3() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase3");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase3"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    rootContext1.add(null, "b");
    service.saveNode(rootContext1, null);

    //
    sync(true);

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root.getChild("a").addChild("foo");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.removeChild("a");
    service.saveNode(root2.getContext(), null);
    sync(true);

    //
    try {
      service.rebaseNode(root.getContext(), null, null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.ADD_CONCURRENTLY_REMOVED_PARENT_NODE, e.getError());
    }

  }

  /**
   * This test is quite important as it ensures that the copy tree during the rebase operation is rebuild from the initial
   * state. Indeed the move / destroy operations would fail otherwise as the move operation would not find its source.
   */
  public void testRebase4() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase4");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase4"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    service.saveNode(rootContext1, null);

    //
    sync(true);

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root.addChild(root.getChild("a").getChild("b"));
    root.removeChild("a");

    //
    service.rebaseNode(root.getContext(), null, null);
  }

  public void testRebaseAddDuplicate() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase_add_duplicate");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase_add_duplicate"));
    //
    sync(true);

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root.addChild("a");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.addChild("a");
    service.saveNode(root2.getContext(), null);
    sync(true);

    //
    try {
      service.rebaseNode(root.getContext(), null, null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.ADD_CONCURRENTLY_ADDED_NODE, e.getError());
    }

  }

  public void testRebaseMoveDuplicate() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase_move_duplicate");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase_move_duplicate"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    service.saveNode(rootContext1, null);

    //
    sync(true);

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root.addChild(root.getChild("a").getChild("b"));

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.addChild("b");
    service.saveNode(root2.getContext(), null);
    sync(true);

    //
    try {
      service.rebaseNode(root.getContext(), null, null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.MOVE_CONCURRENTLY_DUPLICATE_NAME, e.getError());
    }

  }

  public void testRebaseRenameDuplicate() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase_rename_duplicate");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase_rename_duplicate"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    sync(true);

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root.getChild("a").setName("b");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.addChild("b");
    service.saveNode(root2.getContext(), null);
    sync(true);

    //
    try {
      service.rebaseNode(root.getContext(), null, null);
      fail();
    } catch (NavigationServiceException e) {
      assertEquals(NavigationError.RENAME_CONCURRENTLY_DUPLICATE_NAME, e.getError());
    }
  }

  public void testFederation() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase_federation");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase_federation"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a").add(null, "b");
    service.saveNode(rootContext1, null);

    //
    sync(true);

    Node root1 = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    final Node a = root1.getChild("a");
    final Node c = root1.addChild("c");

    //
    Node root2 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    root2.addChild("d").addChild("e");
    service.saveNode(root2.getContext(), null);
    sync(true);

    //
    Iterator<NodeChange<Node>> changes = a.rebase(service, Scope.CHILDREN);
    Iterator<Node> children = root1.getChildren().iterator();
    assertSame(a, children.next());
    assertSame(c, children.next());
    Node d = children.next();
    assertEquals("d", d.getName());
    assertFalse(children.hasNext());
    assertFalse(d.getContext().isExpanded());
    children = a.getChildren().iterator();
    Node b = children.next();
    assertEquals("b", b.getName());
    assertFalse(children.hasNext());
    assertFalse(b.getContext().isExpanded());
    NodeChange.Added<Node> added1 = (NodeChange.Added<Node>) changes.next();
    assertSame(b, added1.getTarget());
    assertSame(null, added1.getPrevious());
    assertSame(a, added1.getParent());
    NodeChange.Added<Node> added2 = (NodeChange.Added<Node>) changes.next();
    assertSame(d, added2.getTarget());
    assertSame(c, added2.getPrevious());
    assertSame(root1, added2.getParent());
    assertFalse(changes.hasNext());
  }

  public void testTransientParent() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase_transient_parent");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase_transient_parent"));

    //
    sync(true);

    Node root = service.loadNode(Node.MODEL, navigation, Scope.CHILDREN, null).getNode();
    Node a = root.addChild("a");
    Node b = root.addChild("b"); // It is only failed if we add more than one transient node

    //
    service.rebaseNode(a.getContext(), Scope.CHILDREN, null);
  }

  public void testRemovedNavigation() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase_removed_navigation");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase_removed_navigation"));

    //
    sync(true);

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();
    service.destroyNavigation(navigation);

    //
    sync(true);

    //
    try {
      service.rebaseNode(root.getContext(), null, null);
    } catch (HierarchyException e) {
      assertSame(HierarchyError.UPDATE_CONCURRENTLY_REMOVED_NODE, e.getError());
    }
  }

  public void testStateRebase() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase_state");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase_state"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    sync(true);

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();

    NodeState state = new NodeState.Builder().label("foo").build();
    root.getChild("a").setState(state);
    assertSame(state, root.getChild("a").getState());

    //
    sync(true);

    //
    Iterator<NodeChange<Node>> changes = root.rebase(service, null);
    assertFalse(changes.hasNext());
    assertSame(state, root.getChild("a").getState());
  }

  public void testNameRebase() throws Exception {
    createNavigation(SiteType.PORTAL, "rebase_name");
    NavigationContext navigation = service.loadNavigation(SiteKey.portal("rebase_name"));
    NodeContext rootContext1 = service.loadNode(Node.MODEL, navigation, Scope.ALL, null);
    rootContext1.add(null, "a");
    service.saveNode(rootContext1, null);

    //
    sync(true);

    Node root = service.loadNode(Node.MODEL, navigation, Scope.ALL, null).getNode();

    Node a = root.getChild("a");
    a.setName("b");
    assertSame("b", a.getName());

    //
    sync(true);

    //
    Iterator<NodeChange<Node>> changes = root.rebase(service, null);
    assertFalse(changes.hasNext());
    assertSame("b", a.getName());
  }

  @Override
  protected NavigationService getNavigationService() {
    PortalContainer container = PortalContainer.getInstance();
    return (NavigationService) container.getComponentInstanceOfType(NavigationService.class);
  }
}
