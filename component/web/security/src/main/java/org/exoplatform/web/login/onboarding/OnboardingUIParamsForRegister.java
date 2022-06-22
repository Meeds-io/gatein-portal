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

import java.security.SecureRandom;
import java.util.*;

import javax.servlet.http.HttpSession;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.login.LoginHandler;
import org.exoplatform.web.login.UIParamsExtension;
import org.exoplatform.web.register.RegisterHandler;
import org.exoplatform.web.security.security.SecureRandomService;

public class OnboardingUIParamsForRegister implements UIParamsExtension {

  public static final String        ONBOARDING_ENABLED_PARAM    = "enabled";

  public static final String        ONBOARDING_REGISTER_ENABLED = "onboardingRegisterEnabled";

  public static final String        ONBOARDING_REGISTER_TOKEN   = "onboardingRegisterToken";

  private static final List<String> EXTENSION_NAMES             = Arrays.asList(RegisterHandler.REGISTER_EXTENSION_NAME,
                                                                                LoginHandler.LOGIN_EXTENSION_NAME);

  private SecureRandomService       secureRandomService;

  private boolean                   registerEnabled;

  public OnboardingUIParamsForRegister(SecureRandomService secureRandomService, InitParams params) {
    this.secureRandomService = secureRandomService;
    if (params != null && params.containsKey(ONBOARDING_ENABLED_PARAM)) {
      this.registerEnabled = Boolean.parseBoolean(params.getValueParam(ONBOARDING_ENABLED_PARAM).getValue());
    }
  }

  @Override
  public List<String> getExtensionNames() {
    return EXTENSION_NAMES;
  }

  @Override
  public Map<String, Object> extendParameters(ControllerContext controllerContext, String extensionName) {
    if (this.registerEnabled) {
      Map<String, Object> params = new HashMap<>();
      params.put(RegisterHandler.REGISTER_ENABLED, true);
      params.put(ONBOARDING_REGISTER_ENABLED, true);
      if (controllerContext.getRequest().getAttribute(OnboardingRegisterFilter.ON_BOARDING_EMAIL_SENT) == Boolean.TRUE) {
        params.put(OnboardingRegisterFilter.ON_BOARDING_EMAIL_SENT, true);
      }
      params.put(ONBOARDING_REGISTER_TOKEN, generateRegisterToken(controllerContext.getRequest().getSession(true)));
      return params;
    }
    return Collections.emptyMap();
  }

  public String generateRegisterToken(HttpSession session) {
    String token = getRegisterToken(session);
    if (token != null) {
      return token;
    }
    SecureRandom secureRandom = secureRandomService.getSecureRandom();
    token = secureRandom.nextLong() + "-" + secureRandom.nextLong() + "-" + secureRandom.nextLong();
    session.setAttribute(ONBOARDING_REGISTER_TOKEN, token);
    return token;
  }

  public String getRegisterToken(HttpSession session) {
    return session == null ? null : (String) session.getAttribute(ONBOARDING_REGISTER_TOKEN);
  }

  public boolean isRegisterEnabled() {
    return registerEnabled;
  }
}
