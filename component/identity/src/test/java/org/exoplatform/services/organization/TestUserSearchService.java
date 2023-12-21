package org.exoplatform.services.organization;

import java.util.Arrays;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.organization.search.UserSearchService;

@ConfiguredBy(
  {
      @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
      @ConfigurationUnit(
          scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml"
      ) }
)
public class TestUserSearchService extends AbstractKernelTest {
  private UserSearchService   userSearchService;

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
    query.setUserName("*roo*");
    users = userHandler.findUsersByQuery(query);
    assertNotNull(users);
    assertEquals(1, users.getSize());

    searchedUsers = userSearchService.searchUsers("roo");
    assertNotNull(searchedUsers);
    assertEquals(users.getSize(), searchedUsers.getSize());
  }

  public void testUserSearchSwitchStatus() throws Exception {
    UserHandler userHandler = organizationService.getUserHandler();
    ListAccess<User> users = userHandler.findAllUsers();
    assertNotNull(users);

    ListAccess<User> searchedUsers = userSearchService.searchUsers(null, UserStatus.ANY);
    assertNotNull(searchedUsers);
    assertEquals(users.getSize(), searchedUsers.getSize());
    assertTrue(users.getSize() > 1);
    User[] usersArray = users.load(0, users.getSize());

    User johnUser = Arrays.stream(usersArray).filter(user -> user.getUserName().equals("john")).findFirst().orElse(null);
    assertNotNull(johnUser);
    organizationService.getUserHandler().setEnabled(johnUser.getUserName(), false, true);

    try {
      searchedUsers = userSearchService.searchUsers(null, UserStatus.ENABLED);
      assertNotNull(searchedUsers);
      assertEquals(users.getSize() - 1, searchedUsers.getSize());
      assertEquals(usersArray.length - 1, searchedUsers.getSize());

      searchedUsers = userSearchService.searchUsers(null, UserStatus.DISABLED);
      assertEquals(1, searchedUsers.getSize());

      searchedUsers = userSearchService.searchUsers(null, UserStatus.ANY);
      assertNotNull(searchedUsers);
      assertEquals(users.getSize(), searchedUsers.getSize());
      assertEquals(usersArray.length, searchedUsers.getSize());
    } finally {
      organizationService.getUserHandler().setEnabled(johnUser.getUserName(), true, true);
    }
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    RequestLifeCycle.end();
  }
}
