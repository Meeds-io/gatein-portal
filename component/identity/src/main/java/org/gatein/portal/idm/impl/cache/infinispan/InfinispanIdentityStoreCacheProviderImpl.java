package org.gatein.portal.idm.impl.cache.infinispan;

import org.gatein.portal.idm.impl.cache.infinispan.tree.Fqn;
import org.gatein.portal.idm.impl.cache.infinispan.tree.Node;
import org.gatein.portal.idm.impl.cache.infinispan.tree.TreeCache;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.impl.api.SimpleAttribute;
import org.picketlink.idm.impl.types.SimpleIdentityObject;
import org.picketlink.idm.impl.types.SimpleIdentityObjectRelationship;
import org.picketlink.idm.impl.types.SimpleIdentityObjectRelationshipType;
import org.picketlink.idm.impl.types.SimpleIdentityObjectType;
import org.picketlink.idm.spi.cache.IdentityObjectRelationshipNameSearch;
import org.picketlink.idm.spi.cache.IdentityObjectRelationshipSearch;
import org.picketlink.idm.spi.cache.IdentityObjectSearch;
import org.picketlink.idm.spi.cache.IdentityStoreCacheProvider;
import org.picketlink.idm.spi.configuration.IdentityConfigurationContext;
import org.picketlink.idm.spi.configuration.IdentityConfigurationContextRegistry;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectAttribute;
import org.picketlink.idm.spi.model.IdentityObjectRelationship;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache provider implementation based on Infinispan and it's tree cache API
 *
 */
public class InfinispanIdentityStoreCacheProviderImpl extends AbstractInfinispanCacheProvider implements IdentityStoreCacheProvider
{
   private static Logger log = Logger.getLogger(InfinispanIdentityStoreCacheProviderImpl.class.getName());

   public static final String NODE_OBJECT_KEY = "object";

   public static final String NODE_IO_COUNT = "NODE_IO_COUNT";

   public static final String NODE_IO_ATTRIBUTES = "NODE_IO_ATTRIBUTES";

   public static final String NODE_OBJECTS = "NODE_OBJECTS";

   public static final String NODE_REL_PROPS = "NODE_REL_PROPS";

   public static final String NODE_REL_NAME_PROPS = "NODE_REL_NAME_PROPS";

   public static final String NODE_IO_SEARCH = "NODE_IO_SEARCH";

   public static final String NODE_IO_REL_SEARCH = "NODE_IO_REL_SEARCH";

   public static final String NODE_IO_REL_NAME_SEARCH = "NODE_IO_REL_NAME_SEARCH";

   public static final String MAIN_ROOT_STORE = "NODE_MAIN_ROOT_STORE";

   @Override
   protected String getRootNode()
   {
      return MAIN_ROOT_STORE;
   }

   public void initialize(Map<String, String> properties, IdentityConfigurationContext configurationContext)
   {
      IdentityConfigurationContextRegistry registry = configurationContext.getConfigurationRegistry();
      super.initialize(properties, registry);
   }

   @Override
   protected TreeCache getCacheFromRegistry(Object registry, String registryName) throws IdentityException
   {
      IdentityConfigurationContextRegistry reg = (IdentityConfigurationContextRegistry)registry;
      return (TreeCache)reg.getObject(registryName);
   }

   private Fqn getFqn(String ns, String node, int hash)
   {
      return Fqn.fromString(getNamespacedFqn(ns) + "/" + node + "/" + hash);
   }

   public String getNamespace(String storeId, String sessionId, String realmId)
   {
      if (realmId == null)
      {
         return getNamespace(sessionId);
      }
      return storeId + "/" + sessionId + "/" + realmId;
   }

