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

import static io.meeds.portal.security.service.SecuritySettingService.DEFAULT_REGISTRATION_EXTERNAL_USER;
import static io.meeds.portal.security.service.SecuritySettingService.DEFAULT_REGISTRATION_TYPE;
import static io.meeds.portal.security.service.SecuritySettingService.EXTERNAL_USERS_GROUP;
import static io.meeds.portal.security.service.SecuritySettingService.EXTRA_GROUPS_SEPARATOR;
import static io.meeds.portal.security.service.SecuritySettingService.INTERNAL_USERS_GROUP;
import static io.meeds.portal.security.service.SecuritySettingService.REGISTRATION_EXTERNAL_USER_PARAM;
import static io.meeds.portal.security.service.SecuritySettingService.REGISTRATION_EXTRA_GROUPS_PARAM;
import static io.meeds.portal.security.service.SecuritySettingService.REGISTRATION_TYPE_PARAM;
import static io.meeds.portal.security.service.SecuritySettingService.SECURITY_CONTEXT;
import static io.meeds.portal.security.service.SecuritySettingService.SECURITY_SCOPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.commons.codec.binary.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.model.RegistrationSetting;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SettingSecurityServieTest {

  @Mock
  private SettingService         settingService;

  private SecuritySettingService securitySettingService;

  @Before
  public void setUp() {
    securitySettingService = new SecuritySettingService(settingService);
  }

  @Test
  public void testGetRegistrationSetting() {
    RegistrationSetting registrationSetting = securitySettingService.getRegistrationSetting();
    assertNotNull(registrationSetting); // NOSONAR
    assertEquals(DEFAULT_REGISTRATION_TYPE, registrationSetting.getType());
    assertEquals(DEFAULT_REGISTRATION_EXTERNAL_USER, registrationSetting.isExternalUser());
    assertNotNull(registrationSetting.getExtraGroupIds());
    assertEquals(0, registrationSetting.getExtraGroupIds().length);
  }

  @Test
  public void testSaveRegistrationSetting() {
    securitySettingService.saveRegistrationSetting(new RegistrationSetting());
    verify(settingService, times(3)).set(eq(SECURITY_CONTEXT), eq(SECURITY_SCOPE), anyString(), any());
  }

  @Test
  public void testGetRegistrationGroupIds() {
    String[] registrationGroupIds = securitySettingService.getRegistrationGroupIds();
    assertNotNull(registrationGroupIds);
    assertEquals(1, registrationGroupIds.length);
    assertEquals(INTERNAL_USERS_GROUP, registrationGroupIds[0]);

    when(settingService.get(eq(SECURITY_CONTEXT),
                            eq(SECURITY_SCOPE),
                            eq(REGISTRATION_EXTERNAL_USER_PARAM))).thenReturn((SettingValue) SettingValue.create(true));

    registrationGroupIds = securitySettingService.getRegistrationGroupIds();
    assertNotNull(registrationGroupIds);
    assertEquals(1, registrationGroupIds.length);
    assertEquals(EXTERNAL_USERS_GROUP, registrationGroupIds[0]);

    when(settingService.get(eq(SECURITY_CONTEXT),
                            eq(SECURITY_SCOPE),
                            eq(REGISTRATION_EXTERNAL_USER_PARAM))).thenReturn((SettingValue) SettingValue.create(false));

    registrationGroupIds = securitySettingService.getRegistrationGroupIds();
    assertNotNull(registrationGroupIds);
    assertEquals(1, registrationGroupIds.length);
    assertEquals(INTERNAL_USERS_GROUP, registrationGroupIds[0]);
  }

  @Test
  public void testGetRegistrationType() {
    UserRegistrationType registrationType = securitySettingService.getRegistrationType();
    assertNotNull(registrationType);
    assertEquals(DEFAULT_REGISTRATION_TYPE, registrationType);

    when(settingService.get(eq(SECURITY_CONTEXT),
                            eq(SECURITY_SCOPE),
                            eq(REGISTRATION_TYPE_PARAM))).thenReturn((SettingValue) SettingValue.create(UserRegistrationType.OPEN.name()));

    registrationType = securitySettingService.getRegistrationType();
    assertNotNull(registrationType);
    assertEquals(UserRegistrationType.OPEN, registrationType);

    when(settingService.get(eq(SECURITY_CONTEXT),
                            eq(SECURITY_SCOPE),
                            eq(REGISTRATION_TYPE_PARAM))).thenReturn((SettingValue) SettingValue.create(UserRegistrationType.RESTRICTED.name()));

    registrationType = securitySettingService.getRegistrationType();
    assertNotNull(registrationType);
    assertEquals(UserRegistrationType.RESTRICTED, registrationType);
  }

  @Test
  public void testSaveRegistrationType() {
    securitySettingService.saveRegistrationType(UserRegistrationType.OPEN);
    verify(settingService,
           times(1)).set(eq(SECURITY_CONTEXT),
                         eq(SECURITY_SCOPE),
                         eq(REGISTRATION_TYPE_PARAM),
                         argThat(args -> StringUtils.equals(args.getValue().toString(), UserRegistrationType.OPEN.name())));
    securitySettingService.saveRegistrationType(UserRegistrationType.RESTRICTED);
    verify(settingService,
           times(1)).set(eq(SECURITY_CONTEXT),
                         eq(SECURITY_SCOPE),
                         eq(REGISTRATION_TYPE_PARAM),
                         argThat(args -> StringUtils.equals(args.getValue().toString(), UserRegistrationType.RESTRICTED.name())));
  }

  @Test
  public void testIsRegistrationExternalUser() {
    assertEquals(DEFAULT_REGISTRATION_EXTERNAL_USER, securitySettingService.isRegistrationExternalUser());

    when(settingService.get(eq(SECURITY_CONTEXT),
                            eq(SECURITY_SCOPE),
                            eq(REGISTRATION_EXTERNAL_USER_PARAM))).thenReturn((SettingValue) SettingValue.create(true));
    assertTrue(securitySettingService.isRegistrationExternalUser());

    when(settingService.get(eq(SECURITY_CONTEXT),
                            eq(SECURITY_SCOPE),
                            eq(REGISTRATION_EXTERNAL_USER_PARAM))).thenReturn((SettingValue) SettingValue.create(false));
    assertFalse(securitySettingService.isRegistrationExternalUser());
  }

  @Test
  public void testSaveRegistrationExternalUser() {
    securitySettingService.saveRegistrationExternalUser(true);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(REGISTRATION_EXTERNAL_USER_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "true")));
    securitySettingService.saveRegistrationExternalUser(false);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(REGISTRATION_EXTERNAL_USER_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "false")));
  }

  @Test
  public void testGetRegistrationExtraGroupIds() {
    assertEquals(Arrays.asList(INTERNAL_USERS_GROUP), Arrays.asList(securitySettingService.getRegistrationGroupIds()));

    when(settingService.get(eq(SECURITY_CONTEXT),
                            eq(SECURITY_SCOPE),
                            eq(REGISTRATION_EXTRA_GROUPS_PARAM))).thenReturn((SettingValue) SettingValue.create(""));
    String[] registrationGroupIds = securitySettingService.getRegistrationGroupIds();
    assertNotNull(registrationGroupIds);
    assertEquals(1, registrationGroupIds.length);
    assertEquals(INTERNAL_USERS_GROUP, registrationGroupIds[0]);

    when(settingService.get(eq(SECURITY_CONTEXT),
                            eq(SECURITY_SCOPE),
                            eq(REGISTRATION_EXTRA_GROUPS_PARAM))).thenReturn((SettingValue) SettingValue.create("group1,group2"));
    registrationGroupIds = securitySettingService.getRegistrationGroupIds();
    assertNotNull(registrationGroupIds);
    assertEquals(3, registrationGroupIds.length);
    assertEquals("group1", registrationGroupIds[0]);
    assertEquals("group2", registrationGroupIds[1]);
    assertEquals(INTERNAL_USERS_GROUP, registrationGroupIds[2]);
  }

  @Test
  public void testSaveRegistrationExtraGroupIds() {
    securitySettingService.saveRegistrationExtraGroupIds(new String[0]);
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(REGISTRATION_EXTRA_GROUPS_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(), "")));
    securitySettingService.saveRegistrationExtraGroupIds(new String[] { "/group1", "/group2" });
    verify(settingService, times(1)).set(eq(SECURITY_CONTEXT),
                                         eq(SECURITY_SCOPE),
                                         eq(REGISTRATION_EXTRA_GROUPS_PARAM),
                                         argThat(args -> StringUtils.equals(args.getValue().toString(),
                                                                            "/group1" + EXTRA_GROUPS_SEPARATOR + "/group2")));
  }

}
