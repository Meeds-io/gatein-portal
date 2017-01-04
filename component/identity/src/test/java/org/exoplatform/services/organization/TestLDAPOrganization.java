package org.exoplatform.services.organization;

import exo.portal.component.identiy.opendsconfig.DSConfig;
import exo.portal.component.identiy.opendsconfig.opends.OpenDSService;

import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import junit.framework.Assert;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.idm.PicketLinkIDMCacheService;
import org.exoplatform.services.organization.idm.UserDAOImpl;

import org.opends.server.tools.LDAPModify;

/**
 * Created by exo on 5/5/16.
 */
@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-ldap-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/services/organization/TestOrganization-configuration.xml") })
public class TestLDAPOrganization extends TestOrganization {

  private static Log                      log                             = ExoLogger.getLogger(TestLDAPOrganization.class.getName());

  protected UserHandler uHandler;

  OpenDSService                           openDSService                   = new OpenDSService(null);

  PortalContainer container;

  OrganizationService organization;

  PicketLinkIDMCacheService picketLinkIDMCacheService;


  @Override
  protected void beforeRunBare() {
    try {
      openDSService.start();
      openDSService.initLDAPServer();
    } catch (Exception e) {
      log.error("Error in starting up OPENDS", e);
      e.printStackTrace();
    }
    super.beforeRunBare();
  }

  @Override
  protected void setUp() throws Exception {
    container = PortalContainer.getInstance();
    organization = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
    uHandler = organization.getUserHandler();
    picketLinkIDMCacheService = (PicketLinkIDMCacheService) container.getComponentInstanceOfType(PicketLinkIDMCacheService.class);
    super.setUp();
    synchronizeUsers();
  }

  public void synchronizeUsers() throws Exception {
    begin();
    assertNotNull("Cannot find OrganizationService", organization);
    UserDAOImpl userHandler = (UserDAOImpl) organization.getUserHandler();
    ListAccess<User> userListAccess = organization.getUserHandler().findAllUsers();
    User[] users = userListAccess.load(0, userListAccess.getSize());
      end();
    for (User user : users) {
      String username = user.getUserName();

      if (user.getCreatedDate() == null) {
        user.setCreatedDate(new Date());
      }
      log.info("Invoke " + username + " user synchronization ");
      Collection<UserEventListener> userDAOListeners = userHandler.getUserEventListeners();
      for (UserEventListener userEventListener : userDAOListeners) {
        begin();
        try {
          userEventListener.preSave(user, true);
        } catch (Exception e) {
          log.info("\t\tFailed to call preSave for " + username + " User with listener : " + userEventListener.getClass());
          e.printStackTrace();
        } finally {
          end();
        }
      }
      for (UserEventListener userEventListener : userDAOListeners) {
        try {
          begin();
          userEventListener.postSave(user, true);
        } catch (Exception e) {
          log.info("\t\tFailed to call postSave for " + username + " User with listener : " + userEventListener.getClass());
          e.printStackTrace();
        } finally {
          end();
        }
      }
    }
  }

  public void testFindGroupHierachy() throws Exception {
      GroupHandler handler = organizationService.getGroupHandler();
      Group group = handler.findGroupById("/organization_hierarcy/OrganizationC");

      Collection childGoups = handler.findGroups(group);
      Assert.assertTrue(childGoups.size() > 0);
  }

  public void testFindUser() throws Exception {
    assertNotNull(organization);
    begin();
    User test2 = organization.getUserHandler().findUserByName("admin");
    assertNotNull(test2);
    openDSService.cleanUpDN("uid=admin,ou=People,o=test,dc=portal,dc=example,dc=com");
    picketLinkIDMCacheService.invalidateAll();
    test2 = organization.getUserHandler().findUserByName("admin");
    assertNull(test2);
    end();
  }

