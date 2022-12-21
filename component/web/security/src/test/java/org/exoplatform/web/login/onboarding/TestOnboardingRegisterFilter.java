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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.impl.UserImpl;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.exoplatform.web.security.security.SecureRandomService;

@RunWith(MockitoJUnitRunner.class)
public class TestOnboardingRegisterFilter {

  @Mock
  private PortalContainer               container;

  @Mock
  private PasswordRecoveryService       passwordRecoveryService;

  @Mock
  private OnboardingUIParamsForRegister onboardingUIParamsForRegister;

  @Mock
  private OrganizationService           organizationService;

  @Mock
  private SecureRandomService           secureRandomService;

  @Before
  public void setUp() {
    ExoContainerContext.setCurrentContainer(container);
    when(container.getComponentInstanceOfType(PasswordRecoveryService.class)).thenReturn(passwordRecoveryService);
    when(container.getComponentInstanceOfType(OnboardingUIParamsForRegister.class)).thenReturn(onboardingUIParamsForRegister);
    when(container.getComponentInstanceOfType(OrganizationService.class)).thenReturn(organizationService);
    when(container.getComponentInstanceOfType(SecureRandomService.class)).thenReturn(secureRandomService);
  }

  @After
  public void teardown() {
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void testFilter() throws Exception {// NOSONAR
    String email = "test2@user.com";
    String token = "token2";

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
  public void testRegisterWithInvalidToken() throws Exception {// NOSONAR

    String email = "test@user.com";
    String token = "token";

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

    verify(organizationService, times(0)).getUserHandler();
    verify(passwordRecoveryService, times(0)).sendOnboardingEmail(any(), any(), any());
    verify(request, times(0)).setAttribute(eq(OnboardingRegisterFilter.ON_BOARDING_EMAIL_SENT), eq(true));
  }

  @Test
  public void testRegisterWithInvalidEmail() throws Exception {// NOSONAR
    String email = "invalid";
    String token = "token";

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
