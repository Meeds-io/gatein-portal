package org.exoplatform.services.organization;

import exo.portal.component.identiy.opendsconfig.DSConfig;
import exo.portal.component.identiy.opendsconfig.opends.OpenDSService;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
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

  public static final String              LDAP_HOST                       = "localhost";

  public static final String              LDAP_PORT                       = "10389";

  public static final String              LDAP_PROVIDER_URL               = "ldap://" + LDAP_HOST + ":" + LDAP_PORT;

  public static final String              LDAP_PRINCIPAL                  = "cn=Directory Manager";

  public static final String              LDAP_CREDENTIALS                = "password";

  public String                           EMBEDDED_OPEN_DS_DIRECTORY_NAME = "EmbeddedOpenDS";

  protected DSConfig                      directoryConfig;

  public String                           directories                     = "ldap/datasources/directories.xml";

  // By default use embedded OpenDS
  private String                          directoryName                   = EMBEDDED_OPEN_DS_DIRECTORY_NAME;

  public static Hashtable<String, String> env                             = new Hashtable<String, String>();

  OpenDSService                           openDSService                   = new OpenDSService(null);

  String                                  identityConfig;

  PortalContainer container;

  OrganizationService organization;

  PicketLinkIDMCacheService picketLinkIDMCacheService;


  @Override
  protected void beforeRunBare() {
    try {
      openDSService.start();
      loadConfig();
      populateLDIF();
      populate();
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

  public void testFindUser() throws Exception {
    assertNotNull(organization);
    begin();
    User test2 = organization.getUserHandler().findUserByName("admin");
    assertNotNull(test2);
    cleanUpDN("uid=admin,ou=People,o=test,dc=portal,dc=example,dc=com");
    picketLinkIDMCacheService.invalidateAll();
    test2 = organization.getUserHandler().findUserByName("admin");
    assertNull(test2);
    end();
  }

  public void populateLDIFFile(String ldif) throws Exception {

    URL ldifURL = Thread.currentThread().getContextClassLoader().getResource(ldif);

    System.out.println("LDIF: " + ldifURL.toURI().getPath());

    String[] cmd = new String[] { "-h", directoryConfig.getHost(), "-p", directoryConfig.getPort(), "-D",
        directoryConfig.getAdminDN(), "-w", directoryConfig.getAdminPassword(), "-a", "-f", ldifURL.toURI().getPath() };

    // Not sure why... but it actually does make a difference...
    if (directoryName.equals(EMBEDDED_OPEN_DS_DIRECTORY_NAME)) {
      System.out.println("Populate success: " + (LDAPModify.mainModify(cmd, false, System.out, System.err) == 0));
    } else {
      System.out.println("Populate success: " + (LDAPModify.mainModify(cmd) == 0));
    }
  }

  public void populate() throws Exception {
    populateLDIFFile("ldap/ldap/initial-opends.ldif");
  }

  public void populateLDIF() throws Exception {
    String ldif = directoryConfig.getPopulateLdif();
    URL ldifURL = Thread.currentThread().getContextClassLoader().getResource(ldif);

    System.out.println("LDIF: " + ldifURL.toURI().getPath());

    String[] cmd = new String[] { "-h", directoryConfig.getHost(), "-p", directoryConfig.getPort(), "-D",
        directoryConfig.getAdminDN(), "-w", directoryConfig.getAdminPassword(), "-a", "-f", ldifURL.toURI().getPath() };

    // Not sure why... but it actually does make a difference...
    if (directoryName.equals(EMBEDDED_OPEN_DS_DIRECTORY_NAME)) {
      System.out.println("Populate success: " + (LDAPModify.mainModify(cmd, false, System.out, System.err) == 0));
    } else {
      System.out.println("Populate success: " + (LDAPModify.mainModify(cmd) == 0));
    }
  }

  public void loadConfig() throws Exception {
    directoryConfig = DSConfig.obtainConfig(directories, directoryName);

    identityConfig = directoryConfig.getConfigFile();

    env.put(Context.INITIAL_CONTEXT_FACTORY, directoryConfig.getContextFactory());
    // Use description to store URL to be able to prefix with "ldaps://"
    env.put(Context.PROVIDER_URL, directoryConfig.getDescription());
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, directoryConfig.getAdminDN());
    env.put(Context.SECURITY_CREDENTIALS, directoryConfig.getAdminPassword());
  }

  private LdapContext getLdapContext() throws Exception {
    Hashtable<String, String> env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, LDAP_PROVIDER_URL);
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, LDAP_PRINCIPAL);
    env.put(Context.SECURITY_CREDENTIALS, LDAP_CREDENTIALS);

    return new InitialLdapContext(env, null);
  }

  protected void cleanUpDN(String dn) throws Exception {
    DirContext ldapCtx = getLdapContext();

    try {
      log.info("Removing: " + dn);

      removeContext(ldapCtx, dn);
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
      ldapCtx.close();
    }
  }

  // subsequent remove of javax.naming.Context
  protected void removeContext(Context mainCtx, String name) throws Exception {
    Context deleteCtx = (Context) mainCtx.lookup(name);
    NamingEnumeration subDirs = mainCtx.listBindings(name);

    while (subDirs.hasMoreElements()) {
      Binding binding = (Binding) subDirs.nextElement();
      String subName = binding.getName();

      removeContext(deleteCtx, subName);
    }

    mainCtx.unbind(name);
  }

}
