package org.exoplatform.portal.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.junit.Test;

import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

public class DefaultGroupVisibilityPluginTest {

  @Test
  public void shouldHasPermissionWhenUserIsSuperUser() {
    // Given
    UserACL userACL = mock(UserACL.class);
    when(userACL.getSuperUser()).thenReturn("john");
    DefaultGroupVisibilityPlugin plugin = new DefaultGroupVisibilityPlugin(userACL);

    Identity userIdentity = new Identity("john", Arrays.asList(new MembershipEntry("/platform/users", "manager")));
    Group groupPlatform = new GroupImpl();
    groupPlatform.setId("/platform");
    Group groupPlatformUsers = new GroupImpl();
    groupPlatformUsers.setId("/platform/users");

    // When
    boolean hasPermissionOnPlatform = plugin.hasPermission(userIdentity, groupPlatform);
    boolean hasPermissionOnPlatformUsers = plugin.hasPermission(userIdentity, groupPlatformUsers);

    // Then
    assertTrue(hasPermissionOnPlatform);
    assertTrue(hasPermissionOnPlatformUsers);
  }

  @Test
  public void shouldHasPermissionWhenUserIsPlatformAdministrator() {
    // Given
    UserACL userACL = mock(UserACL.class);
    when(userACL.getSuperUser()).thenReturn("root");
    when(userACL.getAdminGroups()).thenReturn("/platform/administrators");
    DefaultGroupVisibilityPlugin plugin = new DefaultGroupVisibilityPlugin(userACL);

    Identity userIdentity = new Identity("john", Arrays.asList(new MembershipEntry("/platform/administrators", "manager")));
    Group groupPlatform = new GroupImpl();
    groupPlatform.setId("/platform");
    Group groupPlatformUsers = new GroupImpl();
    groupPlatformUsers.setId("/platform/users");

    // When
    boolean hasPermissionOnPlatform = plugin.hasPermission(userIdentity, groupPlatform);
    boolean hasPermissionOnPlatformUsers = plugin.hasPermission(userIdentity, groupPlatformUsers);

    // Then
    assertTrue(hasPermissionOnPlatform);
    assertTrue(hasPermissionOnPlatformUsers);
  }

  @Test
  public void shouldHasPermissionWhenUserIsInGivenGroup() {
    // Given
    UserACL userACL = mock(UserACL.class);
    when(userACL.getSuperUser()).thenReturn("root");
    when(userACL.getAdminGroups()).thenReturn("/platform/administrators");
    DefaultGroupVisibilityPlugin plugin = new DefaultGroupVisibilityPlugin(userACL);

    Identity userIdentity = new Identity("john",
                                         Arrays.asList(new MembershipEntry("/platform/developers", "manager"),
                                                       new MembershipEntry("/platform/testers", "member"),
                                                       new MembershipEntry("/organization/rh", "*")));
    Group groupPlatform = new GroupImpl();
    groupPlatform.setId("/platform");
    Group groupPlatformDevelopers = new GroupImpl();
    groupPlatformDevelopers.setId("/platform/developers");
    Group groupPlatformTesters = new GroupImpl();
    groupPlatformTesters.setId("/platform/testers");
    Group groupOrganization = new GroupImpl();
    groupOrganization.setId("/organization");
    Group groupOrganizationRh = new GroupImpl();
    groupOrganizationRh.setId("/organization/rh");
    Group groupDepartments = new GroupImpl();
    groupDepartments.setId("/departments");

    // When
    boolean hasPermissionOnPlatform = plugin.hasPermission(userIdentity, groupPlatform);
    boolean hasPermissionOnPlatformDevelopers = plugin.hasPermission(userIdentity, groupPlatformDevelopers);
    boolean hasPermissionOnPlatformTesters = plugin.hasPermission(userIdentity, groupPlatformTesters);
    boolean hasPermissionOnOrganization = plugin.hasPermission(userIdentity, groupOrganization);
    boolean hasPermissionOnOrganizationRh = plugin.hasPermission(userIdentity, groupOrganizationRh);
    boolean hasPermissionOnODepartments = plugin.hasPermission(userIdentity, groupDepartments);

    // Then
    assertTrue(hasPermissionOnPlatform);
    assertTrue(hasPermissionOnPlatformDevelopers);
    assertFalse(hasPermissionOnPlatformTesters);
    assertTrue(hasPermissionOnOrganization);
    assertTrue(hasPermissionOnOrganizationRh);
    assertFalse(hasPermissionOnODepartments);
  }

}
