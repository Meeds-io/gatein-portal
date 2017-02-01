package org.exoplatform.services.organization;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.organization.search.UserSearchService;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml") })
public class TestUserSearchService extends AbstractKernelTest {
  private UserSearchService userSearchService;
  private OrganizationService organizationService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    userSearchService = getContainer().getComponentInstanceOfType(UserSearchService.class);
    organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    RequestLifeCycle.begin(getContainer());
  }

  public void testUserSearch() throws Exception {
    UserHandler userHandler = organizationService.getUserHandler();
    ListAccess<User> users = userHandler.findAllUsers();
    assertNotNull(users);

    ListAccess<User> searchedUsers = userSearchService.searchUsers(null);
    assertNotNull(searchedUsers);
    assertEquals(users.getSize(), searchedUsers.getSize());
    assertTrue(users.getSize() > 1);

    Query query = new Query();
    query.setUserName("*ro*");
    users = userHandler.findUsersByQuery(query);
    assertNotNull(users);
    assertEquals(1, users.getSize());

    searchedUsers = userSearchService.searchUsers("ro");
    assertNotNull(searchedUsers);
    assertEquals(users.getSize(), searchedUsers.getSize());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    RequestLifeCycle.end();
  }
}
