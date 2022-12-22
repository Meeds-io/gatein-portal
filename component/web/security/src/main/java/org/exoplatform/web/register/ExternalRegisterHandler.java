/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
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
package org.exoplatform.web.register;

import static org.exoplatform.web.security.security.CookieTokenService.EXTERNAL_REGISTRATION_TOKEN;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.wci.security.Credentials;
import org.json.JSONObject;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.rest.UserFieldValidator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.application.JspBasedWebHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

import nl.captcha.Captcha;
import nl.captcha.servlet.CaptchaServletUtil;
import nl.captcha.text.producer.DefaultTextProducer;
import nl.captcha.text.renderer.DefaultWordRenderer;

public class ExternalRegisterHandler extends JspBasedWebHandler {

  private static final Log               LOG                            = ExoLogger.getLogger(ExternalRegisterHandler.class);

  private static final QualifiedName     SERVER_CAPTCHA                 = QualifiedName.create("gtn", "serveCaptcha");

  public static final String             EMAIL_PARAM                    = "email";

  public static final String             LASTNAME_PARAM                 = "lastName";

  public static final String             FIRSTNAME_PARAM                = "firstName";

  public static final String             PASSWORD_PARAM                 = "password";

  public static final String             PASSWORD_CONFIRM_PARAM         = "password2";

  public static final UserFieldValidator PASSWORD_VALIDATOR             =
                                                            new UserFieldValidator(PASSWORD_PARAM, false, false, 8, 255);

  public static final UserFieldValidator LASTNAME_VALIDATOR             = new UserFieldValidator(LASTNAME_PARAM, false, true);

  public static final UserFieldValidator FIRSTNAME_VALIDATOR            = new UserFieldValidator(FIRSTNAME_PARAM, false, true);

  public static final String             NAME                           = "external-registration";

  public static final QualifiedName      TOKEN                          = QualifiedName.create("gtn", "token");

  public static final QualifiedName      LANG                           = QualifiedName.create("gtn", "lang");

  public static final QualifiedName      INIT_URL                       = QualifiedName.create("gtn", "initURL");

  public static final String             REQ_PARAM_ACTION               = "action";

  public static final String             EXTERNALS_GROUP                = "/platform/externals";

  public static final String             LOGIN                          = "/login";

  public static final String             USERS_GROUP                    = "/platform/users";

  public static final String             CAPTCHA_PARAM                  = "captcha";

  public static final String             ACTION_PARAM                   = "action";

  public static final String             SAVE_EXTERNAL_ACTION           = "saveExternal";

  public static final String             EXPIRED_ACTION_NAME            = "expired";

  public static final String             ERROR_MESSAGE_PARAM            = "error";

  public static final String             TOKEN_ID_PARAM                 = "tokenId";

  public static final String             SUCCESS_MESSAGE_PARAM          = "success";

  public static final int                CAPTCHA_WIDTH                  = 200;

  public static final int                CAPTCHA_HEIGHT                 = 50;

  public static final String             EXTERNAL_REGISTRATION_JSP_PATH = "/WEB-INF/jsp/externalRegistration/init_account.jsp"; // NOSONAR

  private static final String            MEMBER                         = "member";

  private ServletContext                 servletContext;

  private RemindPasswordTokenService     remindPasswordTokenService;

  private PasswordRecoveryService        passwordRecoveryService;

  private ResourceBundleService          resourceBundleService;

  private OrganizationService            organizationService;

  public ExternalRegisterHandler(PortalContainer container, // NOSONAR
                                 RemindPasswordTokenService remindPasswordTokenService,
                                 PasswordRecoveryService passwordRecoveryService,
                                 ResourceBundleService resourceBundleService,
                                 OrganizationService organizationService,
                                 LocaleConfigService localeConfigService,
                                 BrandingService brandingService,
                                 JavascriptConfigService javascriptConfigService,
                                 SkinService skinService) {
    super(localeConfigService, brandingService, javascriptConfigService, skinService);
    this.servletContext = container.getPortalContext();
    this.remindPasswordTokenService = remindPasswordTokenService;
    this.passwordRecoveryService = passwordRecoveryService;
    this.resourceBundleService = resourceBundleService;
    this.organizationService = organizationService;
  }

  @Override
  public String getHandlerName() {
    return NAME;
  }

