package org.exoplatform.services.organization;

import exo.portal.component.identiy.opendsconfig.opends.OpenDSService;

import java.util.Collection;
import java.util.Date;

import junit.framework.Assert;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.idm.*;

/**
 * Created by exo on 5/5/16.
 */
@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-ldap-user-only-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml") })
public class TestLDAPUserOnlyOrganization extends TestLDAPOrganization {
  public void synchronizeUsers() throws Exception {
    // Disable synchronization of users
  }

  public void testGetUserGroups() throws Exception {
    begin();

    // Given
    User testUser = userHandler_.findUserByName("jduke");
    Group group = groupHandler_.findGroupById(GROUP_1);
    MembershipType mt1 = mtHandler_.createMembershipTypeInstance();
    mt1.setName("test");
    mtHandler_.createMembershipType(mt1, true);
    membershipHandler_.linkMembership(testUser, group, mt1, true);

    // 
    ListAccess<User> users = userHandler_.findUsersByGroupId(GROUP_1);
    assertTrue("Group memberships is empty", users.getSize() > 0);

    User[] usersArray = users.load(0, users.getSize());
    assertTrue("Loaded group memberships is empty which is not coherent with the size of ListAccess", usersArray.length > 0);

    boolean foundUser = false;
    for (int i = 0; i < usersArray.length && !foundUser; i++) {
      User user = usersArray[i];
        foundUser |= user.getUserName().equals(testUser.getUserName());
    }
    assertTrue("User test is not found in group " + GROUP_1, foundUser);
  }

  public void testFindUser() throws Exception {
    // Disable this test because it deletes an entry from LDAP
  }

}
