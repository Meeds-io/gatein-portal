package org.gatein.portal.idm.impl.cache.infinispan.tree;

import org.gatein.portal.idm.impl.cache.infinispan.InfinispanAPICacheProviderImpl;

import java.util.ArrayList;

/**
 * Implementation of leaf node, which is not persisted in cache at the moment when this object is instantiated
 * Calling of method {@link #put(String, Object)} will enforce persisting of this node into underlying infinispan cache
 *
 */
public class IDMTransientNodeImpl implements Node
{
   /** FQN of this node */
   private final Fqn nodeFqn;

   /** Underlying tree cache */
   private final IDMTreeCacheImpl treeCache;

   public IDMTransientNodeImpl(Fqn nodeFqn, IDMTreeCacheImpl treeCache)
   {
      this.nodeFqn = nodeFqn;
      this.treeCache = treeCache;
   }

   /**
    * Persists the node and value of this node into infinispan cache
    *
    * @param key parameter is defacto unused (only exception is for "query_unique" key as we need to wrap it to collection before save)
    * @param value Value to be added to cache under FQN of this node
    */
   public void put(String key, Object value)
   {
      // Workaround to cover unique query case
      if (InfinispanAPICacheProviderImpl.NODE_QUERY_UNIQUE_KEY.equals(key))
      {
         ArrayList<Object> list = new ArrayList<Object>();
         list.add(value);
         value = list;
      }

      treeCache.addLeafNode(nodeFqn, value);
   }

   public Fqn getFqn()
   {
      return nodeFqn;
   }

   public Object get(String key)
   {
      throw new IllegalStateException("Can't get value of transient node");
   }

   public boolean removeChild(Object childName)
   {
      throw new IllegalStateException("Can't remove child of transient node");
   }

   public void removeChildren()
   {
      throw new IllegalStateException("Can't remove children of transient node");
   }
}
