package org.picketlink.idm.impl.store.ldap;

import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.helper.Tools;
import org.picketlink.idm.impl.model.ldap.LDAPIdentityObjectImpl;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectType;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * extends the class LDAPIdentityStoreImpl from PicketLink Idm in order to
 * improve the fetching members of groups from Active directory by fixing the
 * way when retrieving the ID of the IdentityObject from LDAP in case CN is not
 * equal to the UID attribute value
 *
 */
public class ExoLDAPIdentityStoreImpl extends LDAPIdentityStoreImpl {

  private static Logger log = Logger.getLogger(LDAPIdentityStoreImpl.class.getName());

  public ExoLDAPIdentityStoreImpl(String id) {
    super(id);
  }

  /**
   *  retrieve the ID of the IdentityObject from LDAP according to the customer's configuration
   *  (prevent problems when cn is not equal to the uid attribute )
   *
   * @param ctx the IdentityStoreInvocationContext
   * @param id the identity
   * @return the IdentityObject
   * @throws IdentityException
   */
  @Override
  public IdentityObject findIdentityObject(IdentityStoreInvocationContext ctx, String id) throws IdentityException {
    if (log.isLoggable(Level.FINER)) {
      Tools.logMethodIn(log, Level.FINER, "findIdentityObject", new Object[] { "id", id });
    }
    LdapContext ldapContext = getLDAPContext(ctx);
    try {
      if (id == null) {
        throw new IdentityException("identity id cannot be null");
      }
      String dn = id;
      IdentityObjectType type = null;
      // Recognize the type by ctx DN
      IdentityObjectType[] possibleTypes = getConfiguration(ctx).getConfiguredTypes();
      Set<IdentityObjectType> matches = new HashSet<IdentityObjectType>();
      for (IdentityObjectType possibleType : possibleTypes) {
        String[] typeCtxs = getTypeConfiguration(ctx, possibleType).getCtxDNs();
        for (String typeCtx : typeCtxs) {
          if (Tools.dnEndsWith(dn, typeCtx)) {
            matches.add(possibleType);
            break;
          }
        }
      }
      if (matches.size() == 1) {
        type = matches.iterator().next();
      } else if (matches.size() > 1) {
        //****** Begin changes ****/
        // retrieve the UID attribute from the LDAP configuration
        for (IdentityObjectType match : matches) {
          LDAPIdentityObjectTypeConfiguration typeConfiguration = getTypeConfiguration(ctx, match);
          Name jndiName = new CompositeName().add(dn);
          Attributes attrs = ldapContext.getAttributes(jndiName);
          Attribute nameAttribute = attrs.get(typeConfiguration.getIdAttributeName());
          if (nameAttribute != null) {
            String name = nameAttribute.get().toString();
            LDAPIdentityObjectImpl entry = (LDAPIdentityObjectImpl) this.findIdentityObject(ctx, name, match);
            if (entry != null && Tools.dnEquals(entry.getDn(), dn)) {
              type = match;
              break;
            }
            //****** End changes ****/
          }
        }
      }
      if (type == null) {
        throw new IdentityException("Cannot recognize identity object type by its DN: " + dn);
      }
      // Grab entry
      Name jndiName = new CompositeName().add(dn);
      Attributes attrs = ldapContext.getAttributes(jndiName);
      if (attrs == null) {
        throw new IdentityException("Can't find identity entry with DN: " + dn);
      }
      IdentityObject result = createIdentityObjectInstance(ctx, type, attrs, dn);
      if (log.isLoggable(Level.FINER)) {
        Tools.logMethodOut(log, Level.FINER, "findIdentityObject", result);
      }
      return result;

    } catch (NoSuchElementException e) {
      // log.debug("No identity object found with dn: " + dn, e);
    } catch (NamingException e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }
      throw new IdentityException("Identity object search failed.", e);
    } finally {
      try {
        ldapContext.close();
      } catch (NamingException e) {
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, "Exception occurred: ", e);
        }
        throw new IdentityException("Failed to close LDAP connection", e);
      }
    }
    if (log.isLoggable(Level.FINER)) {
      Tools.logMethodOut(log, Level.FINER, "findIdentityObject", null);
    }
    return null;
  }

  private LdapContext getLDAPContext(IdentityStoreInvocationContext ctx) throws IdentityException {
    LdapContext ldapContext = null;
    try {
      ldapContext = (LdapContext) ctx.getIdentityStoreSession().getSessionContext();
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.finer("Failed to obtain LDAP connection!");
      }
      throw new IdentityException("Could not obtain LDAP connection: ", e);
    }
    if (ldapContext == null) {
      if (log.isLoggable(Level.FINER)) {
        log.finer("Failed to obtain LDAP connection!");
      }
      throw new IdentityException("IllegalState: - Could not obtain LDAP connection");
    }
    return ldapContext;
  }

  private LDAPIdentityObjectTypeConfiguration getTypeConfiguration(IdentityStoreInvocationContext ctx,
                                                                   IdentityObjectType type) throws IdentityException {
    return getConfiguration(ctx).getTypeConfiguration(type.getName());
  }

  private LDAPIdentityStoreConfiguration getConfiguration(IdentityStoreInvocationContext ctx) throws IdentityException {
    return configuration;
  }

}
