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

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import javax.portlet.PortletException;
import javax.servlet.*;
import javax.servlet.http.*;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.wci.security.Credentials;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.resources.*;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.login.recovery.PasswordRecoveryServiceImpl;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

import nl.captcha.Captcha;
import nl.captcha.servlet.CaptchaServletUtil;
import nl.captcha.text.producer.DefaultTextProducer;
import nl.captcha.text.renderer.DefaultWordRenderer;



public class OnboardingHandler extends WebRequestHandler {
    private static final QualifiedName SERVER_CAPTCHA = QualifiedName.create("gtn", "serveCaptcha");

    protected static Log                   log              = ExoLogger.getLogger(OnboardingHandler.class);


    public static final String NAME = "on-boarding";

    public static final QualifiedName TOKEN = QualifiedName.create("gtn", "token");
    public static final QualifiedName LANG = QualifiedName.create("gtn", "lang");
    public static final QualifiedName INIT_URL = QualifiedName.create("gtn", "initURL");

    public static final String REQ_PARAM_ACTION = "action";

    private static final ThreadLocal<Locale> currentLocale = new ThreadLocal<Locale>();

    private static final String     TEXT_HTML_CONTENT_TYPE     = "text/html; charset=UTF-8";

    protected int _width = 200;

    protected int _height = 50;


    @Override
    public String getHandlerName() {
        return NAME;
    }

    @Override
    public boolean execute(ControllerContext context) throws Exception {
        HttpServletRequest req = context.getRequest();
        HttpServletResponse res = context.getResponse();
        res.setContentType(TEXT_HTML_CONTENT_TYPE);
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
        OrganizationService organizationService = getService(OrganizationService.class);
        ResourceBundleService bundleService = getService(ResourceBundleService.class);
        ResourceBundle bundle = bundleService.getResourceBundle(bundleService.getSharedResourceBundleNames(), locale);
        RemindPasswordTokenService remindPasswordTokenService= getService(RemindPasswordTokenService.class);

        String token = context.getParameter(TOKEN);

        String serveCaptcha=context.getParameter(SERVER_CAPTCHA);

        String requestAction = req.getParameter(REQ_PARAM_ACTION);

        if ("true".equals(serveCaptcha)) {
            return serveCaptchaImage(req,res);
        }

        if (token != null && !token.isEmpty()) {
            String tokenId = context.getParameter(TOKEN);

            //. Check tokenID is expired or not
            Credentials credentials = service.verifyToken(tokenId,remindPasswordTokenService.ONBOARD_TOKEN);
            if (credentials == null) {
                //. TokenId is expired
                return dispatch("/WEB-INF/jsp/onboarding/token_expired.jsp", servletContext, req, res);
            }
            final String username = credentials.getUsername();

            if ("resetPassword".equalsIgnoreCase(requestAction)) {
                String reqUser = req.getParameter("username");
                String password = req.getParameter("password");
                String confirmPass = req.getParameter("password2");
                String captcha = req.getParameter("captcha");

                List<String> errors = new ArrayList<String>();
                String success = "";

                if (captcha == null || !isValid(req.getSession(), captcha)) {
                    String message = bundle.getString("gatein.forgotPassword.captchaError");
                    errors.add(message);
                }

                if (reqUser == null || !reqUser.equals(username)) {
                    // Username is changed
                    String message = bundle.getString("gatein.forgotPassword.usernameChanged");
                    message = message.replace("{0}", username);
                    errors.add(message);
                } else {
                  if (password == null || !customPasswordPattern.matcher(password).matches() || customPasswordMaxlength < password.length() || customPasswordMinlength > password.length() ) {
                        String passwordpolicyProperty = PropertyManager.getProperty("gatein.validators.passwordpolicy.format.message");
                        errors.add(passwordpolicyProperty != null ? passwordpolicyProperty : bundle.getString("onboarding.login.passwordCondition"));
                    }
                    if (!password.equals(confirmPass)) {
                        errors.add(bundle.getString("gatein.forgotPassword.confirmPasswordNotMatch"));
                    }
                }

                // Invalidate the capcha
                req.getSession().removeAttribute(NAME);

                //
                if (errors.isEmpty()) {
                    if (service.changePass(tokenId, remindPasswordTokenService.ONBOARD_TOKEN, username, password)) {
                        success = bundle.getString("gatein.forgotPassword.resetPasswordSuccess");
                        password = "";
                        confirmPass = "";
                        String currentPortalContainerName = PortalContainer.getCurrentPortalContainerName();
                        res.sendRedirect("/" + currentPortalContainerName + "/login?email=" + organizationService.getUserHandler().findUserByName(username).getEmail());
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
    
            
            String random = "&v=" + Calendar.getInstance().getTimeInMillis();
    
    
            return dispatch("/WEB-INF/jsp/onboarding/reset_password.jsp", servletContext, req, res);
        }
        return false;
    }
    
    private boolean isValid(HttpSession session, String captchaValue) {
        Captcha captcha = (Captcha) session.getAttribute(NAME);
        return ((captcha != null) && (captcha.isCorrect(captchaValue)));
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
    
    
    public boolean serveCaptchaImage(HttpServletRequest req, HttpServletResponse resp) throws PortletException,
                                                                                            java.io.IOException {
        HttpSession session = req.getSession();
        Captcha captcha;
        if (session.getAttribute(NAME) == null) {
            List<java.awt.Font> textFonts = Arrays.asList(
                new Font("Arial", Font.BOLD, 40),
                new Font("Courier", Font.BOLD, 40));
            captcha = new Captcha.Builder(_width, _height)
                .addText(new DefaultTextProducer(5),
                         new DefaultWordRenderer(Color.WHITE, textFonts))
                .gimp()
                .addNoise()
                .addBackground()
                .build();
            
            session.setAttribute(NAME, captcha);
            writeImage(resp, captcha.getImage());
            
        }
        
        captcha = (Captcha) session.getAttribute(NAME);
        writeImage(resp, captcha.getImage());
    
        return true;
    }
    
    public static void writeImage(HttpServletResponse response, BufferedImage bi) {
        response.setHeader("Cache-Control", "private,no-cache,no-store");
        response.setContentType("image/png"); // PNGs allow for transparency. JPGs do not.
        try {
            CaptchaServletUtil.writeImage(response.getOutputStream(), bi);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
