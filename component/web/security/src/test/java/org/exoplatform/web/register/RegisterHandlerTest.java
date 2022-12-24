/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.web.register;

import static org.exoplatform.web.register.RegisterHandler.CAPTCHA_PARAM;
import static org.exoplatform.web.register.RegisterHandler.EMAIL_PARAM;
import static org.exoplatform.web.register.RegisterHandler.ERROR_MESSAGE_PARAM;
import static org.exoplatform.web.register.RegisterHandler.NAME;
import static org.exoplatform.web.register.RegisterHandler.ON_BOARDING_EMAIL_SENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;
import org.exoplatform.web.security.security.RemindPasswordTokenService;

import nl.captcha.Captcha;

@RunWith(MockitoJUnitRunner.class)
public class RegisterHandlerTest {

  private static final Locale        REQUEST_LOCALE = Locale.ENGLISH;

  private static final String        CONTEXT_PATH   = "/portal";

  private static final String        EMAIL          = "email@test.com";

  private static final String        CAPTCHA_VALUE  = "captchaValue";

  @Mock
  private ServletContext             servletContext;

  @Mock
  private PortalContainer            container;

  @Mock
  private RemindPasswordTokenService remindPasswordTokenService;

  @Mock
  private PasswordRecoveryService    passwordRecoveryService;

  @Mock
  private ResourceBundleService      resourceBundleService;

  @Mock
  private ResourceBundle             resourceBundle;

  @Mock
  private OrganizationService        organizationService;

  @Mock
  private UserHandler                userHandler;

  @Mock
  private GroupHandler               groupHandler;

  @Mock
  private MembershipTypeHandler      membershipTypeHandler;

  @Mock
  private MembershipHandler          membershipHandler;

  @Mock
  private LocaleConfigService        localeConfigService;

  @Mock
  private BrandingService            brandingService;

  @Mock
  private RegisterUIParamsExtension  registerUIParamsExtension;

  @Mock
  private JavascriptConfigService    javascriptConfigService;

  @Mock
  private WebAppController           controller;

  @Mock
  private Router                     router;

  @Mock
  private InitParams                 params;

  @Mock
  private HttpSession                session;

  @Mock
  private HttpServletRequest         request;

  @Mock
  private HttpServletResponse        response;

  @Mock
  private RequestDispatcher          requestDispatcher;

  @Mock
  private SkinService                skinService;

  @Mock
  private Captcha                    captcha;

  private ControllerContext          controllerContext;

  private RegisterHandler            registerHandler;

  private Map<String, Object>        applicationParameters;

