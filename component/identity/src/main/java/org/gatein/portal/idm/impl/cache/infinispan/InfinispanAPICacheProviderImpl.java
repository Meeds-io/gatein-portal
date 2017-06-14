package org.gatein.portal.idm.impl.cache.infinispan;

import org.gatein.portal.idm.impl.cache.infinispan.tree.Fqn;
import org.gatein.portal.idm.impl.cache.infinispan.tree.Node;
import org.gatein.portal.idm.impl.cache.infinispan.tree.TreeCache;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.api.Group;
import org.picketlink.idm.api.IdentitySearchCriteria;
import org.picketlink.idm.api.Role;
import org.picketlink.idm.api.RoleType;
import org.picketlink.idm.api.User;
import org.picketlink.idm.api.cfg.IdentityConfigurationRegistry;
import org.picketlink.idm.api.query.GroupQuery;
import org.picketlink.idm.api.query.RoleQuery;
import org.picketlink.idm.api.query.UserQuery;
import org.picketlink.idm.cache.APICacheProvider;
import org.picketlink.idm.cache.GroupSearch;
import org.picketlink.idm.cache.RelationshipSearch;
import org.picketlink.idm.cache.RoleSearch;
import org.picketlink.idm.cache.RoleTypeSearch;
import org.picketlink.idm.cache.UserSearch;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.api.model.GroupKey;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache provider implementation based on Infinispan and it's tree cache API
 *
 */
public class InfinispanAPICacheProviderImpl extends AbstractInfinispanCacheProvider implements APICacheProvider
{
   private static Logger log = Logger.getLogger(InfinispanAPICacheProviderImpl.class.getName());

   public static final String NODE_OBJECT_KEY = "object";

   public static final String NODE_QUERY_KEY = "query";

   public static final String NODE_QUERY_UNIQUE_KEY = "query_unique";

   public static final String NODE_USERS = "USERS";

   public static final String NODE_USERS_COUNT = "USERS_COUNT";

   public static final String NODE_USERS_QUERIES = "USERS_QUERIES";

   public static final String NODE_USERS_CRITERIA = "USERS_CRITERIA";

   public static final String NODE_USERS_SEARCHES = "USERS_SEARCHES";

   public static final String NODE_GROUPS = "GROUPS";

   public static final String NODE_GROUPS_COUNT = "GROUPS_COUNT";

   public static final String NODE_GROUPS_QUERIES = "GROUPS_QUERIES";

   public static final String NODE_GROUPS_CRITERIA = "GROUPS_CRITERIA";

   public static final String NODE_GROUPS_SEARCHES = "GROUPS_SEARCHES";

   public static final String NODE_ROLES = "ROLES";

   public static final String NODE_ROLE_TYPES = "ROLE_TYPES";

   public static final String NODE_ROLE_QUERIES = "ROLE_QUERIES";

   public static final String NODE_ATTRIBUTES = "ATTRIBUTES";

   public static final String NODE_ROLE_PROPERTIES = "NODE_ROLE_PROPERTIES";

   public static final String NODE_ROLE_TYPE_PROPERTIES = "NODE_ROLE_TYPE_PROPERTIES";

   public static final String NODE_ROLE_SEARCHES = "NODE_ROLE_SEARCHES";

   public static final String NODE_ROLE_TYPE_SEARCHES = "NODE_ROLE_TYPE_SEARCHES";

   public static final String NODE_RELATIONSHIP_SEARCHES = "NODE_RELATIONSHIP_SEARCHES";

   public static final String MAIN_ROOT_API = "NODE_MAIN_ROOT_API";

   @Override
   protected String getRootNode()
   {
      return MAIN_ROOT_API;
   }

   public void initialize(Map<String, String> properties, IdentityConfigurationRegistry configurationRegistry)
   {
      super.initialize(properties, configurationRegistry);
   }

