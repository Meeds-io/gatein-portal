package org.exoplatform.web.login.recovery;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.portal.idm.impl.repository.ExoFallbackIdentityStoreRepository;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.impl.api.session.IdentitySessionImpl;
import org.picketlink.idm.impl.api.session.context.IdentitySessionContext;
import org.picketlink.idm.impl.model.ldap.LDAPIdentityObjectImpl;
import org.picketlink.idm.spi.configuration.metadata.IdentityConfigurationMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityObjectTypeMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityStoreConfigurationMetaData;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.repository.IdentityStoreRepository;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.PicketLinkIDMServiceImpl;
import org.exoplatform.services.organization.idm.externalstore.PicketLinkIDMExternalStoreService;

public class DefaultChangePasswordConnector extends ChangePasswordConnector {
  
  private OrganizationService organizationService;
  private PicketLinkIDMService picketLinkIDMService;
  private PicketLinkIDMExternalStoreService picketLinkIDMExternalStoreService;
  
  public final static String LOG_SERVICE_NAME = "changePassword";
  
  
  protected static Log log = ExoLogger.getLogger(DefaultChangePasswordConnector.class);
  
  private boolean allowChangeExternalPassword;
  
  private String ldapWriteUser;
  private String ldapWritePwd;
  
  
  public DefaultChangePasswordConnector(InitParams initParams, OrganizationService organizationService,
                                        PicketLinkIDMService picketLinkIDMService, PicketLinkIDMExternalStoreService picketLinkIDMExternalStoreService) {
    this.organizationService=organizationService;
    this.picketLinkIDMService=picketLinkIDMService;
    this.picketLinkIDMExternalStoreService=picketLinkIDMExternalStoreService;
    ValueParam allowChangeExternalPasswordValue = initParams.getValueParam("allowChangeExternalPassword");
    this.allowChangeExternalPassword = allowChangeExternalPasswordValue != null ? Boolean.parseBoolean(allowChangeExternalPasswordValue.getValue()) : false;
    this.ldapWriteUser=initParams.getValueParam("ldapWriteUser") !=null ?
                       initParams.getValueParam("ldapWriteUser").getValue() : "";
    this.ldapWritePwd=initParams.getValueParam("ldapWritePwd") !=null ?initParams.getValueParam("ldapWritePwd").getValue() : "";
  }
  
  /**
   * @return the allowChangeExternalPassword
   */
  @Override
  public boolean isAllowChangeExternalPassword() {
    return allowChangeExternalPassword;
  }
  
  @Override
  public void changePassword(final String username, final String password) throws Exception {
    User user = organizationService.getUserHandler().findUserByName(username);
    
    if (user.isInternalStore()) {
      changeInternalPassword(user, password);
    } else if (allowChangeExternalPassword) {
      changeExternalPassword(user,password);
    } else {
      throw new Exception("Change password in external store in not allowed");
    }
  }
  