  @Override
  public boolean execute(ControllerContext controllerContext) throws Exception {
    HttpServletRequest request = controllerContext.getRequest();
    HttpServletResponse response = controllerContext.getResponse();

    Locale locale = request.getLocale();
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(resourceBundleService.getSharedResourceBundleNames(),
                                                                            locale);

    String serveCaptcha = controllerContext.getParameter(SERVER_CAPTCHA);
    if ("true".equals(serveCaptcha)) {
      return serveCaptchaImage(request, response);
    }

    String token = controllerContext.getParameter(TOKEN);
    Map<String, Object> parameters = new HashMap<>();
    Credentials credentials = StringUtils.isBlank(token) ? null
                                                         : passwordRecoveryService.verifyToken(token,
                                                                                               EXTERNAL_REGISTRATION_TOKEN);
    if (credentials == null) {
      parameters.put(ACTION_PARAM, EXPIRED_ACTION_NAME);
      // Token expired
      return dispatch(controllerContext, request, response, parameters);
    }

    String requestAction = request.getParameter(REQ_PARAM_ACTION);
    String email = credentials.getUsername();

    if (findUser(email) != null) {
      redirectToLogin(request, response, email);
      return true;
    }

    if (SAVE_EXTERNAL_ACTION.equalsIgnoreCase(requestAction)) {
      String requestEmail = request.getParameter(EMAIL_PARAM);
      String firstName = request.getParameter(FIRSTNAME_PARAM);
      String lastName = request.getParameter(LASTNAME_PARAM);
      String password = request.getParameter(PASSWORD_PARAM);
      String confirmPass = request.getParameter(PASSWORD_CONFIRM_PARAM);
      String captcha = request.getParameter(CAPTCHA_PARAM);

      if (!isValidCaptch(request.getSession(), captcha)) {
        parameters.put(ERROR_MESSAGE_PARAM, resourceBundle.getString("gatein.forgotPassword.captchaError"));
      } else if (validateUserAndPassword(email, requestEmail, password, confirmPass, parameters, resourceBundle, locale)
          && validateUserFullName(firstName, lastName, parameters, locale)) {
        try {
          String username = createExternalUser(email, firstName, lastName, password);
          passwordRecoveryService.sendExternalConfirmationAccountEmail(username, locale, getBaseUrl(request));
          remindPasswordTokenService.deleteTokensByUsernameAndType(email, CookieTokenService.EXTERNAL_REGISTRATION_TOKEN);
          redirectToLogin(request, response, email);
          return true;
        } catch (Exception e) {
          LOG.warn("Error while registering external user", e);
          parameters.put(ERROR_MESSAGE_PARAM, resourceBundle.getString("external.registration.fail.create.user"));
        }
      }
      parameters.put(FIRSTNAME_PARAM, firstName);
      parameters.put(LASTNAME_PARAM, lastName);
      parameters.put(PASSWORD_PARAM, password);
      parameters.put(PASSWORD_CONFIRM_PARAM, confirmPass);
    }
    parameters.put(EMAIL_PARAM, escapeXssCharacters(email));
    parameters.put(TOKEN_ID_PARAM, token);

    return dispatch(controllerContext, request, response, parameters);
  }

  private boolean validateUserFullName(String firstName,
                                       String lastName,
                                       Map<String, Object> parameters,
                                       Locale locale) {
    String errorMessage = FIRSTNAME_VALIDATOR.validate(locale, firstName);
    if (StringUtils.isBlank(errorMessage)) {
      errorMessage = LASTNAME_VALIDATOR.validate(locale, lastName);
    }
    if (StringUtils.isNotBlank(errorMessage)) {
      parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
      return false;
    } else {
      return true;
    }
  }

  private boolean validateUserAndPassword(String tokenUsername,
                                          String requestedUsername,
                                          String password,
                                          String confirmPass,
                                          Map<String, Object> parameters,
                                          ResourceBundle bundle,
                                          Locale locale) {
    if (requestedUsername == null || !requestedUsername.equals(tokenUsername)) {
      String errorMessage = bundle.getString("gatein.forgotPassword.usernameChanged");
      errorMessage = errorMessage.replace("{0}", tokenUsername);
      parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
      return false;
    } else if (!StringUtils.equals(password, confirmPass)) {
      parameters.put(ERROR_MESSAGE_PARAM, bundle.getString("gatein.forgotPassword.confirmPasswordNotMatch"));
      return false;
    } else {
      String errorMessage = PASSWORD_VALIDATOR.validate(locale, password);
      if (StringUtils.isNotBlank(errorMessage)) {
        parameters.put(ERROR_MESSAGE_PARAM, errorMessage);
        return false;
      }
    }
    return true;
  }

  private String createExternalUser(String email, String firstName, String lastName, String password) throws Exception {
    String username = generateExternalRegistrationUsername(firstName, lastName);
    User externalUser = organizationService.getUserHandler().createUserInstance(username);
    externalUser.setFirstName(firstName);
    externalUser.setLastName(lastName);
    externalUser.setPassword(password);
    if (email != null) {
      externalUser.setEmail(email);
    }
    organizationService.getUserHandler().createUser(externalUser, true);
    deleteFromInternalUsersGroup(username);

    Group externalUsersGroup = organizationService.getGroupHandler().findGroupById(EXTERNALS_GROUP);
    MembershipType memberMembershipType = organizationService.getMembershipTypeHandler().findMembershipType(MEMBER);
    organizationService.getMembershipHandler()
                       .linkMembership(externalUser,
                                       externalUsersGroup,
                                       memberMembershipType,
                                       true);
    return username;
  }