  @Before
  public void setUp() throws Exception {
    this.applicationParameters = null;
    ExoContainerContext.setCurrentContainer(container);
    lenient().when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(resourceBundleService);

    when(container.getPortalContext()).thenReturn(servletContext);
    when(request.getContextPath()).thenReturn(CONTEXT_PATH);
    when(request.getSession()).thenReturn(session);
    when(session.getAttribute(NAME)).thenReturn(captcha);
    when(captcha.isCorrect(CAPTCHA_VALUE)).thenReturn(true);
    when(request.getParameter(CAPTCHA_PARAM)).thenReturn(CAPTCHA_VALUE);
    when(request.getLocale()).thenReturn(REQUEST_LOCALE);
    LocaleConfigImpl localeConfig = new LocaleConfigImpl();
    localeConfig.setLocale(REQUEST_LOCALE);
    when(localeConfigService.getLocaleConfig(REQUEST_LOCALE.getLanguage())).thenReturn(localeConfig);

    when(resourceBundleService.getSharedResourceBundleNames()).thenReturn(new String[0]);
    when(resourceBundleService.getResourceBundle(any(String[].class), eq(REQUEST_LOCALE))).thenReturn(resourceBundle);
    when(resourceBundle.getString(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    when(javascriptConfigService.getJSConfig(any(), eq(REQUEST_LOCALE))).thenReturn(new JSONObject());
    when(servletContext.getRequestDispatcher(any())).thenReturn(requestDispatcher);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(registerUIParamsExtension.isRegisterEnabled()).thenReturn(true);

    registerHandler = new RegisterHandler(container, // NOSONAR
                                          resourceBundleService,
                                          passwordRecoveryService,
                                          organizationService,
                                          localeConfigService,
                                          brandingService,
                                          javascriptConfigService,
                                          skinService,
                                          registerUIParamsExtension,
                                          params) {
      @Override
      protected void extendApplicationParameters(ControllerContext controllerContext,
                                                 JSONObject applicationParameters,
                                                 Map<String, Object> additionalParameters) {
        RegisterHandlerTest.this.applicationParameters = additionalParameters;
        super.extendApplicationParameters(controllerContext, applicationParameters, additionalParameters);
      }
    };
  }

  @After
  public void teardown() throws Exception {
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void testGetRequiresLifeCycle() {
    assertTrue(registerHandler.getRequiresLifeCycle());
  }

  @Test
  public void testGetHandlerName() {
    assertEquals(NAME, registerHandler.getHandlerName());
  }

  @Test
  public void testDisplayRegisterPage() throws Exception {
    prepareResetPasswordContext();

    registerHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertNull(applicationParameters.get(EMAIL_PARAM));
    assertFalse(applicationParameters.containsKey(ERROR_MESSAGE_PARAM));

    verify(passwordRecoveryService, never()).sendExternalRegisterEmail(any(), any(), any(), any(), any(), eq(false));
  }

  @Test
  public void testRegisterWithBadEmailFormat() throws Exception {
    prepareResetPasswordContext();

    String email = "testFakeEmail";
    when(request.getParameter(EMAIL_PARAM)).thenReturn(email);

    registerHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(email, applicationParameters.get(EMAIL_PARAM));
    assertEquals("EmailAddressValidator.msg.Invalid-input", applicationParameters.get(ERROR_MESSAGE_PARAM));

    verify(passwordRecoveryService, never()).sendExternalRegisterEmail(any(), any(), any(), any(), any(), eq(false));
  }

  @Test
  public void testRegisterWithInvalidCaptcha() throws Exception {
    prepareResetPasswordContext();

    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(captcha.isCorrect(CAPTCHA_VALUE)).thenReturn(false);

    registerHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(EMAIL, applicationParameters.get(EMAIL_PARAM));
    assertEquals("gatein.forgotPassword.captchaError", applicationParameters.get(ERROR_MESSAGE_PARAM));

    verify(passwordRecoveryService, never()).sendExternalRegisterEmail(any(), any(), any(), any(), any(), eq(false));
  }
  
  @Test
  public void testRegisterWithKnownEmailAsUsername() throws Exception {
    prepareResetPasswordContext();

    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    when(userHandler.findUserByName(EMAIL)).thenReturn(mock(User.class));

    registerHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(EMAIL, applicationParameters.get(EMAIL_PARAM));
    assertNotNull(applicationParameters.get(ON_BOARDING_EMAIL_SENT));
    assertTrue((Boolean) applicationParameters.get(ON_BOARDING_EMAIL_SENT));

    verify(passwordRecoveryService, never()).sendExternalRegisterEmail(any(), any(), any(), any(), any(), eq(false));
  }

  @Test
  public void testRegisterWithKnownEmail() throws Exception {
    prepareResetPasswordContext();

    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);
    @SuppressWarnings("unchecked")
    ListAccess<User> listAccess = mock(ListAccess.class);
    when(listAccess.getSize()).thenReturn(1);
    when(userHandler.findUsersByQuery(any(), any())).thenReturn(listAccess);

    registerHandler.execute(controllerContext);

    assertNotNull(applicationParameters);
    assertEquals(EMAIL, applicationParameters.get(EMAIL_PARAM));
    assertNotNull(applicationParameters.get(ON_BOARDING_EMAIL_SENT));
    assertTrue((Boolean) applicationParameters.get(ON_BOARDING_EMAIL_SENT));

    verify(passwordRecoveryService, never()).sendExternalRegisterEmail(any(), any(), any(), any(), any(), eq(false));
  }

  @Test
  public void testRedirectToLoginWhenValid() throws Exception {
    prepareResetPasswordContext();

    when(request.getParameter(EMAIL_PARAM)).thenReturn(EMAIL);

    registerHandler.execute(controllerContext);

    verify(passwordRecoveryService, times(1)).sendExternalRegisterEmail(eq(null), eq(EMAIL), eq(REQUEST_LOCALE), eq(null), any(), eq(false));
  }

  private void prepareResetPasswordContext() {
    controllerContext = new ControllerContext(controller,
                                              router,
                                              request,
                                              response,
                                              new HashMap<>());
  }

}
