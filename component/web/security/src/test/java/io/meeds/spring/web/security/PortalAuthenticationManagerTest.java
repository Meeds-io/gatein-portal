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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.security.StateKey;
import org.exoplatform.services.security.jaas.UserPrincipal;

/**
 * Pins the contract of the platform's only AuthenticationProvider — the one
 * every Spring WAR's authentication rests on, and the one
 * {@link PortalRememberMeFilter} now calls directly since it can no longer go
 * through {@code request.login()}. Nothing exercised it before: the narrowing
 * of {@link PortalAuthenticationManager#supports(Class)} shipped green and
 * broke remember-me on every Spring WAR.
 */
@ExtendWith(MockitoExtension.class)
class PortalAuthenticationManagerTest {

  private static final String               USERNAME = "john";

  @Mock
  private Authenticator                     authenticator;

  @Mock
  private IdentityRegistry                  identityRegistry;

  @Mock
  private ConversationRegistry              conversationRegistry;

  @Mock
  private OrganizationService               organizationService;

  @Mock
  private UserHandler                       userHandler;

  @Mock
  private User                              user;

  private MockedStatic<ExoContainerContext> containerContext;

  private PortalAuthenticationManager       authenticationManager;

  @BeforeEach
  void setUp() {
    resetStaticServiceCaches();
    authenticationManager = new PortalAuthenticationManager();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
  }

  @AfterEach
  void tearDown() {
    if (containerContext != null) {
      containerContext.close();
      containerContext = null;
    }
    RequestContextHolder.resetRequestAttributes();
    ConversationState.setCurrent(null);
    resetStaticServiceCaches();
  }

  @Test
  void supportsPreAuthenticatedAndAnonymousTokensOnly() {
    // The load-bearing reason a Spring-side filter cannot authenticate through
    // HttpServletRequest.login(): the Spring Security request wrapper submits a
    // UsernamePasswordAuthenticationToken, which no provider of the platform
    // accepts. Widening this would authenticate any username with no credential.
    assertTrue(authenticationManager.supports(PreAuthenticatedAuthenticationToken.class));
    assertTrue(authenticationManager.supports(AnonymousAuthenticationToken.class));
    assertFalse(authenticationManager.supports(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void authenticatesTheStringPrincipalOfAnUnauthenticatedRequest() throws Exception {
    // The contract PortalRememberMeFilter depends on: no ConversationState and
    // no remote user on the request, so the identity is resolved from the
    // token's String principal alone.
    givenServices();
    when(identityRegistry.getIdentity(USERNAME)).thenReturn(platformUserIdentity());
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.findUserByName(USERNAME, UserStatus.ENABLED)).thenReturn(user);

    Authentication authentication = authenticate();

    assertInstanceOf(PreAuthenticatedAuthenticationToken.class, authentication);
    assertTrue(authentication.isAuthenticated());
    assertInstanceOf(UserPrincipal.class, authentication.getPrincipal());
    assertEquals(USERNAME, ((UserPrincipal) authentication.getPrincipal()).getName());
    assertEquals(USERNAME, authentication.getCredentials());
    assertTrue(authentication.getAuthorities()
                             .stream()
                             .map(GrantedAuthority::getAuthority)
                             .toList()
                             .contains("users"));
    verify(conversationRegistry).register(any(StateKey.class), any(ConversationState.class));
  }

  @Test
  void returnsAnonymousForADisabledUser() throws Exception {
    givenServices();
    when(identityRegistry.getIdentity(USERNAME)).thenReturn(platformUserIdentity());
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.findUserByName(USERNAME, UserStatus.ENABLED)).thenReturn(null);

    Authentication authentication = authenticate();

    assertInstanceOf(AnonymousAuthenticationToken.class, authentication);
    assertEquals(IdentityConstants.ANONIM, authentication.getPrincipal());
  }

  @Test
  void returnsAnonymousForAUserOutsideThePlatformGroups() {
    givenServices();
    when(identityRegistry.getIdentity(USERNAME)).thenReturn(new Identity(USERNAME,
                                                                        List.of(new MembershipEntry("/organization/employees")),
                                                                        List.of("employees")));

    Authentication authentication = authenticate();

    assertInstanceOf(AnonymousAuthenticationToken.class, authentication);
  }

  @Test
  void wrapsAnInternalFailureIntoAnAuthenticationServiceException() throws Exception {
    // Why PortalRememberMeFilter must not delete the remember-me cookie when
    // authentication throws: a transient IDM or database failure arrives here,
    // indistinguishable from any other internal error.
    givenServices();
    when(identityRegistry.getIdentity(USERNAME)).thenReturn(null);
    when(authenticator.createIdentity(USERNAME)).thenThrow(new IllegalStateException("IDM unreachable"));

    assertThrows(AuthenticationServiceException.class, this::authenticate);
  }

  private Authentication authenticate() {
    return authenticationManager.authenticate(new PreAuthenticatedAuthenticationToken(USERNAME, ""));
  }

  /**
   * Binds the Kernel services the provider resolves statically. Every test that
   * resolves an identity goes through the conversation and identity registries;
   * the organization service and the authenticator are only reached on some
   * branches, so they are stubbed here but left unused by design.
   */
  private void givenServices() {
    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(() -> ExoContainerContext.getService(ConversationRegistry.class)).thenReturn(conversationRegistry);
    containerContext.when(() -> ExoContainerContext.getService(IdentityRegistry.class)).thenReturn(identityRegistry);
    containerContext.when(() -> ExoContainerContext.getService(OrganizationService.class)).thenReturn(organizationService);
    containerContext.when(() -> ExoContainerContext.getService(Authenticator.class)).thenReturn(authenticator);
  }

  private Identity platformUserIdentity() {
    return new Identity(USERNAME, List.of(new MembershipEntry("/platform/users")), List.of("users"));
  }

  /**
   * {@link PortalAuthenticationManager} caches its Kernel services in static
   * fields, so one test's mocks would otherwise be reused by the next one after
   * the static mock is closed.
   */
  private void resetStaticServiceCaches() {
    for (String name : List.of("organizationService", "conversationRegistry", "identityRegistry", "authenticator")) {
      try {
        Field field = PortalAuthenticationManager.class.getDeclaredField(name);
        field.setAccessible(true); // NOSONAR test-only reset of a static cache
        field.set(null, null);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Cannot reset the static service cache " + name, e);
      }
    }
  }

}
