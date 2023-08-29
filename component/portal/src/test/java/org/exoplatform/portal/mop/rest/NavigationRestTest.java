/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.portal.mop.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.rest.model.SiteRestEntity;
import org.exoplatform.portal.mop.rest.model.UserNodeRestEntity;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.portal.config.NavigationCategoryService;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.rest.NavigationRest.ResultUserNavigation;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.mop.user.UserPortalImpl;
import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.test.mock.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class NavigationRestTest extends BaseRestServicesTestCase {

  @Mock
  private UserPortalConfigService   portalConfigService;

  @Mock
  private NavigationCategoryService navigationCategoryService;

  @Mock
  private LayoutService             layoutService;

  @Mock
  private OrganizationService       organizationService;

  @Mock
  private UserACL                   userACL;

  protected Class<?> getComponentClass() {
    return NavigationRest.class;
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    binder.addResource(new NavigationRest(portalConfigService, navigationCategoryService, layoutService, organizationService, userACL), null);
  }

  @Override
  protected void registry(Class<?> resourceClass) throws Exception {
    // Nothing to do
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetSiteNavigation() throws Exception {
    String path = "/v1/navigations/";
    String username = "testuser";
    String defaultPortalName = "default";
    String globalPortalName = "global";
    String portalName = "test";

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    UserPortalConfig userPortalConfig = mock(UserPortalConfig.class);
    UserPortalImpl userPortal = mock(UserPortalImpl.class);
    NavigationState navigationState = mock(NavigationState.class);
    List<UserNavigation> userNavigations = Arrays.asList(
                                                         new UserNavigation(userPortal,
                                                                            new NavigationContext(SiteKey.portal(globalPortalName),
                                                                                                  navigationState),
                                                                            false),
                                                         new UserNavigation(userPortal,
                                                                            new NavigationContext(SiteKey.portal(portalName),
                                                                                                  navigationState),
                                                                            false));

    startUserSession(username);
    when(portalConfigService.getDefaultPortal()).thenReturn(defaultPortalName);
    when(portalConfigService.getGlobalPortal()).thenReturn(globalPortalName);
    when(portalConfigService.getUserPortalConfig(eq(defaultPortalName), eq(username), any())).thenReturn(userPortalConfig);
    when(userPortalConfig.getUserPortal()).thenReturn(userPortal);
    when(userPortal.getNavigations()).thenReturn(userNavigations);

    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);

    assertEquals(200, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof List);
    List<ResultUserNavigation> navigations = (List<ResultUserNavigation>) entity;
    assertEquals(1, navigations.size());
    assertEquals(portalName, navigations.get(0).getKey().getName());
    assertEquals(SiteType.PORTAL, navigations.get(0).getKey().getType());

    when(userPortal.getNavigations()).thenThrow(IllegalStateException.class);
    resp = launcher.service("GET", path, "", null, null, envctx);
    assertEquals(500, resp.getStatus());
  }

  @Test
  public void testGetNavigationCategories() throws Exception {
    String path = "/v1/navigations/categories";

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    TreeMap<String, String> navs = new TreeMap<>();
    navs.put("test", "value");
    Map<String, Integer> categoriesOrder = new HashMap<>();
    categoriesOrder.put("test", 12);
    Map<String, Integer> urisOrder = new HashMap<>();
    urisOrder.put("test", 15);

    when(navigationCategoryService.getNavigationCategories()).thenReturn(navs);
    when(navigationCategoryService.getNavigationCategoriesOrder()).thenReturn(categoriesOrder);
    when(navigationCategoryService.getNavigationUriOrder()).thenReturn(urisOrder);

    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);

    assertEquals(200, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof String);
    JSONObject jsonObject = new JSONObject(entity.toString());
    assertEquals(200, resp.getStatus());
    assertEquals(navs.get("test"), jsonObject.getJSONObject("navs").get("test"));
    assertEquals(categoriesOrder.get("test"), jsonObject.getJSONObject("categoriesOrder").get("test"));
    assertEquals(urisOrder.get("test"), jsonObject.getJSONObject("urisOrder").get("test"));
  }
  
  @Test
  public void testGetSiteNavigationWithPageDetails() throws Exception {
    String path = "/v1/navigations/PORTAL?siteName=SiteName&expand=true";

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    startUserSession("root1");
    Collection<UserNode> nodes = new ArrayList<>();
    Page nodePage = mock(Page.class);
    UserNode userNode = mock(UserNode.class);
    nodes.add(userNode);
    UserPortalConfig userPortalConfig = mock(UserPortalConfig.class);
    UserPortal userPortal = mock(UserPortal.class);
    GroupHandler groupHandler = mock(GroupHandler.class);
    Group group = mock(Group.class);
    when(organizationService.getGroupHandler()).thenReturn(groupHandler);
    when(groupHandler.findGroupById("/platform/users")).thenReturn(group);
    PageKey pageKey = PageKey.parse("portal::page::ref");
    when(layoutService.getPage(pageKey)).thenReturn(nodePage);
    when(nodePage.getEditPermission()).thenReturn("*:/platform/users");
    when(nodePage.getAccessPermissions()).thenReturn(new String[] {"*:/platform/users"});
    when(nodePage.getType()).thenReturn("LINK");
    when(nodePage.getLink()).thenReturn("www.test.com");
    when(userNode.getPageRef()).thenReturn(pageKey);
    when(portalConfigService.getUserPortalConfig(anyString(), anyString(), any())).thenReturn(userPortalConfig);
    when(userPortalConfig.getUserPortal()).thenReturn(userPortal);
    when(userPortal.getNodes(any(SiteType.class) , any(Scope.class), any(UserNodeFilterConfig.class),anyBoolean())).thenReturn(nodes);
    when(userACL.hasEditPermission(any(Page.class))).thenReturn(true);
    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);
    Object entity = resp.getEntity();
    
    assertEquals(200, resp.getStatus());
    assertNotNull(entity);
    List<UserNodeRestEntity> resultUserNodes = (List<UserNodeRestEntity>) resp.getEntity();
    assertEquals(1, resultUserNodes.size());
    assertEquals("*", resultUserNodes.get(0).getPageEditPermission().get("membershipType"));
    assertEquals(group, resultUserNodes.get(0).getPageEditPermission().get("group"));
    assertEquals(1, resultUserNodes.get(0).getPageAccessPermissions().size());
    assertEquals("*", resultUserNodes.get(0).getPageAccessPermissions().get(0).get("membershipType"));
    assertEquals(group, resultUserNodes.get(0).getPageAccessPermissions().get(0).get("group"));

    when(nodePage.getEditPermission()).thenReturn("manager:/platform/users");
    when(nodePage.getAccessPermissions()).thenReturn(new String[] {"Everyone"});

    resp = launcher.service("GET", path, "", null, null, envctx);
    entity = resp.getEntity();
    
    assertEquals(200, resp.getStatus());
    assertNotNull(entity);
    resultUserNodes = (List<UserNodeRestEntity>) resp.getEntity();
    assertEquals(1, resultUserNodes.size());
    assertEquals("manager", resultUserNodes.get(0).getPageEditPermission().get("membershipType"));
    assertEquals(group, resultUserNodes.get(0).getPageEditPermission().get("group"));
    assertEquals(1, resultUserNodes.get(0).getPageAccessPermissions().size());
    assertEquals("Everyone", resultUserNodes.get(0).getPageAccessPermissions().get(0).get("membershipType"));
    assertEquals(null, resultUserNodes.get(0).getPageAccessPermissions().get(0).get("group"));
    assertEquals("www.test.com", resultUserNodes.get(0).getPageLink());
  }

  @Test
  public void testGetSitesWithNavigationNodes() throws Exception {
    String path = "/v1/navigations/sites";

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(path, null, 0, "GET", null);
    envctx.put(HttpServletRequest.class, httpRequest);

    startUserSession("root1");
    PortalConfig site1 = mock(PortalConfig.class);
    when(site1.getType()).thenReturn(SiteType.PORTAL.getName());
    when(site1.getName()).thenReturn("site1");
    when(site1.getDisplayOrder()).thenReturn(1);
    when(site1.getEditPermission()).thenReturn("*:/platform/users");
    when(site1.getAccessPermissions()).thenReturn(new String[] {"*:/platform/users"});
    PortalConfig site2 = mock(PortalConfig.class);
    when(site2.getType()).thenReturn(SiteType.PORTAL.getName());
    when(site2.getName()).thenReturn("site2");
    when(site2.getDisplayOrder()).thenReturn(2);
    when(site2.getEditPermission()).thenReturn("*:/platform/users");
    when(site2.getAccessPermissions()).thenReturn(new String[] {"*:/platform/users"});
    List<PortalConfig> sites = new ArrayList<>();
    sites.add(site1);
    sites.add(site2);
    when(layoutService.getUserPortalSitesOrderedByDisplayOrder()).thenReturn(sites);

    Collection<UserNode> nodes = new ArrayList<>();
    UserNode userRootNode = mock(UserNode.class);
    UserNode userNode = mock(UserNode.class);
    nodes.add(userNode);

    GroupHandler groupHandler = mock(GroupHandler.class);
    Group group = mock(Group.class);
    when(organizationService.getGroupHandler()).thenReturn(groupHandler);
    when(groupHandler.findGroupById("/platform/users")).thenReturn(group);
    when(userNode.getPageRef()).thenReturn(null);
    UserPortalConfig userPortalConfig = mock(UserPortalConfig.class);
    UserNavigation userNavigation = mock(UserNavigation.class);
    when(portalConfigService.getUserPortalConfig(anyString(), anyString(), any())).thenReturn(userPortalConfig);
    UserPortal userPortal = mock(UserPortal.class);
    when(userPortalConfig.getUserPortal()).thenReturn(userPortal);
    when(userPortal.getNavigation(any(SiteKey.class))).thenReturn(userNavigation);
    when(userPortal.getNode(any(UserNavigation.class), any(Scope.class), any(UserNodeFilterConfig.class),any())).thenReturn(userRootNode);
    when(userRootNode.getChildren()).thenReturn(nodes);
    ContainerResponse resp = launcher.service("GET", path, "", null, null, envctx);
    Object entity = resp.getEntity();

    assertEquals(200, resp.getStatus());
    assertNotNull(entity);
    List<SiteRestEntity> results = (List<SiteRestEntity>) resp.getEntity();
    assertEquals(2, results.size());
    assertEquals(1, results.get(0).getDisplayOrder());
    assertEquals(2, results.get(1).getDisplayOrder());


  }
}
