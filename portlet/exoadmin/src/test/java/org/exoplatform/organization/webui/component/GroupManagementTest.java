package org.exoplatform.organization.webui.component;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singleton;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml") })
public class GroupManagementTest extends AbstractKernelTest {

  protected OrganizationService organizationService;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    begin();
  }

  @After
  public void tearDown() throws Exception {
    end();
    super.tearDown();
  }

  @Test
  public void testIsMembershipOfGroup() {
    try {
      assertTrue(GroupManagement.isMembershipOfGroup("demo", "member", "/platform/users"));
      userLogin("demo");
      assertTrue(GroupManagement.isMembershipOfGroup(null, "member", "/platform/users"));
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testIsManagerOfGroup() {
    try {
      assertTrue(GroupManagement.isManagerOfGroup("john", "/organization/management/executive-board"));
      userLogin("john");
      assertTrue(GroupManagement.isManagerOfGroup(null, "/organization/management/executive-board"));
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testIsMemberOfGroup() {
    try {
      assertTrue(GroupManagement.isMemberOfGroup("mary", "/platform/users"));
      userLogin("mary");
      assertTrue(GroupManagement.isMemberOfGroup(null, "/platform/users"));
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testIsRelatedOfGroup() {
    try {
      assertTrue(GroupManagement.isRelatedOfGroup("mary", "/platform"));
      userLogin("mary");
      assertTrue(GroupManagement.isRelatedOfGroup(null, "/platform"));
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testGetRelatedGroups() {
    try {
      Group organizationGroup = organizationService.getGroupHandler().findGroupById("/organization");
      Group managementGroup = organizationService.getGroupHandler().findGroupById("/management");
      Group customersGroup = organizationService.getGroupHandler().findGroupById("/customers");
      assertEquals(1, GroupManagement.getRelatedGroups("john", singleton(organizationGroup)).size());
      assertEquals(1, GroupManagement.getRelatedGroups("john", singleton(managementGroup)).size());
      assertEquals(0, GroupManagement.getRelatedGroups("john", singleton(customersGroup)).size());
      userLogin("john");
      assertEquals(1, GroupManagement.getRelatedGroups(null, singleton(managementGroup)).size());
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testIsAdministrator() {
    try {
      assertTrue(GroupManagement.isAdministrator("root"));
      assertTrue(GroupManagement.isAdministrator("john"));
      assertFalse(GroupManagement.isAdministrator("mary"));
      userLogin("john");
      assertTrue(GroupManagement.isAdministrator(null));
      userLogin("mary");
      assertFalse(GroupManagement.isAdministrator(null));
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testIsSuperUserOfGroup() {
    assertTrue(GroupManagement.isSuperUserOfGroup("root", "/platform/administrators"));
    assertTrue(GroupManagement.isSuperUserOfGroup("john", "/organization/management/executive-board"));
    assertFalse(GroupManagement.isSuperUserOfGroup("mary", "/platform/users"));
    userLogin("mary");
    assertFalse(GroupManagement.isSuperUserOfGroup(null, "/platform/users"));
  }

  private void userLogin(String userName) {
    Identity identity = new Identity(userName);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
  }
}