   public void putIdentityObjectCount(String ns, String type, int count)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_COUNT, type);

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, count);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObject count stored in cache: " + count + "; type=" + type
                  + ";namespace=" + ns);
         }
      }
   }

   public int getIdentityObjectCount(String ns, String type)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_COUNT, type);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         int count = -1;
         Integer i = (Integer)node.get(NODE_OBJECT_KEY);
         if (i != null)
         {
            count = i;
         }

         if (log.isLoggable(Level.FINER) && count != -1)
         {
            log.finer(this.toString() + "IdentityObject count found in cache: " + count + "; type=" + type
                  + ";namespace=" + ns);
         }

         return count;
      }

      return -1;
   }

   public void invalidateIdentityObjectCount(String ns, String type)
   {
      removeNode(Fqn.fromString(getNamespacedFqn(ns) + "/" + NODE_IO_COUNT + "/" + type));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObject count. Namespace:" + ns + "; type=" + type
               + ";namespace=" + ns);
      }
   }

   public void putIdentityObjectSearch(String ns, IdentityObjectSearch search, Collection<IdentityObject> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_SEARCH, search.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, safeCopyIO(results));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObject search stored in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }
      }
   }

   public Collection<IdentityObject> getIdentityObjectSearch(String ns, IdentityObjectSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_SEARCH, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Collection<IdentityObject> results = (Collection<IdentityObject>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && results != null)
         {
            log.finer(this.toString() + "IdentityObject search found in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }

         return results;
      }

      return null;
   }

   public void invalidateIdentityObjectSearches(String ns)
   {
      removeNode(getFqn(ns, NODE_IO_SEARCH));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObject searches. Namespace:" + ns);
      }
   }

   public void putIdentityObjectRelationshipSearch(String ns, IdentityObjectRelationshipSearch search, Set<IdentityObjectRelationship> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_REL_SEARCH, search.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, safeCopyIOR(results));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObjectRelationship search stored in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }
      }
   }

   public Set<IdentityObjectRelationship> getIdentityObjectRelationshipSearch(String ns, IdentityObjectRelationshipSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_REL_SEARCH, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Set<IdentityObjectRelationship> results = (Set<IdentityObjectRelationship>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && results != null)
         {
            log.finer(this.toString() + "IdentityObjectRelationship search found in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }

         return results;
      }

      return null;
   }

   public void invalidateIdentityObjectRelationshipSearches(String ns)
   {
      removeNode(getFqn(ns, NODE_IO_REL_SEARCH));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObjectRelationship searches. Namespace:" + ns);
      }
   }

   public void putIdentityObjectRelationshipNameSearch(String ns, IdentityObjectRelationshipNameSearch search, Set<String> results)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_REL_NAME_SEARCH, search.hashCode());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, results);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObjectRelationshipName search stored in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }
      }
   }

   public Set<String> getIdentityObjectRelationshipNameSearch(String ns, IdentityObjectRelationshipNameSearch search)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_REL_NAME_SEARCH, search.hashCode());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Set<String> results = (Set<String>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && results != null)
         {
            log.finer(this.toString() + "IdentityObjectRelationshipName search found in cache: results.size()=" + results.size()
                  + ";namespace=" + ns);
         }

         return results;
      }

      return null;
   }

   public void invalidateIdentityObjectRelationshipNameSearches(String ns)
   {
      removeNode(getFqn(ns, NODE_IO_REL_NAME_SEARCH));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObjectRelationshipName searches. Namespace:" + ns);
      }
   }

   public void putProperties(String ns, IdentityObjectRelationship relationship, Map<String, String> properties)
   {
      Fqn nodeFqn = getFqn(ns, NODE_REL_PROPS, decode(relationship));

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, properties);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObjectRelationship properties stored in cache: relationship="
                  + relationship + "; properties.size()=" + properties.size() + ";namespace=" + ns);
         }
      }
   }

   private String decode(IdentityObjectRelationship r)
   {
      return r.getFromIdentityObject().getIdentityType().getName() +
            r.getFromIdentityObject().getName() +
            r.getToIdentityObject().getIdentityType().getName() +
            r.getToIdentityObject().getName() +
            r.getType().getName();
   }

   public Map<String, String> getProperties(String ns, IdentityObjectRelationship relationship)
   {
      Fqn nodeFqn = getFqn(ns, NODE_REL_PROPS, decode(relationship));

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Map<String, String> props = (Map<String, String>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && props != null)
         {
            log.finer(this.toString() + "IdentityObjectRelationship properties found in cache: properties.size()=" + props.size() +
                  "; relationship=" + relationship + ";namespace=" + ns);
         }

         return props;
      }

      return null;
   }

   public void invalidateRelationshipProperties(String ns, IdentityObjectRelationship relationship)
   {
      removeNode(getFqn(ns, NODE_REL_PROPS, decode(relationship)));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObjectRelationship properties. Namespace:" + ns
               + "; relationship=" + relationship + ";namespace=" + ns);
      }
   }

   public void invalidateRelationshipProperties(String ns)
   {
      removeNode(getFqn(ns, NODE_REL_PROPS));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObjectRelationship properties. Namespace:" + ns);
      }
   }

   public void putProperties(String ns, String name, Map<String, String> properties)
   {
      Fqn nodeFqn = getFqn(ns, NODE_REL_NAME_PROPS, name);

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, properties);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObjectRelationshipName properties stored in cache: name="
                  + name + "; properties.size()=" + properties.size() + ";namespace=" + ns);
         }
      }
   }

   public Map<String, String> getProperties(String ns, String name)
   {
      Fqn nodeFqn = getFqn(ns, NODE_REL_NAME_PROPS, name);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Map<String, String> props = (Map<String, String>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && props != null)
         {
            log.finer(this.toString() + "IdentityObjectRelationshipName properties found in cache: properties.size()=" + props.size() +
                  "; name=" + name + ";namespace=" + ns);
         }

         return props;
      }

      return null;
   }

   public void invalidateRelationshipNameProperties(String ns, String relationship)
   {
      removeNode(getFqn(ns, NODE_REL_NAME_PROPS, relationship));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObjectRelationshipName properties." +
               " Namespace:" + ns + "; name=" + relationship);
      }
   }

   public void invalidateRelationshipNameProperties(String ns)
   {
      removeNode(getFqn(ns, NODE_REL_NAME_PROPS));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObjectRelationshipName properties. " +
               "Namespace:" + ns);
      }

   }

   public void putIdentityObjectAttributes(String ns, IdentityObject io, Map<String, IdentityObjectAttribute> attributes)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_ATTRIBUTES, io.getIdentityType().getName() + io.getName());

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, safeCopyAttr(attributes));

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "IdentityObject attributes stored in cache: io=" + io
                  + "; attributes.size()=" + attributes.size() + ";namespace=" + ns);
         }
      }
   }

   public Map<String, IdentityObjectAttribute> getIdentityObjectAttributes(String ns, IdentityObject io)
   {
      Fqn nodeFqn = getFqn(ns, NODE_IO_ATTRIBUTES, io.getIdentityType().getName() + io.getName());

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Map<String, IdentityObjectAttribute> props = (Map<String, IdentityObjectAttribute>)node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && props != null)
         {
            log.finer(this.toString() + "IIdentityObject attributes found in cache: attributes.size()=" + props.size() +
                  "; io=" + io + ";namespace=" + ns);
         }

         return props;
      }

      return null;
   }

   public void invalidateIdentityObjectAttriubtes(String ns, IdentityObject io)
   {
      removeNode(getFqn(ns, NODE_IO_ATTRIBUTES, io.getIdentityType().getName() + io.getName()));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObject attributes. Namespace:" + ns + "; io=" + io);
      }
   }

   public void invalidateIdentityObjectAttriubtes(String ns)
   {
      removeNode(getFqn(ns, NODE_IO_ATTRIBUTES));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating IdentityObject attributes. Namespace:" + ns);
      }
   }

   public void putObject(String ns, int hash, Object value)
   {
      Fqn nodeFqn = getFqn(ns, NODE_OBJECTS, hash);

      Node ioNode = addNode(nodeFqn);

      if (ioNode != null)
      {
         ioNode.put(NODE_OBJECT_KEY, value);

         if (log.isLoggable(Level.FINER))
         {
            log.finer(this.toString() + "Object stored in cache: hash=" + hash
                  + "; value=" + value + ";namespace=" + ns);
         }
      }
   }

   public Object getObject(String ns, int hash)
   {
      Fqn nodeFqn = getFqn(ns, NODE_OBJECTS, hash);

      Node node = getNode(nodeFqn);

      if (node != null)
      {
         Object value = node.get(NODE_OBJECT_KEY);

         if (log.isLoggable(Level.FINER) && value != null)
         {
            log.finer(this.toString() + "Object found in cache: hash" + hash +
                  ";namespace=" + ns);
         }

         return value;
      }

      return null;
   }

   public void invalidateObject(String ns, int hash)
   {
      removeNode(getFqn(ns, NODE_OBJECTS, hash));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating object. Namespace:" + ns + "; hash=" + hash);
      }
   }

   public void invalidateObjects(String ns)
   {
      removeNode(getFqn(ns, NODE_OBJECTS));
      if (log.isLoggable(Level.FINER))
      {
         log.finer(this.toString() + "Invalidating objects. Namespace:" + ns);
      }
   }

   private List<IdentityObject> safeCopyIO(Collection<IdentityObject> res)
   {
      List<IdentityObject> nr = new LinkedList<IdentityObject>();

      for (IdentityObject io : res)
      {
         nr.add(new SimpleIdentityObject(io.getName(),
               new SimpleIdentityObjectType(io.getIdentityType().getName())));
      }

      return nr;
   }

   private Set<IdentityObjectRelationship> safeCopyIOR(Set<IdentityObjectRelationship> res)
   {
      Set<IdentityObjectRelationship> nr = new HashSet<IdentityObjectRelationship>();

      for (IdentityObjectRelationship ior : res)
      {
         IdentityObject from = new SimpleIdentityObject(ior.getFromIdentityObject().getName(),
               new SimpleIdentityObjectType(ior.getFromIdentityObject().getIdentityType().getName()));
         IdentityObject to = new SimpleIdentityObject(ior.getToIdentityObject().getName(),
               new SimpleIdentityObjectType(ior.getToIdentityObject().getIdentityType().getName()));

         nr.add(new SimpleIdentityObjectRelationship(from, to, ior.getName(), new SimpleIdentityObjectRelationshipType(ior.getType().getName())));
      }

      return nr;
   }

   private Map<String, IdentityObjectAttribute> safeCopyAttr(Map<String, IdentityObjectAttribute> res)
   {
      Map<String, IdentityObjectAttribute> nr = new HashMap<String, IdentityObjectAttribute>();

      for (IdentityObjectAttribute attr : res.values())
      {
         nr.put(attr.getName(), new SimpleAttribute(attr));
      }

      return nr;
   }
}
