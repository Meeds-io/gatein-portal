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

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.web.HttpSessionStateKey;
import org.exoplatform.web.login.LoginUtils;
import org.exoplatform.web.security.security.CookieTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A Web filter to authenticate user Identity using 'rememberme' cookie if
 * present.<br>
 * The cookie token is validated by {@link CookieTokenService}; a valid token is
 * a <b>pre-authentication</b>: the user is authenticated on the Spring Security
 * side with a {@link PreAuthenticatedAuthenticationToken} and the resulting
 * {@link SecurityContext} is saved in the HTTP session, so that the following
 * requests of the same session are authenticated without re-reading the
 * cookie. No password login is attempted through the Servlet API: on a Spring
 * WAR, {@link HttpServletRequest#login(String, String)} is intercepted by
 * Spring Security and routed to the {@code AuthenticationManager} as a
 * {@code UsernamePasswordAuthenticationToken}, which
 * {@link PortalAuthenticationManager} deliberately does not support.<br>
 * Note: added to be included in class packages scan for Spring
 */
public class PortalRememberMeFilter extends AbstractFilter {

  private static final Log                LOG = ExoLogger.getLogger(PortalRememberMeFilter.class);

  private ConversationRegistry            conversationRegistry;

  private IdentityRegistry                identityRegistry;

  private Authenticator                   authenticator;

  private final AuthenticationProvider    authenticationProvider;

  private final SecurityContextRepository securityContextRepository;

  public PortalRememberMeFilter(AuthenticationProvider authenticationProvider) {
    // Writes the default HttpSessionSecurityContextRepository
    // SPRING_SECURITY_CONTEXT_KEY session attribute, which the chain's
    // SecurityContextHolderFilter reads on the next requests of the session
    // (the chain's own repository is the delegating one built by
    // SessionManagementConfigurer.init, same key)
    this(authenticationProvider,
         new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
                                                 new HttpSessionSecurityContextRepository()));
  }

  PortalRememberMeFilter(AuthenticationProvider authenticationProvider,
                         SecurityContextRepository securityContextRepository) {
    this.authenticationProvider = authenticationProvider;
    this.securityContextRepository = securityContextRepository;
  }

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;
    if (request.getRemoteUser() == null) {
      login(request, response);
    }
    chain.doFilter(req, resp);
  }

  private void login(HttpServletRequest request, HttpServletResponse response) {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainerIfPresent();
    ExoContainer container = getContainer();
    ExoContainerContext.setCurrentContainer(container);
    try {
      String username = getRememberMeTokenUser(request);
      if (username != null) {
        try {
          Authentication authentication = authenticate(request, response, username);
          if (authentication != null) {
            Identity identity = getIdentity(container, username);
            if (identity != null) {
              ConversationState state = new ConversationState(identity);
              getConversationRegistry(container).register(new HttpSessionStateKey(request.getSession()),
                                                          state);
              ConversationState.setCurrent(state);
            }
          }
        } catch (Exception e) {
          clearInvalidToken(request, response);
          LOG.warn("Error while logging in user {} using rememberme token, invalidate token", username, e);
        }
      }
    } finally {
      ExoContainerContext.setCurrentContainer(currentContainer);
    }
  }

  /**
   * Authenticates the user whose rememberme token was already validated, as a
   * pre-authenticated principal, and saves the resulting
   * {@link SecurityContext} in the current thread and in the HTTP session.
   * 
   * @param request {@link HttpServletRequest}
   * @param response {@link HttpServletResponse}
   * @param username validated token owner
   * @return the fully authenticated {@link Authentication}, or null when the
   *         provider did not authenticate the user (disabled user, user not
   *         member of a platform group...)
   */
  private Authentication authenticate(HttpServletRequest request, HttpServletResponse response, String username) {
    Authentication authentication = authenticationProvider.authenticate(new PreAuthenticatedAuthenticationToken(username,
                                                                                                                ""));
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
    return authentication;
  }

  private String getRememberMeTokenUser(HttpServletRequest request) {
    String token = LoginUtils.getRememberMeTokenCookie(request);
    if (token != null) {
      ExoContainer container = getContainer();
      CookieTokenService tokenservice = container.getComponentInstanceOfType(CookieTokenService.class);
      return tokenservice.validateToken(token, false);
    }
    return null;
  }

  private void clearInvalidToken(HttpServletRequest request, HttpServletResponse response) {
    if (request.getRemoteUser() == null) {
      Cookie cookie = new Cookie(LoginUtils.COOKIE_NAME, "");
      cookie.setPath("/");
      cookie.setMaxAge(0);
      cookie.setHttpOnly(true);
      cookie.setSecure(request.isSecure());
      response.addCookie(cookie);
    }
  }

  private Identity getIdentity(ExoContainer container, String userId) {
    Identity identity = getIdentityRegistry(container).getIdentity(userId);
    if (identity == null) {
      try {
        identity = getAuthenticator(container).createIdentity(userId);
        identityRegistry.register(identity);
      } catch (Exception e) {
        LOG.warn("Unable restore identity of user {}", userId, e);
      }
    }
    return identity;
  }

  private IdentityRegistry getIdentityRegistry(ExoContainer container) {
    if (identityRegistry == null) {
      identityRegistry = container.getComponentInstanceOfType(IdentityRegistry.class);
    }
    return identityRegistry;
  }

  private ConversationRegistry getConversationRegistry(ExoContainer container) {
    if (conversationRegistry == null) {
      conversationRegistry = container.getComponentInstanceOfType(ConversationRegistry.class);
    }
    return conversationRegistry;
  }

  private Authenticator getAuthenticator(ExoContainer container) {
    if (authenticator == null) {
      authenticator = container.getComponentInstanceOfType(Authenticator.class);
    }
    return authenticator;
  }

}
