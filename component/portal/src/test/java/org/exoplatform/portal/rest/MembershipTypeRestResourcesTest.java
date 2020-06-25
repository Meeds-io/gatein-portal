package org.exoplatform.portal.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.json.JSONObject;

import org.exoplatform.portal.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.impl.MembershipTypeImpl;
import org.exoplatform.services.rest.impl.ContainerResponse;

public class MembershipTypeRestResourcesTest extends BaseRestServicesTestCase {

  private static final String MANDATORY_MS_TYPE = "manager";

  private static final String MEMBERSHIP_TYPE_1 = "mt1";

  private static final String MEMBERSHIP_TYPE_2 = "mt2";

  protected Class<?> getComponentClass() {
    return MembershipTypeRestResourcesV1.class;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    OrganizationService organizationService = mock(OrganizationService.class);
    MembershipTypeHandler membershipTypeHandler = mock(MembershipTypeHandler.class);

    when(organizationService.getMembershipTypeHandler()).thenReturn(membershipTypeHandler);
    MembershipTypeImpl membershipType1 = new MembershipTypeImpl(MEMBERSHIP_TYPE_1, null, MEMBERSHIP_TYPE_1);
    when(membershipTypeHandler.findMembershipType(MEMBERSHIP_TYPE_1)).thenReturn(membershipType1);

    MembershipTypeImpl membershipType2 = new MembershipTypeImpl(MEMBERSHIP_TYPE_2, null, MEMBERSHIP_TYPE_2);
    when(membershipTypeHandler.findMembershipType(MEMBERSHIP_TYPE_2)).thenReturn(membershipType2);

    when(membershipTypeHandler.findMembershipTypes()).thenReturn(Arrays.asList(membershipType1, membershipType2));

    getContainer().unregisterComponent(OrganizationService.class);
    getContainer().registerComponentInstance("org.exoplatform.services.organization.OrganizationService", organizationService);

    startUserSession("testuser");
  }

  @Override
  public void tearDown() throws Exception {
    getContainer().unregisterComponent("org.exoplatform.services.organization.OrganizationService");
    super.tearDown();
  }

  public void testCreateMembershipType() throws Exception {
    JSONObject data = new JSONObject();

    ContainerResponse response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());

    data.put("name", MEMBERSHIP_TYPE_1);
    response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());

    data.put("name", "NEW_MEMBERSHIP_TYPE");
    response = getResponse("POST", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(204, response.getStatus());
  }

  public void testUpdateMembershipType() throws Exception {
    JSONObject data = new JSONObject();

    ContainerResponse response = getResponse("PUT", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertNotNull(response.getEntity());

    data.put("name", "NOT_EXISTING");
    response = getResponse("PUT", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    data.put("name", MEMBERSHIP_TYPE_1);
    response = getResponse("PUT", "/v1/membershipTypes", data.toString());
    assertNotNull(response);
    assertEquals(204, response.getStatus());
  }

  public void testDeleteMembershipType() throws Exception {
    ContainerResponse response = launcher.service("DELETE", "/v1/membershipTypes/NOT_EXISTING", "", null, null, null);
    assertNotNull(response);
    assertEquals(404, response.getStatus());

    response = launcher.service("DELETE", "/v1/membershipTypes/" + MANDATORY_MS_TYPE, "", null, null, null);
    assertNotNull(response);
    assertEquals(400, response.getStatus());
    assertEquals("MandatoryMembershipType", response.getEntity());

    response = launcher.service("DELETE", "/v1/membershipTypes/" + MEMBERSHIP_TYPE_1, "", null, null, null);
    assertNotNull(response);
    assertEquals(204, response.getStatus());
  }

  public void testGetMembershipTypes() throws Exception {
    ContainerResponse response = launcher.service("GET", "/v1/membershipTypes", "", null, null, null);
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity() instanceof Collection);

    @SuppressWarnings("unchecked")
    Collection<MembershipType> membershipTypes = (Collection<MembershipType>) response.getEntity();
    assertEquals(2, membershipTypes.size());
  }

}
