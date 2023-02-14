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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

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

  protected Class<?> getComponentClass() {
    return NavigationRest.class;
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    binder.addResource(new NavigationRest(portalConfigService, navigationCategoryService), null);
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

}
