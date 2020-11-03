/*
 * Copyright (C) 2020 eXo Platform SAS.
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

package org.exoplatform.web.login.onboarding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.wci.security.Credentials;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.login.recovery.PasswordRecoveryServiceImpl;

public class OnboardingHandler extends WebRequestHandler {
    protected static Logger log = LoggerFactory.getLogger(OnboardingHandler.class);


    public static final String NAME = "on-boarding";

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
        Pattern customPasswordPattern = Pattern.compile(PropertyManager.getProperty("gatein.validators.passwordpolicy.regexp"));
        int customPasswordMaxlength = Integer.parseInt(PropertyManager.getProperty("gatein.validators.passwordpolicy.length.max"));
        int customPasswordMinlength = Integer.parseInt(PropertyManager.getProperty("gatein.validators.passwordpolicy.length.min"));

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
        ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);

        String token = context.getParameter(TOKEN);

        String requestAction = req.getParameter(REQ_PARAM_ACTION);

        if (token != null && !token.isEmpty()) {
            String tokenId = context.getParameter(TOKEN);

            //. Check tokenID is expired or not
            Credentials credentials = service.verifyToken(tokenId);
            if (credentials == null) {
                //. TokenId is expired
                return dispatch("/onboarding/jsp/token_expired.jsp", servletContext, req, res);
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
                  if (password == null || !customPasswordPattern.matcher(password).matches() || customPasswordMaxlength < password.length() || customPasswordMinlength > password.length() ) {
                        errors.add(PropertyManager.getProperty("gatein.validators.passwordpolicy.format.message"));
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

            return dispatch("/onboarding/jsp/reset_password.jsp", servletContext, req, res);
        }
        return false;
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
    
    public String escapeXssCharacters(String message){
        message = (message == null) ? null : message.replace("&", "&amp").replace("<","&lt;").replace(">","&gt;")
                                    .replace("\"","&quot;")
                                    .replace("'","&#x27;")
                                    .replace("/","&#x2F;");
        return message;
    }
}
