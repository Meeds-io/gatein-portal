/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.web.login;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.gatein.wci.ServletContainer;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.authentication.AuthenticationException;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.web.security.security.CookieTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * First harness for the legacy portal remember-me filter, which had none. It
 * pins the semantics this filter shares with its Spring counterpart
 * {@code io.meeds.spring.web.security.PortalRememberMeFilter}: the rememberme
 * cookie is written at path "/" and is therefore the *same* cookie on both
 * paths, so neither may delete it because a login failed — the token was
 * validated moments before, and a rejected JAAS login arrives here as a WCI
 * {@link AuthenticationException} wrapping the container's own failure.
 */
@ExtendWith(MockitoExtension.class)
class RememberMeFilterTest {

  private static final String              USERNAME = "john";

  private static final String              TOKEN    = "rememberme-token";

  @Mock
  private ExoContainer                     container;

  @Mock
  private CookieTokenService               cookieTokenService;

  @Mock
  private ServletContainer                 servletContainer;

  @Mock
  private FilterConfig                     filterConfig;

  @Mock
  private ServletContext                   servletContext;

  @Mock
  private HttpServletRequest               request;

  @Mock
  private HttpServletResponse              response;

  @Mock
  private FilterChain                       chain;

  private MockedStatic<ServletContainerFactory> servletContainerFactory;

  private RememberMeFilter                 filter;

  @BeforeEach
  void setUp() throws Exception {
    ExoContainerContext.setCurrentContainer(container);
    lenient().when(filterConfig.getServletContext()).thenReturn(servletContext);
    lenient().when(servletContext.getServletContextName()).thenReturn("portal");
    lenient().when(filterConfig.getInitParameter("ignoredPaths")).thenReturn("/ignored");
    lenient().when(container.getComponentInstanceOfType(CookieTokenService.class)).thenReturn(cookieTokenService);
    lenient().when(request.getRemoteUser()).thenReturn(null);
    lenient().when(request.getServletPath()).thenReturn("/intranet");
    lenient().when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(LoginUtils.COOKIE_NAME, TOKEN) });
    lenient().when(cookieTokenService.validateToken(TOKEN, false)).thenReturn(USERNAME);
    servletContainerFactory = mockStatic(ServletContainerFactory.class);
    servletContainerFactory.when(ServletContainerFactory::getServletContainer).thenReturn(servletContainer);
    filter = new RememberMeFilter();
    filter.init(filterConfig);
  }

  @AfterEach
  void tearDown() {
    servletContainerFactory.close();
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  void unmarkedRejectionKeepsTheTokenCookie() throws Exception {
    // A rejection the chain did not mark is indistinguishable from an outage,
    // so the token survives it.
    doThrow(new AuthenticationException("rejected")).when(servletContainer).login(any(), any(), any());

    filter.doFilter(request, response, chain);

    verify(response, never()).addCookie(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void disabledUserRejectionClearsTheTokenCookie() throws Exception {
    // The one permanent rejection this chain marks: FilterDisabledLoginModule
    // sets the attribute before throwing, so the browser must stop re-running
    // the whole JAAS chain on every request until the cookie expires.
    doThrow(new AuthenticationException("disabled")).when(servletContainer).login(any(), any(), any());
    when(request.getAttribute(FilterDisabledLoginModule.DISABLED_USER_NAME)).thenReturn(USERNAME);

    filter.doFilter(request, response, chain);

    verify(response).addCookie(argThat(cookie -> LoginUtils.COOKIE_NAME.equals(cookie.getName())
                                                 && cookie.getMaxAge() == 0
                                                 && "".equals(cookie.getValue())
                                                 && "/".equals(cookie.getPath())));
    verify(chain).doFilter(request, response);
  }

  @Test
  void internalFailureKeepsTheTokenCookie() throws Exception {
    doThrow(new IllegalStateException("IDM unreachable")).when(servletContainer).login(any(), any(), any());

    filter.doFilter(request, response, chain);

    verify(response, never()).addCookie(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void validTokenIsSubmittedToTheContainerOnce() throws Exception {
    filter.doFilter(request, response, chain);

    verify(servletContainer).login(any(), any(), any());
    verify(response, never()).addCookie(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void invalidTokenIsNeverSubmittedToTheContainer() throws Exception {
    when(cookieTokenService.validateToken(TOKEN, false)).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(servletContainer, never()).login(any(), any(), any());
    verify(response, never()).addCookie(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void alreadyAuthenticatedRequestIsLeftUntouched() throws Exception {
    when(request.getRemoteUser()).thenReturn(USERNAME);

    filter.doFilter(request, response, chain);

    verify(cookieTokenService, never()).validateToken(any(), any(Boolean.class));
    verify(servletContainer, never()).login(any(), any(), any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void ignoredPathSkipsTheTokenEntirely() throws Exception {
    when(request.getServletPath()).thenReturn("/ignored/health");

    filter.doFilter(request, response, chain);

    verify(cookieTokenService, never()).validateToken(any(), any(Boolean.class));
    verify(servletContainer, never()).login(any(), any(), any());
    verify(chain).doFilter(request, response);
  }

}
