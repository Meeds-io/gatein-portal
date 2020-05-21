package org.exoplatform.portal.rest;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONObject;
import org.mockito.Mockito;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.idm.UserImpl;
import org.exoplatform.services.resources.ResourceBundleService;
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

    ResourceBundleService resourceBundleService = container.getComponentInstanceOfType(ResourceBundleService.class);
    if (resourceBundleService == null) {
      resourceBundleService = Mockito.mock(ResourceBundleService.class);
      container.registerComponentInstance(resourceBundleService);
    }
  }

  @Override
  public void tearDown() throws Exception {
    getContainer().unregisterComponent("org.exoplatform.services.organization.OrganizationService");
    getContainer().unregisterComponent("org.exoplatform.portal.config.UserACL");
    super.tearDown();
  }

  public void testUnauthorizedNotSameUser() throws Exception {
    // Given
    String path = getChangePasswordPath(USER_1);
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
    String path = getChangePasswordPath(USER_1);
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

  public void testChangePasswordUserNotFoundError() throws Exception {
    // Given
    String path = getChangePasswordPath(USER_2);
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
    String path = getChangePasswordPath(USER_1);
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
    String path = getChangePasswordPath(USER_1);
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

  public void testCreateUser() throws Exception {
    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(null);
    @SuppressWarnings("unchecked")
    ListAccess<User> listAccess = mock(ListAccess.class);
    when(userHandler.findUsersByQuery(any(), any())).thenReturn(listAccess);
    when(listAccess.getSize()).thenReturn(0);
    UserImpl user = new UserImpl(USER_2);
    when(userHandler.createUserInstance(eq(USER_2))).thenReturn(user);

    startSessionAs(USER_1);

    JSONObject data = new JSONObject();

    ContainerResponse response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", "");
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", "");
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", "");
    data.put("password", "password");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", "");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    verify(userHandler, atMost(0)).createUser(any(User.class), anyBoolean());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", USER_2 + "@example.com");
    response = getResponse("POST", "/v1/users", data.toString());
    assertNotNull(response);
    assertNull(response.getEntity());
    assertEquals(204, response.getStatus());

    verify(userHandler, atLeast(1)).createUser(eq(user), eq(true));
  }

  public void testUpdateUser() throws Exception {
    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(null);
    @SuppressWarnings("unchecked")
    ListAccess<User> listAccess = mock(ListAccess.class);
    when(userHandler.findUsersByQuery(any(), any())).thenReturn(listAccess);
    when(listAccess.getSize()).thenReturn(0);

    String email = USER_2 + "@example.com";
    UserImpl user = new UserImpl(USER_2);
    user.setEmail(email);
    user.setFirstName(USER_2);
    user.setLastName(USER_2);
    user.setEnabled(false);

    startSessionAs(USER_1);
    
    JSONObject data = new JSONObject();

    ContainerResponse response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("email", email);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    when(userHandler.findUserByName(eq(USER_2), any())).thenReturn(user);
    data.put("userName", USER_2);
    data.put("lastName", "");
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", email);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", "");
    data.put("password", "password");
    data.put("email", email);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("email", email);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNull(response.getEntity());
    assertEquals(204, response.getStatus());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", "");
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNotNull(response.getEntity());
    assertEquals(response.getEntity().toString(), 400, response.getStatus());

    verify(userHandler, atMost(0)).saveUser(any(User.class), anyBoolean());
    verify(userHandler, atMost(0)).setEnabled(anyString(), anyBoolean(), anyBoolean());

    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "password");
    data.put("email", email);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNull(response.getEntity());
    assertEquals(204, response.getStatus());

    verify(userHandler, atLeast(1)).saveUser(eq(user), eq(true));
    verify(userHandler, atMost(0)).setEnabled(anyString(), anyBoolean(), anyBoolean());
    
    data.put("userName", USER_2);
    data.put("lastName", USER_2);
    data.put("firstName", USER_2);
    data.put("password", "");
    data.put("email", email);
    data.put("enabled", true);
    response = getResponse("PUT", "/v1/users", data.toString());
    assertNotNull(response);
    assertNull(response.getEntity());
    assertEquals(204, response.getStatus());

    verify(userHandler, atMost(1)).saveUser(eq(user), eq(true));
    verify(userHandler, atLeast(1)).setEnabled(anyString(), anyBoolean(), anyBoolean());
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

  private String getChangePasswordPath(String username) {
    return "/v1/users/" + username + "/changePassword";
  }
}
