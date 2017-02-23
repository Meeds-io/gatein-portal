/*
 * Copyright (C) 2015 eXo Platform SAS.
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

package org.exoplatform.web.login.recovery;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;

import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import org.exoplatform.services.organization.DisabledUserException;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;

import org.gatein.wci.security.Credentials;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class PasswordRecoveryHandler extends WebRequestHandler {
    protected static Logger log = LoggerFactory.getLogger(PasswordRecoveryHandler.class);


    public static final String NAME = "forgot-password";

    public static final QualifiedName TOKEN = QualifiedName.create("gtn", "token");
    public static final QualifiedName LANG = QualifiedName.create("gtn", "lang");
    public static final QualifiedName INIT_URL = QualifiedName.create("gtn", "initURL");

    public static final String REQ_PARAM_ACTION = "action";

    private static final ThreadLocal<Locale> currentLocale = new ThreadLocal<Locale>();

    @Override
    public String getHandlerName() {
        return NAME;
    }

    @Override
    public boolean execute(ControllerContext context) throws Exception {
        HttpServletRequest req = context.getRequest();
        HttpServletResponse res = context.getResponse();
        PortalContainer container = PortalContainer.getCurrentInstance(req.getServletContext());
        ServletContext servletContext = container.getPortalContext();

        Locale requestLocale = null;
        String lang = context.getParameter(LANG);
        Locale locale;
        if (lang != null && lang.length() > 0) {
            requestLocale = I18N.parseTagIdentifier(lang);
            locale = requestLocale;
        } else {
            locale = calculateLocale(context);
        }
        currentLocale.set(locale);
        req.setAttribute("request_locale", locale);

        PasswordRecoveryServiceImpl service = getService(PasswordRecoveryServiceImpl.class);
        ResourceBundleService bundleService = getService(ResourceBundleService.class);
        OrganizationService orgService = getService(OrganizationService.class);
        ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

        String token = context.getParameter(TOKEN);
        String initURL = context.getParameter(INIT_URL);

        String requestAction = req.getParameter(REQ_PARAM_ACTION);

        if (token != null && !token.isEmpty()) {
            String tokenId = context.getParameter(TOKEN);

            //. Check tokenID is expired or not
            Credentials credentials = service.verifyToken(tokenId);
            if (credentials == null) {
                //. TokenId is expired
                return dispatch("/forgotpassword/jsp/token_expired.jsp", servletContext, req, res);
            }
            final String username = credentials.getUsername();

            if ("resetPassword".equalsIgnoreCase(requestAction)) {
                String reqUser = req.getParameter("username");
                String password = req.getParameter("password");
                String confirmPass = req.getParameter("password2");

                List<String> errors = new ArrayList<String>();
                String success = "";

                if (reqUser == null || !reqUser.equals(username)) {
                    // Username is changed
                    String message = bundle.getString("gatein.forgotPassword.usernameChanged");
                    message = message.replace("{0}", username);
                    errors.add(message);
                } else {
                    if (password == null || password.isEmpty() || password.length() < 6 || password.length() > 30) {
                        errors.add(bundle.getString("gatein.forgotPassword.invalidPassword"));
                    }
                    if (confirmPass == null || confirmPass.length() < 6 || confirmPass.length() > 30) {
                        errors.add(bundle.getString("gatein.forgotPassword.invalidConfirmPassword"));
                    }
                    if (!password.equals(confirmPass)) {
                        errors.add(bundle.getString("gatein.forgotPassword.confirmPasswordNotMatch"));
                    }
                }

                //
                if (errors.isEmpty()) {
                    if (service.changePass(tokenId, username, password)) {
                        success = bundle.getString("gatein.forgotPassword.resetPasswordSuccess");
                        password = "";
                        confirmPass = "";
                    } else {
                        errors.add(bundle.getString("gatein.forgotPassword.resetPasswordFailure"));
                    }
                }
                req.setAttribute("password", password);
                req.setAttribute("password2", confirmPass);
                req.setAttribute("errors", errors);
                req.setAttribute("success", success);
            }

            req.setAttribute("tokenId", tokenId);
            req.setAttribute("username", escapeXssCharacters(username));

            return dispatch("/forgotpassword/jsp/reset_password.jsp", servletContext, req, res);

        } else {
            //.
            if ("send".equalsIgnoreCase(requestAction)) {
                String user = req.getParameter("username");
                if (user != null && !user.trim().isEmpty()) {
                    User u;

                    //
                    try {
                        u = findUser(orgService, user);
                        if (u == null) {
                            req.setAttribute("error", bundle.getString("gatein.forgotPassword.userNotExist"));
                        }
                    } catch (DisabledUserException e) {
                        req.setAttribute("error", bundle.getString("gatein.forgotPassword.userDisabled"));
                        u = null;
                    } catch (Exception ex) {
                        req.setAttribute("error", bundle.getString("gatein.forgotPassword.loadUserError"));
                        u = null;
                    }

                    //
                    if (u != null) {
                        if (service.sendRecoverPasswordEmail(u, Locale.ENGLISH, req)) {
                            req.setAttribute("success", bundle.getString("gatein.forgotPassword.emailSendSuccessful"));
                            user = "";
                        } else {
                            req.setAttribute("error", bundle.getString("gatein.forgotPassword.emailSendFailure"));
                        }
                    }

                    req.setAttribute("username", escapeXssCharacters(user));
                } else {
                    req.setAttribute("error", bundle.getString("gatein.forgotPassword.emptyUserOrEmail"));
                }
            }

            if (initURL != null) {
                req.setAttribute("initURL", initURL);
            }
            return dispatch("/forgotpassword/jsp/forgot_password.jsp", servletContext, req, res);
        }
    }

    protected boolean dispatch(String path, ServletContext context, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        RequestDispatcher dispatcher = context.getRequestDispatcher(path);
        if (dispatcher != null) {
            dispatcher.forward(req, res);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean getRequiresLifeCycle() {
        return true;
    }

    private <T> T getService(Class<T> clazz) {
        return ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(clazz);
    }

    public static Locale getCurrentLocale() {
        return currentLocale.get();
    }

    //TODO: how to reuse some method from LocalizationLifecycle
    private static final String LOCALE_COOKIE = "LOCALE";
    private static final String LOCALE_SESSION_ATTR = "org.gatein.LOCALE";
    private Locale calculateLocale(ControllerContext context) {
        LocalePolicy localePolicy = getService(LocalePolicy.class);
    
        HttpServletRequest request = HttpServletRequest.class.cast(context.getRequest());
    
        LocaleContextInfo localeCtx = LocaleContextInfoUtils.buildLocaleContextInfo(request);

        Set<Locale> supportedLocales = LocaleContextInfoUtils.getSupportedLocales();
        
        Locale locale = localePolicy.determineLocale(localeCtx);
        boolean supported = supportedLocales.contains(locale);

        if (!supported && !"".equals(locale.getCountry())) {
            locale = new Locale(locale.getLanguage());
            supported = supportedLocales.contains(locale);
        }
        if (!supported) {
            if (log.isWarnEnabled())
                log.warn("Unsupported locale returned by LocalePolicy: " + localePolicy + ". Falling back to 'en'.");
            locale = Locale.ENGLISH;
        }

        return locale;
    }
    
    private User findUser(OrganizationService orgService, String usernameOrEmail) throws Exception {
      if (usernameOrEmail == null || usernameOrEmail.isEmpty()) {
          return null;
      }

      User user = null;
      UserHandler uHandler = orgService.getUserHandler();
      user = uHandler.findUserByName(usernameOrEmail, UserStatus.ANY);
      if (user == null && usernameOrEmail.contains("@")) {
          Query query = new Query();
          query.setEmail(usernameOrEmail);
          ListAccess<User> list = uHandler.findUsersByQuery(query, UserStatus.ANY);
          if (list != null && list.getSize() > 0) {
              user = list.load(0, 1)[0];
          }
      }

      if (user != null && !user.isEnabled()) {
          throw new DisabledUserException(user.getUserName());
      }

      return user;
  }

    public String escapeXssCharacters(String message){
        message = (message == null) ? null : message.replace("&", "&amp").replace("<","&lt;").replace(">","&gt;")
                                    .replace("\"","&quot;")
                                    .replace("'","&#x27;")
                                    .replace("/","&#x2F;");
        return message;
    }
}