  private void deleteFromInternalUsersGroup(String username) throws Exception {
    Collection<Membership> usersMemberhips = organizationService.getMembershipHandler()
                                                                .findMembershipsByUserAndGroup(username, USERS_GROUP);
    if (CollectionUtils.isNotEmpty(usersMemberhips)) {
      for (Membership usersMemberhip : usersMemberhips) {
        organizationService.getMembershipHandler().removeMembership(usersMemberhip.getId(), true);
      }
    }
  }

  private StringBuilder getBaseUrl(HttpServletRequest request) {
    StringBuilder url = new StringBuilder();
    url.append(request.getScheme()).append("://").append(request.getServerName());
    if (request.getServerPort() != 80 && request.getServerPort() != 443) {
      url.append(':').append(request.getServerPort());
    }
    url.append(servletContext.getContextPath());
    return url;
  }

  private boolean isValidCaptch(HttpSession session, String captchaValue) {
    Captcha captcha = (Captcha) session.getAttribute(NAME);
    return ((captcha != null) && (captcha.isCorrect(captchaValue)));
  }

  protected void extendApplicationParameters(JSONObject applicationParameters, Map<String, Object> additionalParameters) {
    additionalParameters.forEach(applicationParameters::put);
  }

  private boolean dispatch(ControllerContext controllerContext,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Map<String, Object> parameters) throws Exception {
    // Invalidate the Captcha
    request.getSession().removeAttribute(NAME);

    super.prepareDispatch(controllerContext,
                          "PORTLET/social-portlet/ExternalOnboarding",
                          Collections.emptyList(),
                          Collections.singletonList("portal/login"),
                          params -> extendApplicationParameters(params, parameters));
    servletContext.getRequestDispatcher(EXTERNAL_REGISTRATION_JSP_PATH).include(request, response);
    return true;
  }

  private void redirectToLogin(HttpServletRequest request, HttpServletResponse response, String email) throws IOException {
    // Invalidate the Captcha
    request.getSession().removeAttribute(NAME);

    response.sendRedirect(servletContext.getContextPath() + LOGIN + "?email=" + email);
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  private String escapeXssCharacters(String message) {
    message = (message == null) ? null
                                : message.replace("&", "&amp")
                                         .replace("<", "&lt;")
                                         .replace(">", "&gt;")
                                         .replace("\"", "&quot;")
                                         .replace("'", "&#x27;")
                                         .replace("/", "&#x2F;");
    return message;
  }

  private User findUser(String usernameOrEmail) throws Exception {
    User user = organizationService.getUserHandler().findUserByName(usernameOrEmail, UserStatus.ANY);
    if (user == null && usernameOrEmail.contains("@")) {
      Query query = new Query();
      query.setEmail(usernameOrEmail);
      ListAccess<User> list = organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY);
      if (list != null && list.getSize() > 0) {
        user = list.load(0, 1)[0];
      }
    }
    return user;
  }

  private boolean serveCaptchaImage(HttpServletRequest req, HttpServletResponse resp) {
    HttpSession session = req.getSession();
    Captcha captcha;
    if (session.getAttribute(NAME) == null) {
      List<Font> textFonts = Arrays.asList(new Font("Arial", Font.BOLD, 40), new Font("Courier", Font.BOLD, 40));
      captcha = new Captcha.Builder(CAPTCHA_WIDTH, CAPTCHA_HEIGHT)
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

  private void writeImage(HttpServletResponse response, BufferedImage bi) {
    response.setHeader("Cache-Control", "private,no-cache,no-store");
    response.setContentType("image/png"); // PNGs allow for transparency. JPGs
                                          // do not.
    try {
      CaptchaServletUtil.writeImage(response.getOutputStream(), bi);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  private String generateExternalRegistrationUsername(String firstname, String lastname) throws Exception {
    String userNameBase = new StringBuffer(firstname.replaceAll("\\s", "")).append(".")
                                                                           .append(lastname.replaceAll("\\s", ""))
                                                                           .toString()
                                                                           .toLowerCase();
    userNameBase = unAccent(userNameBase);
    String username = userNameBase;
    Random rand = new Random();// NOSONAR
    // Check if user name already existed (with identity manager, need to
    // move the handler to social)
    while (findUser(username) != null) {
      int num = rand.nextInt(89) + 10;// range between 10 and 99.
      username = userNameBase + String.valueOf(num);
    }
    return username;
  }

  private String unAccent(String src) {
    return Normalizer.normalize(src, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").replace("'", "");
  }
}
