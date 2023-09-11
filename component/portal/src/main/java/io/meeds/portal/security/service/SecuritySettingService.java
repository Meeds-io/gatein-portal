/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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
package io.meeds.portal.security.service;

import static io.meeds.portal.security.constant.UserRegistrationType.OPEN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.model.RegistrationSetting;

public class SecuritySettingService {

  protected static final String               INTERNAL_USERS_GROUP               = "/platform/users";

  protected static final String               EXTERNAL_USERS_GROUP               = "/platform/externals";

  protected static final Context              SECURITY_CONTEXT                   = Context.GLOBAL.id("SECURITY");

  protected static final Scope                SECURITY_SCOPE                     = Scope.APPLICATION.id("SECURITY");

  protected static final String               REGISTRATION_TYPE_PARAM            = "REGISTRATION_TYPE";

  protected static final String               REGISTRATION_EXTERNAL_USER_PARAM   = "REGISTRATION_EXTERNAL_USER";

  protected static final String               REGISTRATION_EXTRA_GROUPS_PARAM    = "REGISTRATION_EXTRA_GROUPS";

  protected static final String               EXTRA_GROUPS_SEPARATOR             = ",";

  protected static final UserRegistrationType DEFAULT_REGISTRATION_TYPE          =
                                                                        UserRegistrationType.valueOf(System.getProperty("meeds.settings.access.type.default",
                                                                                                                        OPEN.name()).toUpperCase());

  protected static final boolean              DEFAULT_REGISTRATION_EXTERNAL_USER =
                                                                                 Boolean.parseBoolean(System.getProperty("meeds.settings.access.externalUsers",
                                                                                                                         "false").toLowerCase());

  private RegistrationSetting                 registrationSetting;

  private SettingService                      settingService;

  public SecuritySettingService(SettingService settingService) {
    this.settingService = settingService;
  }

  public RegistrationSetting getRegistrationSetting() {
    if (registrationSetting == null) {
      registrationSetting = new RegistrationSetting(getRegistrationType(),
                                                    isRegistrationExternalUser(),
                                                    getRegistrationExtraGroupIds());
    }
    return registrationSetting;
  }

  public void saveRegistrationSetting(RegistrationSetting registrationSetting) {
    saveRegistrationType(registrationSetting.getType());
    saveRegistrationExternalUser(registrationSetting.isExternalUser());
    saveRegistrationExtraGroupIds(registrationSetting.getExtraGroupIds());
  }

  public String[] getRegistrationGroupIds() {
    List<String> registrationExtraGroupIds = new ArrayList<>(Arrays.asList(getRegistrationExtraGroupIds()));
    if (isRegistrationExternalUser()) {
      registrationExtraGroupIds.add(EXTERNAL_USERS_GROUP);
    } else {
      registrationExtraGroupIds.add(INTERNAL_USERS_GROUP);
    }
    return registrationExtraGroupIds.stream().filter(StringUtils::isNotBlank).distinct().toList().toArray(new String[0]);
  }

  public UserRegistrationType getRegistrationType() {
    SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT, SECURITY_SCOPE, REGISTRATION_TYPE_PARAM);
    if (settingValue == null || settingValue.getValue() == null) {
      return DEFAULT_REGISTRATION_TYPE;
    } else {
      return UserRegistrationType.valueOf(settingValue.getValue().toString());
    }
  }

  public void saveRegistrationType(UserRegistrationType registrationType) {
    try {
      if (registrationType == null) {
        registrationType = DEFAULT_REGISTRATION_TYPE;
      }
      settingService.set(SECURITY_CONTEXT,
                         SECURITY_SCOPE,
                         REGISTRATION_TYPE_PARAM,
                         SettingValue.create(registrationType.toString()));
    } finally {
      registrationSetting = null;
    }
  }

  public boolean isRegistrationExternalUser() {
    SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT, SECURITY_SCOPE, REGISTRATION_EXTERNAL_USER_PARAM);
    if (settingValue == null || settingValue.getValue() == null) {
      return DEFAULT_REGISTRATION_EXTERNAL_USER;
    } else {
      return Boolean.parseBoolean(settingValue.getValue().toString());
    }
  }

  public void saveRegistrationExternalUser(boolean externalUser) {
    try {
      settingService.set(SECURITY_CONTEXT,
                         SECURITY_SCOPE,
                         REGISTRATION_EXTERNAL_USER_PARAM,
                         SettingValue.create(String.valueOf(externalUser)));
    } finally {
      registrationSetting = null;
    }
  }

  public String[] getRegistrationExtraGroupIds() {
    SettingValue<?> settingValue = settingService.get(SECURITY_CONTEXT, SECURITY_SCOPE, REGISTRATION_EXTRA_GROUPS_PARAM);
    if (settingValue == null || settingValue.getValue() == null) {
      return new String[0];
    } else {
      return Arrays.stream(settingValue.getValue().toString().split(EXTRA_GROUPS_SEPARATOR))
                   .filter(StringUtils::isNotBlank)
                   .distinct()
                   .toArray(String[]::new);
    }
  }

  public void saveRegistrationExtraGroupIds(String[] groupIds) {
    try {
      if (groupIds == null) {
        groupIds = new String[0];
      }
      settingService.set(SECURITY_CONTEXT,
                         SECURITY_SCOPE,
                         REGISTRATION_EXTRA_GROUPS_PARAM,
                         SettingValue.create(StringUtils.join(groupIds, EXTRA_GROUPS_SEPARATOR)));
    } finally {
      registrationSetting = null;
    }
  }

}