   @Override
   protected TreeCache getCacheFromRegistry(Object registry, String registryName) throws IdentityException
   {
      IdentityConfigurationRegistry reg = (IdentityConfigurationRegistry)registry;
      return (TreeCache)reg.getObject(registryName);
   }

   public void putUser(String ns, User user)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS, user.getKey());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, user);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "User stored in cache: " + user.getId() + ";namespace=" + ns);
         }
      }
   }

   public User getUser(String ns, String id)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS, id);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         User user = (User)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && user != null)
         {
            log.finer(this.toString() + "User found in cache: id=" + user.getId() + ";namespace=" + ns);
         }

         return user;
      }

      return null;
   }

   public void removeUser(String ns, String id)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS, id);

      removeNode(nodeFqn);

      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "User removed from cache: id= " + id + ";namespace=" + ns);
      }
   }

   public void putUsers(String ns, IdentitySearchCriteria criteria, Collection<User> users)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_CRITERIA, criteria != null ? criteria.hashCode() : null);

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, unmodifiableCollection(users));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "User criteria search stored in cache: users.size()=" + users.size() +
                  "; criteria.hash()=" + criteria + ";namespace=" + ns);
         }
      }
   }

   public Collection<User> getUsers(String ns, IdentitySearchCriteria criteria)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_CRITERIA, criteria != null ? criteria.hashCode() : null);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<User> users = (Collection<User>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && users != null)
         {
            log.finer(this.toString() + "User criteria search found in cache: users.size()=" + users.size() +
                  "; criteria.hash()=" + criteria + ";namespace=" + ns);
         }

         return users;
      }

      return null;
   }

   public void invalidateUsers(String ns)
   {
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_USERS));
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_USERS_COUNT));
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_USERS_CRITERIA));
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_USERS_QUERIES));
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_USERS_SEARCHES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating Users cache. Namespace:" + ns + ";namespace=" + ns);
      }
   }

   public void putUserCount(String ns, int count)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_COUNT);

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, count);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Users count stored in cache: " + count + ";namespace=" + ns);
         }
      }
   }

   public int getUserCount(String ns)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_COUNT);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         int count = -1;
         Integer i = (Integer)node.get(NODE_OBJECT_KEY);
         if (i != null)
         {
            count = i.intValue();
         }

         if (log.isLoggable(Level.FINER) && count != -1)
         {
            log.finer(this.toString() + "User count found in cache: " + count + ";namespace=" + ns);
         }

         return count;
      }

      return -1;
   }

   public void invalidateUserCount(String ns)
   {
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_USERS_COUNT));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating User count. Namespace:" + ns + ";namespace=" + ns);
      }
   }

   public void putGroup(String ns, Group group)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS, group.getKey());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, group);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Group stored in cache: " + group.getKey() + ";namespace=" + ns);
         }
      }
   }

   public Group getGroup(String ns, String groupType, String groupName)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS, GroupKey.parseKey(groupName, groupType));

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Group group = (Group)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && group != null)
         {
            log.finer(this.toString() + "Group found in cache: id=" + group.getKey() + ";namespace=" + ns);
         }

         return group;
      }

      return null;
   }

   public void removeGroup(String ns, String groupType, String groupName)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS, GroupKey.parseKey(groupName, groupType));

      removeNode(nodeFqn);

      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Group removed from cache: id= " + GroupKey.parseKey(groupName, groupType)
               + ";namespace=" + ns);
      }
   }

   public void putGroups(String ns, IdentitySearchCriteria criteria, Collection<Group> groups)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_CRITERIA, criteria != null ? criteria.hashCode() : null);

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, unmodifiableCollection(groups));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Group criteria search stored in cache: groups.size()=" + groups.size() +
                  "; criteria.hash()=" + criteria + ";namespace=" + ns);
         }
      }
   }

   public Collection<Group> getGroups(String ns, IdentitySearchCriteria criteria)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_CRITERIA, criteria != null ? criteria.hashCode() : null);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<Group> groups = (Collection<Group>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && groups != null)
         {
            log.finer(this.toString() + "Group criteria search found in cache: groups.size()=" + groups.size() +
                  "; criteria.hash()=" + criteria + ";namespace=" + ns);
         }

         return groups;
      }

      return null;
   }

   public void invalidateGroups(String ns)
   {
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_GROUPS));
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_GROUPS_COUNT));
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_GROUPS_CRITERIA));
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_GROUPS_QUERIES));
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_GROUPS_SEARCHES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating Groups cache. Namespace:" + ns + ";namespace=" + ns);
      }
   }

   public void putGroupCount(String ns, String groupType, int count)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_COUNT, groupType);

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, count);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Group count stored in cache: type=" + groupType + "; count=" + count
                  + ";namespace=" + ns);
         }
      }
   }

   public int getGroupCount(String ns, String groupType)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_COUNT, groupType);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         int count = -1;
         Integer i = (Integer)node.get(NODE_OBJECT_KEY);
         if (i != null)
         {
            count = i.intValue();
         }

         if (log.isLoggable(Level.FINER) && count != -1)
         {
            log.finer(this.toString() + "Group count found in cache: groupType=" + groupType + "; count=" + count
                  + ";namespace=" + ns);
         }

         return count;
      }

      return -1;
   }

   public void invalidateGroupCount(String ns, String groupType)
   {
      removeNode(getFqn(ns, NODE_GROUPS_COUNT, groupType));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating Group count. Namespace:" + ns);
      }
   }

   public void putRole(String ns, Role role)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLES, role.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, role);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Role stored in cache: " + role + ";namespace=" + ns);
         }
      }
   }

   public Role getRole(String ns, Role role)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLES, role.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Role result = (Role)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && result != null)
         {
            log.finer(this.toString() + "Role found in cache: id=" + result + ";namespace=" + ns);
         }

         return result;
      }

      return null;
   }

   public void removeRole(String ns, Role role)
   {
      removeNode(getFqn(ns, NODE_ROLES, role.hashCode()));
   }

   public void putRoleType(String ns, RoleType role)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_TYPES, role.getName());

      Node ioNode = addNode(nodeFqn);


      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, role);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "RoleType stored in cache: name=" + role.getName() + ";namespace=" + ns);
         }
      }
   }

   public RoleType getRoleType(String ns, RoleType role)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_TYPES, role.getName());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         RoleType result = (RoleType)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && result != null)
         {
            log.finer(this.toString() + "RoleType found in cache: name=" + role.getName() + ";namespace=" + ns);
         }

         return result;
      }

      return null;
   }

   public void removeRoleType(String ns, RoleType roleType)
   {
      removeNode(getFqn(ns, NODE_ROLE_TYPES, roleType.getName()));
   }

   public void putAttributes(String ns, String id, Map<String, Attribute> attributes)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ATTRIBUTES, id);

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, Collections.unmodifiableMap(attributes));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Attributes stored in cache: id=" + id + "; attributes.size()="
                  + attributes.size() + ";namespace=" + ns);
         }
      }
   }

   public Map<String, Attribute> getAttributes(String ns, String id)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ATTRIBUTES, id);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Map<String, Attribute> attributes = (Map<String, Attribute>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && attributes != null)
         {
            log.finer(this.toString() + "Attributes search found in cache: attributes.size()=" + attributes.size() +
                  "; id=" + id + ";namespace=" + ns);
         }

         return attributes;
      }

      return null;
   }

   public void invalidateAttributes(String ns, String id)
   {
      removeNode(getFqn(ns, NODE_ATTRIBUTES, id));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating Attributes. id=" + id +";namespace:" + ns);
      }
   }

   public void invalidateAttributes(String ns)
   {
      removeNode(getFqn(ns, NODE_ATTRIBUTES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating Attributes. Namespace:" + ns);
      }
   }

   public void putProperties(String ns, Role role, Map<String, String> properties)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_PROPERTIES, role.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, Collections.unmodifiableMap(properties));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Role properties stored in cache: role=" + role + "; properties.size()="
                  + properties.size() + ";namespace=" + ns);
         }
      }
   }

   public Map<String, String> getProperties(String ns, Role role)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_PROPERTIES, role.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Map<String, String> props = (Map<String, String>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && props != null)
         {
            log.finer(this.toString() + "Role properties found in cache: properties.size()=" + props.size() +
                  "; role=" + role + ";namespace=" + ns);
         }

         return props;
      }

      return null;
   }

   public void invalidateRoleProperties(String ns, Role role)
   {
      removeNode(getFqn(ns, NODE_ROLE_PROPERTIES, role.hashCode()));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating Role properties. role=" + role + "; Namespace:" + ns);
      }
   }

   public void invalidateRoleProperties(String ns)
   {
      removeNode(getFqn(ns, NODE_ROLE_PROPERTIES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating Role properties. Namespace:" + ns);
      }
   }

   public void putProperties(String ns, RoleType roleType, Map<String, String> properties)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_TYPE_PROPERTIES, roleType.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, Collections.unmodifiableMap(properties));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "RoleType properties stored in cache: roleType=" + roleType
                  + "; properties.size()=" + properties.size() + ";namespace=" + ns);
         }
      }
   }

   public Map<String, String> getProperties(String ns, RoleType roleType)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_TYPE_PROPERTIES, roleType.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Map<String, String> props = (Map<String, String>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && props != null)
         {
            log.finer(this.toString() + "RoleType properties found in cache: properties.size()=" + props.size() +
                  "; roleType=" + roleType + ";namespace=" + ns);
         }

         return props;
      }

      return null;
   }

   public void invalidateRoleTypeProperties(String ns, RoleType roleType)
   {
      removeNode(getFqn(ns, NODE_ROLE_TYPE_PROPERTIES, roleType.hashCode()));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating RoleType properties. roleType=" + roleType + "; Namespace:" + ns);
      }
   }

   public void invalidateRoleTypeProperties(String ns)
   {
      removeNode(getFqn(ns, NODE_ROLE_TYPE_PROPERTIES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating RoleType properties. Namespace:" + ns);
      }
   }

   public void invalidateAllSearches(String ns)
   {
      removeNode(getFqn(ns, NODE_USERS_SEARCHES));
      removeNode(getFqn(ns, NODE_GROUPS_SEARCHES));
      removeNode(getFqn(ns, NODE_RELATIONSHIP_SEARCHES));
      removeNode(getFqn(ns, NODE_ROLE_SEARCHES));
      removeNode(getFqn(ns, NODE_ROLE_TYPE_SEARCHES));

      // Probably should have separate invalidate methods, but for now need to be part of search invalidation
      removeNode(getFqn(ns, NODE_ROLES));
      removeNode(getFqn(ns, NODE_ROLE_TYPES));

      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating all searches. Namespace:" + ns);
      }
   }

   public void putUserSearch(String ns, UserSearch search, Collection<User> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_SEARCHES, search.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, unmodifiableCollection(results));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "User search stored in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }
      }
   }

   public Collection<User> getUserSearch(String ns, UserSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_SEARCHES, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<User> props = (Collection<User>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && props != null)
         {
            log.finer(this.toString() + "User search found in cache: properties.size()=" + props.size()
                  + ";namespace=" + ns);
         }

         return props;
      }

      return null;
   }

   public void putGroupSearch(String ns, GroupSearch search, Collection<Group> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_SEARCHES, search.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, unmodifiableCollection(results));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Group search stored in cache: results.size()=" + results.size() + ";namespace=" + ns);
         }
      }
   }

   public Collection<Group> getGroupSearch(String ns, GroupSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_SEARCHES, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<Group> results = (Collection<Group>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && results != null)
         {
            log.finer(this.toString() + "Group search found in cache: results.size()=" + results.size() + ";namespace=" + ns);
         }

         return results;
      }

      return null;
   }

   public void putRelationshipSearch(String ns, RelationshipSearch search, Boolean result)
   {
      Fqn nodeFqn = getFqn(ns, NODE_RELATIONSHIP_SEARCHES, search.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, result);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Relationship search stored in cache: result=" + result + ";namespace=" + ns);
         }
      }
   }

   public Boolean getRelationshipSearch(String ns, RelationshipSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_RELATIONSHIP_SEARCHES, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Boolean result = (Boolean)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && result != null)
         {
            log.finer(this.toString() + "Relationship search found in cache: properties.size()=" + result + ";namespace=" + ns);
         }

         return result;
      }

      return null;
   }

   public void putRoleSearch(String ns, RoleSearch search, Collection<Role> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_SEARCHES, search.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, unmodifiableCollection(results));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Role search stored in cache: results.size()=" + results.size() + ";namespace=" + ns);
         }
      }
   }

   public Collection<Role> getRoleSearch(String ns, RoleSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_SEARCHES, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<Role> results = (Collection<Role>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && results != null)
         {
            log.finer(this.toString() + "Role search found in cache: results.size()=" + results.size() + ";namespace=" + ns);
         }

         return results;
      }

      return null;
   }

   public void putRoleTypeSearch(String ns, RoleTypeSearch search, Collection<RoleType> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_TYPE_SEARCHES, search.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, unmodifiableCollection(results));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "RoleType search stored in cache: results.size()=" + results.size() + ";namespace=" + ns);
         }
      }
   }

   public Collection<RoleType> getRoleTypeSearch(String ns, RoleTypeSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_TYPE_SEARCHES, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<RoleType> results = (Collection<RoleType>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && results != null)
         {
            log.finer(this.toString() + "RoleType search found in cache: results.size()=" + results.size() + ";namespace=" + ns);
         }

         return results;
      }

      return null;
   }

   public void invalidateAllQueries(String ns)
   {
      removeNode(getFqn(ns, NODE_USERS_QUERIES));
      removeNode(getFqn(ns, NODE_GROUPS_QUERIES));
      removeNode(getFqn(ns, NODE_ROLE_QUERIES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating all queries. Namespace:" + ns);
      }

   }

   public void putUserQuery(String ns, UserQuery q, Collection<User> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_QUERIES, q.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_QUERY_KEY, results);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "User query stored in cache: q.hashCode()=" + q.hashCode()
                  + "; results.size()=" + results.size() + ";namespace=" + ns);
         }
      }
   }

   public void putUserQueryUnique(String ns, UserQuery q, User user)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_QUERIES, q.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_QUERY_UNIQUE_KEY, user);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "User query stored in cache: q.hashCode()=" + q.hashCode() + "; user=" + user
                  + ";namespace=" + ns);
         }
      }
   }

   public Collection<User> getUserQuery(String ns, UserQuery q)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_QUERIES, q.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<User> users = (Collection<User>)node.get(NODE_QUERY_KEY);

         if (log.isLoggable(Level.FINER) && users != null)
         {
            log.finer(this.toString() + "User query found in cache: users.size()=" + users.size() +
                  "; query.hash()=" + q.hashCode() + ";namespace=" + ns);
         }

         return users;
      }

      return null;
   }

   public User getUserQueryUnique(String ns, UserQuery q)
   {
      Fqn nodeFqn = getFqn(ns, NODE_USERS_QUERIES, q.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         User users = (User)node.get(NODE_QUERY_UNIQUE_KEY);

         if (log.isLoggable(Level.FINER) && users != null)
         {
            log.finer(this.toString() + "User query found in cache: user=" + users + ";namespace=" + ns);
         }

         return users;
      }

      return null;
   }

   public void invalidateUserQueries(String ns)
   {
      removeNode(getFqn(ns, NODE_USERS_QUERIES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating User queries. Namespace:" + ns);
      }
   }

   public void putGroupQuery(String ns, GroupQuery q, Collection<Group> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_QUERIES, q.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_QUERY_KEY, unmodifiableCollection(results));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Group query stored in cache: q.hashCode()=" + q.hashCode()
                  + "; results.size()=" + results.size() + ";namespace=" + ns);
         }
      }
   }

   public void putGroupQueryUnique(String ns, GroupQuery q, Group group)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_QUERIES, q.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_QUERY_UNIQUE_KEY, group);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Group query stored in cache: q.hashCode()=" + q.hashCode()
                  + "; group=" + group + ";namespace=" + ns);
         }
      }
   }

   public Collection<Group> getGroupQuery(String ns, GroupQuery q)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_QUERIES, q.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<Group> groups = (Collection<Group>)node.get(NODE_QUERY_KEY);

         if (log.isLoggable(Level.FINER) && groups != null)
         {
            log.finer(this.toString() + "Group query found in cache: groups.size()=" + groups.size() +
                  "; query.hash()=" + q.hashCode() + ";namespace=" + ns);
         }

         return groups;
      }

      return null;
   }

   public Group getGroupQueryUnique(String ns, GroupQuery q)
   {
      Fqn nodeFqn = getFqn(ns, NODE_GROUPS_QUERIES, q.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Group group = (Group)node.get(NODE_QUERY_UNIQUE_KEY);

         if (log.isLoggable(Level.FINER) && group != null)
         {
            log.finer(this.toString() + "Group query found in cache: group=" + group + ";namespace=" + ns);
         }

         return group;
      }

      return null;
   }

   public void invalidateGroupQueries(String ns)
   {
      removeNode(getFqn(ns, NODE_GROUPS_QUERIES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating Group queries. Namespace:" + ns);
      }
   }

   public void putRoleQuery(String ns, RoleQuery q, Collection<Role> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_QUERIES, q.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_QUERY_KEY, results);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Role query stored in cache: q.hashCode()=" + q.hashCode()
                  + "; results.size()=" + results.size() + ";namespace=" + ns);
         }
      }
   }

   public void putRoleQueryUnique(String ns, RoleQuery q, Role role)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_QUERIES, q.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_QUERY_UNIQUE_KEY, role);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Role query stored in cache: q.hashCode()=" + q.hashCode() + "; role=" + role
                  + ";namespace=" + ns);
         }
      }
   }

   public Collection<Role> getRoleQuery(String ns, RoleQuery q)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_QUERIES, q.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<Role> roles = (Collection<Role>)node.get(NODE_QUERY_KEY);

         if (log.isLoggable(Level.FINER) && roles != null)
         {
            log.finer(this.toString() + "Role query found in cache: users.size()=" + roles.size() +
                  "; query.hash()=" + q.hashCode() + ";namespace=" + ns);
         }

         return roles;
      }

      return null;
   }

   public Role getRoleQueryUnique(String ns, RoleQuery q)
   {
      Fqn nodeFqn = getFqn(ns, NODE_ROLE_QUERIES, q.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Role role = (Role)node.get(NODE_QUERY_UNIQUE_KEY);

         if (log.isLoggable(Level.FINER) && role != null)
         {
            log.finer(this.toString() + "Role query found in cache: role=" + role + ";namespace=" + ns);
         }

         return role;
      }

      return null;
   }

   public void invalidateRoleQueries(String ns)
   {
      removeNode(getFqn(ns, NODE_ROLE_QUERIES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating Role queries. Namespace:" + ns);
      }
   }

   private Collection unmodifiableCollection(Collection collection)
   {
      if (collection == null)
      {
         return collection;
      }
      if (collection instanceof List)
      {
         return Collections.unmodifiableList((List)collection);
      }
      if (collection instanceof SortedSet)
      {
         return Collections.unmodifiableSortedSet((SortedSet)collection);
      }
      if (collection instanceof Set)
      {
         return Collections.unmodifiableSet((Set)collection);
      }

      return Collections.unmodifiableCollection(collection);
   }
}
