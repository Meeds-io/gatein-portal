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

package org.exoplatform.web.login.externalRegistration;

import nl.captcha.Captcha;
import nl.captcha.servlet.CaptchaServletUtil;
import nl.captcha.text.producer.DefaultTextProducer;
import nl.captcha.text.renderer.DefaultWordRenderer;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.login.recovery.PasswordRecoveryServiceImpl;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.wci.security.Credentials;

import javax.portlet.PortletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class ExternalRegistrationHandler extends WebRequestHandler {
    private static final QualifiedName SERVER_CAPTCHA = QualifiedName.create("gtn", "serveCaptcha");

    protected static Log                   log              = ExoLogger.getLogger(ExternalRegistrationHandler.class);


    public static final String NAME = "external-registration";

    public static final QualifiedName TOKEN = QualifiedName.create("gtn", "token");
    public static final QualifiedName LANG = QualifiedName.create("gtn", "lang");
    public static final QualifiedName INIT_URL = QualifiedName.create("gtn", "initURL");

    public static final String REQ_PARAM_ACTION = "action";
    public static final String EXTERNALS_GROUP = "/platform/externals";
    public static final String LOGIN = "/login";
    public static final String USERS_GROUP = "/platform/users";

    private static final ThreadLocal<Locale> currentLocale = new ThreadLocal<Locale>();
    
    private static final String MEMBER  = "member";

    protected int _width = 250;

    protected int _height = 50;


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

        StringBuilder url = new StringBuilder();
        url.append(req.getScheme()).append("://").append(req.getServerName());
        if (req.getServerPort() != 80 && req.getServerPort() != 443) {
            url.append(':').append(req.getServerPort());
        }
        url.append(container.getPortalContext().getContextPath());

        PasswordRecoveryServiceImpl service = getService(PasswordRecoveryServiceImpl.class);
        ResourceBundleService bundleService = getService(ResourceBundleService.class);
        OrganizationService organizationService = getService(OrganizationService.class);
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
            Credentials credentials = service.verifyToken(tokenId,remindPasswordTokenService.EXTERNAL_REGISTRATION_TOKEN);;
            if (credentials == null) {
                //. TokenId is expired
                return dispatch("/externalRegistration/jsp/token_expired.jsp", servletContext, req, res);
            }
            String email = credentials.getUsername();
            String currentPortalContainerName = PortalContainer.getCurrentPortalContainerName();
            if ("saveExternal".equalsIgnoreCase(requestAction)) {
                String reqFirstName = req.getParameter("firstName");
                String reqLastName = req.getParameter("lastName");
                String password = req.getParameter("password");
                String confirmPass = req.getParameter("password2");
                String captcha = req.getParameter("captcha");

                List<String> errors = new ArrayList<String>();
                if (captcha == null || !isValid(req.getSession(), captcha)) {
                    String message = bundle.getString("gatein.forgotPassword.captchaError");
                    errors.add(message);
                }

                if (reqFirstName == null || reqLastName == null) {

                    String message = bundle.getString("external.registration.emptyFirstNameOrLastName");
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


                if (errors.isEmpty()) {
                    String username = generateExternalRegistrationUsername(reqFirstName, reqLastName);
                    String randomUserName = username;
                    // Check if user name already existed (with identity manager, need to move the handler to social)
                    while (organizationService.getUserHandler().findUserByName(randomUserName, UserStatus.ANY) != null) {
                        Random rand = new Random();
                        int num = rand.nextInt(89) + 10;// range between 10 and 99.
                        randomUserName = username + String.valueOf(num);
                    }

                    User user = organizationService.getUserHandler().createUserInstance(randomUserName);
                    user.setFirstName(reqFirstName);
                    user.setLastName(reqLastName);
                    user.setPassword(password);
                    if (email != null) {
                      user.setEmail(email);
                    }
                    try {
                        organizationService.getUserHandler().createUser(user, true);// Broadcast user creation event
                        Group group = organizationService.getGroupHandler().findGroupById(EXTERNALS_GROUP);
                        if (organizationService.getMembershipTypeHandler() != null) {
                            Collection<Membership>  usersMemberhips = organizationService.getMembershipHandler().findMembershipsByUserAndGroup(user.getUserName(), USERS_GROUP);
                            for (Membership usersMemberhip : usersMemberhips) {
                              organizationService.getMembershipHandler().removeMembership(usersMemberhip.getId(), true);
                            }
                            organizationService.getMembershipHandler().linkMembership(user, group, organizationService.getMembershipTypeHandler().findMembershipType(MEMBER), true);
                            service.sendExternalConfirmationAccountEmail(randomUserName, locale, url);
                            remindPasswordTokenService.deleteTokensByUsernameAndType(email,CookieTokenService.EXTERNAL_REGISTRATION_TOKEN);
                        }
                    } catch (Exception e) {
                        errors.add(bundle.getString("external.registration.fail.create.user"));
                        req.setAttribute("password", password);
                        req.setAttribute("password2", confirmPass);
                        req.setAttribute("firstName", reqFirstName);
                        req.setAttribute("lastName", reqLastName);
                        req.setAttribute("errors", errors);
                        req.setAttribute("tokenId", tokenId);
                        return dispatch("/externalRegistration/jsp/reset_password.jsp", servletContext, req, res);
                    }
                    res.sendRedirect("/" + currentPortalContainerName + LOGIN + "?email=" + email);
                    return true;
                }
                req.setAttribute("password", password);
                req.setAttribute("password2", confirmPass);
                req.setAttribute("firstName", reqFirstName);
                req.setAttribute("lastName", reqLastName);
                req.setAttribute("errors", errors);
            }
            req.setAttribute("tokenId", tokenId);
            Query query = new Query();
            query.setEmail(email);
            if (organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY).getSize() > 0) {
              res.sendRedirect("/" + currentPortalContainerName + LOGIN + "?email=" + email);
              return true;
            }
            return dispatch("/externalRegistration/jsp/reset_password.jsp", servletContext, req, res);
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
                                                                                            IOException {
        HttpSession session = req.getSession();
        Captcha captcha;
        if (session.getAttribute(NAME) == null) {
            List<Font> textFonts = Arrays.asList(
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



    private static String generateExternalRegistrationUsername (String firstname, String lastname) {

        StringBuffer username = new StringBuffer(firstname.replaceAll("\\s","")).append(".").append(lastname.replaceAll("\\s",""));

        //convert username to lowercase
        return unAccent(username.toString().toLowerCase());
    }

    private static String unAccent(String src) {
        return Normalizer
            .normalize(src, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .replaceAll("'","");
    }
}
