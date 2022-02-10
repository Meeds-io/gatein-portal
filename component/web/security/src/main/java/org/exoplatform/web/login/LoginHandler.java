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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.*;

import org.apache.commons.lang.StringUtils;
import org.gatein.common.text.EntityEncoder;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceScope;
import org.gatein.portal.controller.resource.script.*;
import org.gatein.portal.controller.resource.script.Module;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.authentication.AuthenticationEventType;
import org.gatein.wci.authentication.AuthenticationException;
import org.gatein.wci.security.Credentials;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.resources.*;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.*;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.exoplatform.web.security.AuthenticationRegistry;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.sso.SSOHelper;

public class LoginHandler extends WebRequestHandler {

  private static final Log    LOG                    = ExoLogger.getLogger(LoginHandler.class);

  private static final String TEXT_HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

  private static final String JS_PATHS_PARAM         = "paths";

  private static final String LOGIN_JSP_PATH         = "/WEB-INF/jsp/login/login.jsp";

  private static final String IS_CASE_INSENSITIVE    = "exo.auth.case.insensitive";

  private PortalContainer     container;

  private ServletContext      servletContext;

  private boolean             caseInsensitive        = true;

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
    this.container = PortalContainer.getInstance();
    this.servletContext = container.getPortalContext();
    ServletContainerFactory.getServletContainer().addAuthenticationListener(event -> {
      if (event.getType() == AuthenticationEventType.LOGIN) {
        bindCredentialsToAuthenticationRegistry(container, event.getRequest(), event.getCredentials());
      }
    });
  }

  @Override
  public boolean execute(ControllerContext context) throws Exception {
    HttpServletRequest request = context.getRequest();
    HttpServletResponse response = context.getResponse();
    try {
      // We set the character encoding now to UTF-8 before obtaining parameters
      request.setCharacterEncoding("UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.error("Encoding not supported", e);
    }

    String username = request.getParameter("username");
    String password = request.getParameter("password");

    final String portalContextPath = servletContext.getContextPath();
    HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
      @Override
      public String getContextPath() {
        return portalContextPath;
      }
    };
    StringBuilder loginPath = new StringBuilder(LOGIN_JSP_PATH);
    //
    LoginStatus status = LoginStatus.UNAUTHENTICATED;
    if (request.getRemoteUser() == null) {
      if (username != null && password != null) {
        // email authentication
        if (username.contains("@")) {
          OrganizationService organizationService = this.container.getComponentInstanceOfType(OrganizationService.class);
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
              LOG.warn("Can not login with an email associated to many users");
              response.setContentType(TEXT_HTML_CONTENT_TYPE);
              dispatch(context, loginPath.toString(), LoginStatus.MANY_USERS_WITH_SAME_EMAIL);
              return true;
            } catch (Exception e) {
              LOG.warn("Can not get users by email", e);
              dispatch(context, loginPath.toString(), LoginStatus.FAILED);
              return true;
            }
          }
        }
        Credentials credentials = new Credentials(username, password);

        // This will login or send an AuthenticationException
        try {
          if (caseInsensitive) {
            username = getExactUserName(username);
            credentials = new Credentials(username, password);
          }
          ServletContainerFactory.getServletContainer().login(request, response, credentials);
        } catch (AuthenticationException e) {
          LOG.debug("User authentication failed");
          if (LOG.isTraceEnabled()) {
            LOG.trace(e.getMessage(), e);
          }
        }

        //
        status = request.getRemoteUser() != null ? LoginStatus.AUTHENTICATED : LoginStatus.UNAUTHENTICATED;
        // If we are authenticated
        if (status == LoginStatus.AUTHENTICATED) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("User authenticated successfuly through WCI. Will redirect to initialURI");
          }

          // Handle remember me
          addRememberMeCookie(request, response, credentials);
        }
      }
    } else {
      LOG.debug("User already authenticated. Will redirect to initialURI");
      status = LoginStatus.AUTHENTICATED;
    }

    String initialURI = getInitalUri(request);

    // Redirect to initialURI
    if (status == LoginStatus.AUTHENTICATED) {
      // Response may be already committed in case of SAML or other SSO
      // providers
      if (!response.isCommitted()) {
        response.sendRedirect(response.encodeRedirectURL(initialURI));
      }
    } else {
      // Show login form or redirect to SSO url (/portal/sso) if SSO is enabled
      request.setAttribute("org.gatein.portal.login.initial_uri", initialURI);
      SSOHelper ssoHelper = this.container.getComponentInstanceOfType(SSOHelper.class);

      String disabledUser = (String) request.getAttribute(FilterDisabledLoginModule.DISABLED_USER_NAME);
      boolean meetDisabledUser = disabledUser != null;
      if (ssoHelper.skipJSPRedirection() && meetDisabledUser) {
        response.setContentType(TEXT_HTML_CONTENT_TYPE);
        request.setAttribute("", ssoHelper);
        getServletContext().getRequestDispatcher("/WEB-INF/jsp/login/disabled.jsp").include(wrappedRequest, response);
      } else if (ssoHelper.skipJSPRedirection()) {
        String ssoRedirectUrl = request.getContextPath() + ssoHelper.getSSORedirectURLSuffix();
        ssoRedirectUrl = response.encodeRedirectURL(ssoRedirectUrl);
        if (LOG.isTraceEnabled()) {
          LOG.trace("Redirected to SSO login URL: " + ssoRedirectUrl);
        }
        response.sendRedirect(ssoRedirectUrl);
      } else {
        if (meetDisabledUser) {
          status = LoginStatus.DISABLED_USER;
        }

        response.setContentType(TEXT_HTML_CONTENT_TYPE);
        dispatch(context, loginPath.toString(), status);
      }
    }
    return true;
  }

  private void addRememberMeCookie(HttpServletRequest request, HttpServletResponse response, Credentials credentials) {
    String rememberme = request.getParameter(LoginUtils.COOKIE_NAME);
    if ("true".equals(rememberme)) {
      // Create token for credentials
      CookieTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
      String cookieToken = tokenService.createToken(credentials);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Found a remember me request parameter, created a persistent token " + cookieToken
            + " for it and set it up " + "in the next response");
      }
      Cookie cookie = new Cookie(LoginUtils.COOKIE_NAME, cookieToken);
      cookie.setPath("/");
      cookie.setHttpOnly(true);
      cookie.setMaxAge((int) tokenService.getValidityTime());
      response.addCookie(cookie);
    } else {
      // Handle oauth remember me
      if ("true".equals(request.getSession().getAttribute(LoginUtils.SESSION_ATTRIBUTE_REMEMBER_ME))) {
        CookieTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
        String cookieToken = tokenService.createToken(credentials);
        Cookie cookie = new Cookie(LoginUtils.OAUTH_COOKIE_NAME, cookieToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) tokenService.getValidityTime());
        response.addCookie(cookie);
        request.getSession().removeAttribute(LoginUtils.SESSION_ATTRIBUTE_REMEMBER_ME);
      }
    }
  }

  private void dispatch(ControllerContext controllerContext, String dispatchPath, LoginStatus status) throws Exception {
    HttpServletRequest request = controllerContext.getRequest();
    HttpServletResponse response = controllerContext.getResponse();

    LocaleConfigService localeConfigService = this.container.getComponentInstanceOfType(LocaleConfigService.class);
    LocaleConfig localeConfig = request.getLocale() == null ? localeConfigService.getDefaultLocaleConfig()
                                                            : localeConfigService.getLocaleConfig(request.getLocale()
                                                                                                         .getLanguage());
    if (localeConfig == null) {
      localeConfig = localeConfigService.getDefaultLocaleConfig();
    }
    request.setAttribute("localeConfig", localeConfig);

    Locale locale = localeConfig.getLocale();

    JavascriptManager javascriptManager = new JavascriptManager();
    javascriptManager.loadScriptResource(ResourceScope.SHARED, "bootstrap");
    javascriptManager.loadScriptResource(ResourceScope.PORTLET, "social-portlet/Login");

    JSONObject params = new JSONObject();

    String initialURI = getInitalUri(request);
    params.put("initialUri", EntityEncoder.FULL.encode(initialURI));

    BrandingService brandingService = this.container.getComponentInstanceOfType(BrandingService.class);
    String companyName = brandingService.getCompanyName();
    params.put("companyName", companyName);

    PasswordRecoveryService passRecoveryServ = this.container.getComponentInstanceOfType(PasswordRecoveryService.class);
    String forgotPasswordPath = passRecoveryServ.getPasswordRecoverURL(null, null);
    params.put("forgotPasswordPath", request.getContextPath() + forgotPasswordPath);

    if (status != LoginStatus.AUTHENTICATED && status != LoginStatus.UNAUTHENTICATED) {
      params.put("errorCode", status.getErrorCode());
    }

    String brandingLogo = "/" + PortalContainer.getCurrentPortalContainerName() + "/"
        + PortalContainer.getCurrentRestContextName() + "/v1/platform/branding/logo?v=" + brandingService.getLastUpdatedTime();
    params.put("brandingLogo", brandingLogo);

    javascriptManager.require("PORTLET/social-portlet/Login", "loginApp").addScripts("loginApp.init(" + params.toString() + ");");

    JavascriptConfigService javascriptConfigService = this.container.getComponentInstanceOfType(JavascriptConfigService.class);
    JSONObject jsConfig = javascriptConfigService.getJSConfig(controllerContext, locale);
    request.setAttribute("jsConfig", jsConfig.toString());

    if (jsConfig.has(JS_PATHS_PARAM)) {
      JSONObject jsConfigPaths = jsConfig.getJSONObject(JS_PATHS_PARAM);
      addLoginExtensionModules(javascriptManager, jsConfigPaths);

      LinkedList<String> headerScripts = getHeaderScripts(javascriptManager, jsConfigPaths);
      request.setAttribute("headerScripts", headerScripts);

      Set<String> pageScripts = getPageScripts(javascriptManager, jsConfigPaths);
      request.setAttribute("pageScripts", pageScripts);
    }
    request.setAttribute("inlineScripts", javascriptManager.getJavaScripts());

    String brandingPrimaryColor = brandingService.getThemeColors().get("primaryColor");
    String brandingThemeUrl = "/" + PortalContainer.getCurrentPortalContainerName() + "/"
        + PortalContainer.getCurrentRestContextName() + "/v1/platform/branding/css?v=" + brandingService.getLastUpdatedTime();

    request.setAttribute("brandingPrimaryColor", brandingPrimaryColor);
    request.setAttribute("brandingThemeUrl", brandingThemeUrl);

    List<String> skinUrls = getPageSkins(controllerContext, localeConfig.getOrientation());
    request.setAttribute("skinUrls", skinUrls);

    getServletContext().getRequestDispatcher(dispatchPath).include(request, response);
  }

  private List<String> getPageSkins(ControllerContext controllerContext, Orientation orientation) {
    SkinService skinService = this.container.getComponentInstanceOfType(SkinService.class);

    String skinName = skinService.getDefaultSkin();

    List<SkinConfig> skins = new ArrayList<>();

    Collection<SkinConfig> portalSkins = skinService.getPortalSkins(skinName);
    skins.addAll(portalSkins);

    SkinConfig loginSkin = skinService.getSkin("portal/login", skinName);
    skins.add(loginSkin);

    Collection<SkinConfig> customSkins = skinService.getCustomPortalSkins(skinName);
    skins.addAll(customSkins);
    return skins.stream().map(skin -> {
      SkinURL url = skin.createURL(controllerContext);
      url.setOrientation(orientation);
      return url.toString();
    }).collect(Collectors.toList());
  }

  private Set<String> getPageScripts(JavascriptManager javascriptManager, JSONObject jsConfigPaths) throws JSONException {
    Set<String> pageScripts = new HashSet<>();
    Map<String, Boolean> scriptsIdsMap = javascriptManager.getPageScripts();
    for (Entry<String, Boolean> scriptEntry : scriptsIdsMap.entrySet()) {
      boolean isRemote = scriptEntry.getValue().booleanValue();
      String scriptId = scriptEntry.getKey();
      if (!isRemote && jsConfigPaths.has(scriptId)) {
        String scriptPath = jsConfigPaths.getString(scriptId) + ".js";
        pageScripts.add(scriptPath);
      }
    }
    return pageScripts;
  }

  private LinkedList<String> getHeaderScripts(JavascriptManager javascriptManager,
                                              JSONObject jsConfigPaths) throws JSONException {
    Map<String, Boolean> scriptsURLs = getScripts(javascriptManager);
    LinkedList<String> headerScripts = new LinkedList<>();
    for (Entry<String, Boolean> moduleEntry : scriptsURLs.entrySet()) {
      String module = moduleEntry.getKey();
      String url = jsConfigPaths.has(module) ? jsConfigPaths.getString(module) : null;
      headerScripts.add(url != null ? url + ".js" : module);
    }
    return headerScripts;
  }

  private void addLoginExtensionModules(JavascriptManager javascriptManager, JSONObject jsConfigPaths) {
    @SuppressWarnings("unchecked")
    Iterator<String> keys = jsConfigPaths.keys();
    while (keys.hasNext()) {
      String module = keys.next();
      if (module.contains("LoginExtension")) {
        javascriptManager.require(module);
      }
    }
  }

  private String getInitalUri(HttpServletRequest request) {
    // Obtain initial URI
    String initialURI = request.getParameter("initialURI");

    // Otherwise compute one
    if (initialURI == null || initialURI.length() == 0) {
      initialURI = request.getContextPath();
      LOG.debug("No initial URI found, will use default " + initialURI + " instead ");
    } else {
      LOG.debug("Found initial URI " + initialURI);
    }

    try {
      URI uri = new URI(initialURI);
      if ((uri.getHost() != null) && !(uri.getHost().equals(request.getServerName()))) {
        LOG.warn("Cannot redirect to an URI outside of the current host when using a login redirect. Redirecting to the portal context path instead.");
        initialURI = request.getContextPath();
      }
    } catch (URISyntaxException e) {
      LOG.warn("Initial URI in login link is malformed. Redirecting to the portal context path instead.");
      initialURI = request.getContextPath();
    }
    return initialURI;
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
  private static void bindCredentialsToAuthenticationRegistry(ExoContainer container,
                                                              HttpServletRequest req,
                                                              Credentials credentials) {
    AuthenticationRegistry authRegistry = container.getComponentInstanceOfType(AuthenticationRegistry.class);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Binding credentials to temporary authentication registry for user " + credentials.getUsername());
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
      LOG.warn("Error while retrieving user " + username + " from IDM stores ", exception);
    } finally {
      RequestLifeCycle.end();
    }
    return username;
  }

  public Map<String, Boolean> getScripts(JavascriptManager javascriptManager) {
    FetchMap<ResourceId> requiredResources = javascriptManager.getScriptResources();
    JavascriptConfigService service = this.container.getComponentInstanceOfType(JavascriptConfigService.class);
    Map<ScriptResource, FetchMode> resolved = service.resolveIds(requiredResources);

    Map<String, Boolean> ret = new LinkedHashMap<>();
    Map<String, Boolean> tmp = new LinkedHashMap<>();
    for (ScriptResource rs : resolved.keySet()) {
      ResourceId id = rs.getId();
      // SHARED/bootstrap should be loaded first
      if (ResourceScope.SHARED.equals(id.getScope()) && "bootstrap".equals(id.getName())) {
        ret.put(id.toString(), false);
      } else {
        boolean isRemote = !rs.isEmpty() && rs.getModules().get(0) instanceof Module.Remote;
        tmp.put(id.toString(), isRemote);
      }
    }
    ret.putAll(tmp);
    for (String url : javascriptManager.getExtendedScriptURLs()) {
      ret.put(url, true);
    }

    //
    LOG.debug("Resolved resources for page: " + ret);
    return ret;
  }
}
