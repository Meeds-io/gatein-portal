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
package org.exoplatform.web.login.onboarding;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Locale;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.rest.UserFieldValidator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.exoplatform.web.register.RegisterHandler;
import org.exoplatform.web.register.RegistrationException;
import org.exoplatform.web.security.security.SecureRandomService;

public class OnboardingRegisterFilter implements Filter {

  public static final String              ON_BOARDING_EMAIL_SENT            = "onBoardingEmailSent";

  private static final Log                LOG                               =
                                              ExoLogger.getLogger(OnboardingRegisterFilter.class);

  public static final String              REGISTRATION_ERROR_CODE           = "REGISTRATION_ERROR";

  public static final String              ONBOARDING_REGISTER_REQUEST_PARAM = "onboardingRegister";

  public static final String              EMAIL_REQUEST_PARAM               = "email";

  private static final UserFieldValidator EMAIL_VALIDATOR                   =
                                                          new UserFieldValidator(EMAIL_REQUEST_PARAM, false, false);

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, // NOSONAR
                                                                                                          ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    // If user is already authenticated, no registration form is required
    if (request.getRemoteUser() != null
        || request.getParameter(ONBOARDING_REGISTER_REQUEST_PARAM) == null
        || request.getParameter(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN) == null) {
      chain.doFilter(request, response);
      return;
    }

    if (!ExoContainerContext.getService(OnboardingUIParamsForRegister.class).isRegisterEnabled()) {
      LOG.warn("An hack tentative for user registration was detected");
      response.setStatus(401);
      return;
    }

    String email = request.getParameter(EMAIL_REQUEST_PARAM);
    try {
      validateToken(request);
      validateEmail(email, request.getLocale());
      User user = registerUser(email);
      boolean sent = sendOnboardingEmail(user, request);
      if (sent) {
        request.setAttribute(ON_BOARDING_EMAIL_SENT, true);
      }
    } catch (RegistrationException e) {
      request.setAttribute(RegisterHandler.REGISTER_ERROR_PARAM, e.getMessage());
    } catch (Exception e) {
      LOG.warn("Error while sending onboarding mail to user {}", email, e);
      request.setAttribute(RegisterHandler.REGISTER_ERROR_PARAM, REGISTRATION_ERROR_CODE);
    }
    chain.doFilter(request, response);
  }

  private void validateToken(HttpServletRequest request) throws RegistrationException {
    Object token = request.getSession(true).getAttribute(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN);
    String tokenSent = request.getParameter(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN);
    if (!StringUtils.equals(String.valueOf(token), tokenSent)) {
      throw new RegistrationException(REGISTRATION_ERROR_CODE);
    }
    request.getSession(true).removeAttribute(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN);
  }

  private boolean sendOnboardingEmail(User user, HttpServletRequest request) {
    PasswordRecoveryService passwordRecoveryService = ExoContainerContext.getService(PasswordRecoveryService.class);
    StringBuilder url = getUrl(request);
    Locale locale = request.getLocale();
    return passwordRecoveryService.sendOnboardingEmail(user, locale, url);
  }

  private void validateEmail(String email, Locale locale) throws Exception {
    if (StringUtils.isBlank(email)) {
      throw new RegistrationException("EMAIL_MANDATORY");
    } else {
      String errorCode = EMAIL_VALIDATOR.validate(locale, email);
      if (StringUtils.isNotBlank(errorCode)) {
        throw new RegistrationException(errorCode);
      }

      OrganizationService organizationService = ExoContainerContext.getService(OrganizationService.class);

      ListAccess<User> users;
      int usersLength = 0;
      try {
        // Check if mail address is already used
        Query query = new Query();
        query.setEmail(email);

        users = organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY);
        usersLength = users == null ? 0 : users.getSize();
      } catch (RuntimeException e) {
        LOG.debug("Error retrieving users list with email {}. Thus, we will consider the email as already used", email, e);
        usersLength = 1;
      }
      if (usersLength > 0) {
        throw new RegistrationException("EMAIL_ALREADY_EXISTS");
      }

      User user = organizationService.getUserHandler().findUserByName(email);
      if (user != null) {
        throw new RegistrationException("USER_ALREADY_EXISTS");
      }
    }
  }

  public User registerUser(String email) throws RegistrationException {
    OrganizationService organizationService = ExoContainerContext.getService(OrganizationService.class);
    SecureRandomService secureRandomService = ExoContainerContext.getService(SecureRandomService.class);

    UserHandler userHandler = organizationService.getUserHandler();
    User user = userHandler.createUserInstance(email);
    try {
      user.setEmail(email);
      user.setFirstName(email);
      user.setLastName("");
      SecureRandom secureRandom = secureRandomService.getSecureRandom();
      user.setPassword(secureRandom.nextLong() + "-" + secureRandom.nextLong() + "-" + secureRandom.nextLong());

      userHandler.createUser(user, true);
      return user;
    } catch (RegistrationException e) {
      throw e;
    } catch (Exception e) {
      LOG.warn("Error regitering user", e);
      throw new RegistrationException(REGISTRATION_ERROR_CODE);
    }
  }

  private StringBuilder getUrl(HttpServletRequest request) {
    StringBuilder url = new StringBuilder();
    if (request != null) {
      url.append(request.getScheme()).append("://").append(request.getServerName());
      if (request.getServerPort() != 80 && request.getServerPort() != 443) {
        url.append(':').append(request.getServerPort());
      }
      url.append("/" + PortalContainer.getCurrentPortalContainerName());
    }
    return url;
  }
}
