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
package org.exoplatform.portal.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.url.PortalURLContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.URIWriter;
import org.exoplatform.web.url.URLFactoryService;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

public class DefaultRequestHandlerTest {

  private static final Log LOG = ExoLogger.getLogger(DefaultRequestHandlerTest.class);

  @Test
  public void testGetDefaultSite() {
    URLFactoryService urlFactory = mock(URLFactoryService.class);
    NodeURL url = mock(NodeURL.class);
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    ControllerContext context = mock(ControllerContext.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    try {
      String defaultSite = "site2";
      when(portalConfigService.getDefaultPortal()).thenReturn(defaultSite);
      when(portalConfigService.getAllPortalNames()).thenReturn(Arrays.asList("site1", defaultSite));
      when(context.getResponse()).thenReturn(response);
      when(context.getRequest()).thenReturn(request);

      doAnswer(invocation -> {
        @SuppressWarnings("unchecked")
        Map<QualifiedName, String> parameters = invocation.getArgumentAt(0, Map.class);
        URIWriter uriWriter = invocation.getArgumentAt(1, URIWriter.class);
        uriWriter.append("/portal/");
        uriWriter.append(parameters.get(NodeURL.REQUEST_SITE_NAME));
        return null;
      }).when(context).renderURL(any(), any());

      when(response.encodeRedirectURL(anyString())).thenAnswer(new Answer<String>() {
        public String answer(InvocationOnMock invocation) {
          return invocation.getArgumentAt(0, String.class);
        }
      });

      when(urlFactory.newURL(any(), any())).thenAnswer(new Answer<NodeURL>() {
        public NodeURL answer(InvocationOnMock invocation) {
          PortalURLContext urlContext = invocation.getArgumentAt(1, PortalURLContext.class);
          when(url.getContext()).thenReturn(urlContext);
          return url;
        }
      });

      when(url.setResource(any())).thenAnswer(new Answer<NodeURL>() {
        public NodeURL answer(InvocationOnMock invocation) {
          NavigationResource navigationResource = invocation.getArgumentAt(0, NavigationResource.class);
          when(url.getResource()).thenReturn(navigationResource);

          assertEquals("Site type on which user is redirected is not of type PORTAL",
                       SiteType.PORTAL,
                       navigationResource.getSiteType());
          assertEquals("Site name on which user is redirected is not coherent",
                       defaultSite,
                       navigationResource.getSiteName());
          return url;
        }
      });

      DefaultRequestHandler defaultRequestHandler = new DefaultRequestHandler(portalConfigService, urlFactory);
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect(eq("/portal/site2"));
    } catch (Exception e) {
      LOG.error("Error while executing method", e);
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetUserDefaultUri() {
    URLFactoryService urlFactory = mock(URLFactoryService.class);
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    ControllerContext context = mock(ControllerContext.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    try {
      String defaultSite = "site2";
      when(portalConfigService.getDefaultPortal()).thenReturn(defaultSite);
      when(portalConfigService.getAllPortalNames()).thenReturn(Arrays.asList("site1", defaultSite));
      when(context.getResponse()).thenReturn(response);
      when(context.getRequest()).thenReturn(request);
      when(request.getRemoteUser()).thenReturn("user");
      when(portalConfigService.getUserHomePage(eq("user"))).thenReturn("/portal/home");

      when(response.encodeRedirectURL(anyString())).thenAnswer(new Answer<String>() {
        public String answer(InvocationOnMock invocation) {
          return invocation.getArgumentAt(0, String.class);
        }
      });

      DefaultRequestHandler defaultRequestHandler = new DefaultRequestHandler(portalConfigService, urlFactory);
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect(eq("/portal/home"));
    } catch (Exception e) {
      LOG.error("Error while executing method", e);
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetNonDefaultSite() {
    URLFactoryService urlFactory = mock(URLFactoryService.class);
    NodeURL url = mock(NodeURL.class);
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    ControllerContext context = mock(ControllerContext.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    try {
      String defaultSite = "site2";
      when(portalConfigService.getDefaultPortal()).thenReturn(defaultSite);
      when(portalConfigService.getAllPortalNames()).thenReturn(Arrays.asList("site1"));
      when(context.getResponse()).thenReturn(response);
      when(context.getRequest()).thenReturn(request);

      doAnswer(invocation -> {
        @SuppressWarnings("unchecked")
        Map<QualifiedName, String> parameters = invocation.getArgumentAt(0, Map.class);
        URIWriter uriWriter = invocation.getArgumentAt(1, URIWriter.class);
        uriWriter.append("/portal/");
        uriWriter.append(parameters.get(NodeURL.REQUEST_SITE_NAME));
        return null;
      }).when(context).renderURL(any(), any());

      when(response.encodeRedirectURL(anyString())).thenAnswer(new Answer<String>() {
        public String answer(InvocationOnMock invocation) {
          return invocation.getArgumentAt(0, String.class);
        }
      });

      when(urlFactory.newURL(any(), any())).thenAnswer(new Answer<NodeURL>() {
        public NodeURL answer(InvocationOnMock invocation) {
          PortalURLContext urlContext = invocation.getArgumentAt(1, PortalURLContext.class);
          when(url.getContext()).thenReturn(urlContext);
          return url;
        }
      });

      when(url.setResource(any())).thenAnswer(new Answer<NodeURL>() {
        public NodeURL answer(InvocationOnMock invocation) {
          NavigationResource navigationResource = invocation.getArgumentAt(0, NavigationResource.class);
          when(url.getResource()).thenReturn(navigationResource);

          assertEquals("Site type on which user is redirected is not of type PORTAL",
                       SiteType.PORTAL,
                       navigationResource.getSiteType());
          assertEquals("Site name on which user is redirected is not coherent",
                       "site1",
                       navigationResource.getSiteName());
          return url;
        }
      });

      DefaultRequestHandler defaultRequestHandler = new DefaultRequestHandler(portalConfigService, urlFactory);
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect(eq("/portal/site1"));
    } catch (Exception e) {
      LOG.error("Error while executing method", e);
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetLoginPageWhenNoSite() {
    URLFactoryService urlFactory = mock(URLFactoryService.class);
    UserPortalConfigService portalConfigService = mock(UserPortalConfigService.class);
    ControllerContext context = mock(ControllerContext.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpServletRequest request = mock(HttpServletRequest.class);

    try {
      String defaultSite = "site2";
      when(portalConfigService.getDefaultPortal()).thenReturn(defaultSite);
      when(portalConfigService.getAllPortalNames()).thenReturn(Arrays.asList());
      when(context.getResponse()).thenReturn(response);
      when(context.getRequest()).thenReturn(request);

      when(response.encodeRedirectURL(anyString())).thenAnswer(new Answer<String>() {
        public String answer(InvocationOnMock invocation) {
          return invocation.getArgumentAt(0, String.class);
        }
      });

      DefaultRequestHandler defaultRequestHandler = new DefaultRequestHandler(portalConfigService, urlFactory);
      defaultRequestHandler.execute(context);
      verify(response).sendRedirect(eq("/portal/login"));
    } catch (Exception e) {
      LOG.error("Error while executing method", e);
      fail(e.getMessage());
    }
  }

}
