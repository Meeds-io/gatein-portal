/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.portal.config;

import static org.junit.Assert.assertArrayEquals;

import java.util.*;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageService;

/**
 * Created by The eXo Platform SARL Author : Tung Pham thanhtungty@gmail.com Nov
 * 13, 2007
 */
public class TestLoadedPOM extends AbstractConfigTest {

  /** . */
  private UserPortalConfigService portalConfigService;

  /** . */
  private DataStorage             storage;

  /** . */
  private PageService             pageService;

  /** . */
  private NavigationService       navService;

  public void setUp() throws Exception {
    super.setUp();
    PortalContainer container = getContainer();
    portalConfigService = container.getComponentInstanceOfType(UserPortalConfigService.class);
    storage = container.getComponentInstanceOfType(DataStorage.class);
    pageService = container.getComponentInstanceOfType(PageService.class);
    navService = container.getComponentInstanceOfType(NavigationService.class);
  }

  public void testLegacyGroupWithNormalizedName() throws Exception {
    SiteKey key = SiteKey.group("/test/legacy");
    NavigationContext nav = navService.loadNavigation(key);
    assertNotNull(nav);
    NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    NodeContext<?> node = root.get(0);
    assertEquals(SiteKey.group("/test/legacy").page("register"), node.getState().getPageRef());

    Page page = storage.getPage("group::/test/legacy::register");
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

    Page page = storage.getPage("group::/test/normalized::register");
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

  public void testPortal() throws Exception {
    PortalConfig portal = storage.getPortalConfig("test");
    assertNotNull(portal);

    assertEquals("test", portal.getName());
    assertEquals("en", portal.getLocale());
    assertArrayEquals(new String[] { "test_portal_access_permissions" }, portal.getAccessPermissions());
    assertEquals("test_edit_permission", portal.getEditPermission());
    assertEquals("test_skin", portal.getSkin());
    assertEquals("test_prop_value", portal.getProperty("prop_key"));
  }

  public void testPageWithoutPageId() throws Exception {
    Page page = storage.getPage("portal::test::test2");
    assertNotNull(page);
    assertEquals("portal::test::test2", page.getPageId());
    assertEquals("test", page.getOwnerId());
    assertEquals("portal", page.getOwnerType());
    assertEquals("test2", page.getName());
  }

  public void testPage() throws Exception {
    Page page = storage.getPage("portal::test::test1");
    assertNotNull(page);

    PageContext pageContext = pageService.loadPage(page.getPageKey());
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