  private void changeExternalPassword(User user, String password) throws Exception {
    
    String ldapUrl="";
    String passwordAttribute="";
    long startTime = System.currentTimeMillis();
    IdentityConfigurationMetaData config = ((PicketLinkIDMServiceImpl)picketLinkIDMService).getConfigMD();
    IdentityStoreConfigurationMetaData identityStoreConfig =
        config.getIdentityStores().stream().filter(identityStoreConfigurationMetaData -> identityStoreConfigurationMetaData.getId().equals("PortalLDAPStore")).findFirst().orElse(null);
    if (identityStoreConfig!=null) {
      ldapUrl=identityStoreConfig.getOptionSingleValue("providerURL");
    }
    
    String ldapType = System.getProperty("exo.ldap.type");
    if (ldapType==null) {
      throw new UnsupportedOperationException("No configured LDAP, unable to change external password");
      
    }
    IdentityObjectTypeMetaData identityObjectTypeMetaData =
        identityStoreConfig.getSupportedIdentityTypes().stream().filter(i -> i.getName().equals(
        "USER")).findFirst().orElse(null);
    if (identityObjectTypeMetaData!=null) {
      passwordAttribute=identityObjectTypeMetaData.getOptionSingleValue("passwordAttributeName");
    }
    String userDN = getUserDN(user);
  
    Hashtable env = new Hashtable();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, ldapUrl);
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, ldapWriteUser);
    env.put(Context.SECURITY_CREDENTIALS, ldapWritePwd);
    InitialDirContext initialContext=null;
    try {
      // Create the initial directory context
      initialContext = new InitialDirContext(env);
      DirContext ctx = (DirContext)initialContext;
      ModificationItem[] mods = new ModificationItem[1];
      
      Attribute mod0;
      if (ldapType.equals("ad")) {
        mod0 = encodeAdPassword(password, passwordAttribute);
      } else if (ldapType.equals("ldap")) {
        mod0 = encodeLdapPassword(password, passwordAttribute);
      } else {
        throw new UnsupportedOperationException("No configured LDAP, unable to change external password");
      }
      mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);
      ctx.modifyAttributes(userDN, mods);
      long totalTime = System.currentTimeMillis() - startTime;
      log.info("service={} operation={} parameters=\"user:{}\" status=ok duration_ms={}",
               LOG_SERVICE_NAME, "changeExternalPassword", user.getUserName(), totalTime);
    }
    catch(Exception e) {
      log.error("Unable to change password by ldap for "+user.getUserName(),e);
      throw e;
    } finally {
      if (initialContext!=null) {
        initialContext.close();
      }
    }
  }
  
  private Attribute encodeLdapPassword(String password, String passwordAttribute) {
    Attribute mod0 = new BasicAttribute(passwordAttribute, password);
    return mod0;
  }
  
  private Attribute encodeAdPassword(String password, String passwordAttribute) {
    String quotedPassword = "\"" + password + "\"";
    char unicodePwd[] = quotedPassword.toCharArray();
    byte pwdArray[] = new byte[unicodePwd.length * 2];
    for (int i=0; i<unicodePwd.length; i++) {
      pwdArray[i*2 + 1] = (byte) (unicodePwd[i] >>> 8);
      pwdArray[i*2 + 0] = (byte) (unicodePwd[i] & 0xff);
    }
    Attribute mod0 = new BasicAttribute(passwordAttribute, pwdArray);
    return mod0;
  }
  
  private String getUserDN(User user) throws Exception {
    IdentitySession identitySession=picketLinkIDMService.getIdentitySession();
    IdentitySessionContext identitySessionContext=null;
    if (identitySession instanceof IdentitySessionImpl) {
      identitySessionContext = ((IdentitySessionImpl)identitySession).getSessionContext();
      IdentityStoreRepository repository = identitySessionContext.getIdentityStoreRepository();
  
      ExoFallbackIdentityStoreRepository exoFallbackIdentityStoreRepository;
      if (repository instanceof  ExoFallbackIdentityStoreRepository) {
        IdentityStoreInvocationContext invocationContext=identitySessionContext.resolveStoreInvocationContext();
        exoFallbackIdentityStoreRepository=(ExoFallbackIdentityStoreRepository)repository;
        exoFallbackIdentityStoreRepository.setUseExternalStore(true);
        try {
          IdentityObject
              identityObject =
              exoFallbackIdentityStoreRepository.findIdentityObject(invocationContext, user.getUserName(),
                                                                    identitySessionContext.getIdentityObjectTypeMapper()
                                                                                          .getIdentityObjectType());
          if (identityObject instanceof LDAPIdentityObjectImpl) {
            return ((LDAPIdentityObjectImpl) identityObject).getDn();
          }
        } finally {
          exoFallbackIdentityStoreRepository.setUseExternalStore(false);
        }
      }
      
    }
    return null;
  }
  
  private void changeInternalPassword(User user, String password) throws Exception {
    long startTime = System.currentTimeMillis();
    user.setPassword(password);
    organizationService.getUserHandler().saveUser(user, true);
    long totalTime = System.currentTimeMillis() - startTime;
  
    log.info("service={} operation={} parameters=\"user:{}\" status=ok duration_ms={}",
             LOG_SERVICE_NAME, "changeInternalPassword", user.getUserName(), totalTime);
  }
}