  public void testFindEnabledUsers() throws Exception {
    ListAccess<User> listEnabled = organization.getUserHandler().findAllUsers(UserStatus.ENABLED);
    ListAccess<User> listDisabled = organization.getUserHandler().findAllUsers(UserStatus.DISABLED);
    ListAccess<User> listAll =organization.getUserHandler().findAllUsers(UserStatus.ANY);
    assertNotNull(listDisabled);
    assertNotNull(listAll);
    assertNotNull(listEnabled);
    assertEquals(0, listDisabled.getSize());
    assertEquals(listAll.getSize(), listEnabled.getSize());

    User adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.ENABLED);
    assertNotNull(adminUser);
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.DISABLED);
    assertNull(adminUser);
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.ANY);
    assertNotNull(adminUser);

    organization.getUserHandler().setEnabled("jduke", false, false );
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.ENABLED);
    assertNull(adminUser);
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.DISABLED);
    assertNotNull(adminUser);
    adminUser = organization.getUserHandler().findUserByName("jduke", UserStatus.ANY);
    assertNotNull(adminUser);

    ListAccess<User> newListEnabled = organization.getUserHandler().findAllUsers(UserStatus.ENABLED);
    ListAccess<User> newListDisabled = organization.getUserHandler().findAllUsers(UserStatus.DISABLED);
    ListAccess<User> newListAll = organization.getUserHandler().findAllUsers(UserStatus.ANY);

    assertNotNull(newListEnabled);
    assertNotNull(newListDisabled);
    assertNotNull(newListAll);
    assertEquals(1, newListDisabled.getSize());
    assertEquals("jduke", newListDisabled.load(0,1)[0].getUserName());
    for(int i=0; i< newListEnabled.getSize(); i++){
      assertNotSame("jduke",newListEnabled.load(i,1)[0].getUserName() );
    }
  }

  public void testFindUsers() throws Exception
  {
    Query query = new Query();
    query.setEmail("email@test");

    // try to find user by email
    assertSizeEquals(1, uHandler.findUsersByQuery(query), UserStatus.ENABLED);
    assertSizeEquals(1, uHandler.findUsersByQuery(query, UserStatus.ENABLED), UserStatus.ENABLED);
    assertSizeEquals(1, uHandler.findUsersByQuery(query, UserStatus.ANY), UserStatus.ANY);

    // try to find user by name with mask
    query = new Query();
    query.setUserName("*tolik*");
    assertSizeEquals(1, uHandler.findUsersByQuery(query));

    // try to find user by name with mask
    query = new Query();
    query.setUserName("tol*");
    assertSizeEquals(1, uHandler.findUsersByQuery(query));

    // try to find user by name with mask
    query = new Query();
    query.setUserName("*lik");
    assertSizeEquals(4, uHandler.findUsersByQuery(query));

    // try to find user by name explicitly
    query = new Query();
    query.setUserName("tolik");
    assertSizeEquals(1, uHandler.findUsersByQuery(query));

    // try to find user by part of name without mask
    query = new Query();
    query.setUserName("tol");
    assertSizeEquals(1, uHandler.findUsersByQuery(query));

    query = new Query();
    query.setUserName("*olik");

    ListAccess<User> users = uHandler.findUsersByQuery(query);

    assertSizeEquals(4, users, UserStatus.ENABLED);
    assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ENABLED), UserStatus.ENABLED);
    assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ANY), UserStatus.ANY);

    User[] allPage = users.load(0, 4);
    User[] page1 = users.load(0, 2);
    User[] page2 = users.load(2, 2);

    assertEquals(allPage[0].getUserName(), page1[0].getUserName());
    assertEquals(allPage[1].getUserName(), page1[1].getUserName());
    assertEquals(allPage[2].getUserName(), page2[0].getUserName());
    assertEquals(allPage[3].getUserName(), page2[1].getUserName());

    try
    {
      users.load(0, 0);
    }
    catch (Exception e)
    {
      fail("Exception is not expected");
    }

    // try to load more than exist
    try
    {
      users.load(0, 5);
      fail("Exception is expected");
    }
    catch (Exception e)
    {
    }

    // try to load more than exist
    try
    {
      users.load(1, 4);
      fail("Exception is expected");
    }
    catch (Exception e)
    {
      //expected
    }

    try
    {
      // Disable the user tolik
      uHandler.setEnabled("tolik", false, true);

      assertSizeEquals(3, uHandler.findUsersByQuery(query), UserStatus.ENABLED);
      assertSizeEquals(3, uHandler.findUsersByQuery(query, UserStatus.ENABLED), UserStatus.ENABLED);
      assertSizeEquals(1, uHandler.findUsersByQuery(query, UserStatus.DISABLED), UserStatus.DISABLED);
      assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ANY), UserStatus.ANY);

      // Enable the user tolik
      uHandler.setEnabled("tolik", true, true);

      assertSizeEquals(4, uHandler.findUsersByQuery(query), UserStatus.ENABLED);
      assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ENABLED), UserStatus.ENABLED);
      assertSizeEquals(0, uHandler.findUsersByQuery(query, UserStatus.DISABLED), UserStatus.DISABLED);
      assertSizeEquals(4, uHandler.findUsersByQuery(query, UserStatus.ANY), UserStatus.ANY);
    }
    catch (UnsupportedOperationException e)
    {
      fail();
    }
  }

  protected void assertSizeEquals(int expectedSize, ListAccess<User> list, UserStatus status) throws Exception
  {
    int size;
    assertEquals(expectedSize, size = list.getSize());
    User[] values = list.load(0, size);
    size = 0;
    for (int i = 0; i < values.length; i++)
    {
      User usr = values[i];
      if (usr != null && status.matches(usr.isEnabled()))
      {
        size++;
      }
    }
    assertEquals(expectedSize, size);
  }

  protected void assertSizeEquals(int expectedSize, ListAccess<?> list) throws Exception
  {
    int size;
    assertEquals(expectedSize, size = list.getSize());
    Object[] values = list.load(0, size);
    size = 0;
    for (int i = 0; i < values.length; i++)
    {
      if (values[i] != null)
      {
        size++;
      }
    }
    assertEquals(expectedSize, size);
  }
}
