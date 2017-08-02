package org.picketlink.idm.impl.store.ldap;

import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.NotYetImplementedException;
import org.picketlink.idm.impl.helper.Tools;
import org.picketlink.idm.impl.model.ldap.LDAPIdentityObjectImpl;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectRelationshipType;
import org.picketlink.idm.spi.model.IdentityObjectType;
import org.picketlink.idm.spi.search.IdentityObjectSearchCriteria;
import org.picketlink.idm.spi.store.IdentityStoreInvocationContext;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
        List<SerializableSearchResult> searchResult = null;
        for (IdentityObjectType possibleTypeMatch : matches) {
          LDAPIdentityObjectTypeConfiguration typeConfiguration = getTypeConfiguration(ctx, possibleTypeMatch);
          Name jndiName = new CompositeName().add(dn);
          Attributes attrs = ldapContext.getAttributes(jndiName);
          String idAttributeName = typeConfiguration.getIdAttributeName();
          Attribute nameAttribute = attrs.get(idAttributeName);
          if (nameAttribute == null) {
            continue;
          }
          String filter = getTypeConfiguration(ctx, possibleTypeMatch).getEntrySearchFilter();
          String[] entryCtxs = getTypeConfiguration(ctx, possibleTypeMatch).getCtxDNs();
          String scope = getTypeConfiguration(ctx, possibleTypeMatch).getEntrySearchScope();
          String name = nameAttribute.get().toString();
          Object[] filterArgs = { name };
          if (filter != null) {
            searchResult = this.searchIdentityObjects(ctx, entryCtxs, filter, filterArgs, new String[] { idAttributeName }, scope,null);
            if (searchResult.size() > 0) {
              type = possibleTypeMatch;
              break;
            }
          } else {
            LDAPIdentityObjectImpl entry = (LDAPIdentityObjectImpl) this.findIdentityObject(ctx, name, possibleTypeMatch);
            if (entry != null && Tools.dnEquals(entry.getDn(), dn)) {
              type = possibleTypeMatch;
              break;
            }
          }
        }
      }
      if (type == null) {
        return null;
      }
      // ****** End changes ****/
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

  /**
   * To fix the exception encountered when trying to retrieve filtered groups, we verify if findIdentityObject() returns null or not
   *
   * @param ctx the IdentityStoreInvocationContext
   * @param identity the IdentityObject
   * @param relationshipType the IdentityObjectRelationshipType
   * @param excludes the excluded IdentityObjectSearchCriteria
   * @param parent if has parent returns true, else return false
   * @param criteria the IdentityObjectSearchCriteria
   * @return IdentityObject collection
   * @throws IdentityException
   */
  @Override
  public Collection<IdentityObject> findIdentityObject(IdentityStoreInvocationContext ctx, IdentityObject identity, IdentityObjectRelationshipType relationshipType, Collection<IdentityObjectType> excludes, boolean parent, IdentityObjectSearchCriteria criteria) throws IdentityException {
    if (log.isLoggable(Level.FINER)) {
      Tools.logMethodIn(log, Level.FINER,"findIdentityObject", new Object[] { "IdentityObject", identity, "IdentityObjectRelationshipType", relationshipType, "parent", parent, "IdentityObjectSearchCriteria", criteria });
    }
    if (relationshipType != null && !relationshipType.getName().equals(MEMBERSHIP_TYPE)) {
      throw new IdentityException("This store implementation supports only '" + MEMBERSHIP_TYPE + "' relationship type");
    }
    LDAPIdentityObjectImpl ldapIO = getSafeLDAPIO(ctx, identity);
    LDAPIdentityObjectTypeConfiguration typeConfig = getTypeConfiguration(ctx, identity.getIdentityType());
    LdapContext ldapContext = getLDAPContext(ctx);
    List<IdentityObject> objects = new LinkedList<IdentityObject>();
    try {
      // If parent simply look for all its members
      if (parent) {
        if (typeConfig.getParentMembershipAttributeName() != null) {
          Name jndiName = new CompositeName().add(ldapIO.getDn());
          Attributes attrs = ldapContext.getAttributes(jndiName);
          Attribute member = attrs.get(typeConfig.getParentMembershipAttributeName());
          if (member != null) {
            NamingEnumeration memberValues = member.getAll();
            while (memberValues.hasMoreElements()) {
              String memberRef = memberValues.nextElement().toString();
              // Ignore placeholder value in memberships
              String placeholder = typeConfig.getParentMembershipAttributePlaceholder();
              if (placeholder != null && memberRef.equalsIgnoreCase(placeholder)) {
                continue;
              }
              if (typeConfig.isParentMembershipAttributeDN()) {
                // ****** Begin changes ****/
                IdentityObject identityObject = findIdentityObject(ctx, memberRef);
                if (identityObject != null) {
                  if (criteria != null && criteria.getFilter() != null) {
                    String name = Tools.stripDnToName(memberRef);
                    String regex = Tools.wildcardToRegex(criteria.getFilter());
                    if (Pattern.matches(regex, name)) {
                      objects.add(identityObject);
                    }
                  } else {
                    objects.add(identityObject);
                  }
                }
                // ****** End changes ****/
              } else {
                // TODO: if relationships are not refered with DNs and only
                // names its not possible to map
                // TODO: them to proper IdentityType and keep name uniqnes per
                // type. Workaround needed
                throw new NotYetImplementedException("LDAP limitation. If relationship targets are not refered with FQDNs "
                    + "and only names, it's not possible to map them to proper IdentityType and keep name uniqnes per type. "
                    + "Workaround needed");
              }
              // break;
            }
          }
        } else {
          objects.addAll(findRelatedIdentityObjects(ctx, identity, ldapIO, criteria, false));
        }
        // if not parent then all parent entries need to be found
      } else {
        if (typeConfig.getChildMembershipAttributeName() == null) {
          objects.addAll(findRelatedIdentityObjects(ctx, identity, ldapIO, criteria, true));
        } else {
          // Escape JNDI special characters
          Name jndiName = new CompositeName().add(ldapIO.getDn());
          Attributes attrs = ldapContext.getAttributes(jndiName);
          Attribute member = attrs.get(typeConfig.getChildMembershipAttributeName());
          if (member != null) {
            NamingEnumeration memberValues = member.getAll();
            while (memberValues.hasMoreElements()) {
              String memberRef = memberValues.nextElement().toString();
              if (typeConfig.isChildMembershipAttributeDN()) {
                // TODO: use direct LDAP query instead of other find method and
                // add attributesFilter
                if (criteria != null && criteria.getFilter() != null) {
                  String name = Tools.stripDnToName(memberRef);
                  String regex = Tools.wildcardToRegex(criteria.getFilter());
                  if (Pattern.matches(regex, name)) {
                    objects.add(findIdentityObject(ctx, memberRef));
                  }
                } else {
                  objects.add(findIdentityObject(ctx, memberRef));
                }
              } else {
                // TODO: if relationships are not refered with DNs and only
                // names its not possible to map
                // TODO: them to proper IdentityType and keep name uniqnes per
                // type. Workaround needed
                throw new NotYetImplementedException("LDAP limitation. If relationship targets are not refered with FQDNs "
                    + "and only names, it's not possible to map them to proper IdentityType and keep name uniqnes per type. "
                    + "Workaround needed");
              }
              // break;
            }
          }
        }
      }
    } catch (NamingException e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "Exception occurred: ", e);
      }
      throw new IdentityException("Failed to resolve relationship", e);
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
    if (criteria != null && criteria.isPaged()) {
      objects = cutPageFromResults(objects, criteria);
    }
    if (criteria != null && criteria.isSorted()) {
      sortByName(objects, criteria.isAscending());
    }
    if (log.isLoggable(Level.FINER)) {
      Tools.logMethodOut(log, Level.FINER, "findIdentityObject", objects);
    }
    return objects;
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

  private void sortByName(List<IdentityObject> objects, final boolean ascending) {
    Collections.sort(objects, new Comparator<IdentityObject>() {
      public int compare(IdentityObject o1, IdentityObject o2) {
        if (ascending) {
          return o1.getName().compareTo(o2.getName());
        } else {
          return o2.getName().compareTo(o1.getName());
        }
      }
    });
  }
  // TODO: dummy and inefficient temporary workaround. Need to be implemented
  // with ldap request control
  private <T> List<T> cutPageFromResults(List<T> objects, IdentityObjectSearchCriteria criteria) {
    List<T> results = new LinkedList<T>();
    if (criteria.getMaxResults() == 0) {
      for (int i = criteria.getFirstResult(); i < objects.size(); i++) {
        if (i < objects.size()) {
          results.add(objects.get(i));
        }
      }
    } else {
      for (int i = criteria.getFirstResult(); i < criteria.getFirstResult() + criteria.getMaxResults(); i++) {
        if (i < objects.size()) {
          results.add(objects.get(i));
        }
      }
    }
    return results;
  }

  private LDAPIdentityObjectImpl getSafeLDAPIO(IdentityStoreInvocationContext ctx, IdentityObject io) throws IdentityException {
    if (io == null) {
      throw new IllegalArgumentException("IdentityObject is null");
    }
    if (io instanceof LDAPIdentityObjectImpl) {
      return (LDAPIdentityObjectImpl) io;
    } else {
      try {
        return (LDAPIdentityObjectImpl) findIdentityObject(ctx, io.getName(), io.getIdentityType());
      } catch (IdentityException e) {
        if (log.isLoggable(Level.FINER)) {
          log.finer("Failed to find IdentityObject in LDAP: " + io);
        }
        throw new IdentityException("Provided IdentityObject is not present in the store. Cannot operate on not stored objects.", e);
      }
    }
  }

}
