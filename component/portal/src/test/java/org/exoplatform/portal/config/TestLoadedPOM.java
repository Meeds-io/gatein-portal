/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.portal.config;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.PageStorage;

/**
 * Created by The eXo Platform SARL Author : Tung Pham thanhtungty@gmail.com Nov
 * 13, 2007
 */
public class TestLoadedPOM extends AbstractConfigTest {

  /** . */
  private LayoutService           layoutService;

  /** . */
  private PageStorage             pageStorage;

  /** . */
  private NavigationService       navService;

  public void setUp() throws Exception {
    super.setUp();
    PortalContainer container = getContainer();
    layoutService = container.getComponentInstanceOfType(LayoutService.class);
    pageStorage = container.getComponentInstanceOfType(PageStorage.class);
    navService = container.getComponentInstanceOfType(NavigationService.class);
  }

  public void testLegacyGroupWithNormalizedName() throws Exception {
    SiteKey key = SiteKey.group("/test/legacy");
    NavigationContext nav = navService.loadNavigation(key);
    assertNotNull(nav);
    NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    NodeContext<?> node = root.get(0);
    assertEquals(SiteKey.group("/test/legacy").page("register"), node.getState().getPageRef());

    Page page = layoutService.getPage("group::/test/legacy::register");
    assertNotNull(page);
    assertEquals("group::/test/legacy::register", page.getPageId());
    assertEquals("/test/legacy", page.getOwnerId());
  }

  public void testGroupWithNormalizedName() throws Exception {
    SiteKey key = SiteKey.group("/test/normalized");
    NavigationContext nav = navService.loadNavigation(key);
    assertNotNull(nav);
    NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    NodeContext<?> node = root.get(0);
    assertEquals(SiteKey.group("/test/normalized").page("register"), node.getState().getPageRef());

    Page page = layoutService.getPage("group::/test/normalized::register");
    assertNotNull(page);
    assertEquals("group::/test/normalized::register", page.getPageId());
    assertEquals("/test/normalized", page.getOwnerId());
  }

  public void testNavigation() throws Exception {
    SiteKey key = SiteKey.portal("test");
    NavigationContext nav = navService.loadNavigation(key);
    assertNotNull(nav);

    //
    assertEquals(1, (int) nav.getState().getPriority());

    //
    NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    assertEquals(5, root.getNodeCount());

    //
    NodeContext<?> nodeNavigation = root.get(0);
    assertEquals(0, nodeNavigation.getNodeCount());
    assertEquals("node_name", nodeNavigation.getName());
    assertEquals("node_label", nodeNavigation.getState().getLabel());
    assertEquals("node_icon", nodeNavigation.getState().getIcon());
    GregorianCalendar start = new GregorianCalendar(2000, 2, 21, 1, 33, 0);
    start.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals(start.getTime().getTime(), nodeNavigation.getState().getStartPublicationTime());
    GregorianCalendar end = new GregorianCalendar(2009, 2, 21, 1, 33, 0);
    end.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals(end.getTime().getTime(), nodeNavigation.getState().getEndPublicationTime());
    assertEquals(Visibility.TEMPORAL, nodeNavigation.getState().getVisibility());
  }

  public void testLoadNode() throws Exception {
    NodeContext<?> root = navService.loadNode(SiteKey.portal("test"));
    assertEquals(5, root.getNodeCount());

    NodeContext<?> nodeNavigation = navService.loadNode(SiteKey.portal("test"), "node_name");
    assertEquals(0, nodeNavigation.getNodeCount());
    assertEquals("node_name", nodeNavigation.getName());
    assertEquals("node_label", nodeNavigation.getState().getLabel());
    assertEquals("node_icon", nodeNavigation.getState().getIcon());
    GregorianCalendar start = new GregorianCalendar(2000, 2, 21, 1, 33, 0);
    start.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals(start.getTime().getTime(), nodeNavigation.getState().getStartPublicationTime());
    GregorianCalendar end = new GregorianCalendar(2009, 2, 21, 1, 33, 0);
    end.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals(end.getTime().getTime(), nodeNavigation.getState().getEndPublicationTime());
    assertEquals(Visibility.TEMPORAL, nodeNavigation.getState().getVisibility());
  }

  public void testPortal() throws Exception {
    PortalConfig portal = layoutService.getPortalConfig("test");
    assertNotNull(portal);

    assertEquals("test", portal.getName());
    assertEquals("en", portal.getLocale());
    assertArrayEquals(new String[] { "test_portal_access_permissions" }, portal.getAccessPermissions());
    assertEquals("test_edit_permission", portal.getEditPermission());
    assertEquals("test_skin", portal.getSkin());
    assertEquals("test_prop_value", portal.getProperty("prop_key"));
  }

  public void testPageWithoutPageId() throws Exception {
    Page page = layoutService.getPage("portal::test::test2");
    assertNotNull(page);
    assertEquals("portal::test::test2", page.getPageId());
    assertEquals("test", page.getOwnerId());
    assertEquals("portal", page.getOwnerType());
    assertEquals("test2", page.getName());
  }

  public void testPage() throws Exception {
    Page page = layoutService.getPage("portal::test::test1");
    assertNotNull(page);

    PageContext pageContext = pageStorage.loadPage(page.getPageKey());
    assertNotNull(pageContext);

    //
    assertEquals("test_title", pageContext.getState().getDisplayName());
    assertEquals("test_factory_id", pageContext.getState().getFactoryId());
    assertEquals(Arrays.<String> asList("test_access_permissions"), pageContext.getState().getAccessPermissions());
    assertEquals("test_edit_permission", pageContext.getState().getEditPermission());
    assertEquals(true, pageContext.getState().getShowMaxWindow());

    //
    List<ModelObject> children = page.getChildren();
    assertEquals(2, children.size());

    //
    Container container1 = (Container) children.get(0);
    assertEquals("container_1", container1.getName());
    assertEquals("container_1_title", container1.getTitle());
    assertEquals("container_1_icon", container1.getIcon());
    assertEquals("container_1_template", container1.getTemplate());
    assertTrue(Arrays.equals(new String[] { "container_1_access_permissions" }, container1.getAccessPermissions()));
    assertEquals("container_1_factory_id", container1.getFactoryId());
    assertEquals("container_1_description", container1.getDescription());
    assertEquals("container_1_width", container1.getWidth());
    assertEquals("container_1_height", container1.getHeight());

    //
    Application application1 = (Application) children.get(1);
    assertEquals("application_1_theme", application1.getTheme());
    assertEquals("application_1_title", application1.getTitle());
    assertTrue(Arrays.equals(new String[] { "application_1_access_permissions" }, application1.getAccessPermissions()));
    assertEquals(true, application1.getShowInfoBar());
    assertEquals(true, application1.getShowApplicationState());
    assertEquals(true, application1.getShowApplicationMode());
    assertEquals("application_1_description", application1.getDescription());
    assertEquals("application_1_icon", application1.getIcon());
    assertEquals("application_1_width", application1.getWidth());
    assertEquals("application_1_height", application1.getHeight());
    // assertEquals("portal#test:/web/BannerPortlet/banner",
    // application1.getInstanceState().getWeakReference());
  }
}
