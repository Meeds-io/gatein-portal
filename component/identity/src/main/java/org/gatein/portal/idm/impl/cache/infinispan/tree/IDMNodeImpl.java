package org.gatein.portal.idm.impl.cache.infinispan.tree;


import org.gatein.portal.idm.impl.cache.infinispan.InfinispanAPICacheProviderImpl;
import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.commons.util.Immutables;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Collection;

/**
 * Implementation of Node, which can be either Path node or Leaf node with single value. This node already exists in underlying
 * infinispan cache at the moment of creation of this object.
 *
 */
public class IDMNodeImpl implements Node
{
   private static final Log log = LogFactory.getLog(IDMNodeImpl.class);

   /** FQN of this node */
   private final Fqn nodeFqn;

   /** Underlying infinispan cache */
   private final AdvancedCache<Fqn, Object> cache;

   /** Underlying tree cache */
   private final IDMTreeCacheImpl treeCache;

   /** Value from cache, which is saved in cache under FQN of this node (could be null for newly added cache nodes) */
   private final Object value;

   public IDMNodeImpl(Fqn nodeFqn, AdvancedCache<Fqn, Object> cache, IDMTreeCacheImpl treeCache, Object value)
   {
      this.nodeFqn = nodeFqn;
      this.cache = cache;
      this.treeCache = treeCache;
      this.value = value;
   }

   /**
    * @param key parameter is defacto unused (only exception is for "query_unique" key as we need to wrap it to collection before save)
    * @param value Value to be added to cache under FQN of this node
    */
   public void put(String key, Object value)
   {
      throw new IllegalStateException("Not supported to rewrite value of persistent node");
   }

   /**
    * @param key parameter is defacto unused (only exception is for "query_unique" key as we need to wrap it to collection before save)
    * @return value from cache
    */
   public Object get(String key)
   {
      // Use cached value from "value" if available. Otherwise lookup to infinispan
      Object result;
      if (value == null)
      {
         result = cache.get(nodeFqn);
      }
      else
      {
         result = value;
      }

      // Workaround to cover unique query case
      if (InfinispanAPICacheProviderImpl.NODE_QUERY_UNIQUE_KEY.equals(key))
      {
         Collection<Object> collection = (Collection<Object>)result;
         return collection.iterator().next();
      }
      else
      {
         return result;
      }
   }

   /**
    * Remove child node of this node. It also removes subnodes of child node if some are available
    * Method is useful only for path nodes
    *
    * @param childName name of child to remove
    * @return true of child was successfully removed
    */
   public boolean removeChild(Object childName)
   {
      // First remove record from our structure
      AtomicMap<Object, Fqn> structure = treeCache.getStructure(nodeFqn);
      Fqn childFqn = structure.remove(childName);

      if (childFqn == null)
      {
         childFqn = Fqn.fromString(nodeFqn + "/" + childName);
      }

      // Attempt to get object from cache
      Object child = cache.get(childFqn);

      // Null checks
      if (child == null)
      {
         return false;
      }

      // We are trying to remove non-leaf node. So we need to recursively remove children
      if (child instanceof AtomicMap)
      {
         Node childNode = new IDMNodeImpl(childFqn, cache, treeCache, child);
         childNode.removeChildren();
      }

      // Now real removal of node from cache
      Object o = cache.remove(childFqn);
      if (log.isTraceEnabled())
      {
         log.tracef("Removed node %s", childFqn);
      }
      return o!=null;
   }

   /**
    * Remove all children of this node. Method is useful only for path nodes
    */
   public void removeChildren()
   {
      AtomicMap atomicMap = treeCache.getStructure(nodeFqn);
      for (Object o : Immutables.immutableSetCopy(atomicMap.keySet()))
      {
         removeChild(o);
      }
   }

   /**
    * @return FQN of this node
    */
   public Fqn getFqn()
   {
      return nodeFqn;
   }
}
