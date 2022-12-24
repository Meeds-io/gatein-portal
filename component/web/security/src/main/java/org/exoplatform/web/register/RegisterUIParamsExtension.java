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
package org.exoplatform.web.register;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.login.LoginHandler;
import org.exoplatform.web.login.UIParamsExtension;

public class RegisterUIParamsExtension implements UIParamsExtension {

  public static final String        ONBOARDING_ENABLED_PARAM    = "enabled";

  public static final String        ONBOARDING_REGISTER_ENABLED = "onboardingRegisterEnabled";

  private static final List<String> EXTENSION_NAMES             = Arrays.asList(RegisterHandler.REGISTER_EXTENSION_NAME,
                                                                                LoginHandler.LOGIN_EXTENSION_NAME);

  private boolean                   registerEnabled;

  public RegisterUIParamsExtension(InitParams params) {
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
      return params;
    }
    return Collections.emptyMap();
  }

  public boolean isRegisterEnabled() {
    return registerEnabled;
  }
}
