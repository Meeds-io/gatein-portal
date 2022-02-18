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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

import java.security.SecureRandom;

import javax.servlet.FilterChain;
import javax.servlet.http.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.impl.UserImpl;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.exoplatform.web.security.security.SecureRandomService;

@RunWith(PowerMockRunner.class)
public class TestOnboardingRegisterFilter {

  @Test
  @PrepareForTest({ ExoContainerContext.class })
  public void testFilter() throws Exception {// NOSONAR
    mockStatic(ExoContainerContext.class);

    String email = "test2@user.com";
    String token = "token2";

    OnboardingUIParamsForRegister onboardingUIParamsForRegister = mock(OnboardingUIParamsForRegister.class);
    when(ExoContainerContext.getService(OnboardingUIParamsForRegister.class)).thenReturn(onboardingUIParamsForRegister);
    PasswordRecoveryService passwordRecoveryService = mock(PasswordRecoveryService.class);
    when(ExoContainerContext.getService(PasswordRecoveryService.class)).thenReturn(passwordRecoveryService);
    OrganizationService organizationService = mock(OrganizationService.class);
    when(ExoContainerContext.getService(OrganizationService.class)).thenReturn(organizationService);
    SecureRandomService secureRandomService = mock(SecureRandomService.class);
    when(ExoContainerContext.getService(SecureRandomService.class)).thenReturn(secureRandomService);
    SecureRandom secureRandom = mock(SecureRandom.class);
    when(secureRandomService.getSecureRandom()).thenReturn(secureRandom);
    when(secureRandom.nextLong()).thenReturn(1558666l);
    UserHandler userHandler = mock(UserHandler.class);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.createUserInstance(any())).thenAnswer(invocation -> new UserImpl(invocation.getArgument(0, String.class)));
    when(userHandler.findUsersByQuery(any(), any())).thenReturn(null);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getSession(true)).thenReturn(session);

    OnboardingRegisterFilter onboardingRegisterFilter = new OnboardingRegisterFilter();
    onboardingRegisterFilter.doFilter(request, response, chain);

    verify(request, times(0)).setAttribute(any(), any());

    when(request.getParameter(OnboardingRegisterFilter.ONBOARDING_REGISTER_REQUEST_PARAM)).thenReturn("true");
    when(request.getParameter(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN)).thenReturn(token);
    when(request.getParameter(OnboardingRegisterFilter.EMAIL_REQUEST_PARAM)).thenReturn(email);
    when(session.getAttribute(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN)).thenReturn(token);
    when(onboardingUIParamsForRegister.isRegisterEnabled()).thenReturn(true);
    when(passwordRecoveryService.sendOnboardingEmail(any(), any(), any())).thenReturn(true);

    onboardingRegisterFilter.doFilter(request, response, chain);

    verify(userHandler, times(1)).createUser(any(), eq(true));
    verify(passwordRecoveryService, times(1)).sendOnboardingEmail(any(), any(), any());
    verify(request, times(1)).setAttribute(eq(OnboardingRegisterFilter.ON_BOARDING_EMAIL_SENT), eq(true));
  }

  @Test
  @PrepareForTest({ ExoContainerContext.class })
  public void testRegisterWithInvalidToken() throws Exception {// NOSONAR
    mockStatic(ExoContainerContext.class);

    String email = "test@user.com";
    String token = "token";

    OnboardingUIParamsForRegister onboardingUIParamsForRegister = mock(OnboardingUIParamsForRegister.class);
    when(ExoContainerContext.getService(OnboardingUIParamsForRegister.class)).thenReturn(onboardingUIParamsForRegister);
    PasswordRecoveryService passwordRecoveryService = mock(PasswordRecoveryService.class);
    when(ExoContainerContext.getService(PasswordRecoveryService.class)).thenReturn(passwordRecoveryService);
    OrganizationService organizationService = mock(OrganizationService.class);
    when(ExoContainerContext.getService(OrganizationService.class)).thenReturn(organizationService);
    SecureRandomService secureRandomService = mock(SecureRandomService.class);
    when(ExoContainerContext.getService(SecureRandomService.class)).thenReturn(secureRandomService);
    SecureRandom secureRandom = mock(SecureRandom.class);
    when(secureRandomService.getSecureRandom()).thenReturn(secureRandom);
    when(secureRandom.nextLong()).thenReturn(1558666l);
    UserHandler userHandler = mock(UserHandler.class);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.createUserInstance(any())).thenAnswer(invocation -> new UserImpl(invocation.getArgument(0, String.class)));
    when(userHandler.findUsersByQuery(any(), any())).thenReturn(null);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getSession(true)).thenReturn(session);

    when(request.getParameter(OnboardingRegisterFilter.ONBOARDING_REGISTER_REQUEST_PARAM)).thenReturn("true");
    when(request.getParameter(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN)).thenReturn(token);
    when(request.getParameter(OnboardingRegisterFilter.EMAIL_REQUEST_PARAM)).thenReturn(email);
    when(session.getAttribute(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN)).thenReturn("invalidToken");
    when(onboardingUIParamsForRegister.isRegisterEnabled()).thenReturn(true);

    OnboardingRegisterFilter onboardingRegisterFilter = new OnboardingRegisterFilter();
    onboardingRegisterFilter.doFilter(request, response, chain);

    verify(userHandler, times(0)).createUser(any(), eq(true));
    verify(passwordRecoveryService, times(0)).sendOnboardingEmail(any(), any(), any());
    verify(request, times(0)).setAttribute(eq(OnboardingRegisterFilter.ON_BOARDING_EMAIL_SENT), eq(true));
  }

  @Test
  @PrepareForTest({ ExoContainerContext.class })
  public void testRegisterWithInvalidEmail() throws Exception {// NOSONAR
    mockStatic(ExoContainerContext.class);

    String email = "invalid";
    String token = "token";

    OnboardingUIParamsForRegister onboardingUIParamsForRegister = mock(OnboardingUIParamsForRegister.class);
    when(ExoContainerContext.getService(OnboardingUIParamsForRegister.class)).thenReturn(onboardingUIParamsForRegister);
    PasswordRecoveryService passwordRecoveryService = mock(PasswordRecoveryService.class);
    when(ExoContainerContext.getService(PasswordRecoveryService.class)).thenReturn(passwordRecoveryService);
    OrganizationService organizationService = mock(OrganizationService.class);
    when(ExoContainerContext.getService(OrganizationService.class)).thenReturn(organizationService);
    SecureRandomService secureRandomService = mock(SecureRandomService.class);
    when(ExoContainerContext.getService(SecureRandomService.class)).thenReturn(secureRandomService);
    SecureRandom secureRandom = mock(SecureRandom.class);
    when(secureRandomService.getSecureRandom()).thenReturn(secureRandom);
    when(secureRandom.nextLong()).thenReturn(1558666l);
    UserHandler userHandler = mock(UserHandler.class);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.createUserInstance(any())).thenAnswer(invocation -> new UserImpl(invocation.getArgument(0, String.class)));
    when(userHandler.findUsersByQuery(any(), any())).thenReturn(null);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    FilterChain chain = mock(FilterChain.class);

    when(request.getSession(true)).thenReturn(session);

    when(request.getParameter(OnboardingRegisterFilter.ONBOARDING_REGISTER_REQUEST_PARAM)).thenReturn("true");
    when(request.getParameter(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN)).thenReturn(token);
    when(request.getParameter(OnboardingRegisterFilter.EMAIL_REQUEST_PARAM)).thenReturn(email);
    when(session.getAttribute(OnboardingUIParamsForRegister.ONBOARDING_REGISTER_TOKEN)).thenReturn(token);
    when(onboardingUIParamsForRegister.isRegisterEnabled()).thenReturn(true);
    when(passwordRecoveryService.sendOnboardingEmail(any(), any(), any())).thenReturn(true);

    OnboardingRegisterFilter onboardingRegisterFilter = new OnboardingRegisterFilter();
    onboardingRegisterFilter.doFilter(request, response, chain);

    verify(userHandler, times(0)).createUser(any(), eq(true));
    verify(passwordRecoveryService, times(0)).sendOnboardingEmail(any(), any(), any());
    verify(request, times(0)).setAttribute(eq(OnboardingRegisterFilter.ON_BOARDING_EMAIL_SENT), eq(true));

    // Test valid mail
    when(request.getParameter(OnboardingRegisterFilter.EMAIL_REQUEST_PARAM)).thenReturn("test@user.com");
    onboardingRegisterFilter.doFilter(request, response, chain);

    verify(userHandler, times(1)).createUser(any(), eq(true));
    verify(passwordRecoveryService, times(1)).sendOnboardingEmail(any(), any(), any());
    verify(request, times(1)).setAttribute(eq(OnboardingRegisterFilter.ON_BOARDING_EMAIL_SENT), eq(true));
  }

}
