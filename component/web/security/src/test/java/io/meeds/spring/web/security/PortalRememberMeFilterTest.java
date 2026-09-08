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
package io.meeds.spring.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.jaas.UserPrincipal;
import org.exoplatform.services.security.web.HttpSessionStateKey;
import org.exoplatform.web.login.LoginUtils;
import org.exoplatform.web.security.security.CookieTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Pins the rememberme flow of a Spring WAR: a validated token authenticates the
 * user as a pre-authenticated principal on the Spring Security side, without
 * any {@link HttpServletRequest#login(String, String)} call (which Spring
 * Security routes to the AuthenticationManager as a
 * UsernamePasswordAuthenticationToken nobody supports since Spring Boot 4.1).
 */
@ExtendWith(MockitoExtension.class)
class PortalRememberMeFilterTest {

  private static final String       USERNAME = "john";

  private static final String       TOKEN    = "rememberme-token";

  @Mock
  private AuthenticationProvider    authenticationProvider;

  @Mock
  private SecurityContextRepository securityContextRepository;

  @Mock
  private HttpServletRequest        request;

  @Mock
  private HttpServletResponse       response;

  @Mock
  private HttpSession               session;

  @Mock
  private FilterChain               chain;

  @Mock
  private ExoContainer              container;

  @Mock
  private CookieTokenService        cookieTokenService;

  @Mock
  private IdentityRegistry          identityRegistry;

  @Mock
  private ConversationRegistry      conversationRegistry;

  private PortalRememberMeFilter    filter;

  @BeforeEach
  void setUp() {
    filter = new PortalRememberMeFilter(authenticationProvider, securityContextRepository);
    ExoContainerContext.setCurrentContainer(container);
    lenient().when(container.getComponentInstanceOfType(CookieTokenService.class)).thenReturn(cookieTokenService);
    lenient().when(container.getComponentInstanceOfType(IdentityRegistry.class)).thenReturn(identityRegistry);
    lenient().when(container.getComponentInstanceOfType(ConversationRegistry.class)).thenReturn(conversationRegistry);
    lenient().when(request.getRemoteUser()).thenReturn(null);
    lenient().when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(LoginUtils.COOKIE_NAME, TOKEN) });
    lenient().when(request.getSession()).thenReturn(session);
    lenient().when(session.getId()).thenReturn("session-id");
    lenient().when(cookieTokenService.validateToken(TOKEN, false)).thenReturn(USERNAME);
    lenient().when(identityRegistry.getIdentity(USERNAME)).thenReturn(new Identity(USERNAME));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    ConversationState.setCurrent(null);
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  void validTokenAuthenticatesUserAsPreAuthenticatedPrincipalAndSavesContext() throws Exception {
    Authentication authenticated = authenticatedUser();
    when(authenticationProvider.authenticate(any())).thenReturn(authenticated);

    filter.doFilter(request, response, chain);

    ArgumentCaptor<Authentication> requested = ArgumentCaptor.forClass(Authentication.class);
    verify(authenticationProvider).authenticate(requested.capture());
    assertEquals(PreAuthenticatedAuthenticationToken.class, requested.getValue().getClass());
    assertEquals(USERNAME, requested.getValue().getPrincipal());
    assertFalse(requested.getValue().isAuthenticated());

    assertSame(authenticated, SecurityContextHolder.getContext().getAuthentication());
    ArgumentCaptor<SecurityContext> saved = ArgumentCaptor.forClass(SecurityContext.class);
    verify(securityContextRepository).saveContext(saved.capture(), any(HttpServletRequest.class), any(HttpServletResponse.class));
    assertSame(authenticated, saved.getValue().getAuthentication());

    verify(conversationRegistry).register(any(HttpSessionStateKey.class), any(ConversationState.class));
    assertNotNull(ConversationState.getCurrent());
    assertEquals(USERNAME, ConversationState.getCurrent().getIdentity().getUserId());

    verify(request, never()).login(any(), any());
    verify(response, never()).addCookie(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void savedContextIsReadBackFromHttpSessionOnNextRequest() throws Exception {
    // Public constructor: the real session-backed repository, executed
    Authentication authenticated = authenticatedUser();
    when(authenticationProvider.authenticate(any())).thenReturn(authenticated);
    PortalRememberMeFilter realRepositoryFilter = new PortalRememberMeFilter(authenticationProvider);
    MockHttpSession httpSession = new MockHttpSession();
    MockHttpServletRequest firstRequest = new MockHttpServletRequest();
    firstRequest.setSession(httpSession);
    firstRequest.setCookies(new Cookie(LoginUtils.COOKIE_NAME, TOKEN));

    realRepositoryFilter.doFilter(firstRequest, new MockHttpServletResponse(), chain);

    Object attribute = httpSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    assertNotNull(attribute);
    assertSame(authenticated, ((SecurityContext) attribute).getAuthentication());

    MockHttpServletRequest secondRequest = new MockHttpServletRequest();
    secondRequest.setSession(httpSession);
    Authentication readBack = new HttpSessionSecurityContextRepository().loadDeferredContext(secondRequest)
                                                                        .get()
                                                                        .getAuthentication();
    assertSame(authenticated, readBack);
  }

  @Test
  void anonymousResultLeavesRequestUnauthenticatedAndKeepsToken() throws Exception {
    when(authenticationProvider.authenticate(any())).thenReturn(anonymous());

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(securityContextRepository, never()).saveContext(any(), any(), any());
    verify(conversationRegistry, never()).register(any(), any());
    assertNull(ConversationState.getCurrent());
    verify(response, never()).addCookie(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void providerFailureClearsTokenCookieAndContinuesChain() throws Exception {
    when(authenticationProvider.authenticate(any())).thenThrow(new AuthenticationServiceException("failure"));

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(securityContextRepository, never()).saveContext(any(), any(), any());
    verify(response).addCookie(argThat(cookie -> LoginUtils.COOKIE_NAME.equals(cookie.getName())
                                                 && cookie.getMaxAge() == 0
                                                 && "".equals(cookie.getValue())));
    verify(chain).doFilter(request, response);
  }

  @Test
  void invalidTokenIsNotAuthenticated() throws Exception {
    when(cookieTokenService.validateToken(TOKEN, false)).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(authenticationProvider, never()).authenticate(any());
    verify(response, never()).addCookie(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void requestWithoutCookieIsNotAuthenticated() throws Exception {
    when(request.getCookies()).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(cookieTokenService, never()).validateToken(any(), any(Boolean.class));
    verify(authenticationProvider, never()).authenticate(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void alreadyAuthenticatedRequestIsLeftUntouched() throws Exception {
    when(request.getRemoteUser()).thenReturn(USERNAME);

    filter.doFilter(request, response, chain);

    verify(cookieTokenService, never()).validateToken(any(), any(Boolean.class));
    verify(authenticationProvider, never()).authenticate(any());
    verify(chain).doFilter(request, response);
  }

  private Authentication authenticatedUser() {
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("users"));
    return new PreAuthenticatedAuthenticationToken(new UserPrincipal(USERNAME), USERNAME, authorities);
  }

  private Authentication anonymous() {
    return new AnonymousAuthenticationToken(IdentityConstants.ANONIM,
                                            IdentityConstants.ANONIM,
                                            List.of(new SimpleGrantedAuthority("guests")));
  }

}
