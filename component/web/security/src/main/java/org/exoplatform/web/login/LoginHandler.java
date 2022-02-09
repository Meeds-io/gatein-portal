/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
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
package org.exoplatform.web.login;

import java.io.UnsupportedEncodingException;
import java.net.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.*;

import org.apache.commons.lang.StringUtils;
import org.gatein.wci.ServletContainer;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.authentication.*;
import org.gatein.wci.security.Credentials;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.*;
import org.exoplatform.web.security.AuthenticationRegistry;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.sso.SSOHelper;

public class LoginHandler extends WebRequestHandler {

  private static final String LOGIN_JSP_PATH      = "/WEB-INF/jsp/login/login.jsp";

  private static final int    UNAUTHENTICATED     = 0;

  private static final int    AUTHENTICATED       = 1;

  private static final int    FAILED              = 2;

  private static final Log    log                 = ExoLogger.getLogger(LoginHandler.class);

  private static final String IS_CASE_INSENSITIVE = "exo.auth.case.insensitive";

  private ServletContext      servletContext;

  private boolean             caseInsensitive     = true;

  @Override
  public String getHandlerName() {
    return "login";
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  /**
   * Register WCI authentication listener, which is used to bind credentials to
   * temporary authentication registry after each successful login
   */
  @Override
  public void onInit(WebAppController controller, ServletConfig servletConfig) {
    caseInsensitive = StringUtils.equalsIgnoreCase(PropertyManager.getProperty(IS_CASE_INSENSITIVE), "true");
    PortalContainer container = PortalContainer.getInstance();
    this.servletContext = container.getPortalContext();

    ServletContainerFactory.getServletContainer().addAuthenticationListener(new AuthenticationListener() {
      @Override
      public void onEvent(AuthenticationEvent event) {
        if (event.getType() == AuthenticationEventType.LOGIN) {
          bindCredentialsToAuthenticationRegistry(container, event.getRequest(), event.getCredentials());
        }
      }
    });
  }

  @Override
  public boolean execute(ControllerContext context) throws Exception {
    HttpServletRequest req = context.getRequest();
    HttpServletResponse resp = context.getResponse();
    try {
      // We set the character encoding now to UTF-8 before obtaining parameters
      req.setCharacterEncoding("UTF-8");
    } catch (UnsupportedEncodingException e) {
      log.error("Encoding not supported", e);
    }

    String username = req.getParameter("username");
    String password = req.getParameter("password");

    req.setAttribute("controllerContext", context);

    ServletContext servletContext = getServletContext();
    final String portalContextPath = servletContext.getContextPath();
    HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(req) {
      @Override
      public String getContextPath() {
        return portalContextPath;
      }
    };
    StringBuilder loginPath = new StringBuilder(LOGIN_JSP_PATH);
    //
    int status;
    if (req.getRemoteUser() == null) {
      if (username != null && password != null) {
        // email authentication
        if (username.contains("@")) {
          OrganizationService organizationService = PortalContainer.getInstance()
                                                                   .getComponentInstanceOfType(OrganizationService.class);
          UserHandler userHandler = organizationService.getUserHandler();
          if (userHandler != null) {
            Query emailQuery = new Query();
            emailQuery.setEmail(username);
            ListAccess<User> users;
            try {
              users = userHandler.findUsersByQuery(emailQuery);
              if (users != null && users.getSize() > 0) {
                username = users.load(0, 1)[0].getUserName();
              }
            } catch (RuntimeException e) {
              log.error("Can not login with an email associated to many users");
              req.setAttribute("org.gatein.portal.manyUsersWithSameEmail.error", "whatever");
              resp.setContentType("text/html; charset=UTF-8");
              getServletContext().getRequestDispatcher(loginPath.toString()).include(wrappedRequest, resp);
              return true;
            } catch (Exception e) {
              log.error("Can not get users by email", e);
            }
          }
        }
        Credentials credentials = new Credentials(username, password);
        ServletContainer container = ServletContainerFactory.getServletContainer();

        // This will login or send an AuthenticationException
        try {
          if (caseInsensitive) {
            username = getExactUserName(username);
            credentials = new Credentials(username, password);
          }
          container.login(req, resp, credentials);
        } catch (AuthenticationException e) {
          log.debug("User authentication failed");
          if (log.isTraceEnabled()) {
            log.trace(e.getMessage(), e);
          }
        }

        //
        status = req.getRemoteUser() != null ? AUTHENTICATED : FAILED;

        // If we are authenticated
        if (status == AUTHENTICATED) {
          if (log.isTraceEnabled()) {
            log.trace("User authenticated successfuly through WCI. Will redirect to initialURI");
          }

          // Handle remember me
          String rememberme = req.getParameter(LoginUtils.COOKIE_NAME);
          if ("true".equals(rememberme)) {
            // Create token for credentials
            CookieTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
            String cookieToken = tokenService.createToken(credentials);
            if (log.isDebugEnabled()) {
              log.debug("Found a remember me request parameter, created a persistent token " + cookieToken
                  + " for it and set it up " + "in the next response");
            }
            Cookie cookie = new Cookie(LoginUtils.COOKIE_NAME, cookieToken);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge((int) tokenService.getValidityTime());
            resp.addCookie(cookie);
          } else {
            // Handle oauth remember me
            if ("true".equals(req.getSession().getAttribute(LoginUtils.SESSION_ATTRIBUTE_REMEMBER_ME))) {
              CookieTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
              String cookieToken = tokenService.createToken(credentials);
              Cookie cookie = new Cookie(LoginUtils.OAUTH_COOKIE_NAME, cookieToken);
              cookie.setPath("/");
              cookie.setHttpOnly(true);
              cookie.setMaxAge((int) tokenService.getValidityTime());
              resp.addCookie(cookie);
              req.getSession().removeAttribute(LoginUtils.SESSION_ATTRIBUTE_REMEMBER_ME);
            }
          }
        }
      } else {
        log.debug("username or password not provided. Changing status to UNAUTHENTICATED");
        status = UNAUTHENTICATED;
      }
    } else {
      log.debug("User already authenticated. Will redirect to initialURI");
      status = AUTHENTICATED;
    }

    // Obtain initial URI
    String initialURI = req.getParameter("initialURI");

    // Otherwise compute one
    if (initialURI == null || initialURI.length() == 0) {
      initialURI = req.getContextPath();
      log.debug("No initial URI found, will use default " + initialURI + " instead ");
    } else {
      log.debug("Found initial URI " + initialURI);
    }

    try {
      URI uri = new URI(initialURI);
      if ((uri.getHost() != null) && !(uri.getHost().equals(req.getServerName()))) {
        log.warn("Cannot redirect to an URI outside of the current host when using a login redirect. Redirecting to the portal context path instead.");
        initialURI = req.getContextPath();
      }
    } catch (URISyntaxException e) {
      log.warn("Initial URI in login link is malformed. Redirecting to the portal context path instead.");
      initialURI = req.getContextPath();
    }

    // Redirect to initialURI
    if (status == AUTHENTICATED) {
      // Response may be already committed in case of SAML or other SSO
      // providers
      if (!resp.isCommitted()) {
        resp.sendRedirect(resp.encodeRedirectURL(initialURI));
      }
    } else {
      if (status == FAILED) {
        req.setAttribute("org.gatein.portal.login.error", "whatever");
      }

      // Show login form or redirect to SSO url (/portal/sso) if SSO is enabled
      req.setAttribute("org.gatein.portal.login.initial_uri", initialURI);
      SSOHelper ssoHelper = PortalContainer.getInstance().getComponentInstanceOfType(SSOHelper.class);

      String disabledUser = (String) req.getAttribute(FilterDisabledLoginModule.DISABLED_USER_NAME);
      boolean meetDisabledUser = disabledUser != null;
      if (ssoHelper.skipJSPRedirection() && meetDisabledUser) {
        resp.setContentType("text/html; charset=UTF-8");
        req.setAttribute("", ssoHelper);
        getServletContext().getRequestDispatcher("/WEB-INF/jsp/login/disabled.jsp").include(wrappedRequest, resp);
      } else if (ssoHelper.skipJSPRedirection()) {
        String ssoRedirectUrl = req.getContextPath() + ssoHelper.getSSORedirectURLSuffix();
        ssoRedirectUrl = resp.encodeRedirectURL(ssoRedirectUrl);
        if (log.isTraceEnabled()) {
          log.trace("Redirected to SSO login URL: " + ssoRedirectUrl);
        }
        resp.sendRedirect(ssoRedirectUrl);
      } else {
        if (meetDisabledUser) {
          String errorData = meetDisabledUser ? new LoginError(LoginError.DISABLED_USER_ERROR, disabledUser).toString() : "";
          loginPath.append("?").append(LoginError.ERROR_PARAM).append("=").append(URLEncoder.encode(errorData, "UTF-8"));
        }

        resp.setContentType("text/html; charset=UTF-8");
        getServletContext().getRequestDispatcher(loginPath.toString()).include(wrappedRequest, resp);
      }
    }
    return true;
  }

  /**
   * @return the servletContext
   */
  public ServletContext getServletContext() {
    return servletContext;
  }

  /**
   * Add credentials to {@link ConversationState}.
   *
   * @param credentials the credentials
   */
  private static void bindCredentialsToAuthenticationRegistry(ExoContainer exoContainer,
                                                              HttpServletRequest req,
                                                              Credentials credentials) {
    AuthenticationRegistry authRegistry = (AuthenticationRegistry) exoContainer
                                                                               .getComponentInstanceOfType(AuthenticationRegistry.class);
    if (log.isTraceEnabled()) {
      log.trace("Binding credentials to temporary authentication registry for user " + credentials.getUsername());
    }
    authRegistry.setCredentials(req, credentials);
  }

  /**
   * Get exact username from database
   * 
   * @param username
   * @return
   */
  private String getExactUserName(String username) {
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    RequestLifeCycle.begin(currentContainer);
    try {
      OrganizationService organizationService =
                                              (OrganizationService) currentContainer
                                                                                    .getComponentInstance(OrganizationService.class);
      User user = organizationService.getUserHandler().findUserByName(username);
      if (user != null) {
        username = user.getUserName();
      }
    } catch (Exception exception) {
      log.warn("Error while retrieving user " + username + " from IDM stores ", exception);
    } finally {
      RequestLifeCycle.end();
    }
    return username;
  }
}
