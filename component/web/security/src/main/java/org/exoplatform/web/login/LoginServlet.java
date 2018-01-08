/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.web.login;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.web.AbstractHttpServlet;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.security.AuthenticationRegistry;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.sso.SSOHelper;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.wci.ServletContainer;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.authentication.AuthenticationEvent;
import org.gatein.wci.authentication.AuthenticationEventType;
import org.gatein.wci.authentication.AuthenticationException;
import org.gatein.wci.authentication.AuthenticationListener;
import org.gatein.wci.security.Credentials;

/**
 * The login servlet which proceeds as.
 *
 * 0. If user is already authenticated : nothing happens 1. When username and password are provided, a login is attempted 1.1 if
 * login is successful the user is authenticated 1.2 if login fails the user is not authenticated and the login form is
 * displayed with the user name filled 2. When username is provided, the login form is displayed with the user name filled 3.
 * Finally if nothing is provided the login form is displayed
 *
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class LoginServlet extends AbstractHttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = -1330051083735349589L;

    /** . */
    private static final int UNAUTHENTICATED = 0;

    /** . */
    private static final int AUTHENTICATED = 1;

    /** . */
    private static final int FAILED = 2;

    /** . */
    private static final Logger log = LoggerFactory.getLogger(LoginServlet.class);

    /** . */
    public static final String COOKIE_NAME = "rememberme";

    public static final String OAUTH_COOKIE_NAME = "oauth_rememberme";

    private static final String IS_CASE_INSENSITIVE = "exo.auth.case.insensitive";

    //value of this field need equals with org.gatein.security.oauth.common.OAuthConstants.ATTRIBUTE_REMEMBER_ME
    public static final String SESSION_ATTRIBUTE_REMEMBER_ME = "_rememberme";

    /**
     * Register WCI authentication listener, which is used to bind credentials to temporary authentication registry after each
     * successful login
     *
     * @param config
     * @throws ServletException
     */
    @Override
    protected void afterInit(ServletConfig config) throws ServletException {
        ServletContainerFactory.getServletContainer().addAuthenticationListener(new AuthenticationListener() {
            @Override
            public void onEvent(AuthenticationEvent event) {
                if (event.getType() == AuthenticationEventType.LOGIN) {
                    bindCredentialsToAuthenticationRegistry(getContainer(), event.getRequest(), event.getCredentials());
                }
            }
        });
    }

    /**
     * Extract the remember me token from the request or returns null.
     *
     * @param req the incoming request
     * @return the token
     */
    public static String getRememberMeTokenCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String getOauthRememberMeTokenCookie (HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (OAUTH_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // We set the character encoding now to UTF-8 before obtaining parameters
            req.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Encoding not supported", e);
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        //
        int status;
        boolean caseInsensitive = true;
        String caseInsensitiveString = PropertyManager.getProperty(IS_CASE_INSENSITIVE);
        if(caseInsensitiveString != null) {
            caseInsensitive = Boolean.valueOf(caseInsensitiveString);
        }
        if (req.getRemoteUser() == null) {
            if (username != null && password != null) {
                Credentials credentials = new Credentials(username, password);
                ServletContainer container = ServletContainerFactory.getServletContainer();

                // This will login or send an AuthenticationException
                try {
                    container.login(req, resp, credentials);
                } catch (AuthenticationException e) {
                    if (caseInsensitive) {
                        username = getUserNameCaseInsensitive(username);
                        if (username != null) {
                          try {
                            credentials = new Credentials(username, password);
                            container.login(req, resp, credentials);
                          } catch (AuthenticationException e1) {
                            log.trace("User " + username + " authentication failed", e1);
                          }
                        }
                    } else {
                      log.trace("User " + username + " authentication failed", e);
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
                    String rememberme = req.getParameter("rememberme");
                    if ("true".equals(rememberme)) {
                        // Create token for credentials
                        CookieTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
                        String cookieToken = tokenService.createToken(credentials);
                        if (log.isDebugEnabled()) {
                            log.debug("Found a remember me request parameter, created a persistent token " + cookieToken
                                    + " for it and set it up " + "in the next response");
                        }
                        Cookie cookie = new Cookie(COOKIE_NAME, cookieToken);
                        cookie.setPath(req.getContextPath());
                        cookie.setHttpOnly(true);
                        cookie.setMaxAge((int) tokenService.getValidityTime());
                        resp.addCookie(cookie);

                    } else {

                        //Handle oauth remember me
                        if("true".equals(req.getSession().getAttribute(SESSION_ATTRIBUTE_REMEMBER_ME))) {
                            CookieTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
                            String cookieToken = tokenService.createToken(credentials);
                            Cookie cookie = new Cookie(OAUTH_COOKIE_NAME, cookieToken);
                            cookie.setPath(req.getContextPath());
                            cookie.setMaxAge((int) tokenService.getValidityTime());
                            resp.addCookie(cookie);

                            req.getSession().removeAttribute(SESSION_ATTRIBUTE_REMEMBER_ME);
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
            if (uri.isAbsolute() && !(uri.getHost().equals(req.getServerName()))) {
                log.warn("Cannot redirect to an URI outside of the current host when using a login redirect. Redirecting to the portal context path instead.");
                initialURI = req.getContextPath();
            }
        } catch (URISyntaxException e) {
            log.warn("Initial URI in login link is malformed. Redirecting to the portal context path instead.");
            initialURI = req.getContextPath();
        }

        // Redirect to initialURI
        if (status == AUTHENTICATED) {
            // Response may be already committed in case of SAML or other SSO providers
            if (!resp.isCommitted()) {
                resp.sendRedirect(resp.encodeRedirectURL(initialURI));
            }
        } else {
            if (status == FAILED) {
                req.setAttribute("org.gatein.portal.login.error", "whatever");
            }

            // Show login form or redirect to SSO url (/portal/sso) if SSO is enabled
            req.setAttribute("org.gatein.portal.login.initial_uri", initialURI);
            SSOHelper ssoHelper = getContainer().getComponentInstanceOfType(SSOHelper.class);

            String disabledUser = (String)req.getAttribute(FilterDisabledLoginModule.DISABLED_USER_NAME);
            boolean meetDisabledUser = disabledUser != null;
            ServletContext servletContext = getServletContext();
            final String portalContextPath = servletContext.getContextPath();
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(req) {
              @Override
              public String getContextPath() {
                return portalContextPath;
              }
            };
            if(ssoHelper.skipJSPRedirection() && meetDisabledUser) {
                resp.setContentType("text/html; charset=UTF-8");
                getServletContext().getRequestDispatcher("/login/jsp/disabled.jsp").include(wrappedRequest, resp);
            } else if (ssoHelper.skipJSPRedirection()) {
                String ssoRedirectUrl = req.getContextPath() + ssoHelper.getSSORedirectURLSuffix();
                ssoRedirectUrl = resp.encodeRedirectURL(ssoRedirectUrl);
                if (log.isTraceEnabled()) {
                    log.trace("Redirected to SSO login URL: " + ssoRedirectUrl);
                }
                resp.sendRedirect(ssoRedirectUrl);
            } else {
                StringBuilder loginPath = new StringBuilder("/login/jsp/login.jsp");
                if (meetDisabledUser) {
                    String errorData = meetDisabledUser ? new LoginError(LoginError.DISABLED_USER_ERROR, disabledUser).toString() : "";
                    loginPath.append("?").append(LoginError.ERROR_PARAM).append("=").append(URLEncoder.encode(errorData, "UTF-8"));
                }

                resp.setContentType("text/html; charset=UTF-8");
                getServletContext().getRequestDispatcher(loginPath.toString()).include(wrappedRequest, resp);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    /**
     * Add credentials to {@link ConversationState}.
     *
     * @param credentials the credentials
     */
    private static void bindCredentialsToAuthenticationRegistry(ExoContainer exoContainer, HttpServletRequest req,
            Credentials credentials) {
        AuthenticationRegistry authRegistry = (AuthenticationRegistry) exoContainer
                .getComponentInstanceOfType(AuthenticationRegistry.class);
        if (log.isTraceEnabled()) {
            log.trace("Binding credentials to temporary authentication registry for user " + credentials.getUsername());
        }
        authRegistry.setCredentials(req, credentials);
    }

    /**
     * Get username from database case insensitive
     * 
     * @param username
     * @return
     */
    private String getUserNameCaseInsensitive(String username) {
      try {
          OrganizationService organizationService =
                  (OrganizationService) ExoContainerContext.getCurrentContainer()
                          .getComponentInstance(OrganizationService.class);
          Query query = new Query();
          query.setUserName(username);
          ListAccess<User> users = organizationService.getUserHandler().findUsersByQuery(query);
          if (users.getSize() >= 1) {
              String loadedUsername = "";
              User[] listusers = users.load(0, users.getSize());
              int found = 0;
              for (User user : listusers) {
                  if (username.equalsIgnoreCase(user.getUserName())) {
                      loadedUsername = user.getUserName();
                      found ++;
                  }
              }
              if(found == 1 && StringUtils.isNotBlank(loadedUsername)) {
                  if (StringUtils.equals(username, loadedUsername)) {
                    return null;
                  }
                  username = loadedUsername;
              } else {
                  log.warn("duplicate entry for user " + username);
              }
          }
      } catch (Exception exception) {
          log.warn("Error while retrieving user " + username + " from IDM stores ", exception);
      }
      return username;
    }
}