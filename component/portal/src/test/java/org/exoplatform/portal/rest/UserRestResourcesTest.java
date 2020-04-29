package org.exoplatform.portal.rest;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.idm.UserImpl;
import org.exoplatform.services.rest.impl.*;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.test.mock.MockHttpServletRequest;

public class UserRestResourcesTest extends BaseRestServicesTestCase {

  private static final String USER_1 = "testuser1";

  private static final String USER_2 = "testuser2";

  private UserHandler         userHandler;

  private UserACL             userACL;

  protected Class<?> getComponentClass() {
    return UserRestResourcesV1.class;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    OrganizationService organizationService = mock(OrganizationService.class);
    userHandler = mock(UserHandler.class);
    userACL = mock(UserACL.class);

    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.findUserByName(USER_2)).thenReturn(null);

    UserImpl user = new UserImpl(USER_1);
    when(userHandler.findUserByName(USER_1)).thenReturn(user);

    when(userACL.isSuperUser()).thenReturn(false);
    when(userACL.getAdminGroups()).thenReturn("admins");

    getContainer().unregisterComponent(OrganizationService.class);
    getContainer().unregisterComponent(UserACL.class);

    getContainer().registerComponentInstance("org.exoplatform.services.organization.OrganizationService", organizationService);
    getContainer().registerComponentInstance("org.exoplatform.portal.config.UserACL", userACL);
  }

  @Override
  public void tearDown() throws Exception {
    getContainer().unregisterComponent("org.exoplatform.services.organization.OrganizationService");
    getContainer().unregisterComponent("org.exoplatform.portal.config.UserACL");
    super.tearDown();
  }

  public void testUnauthorizedNotSameUser() throws Exception {
    // Given
    String path = getPath(USER_1);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword";
    MockHttpServletRequest httpRequest = getChangePasswordRequest(path, currentPassword, newPassword);

    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, httpRequest);

    startSessionAs(USER_2);

    // When
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 401, resp.getStatus());
  }

  public void testAdminAuthorizedToChangePassword() throws Exception {
    // Given
    String path = getPath(USER_1);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword";
    MockHttpServletRequest httpRequest = getChangePasswordRequest(path, currentPassword, newPassword);

    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, httpRequest);

    startSessionAs(USER_2);
    when(userACL.isUserInGroup(eq("admins"))).thenReturn(true);

    // When
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 204, resp.getStatus());
    assertNull(resp.getEntity());
  }

  public void testUserNotFoundError() throws Exception {
    // Given
    String path = getPath(USER_2);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword";
    MockHttpServletRequest httpRequest = getChangePasswordRequest(path, currentPassword, newPassword);

    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, httpRequest);

    startSessionAs(USER_2);
    when(userACL.isUserInGroup(eq("admins"))).thenReturn(true);

    // When
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 500, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof String);
    String errorMessage = (String) entity;
    assertEquals(UserRestResourcesV1.USER_NOT_FOUND_ERROR_CODE, errorMessage);
  }

  public void testSameUserWrongPassword() throws Exception {
    // Given
    String path = getPath(USER_1);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword";
    MockHttpServletRequest httpRequest = getChangePasswordRequest(path, currentPassword, newPassword);
    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, httpRequest);

    startSessionAs(USER_1);

    // When
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 500, resp.getStatus());
    Object entity = resp.getEntity();
    assertNotNull(entity);
    assertTrue(entity instanceof String);
    String errorMessage = (String) entity;
    assertEquals(UserRestResourcesV1.WRONG_USER_PASSWORD_ERROR_CODE, errorMessage);
  }

  public void testSameUserAuthorizedToChangePassword() throws Exception {
    // Given
    String path = getPath(USER_1);
    String currentPassword = "currentPassword";
    String newPassword = "newPassword";
    MockHttpServletRequest httpRequest = getChangePasswordRequest(path, currentPassword, newPassword);
    EnvironmentContext envctx = new EnvironmentContext();
    envctx.put(HttpServletRequest.class, httpRequest);

    startSessionAs(USER_1);

    // When
    when(userHandler.authenticate(USER_1, currentPassword)).thenReturn(true);
    ContainerResponse resp = launcher.service("PATCH",
                                              path,
                                              "",
                                              getChangePasswordHeaders(),
                                              getChangePasswordData(currentPassword, newPassword),
                                              envctx);

    // Then
    assertEquals(String.valueOf(resp.getEntity()), 204, resp.getStatus());
  }

  private void startSessionAs(String username) {
    Identity identity = new Identity(username);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
  }

  private MockHttpServletRequest getChangePasswordRequest(String path, String currentPassword, String newPassword) {
    byte[] formData = getChangePasswordData(currentPassword, newPassword);
    ByteArrayInputStream dataInputStream = new ByteArrayInputStream(formData);
    return new MockHttpServletRequest(path,
                                      dataInputStream,
                                      dataInputStream.available(),
                                      "PATCH",
                                      getChangePasswordHeaders());
  }

  private byte[] getChangePasswordData(String currentPassword, String newPassword) {
    return ("currentPassword=" + currentPassword
        + "&newPassword="
        + newPassword).getBytes();
  }

  private MultivaluedMap<String, String> getChangePasswordHeaders() {
    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("Content-Type", "application/x-www-form-urlencoded");
    return headers;
  }

  private String getPath(String username) {
    return "/v1/users/" + username + "/changePassword";
  }
}
