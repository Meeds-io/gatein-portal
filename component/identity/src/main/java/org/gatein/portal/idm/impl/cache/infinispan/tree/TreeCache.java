package org.gatein.portal.idm.impl.cache.infinispan.tree;

import org.infinispan.Cache;

/**
 * Implementation of tree cache
 *
 */
public interface TreeCache
{
   /**
    * Verify if item exists in cache
    *
    * @param f FQN, which acts as a key
    * @return true if item exists in cache
    */
   public boolean exists(Fqn f);

   /**
    * Return transient node, which is not synced (persisted) in underlying infinispan cache. It will be persisted to cache when method
    * {@link IDMTreeCacheImpl#addLeafNode(Fqn, Object)} will be called.
    *
    * @param nodeFqn FQN of particular leaf transient node
    * @return Node, which is not transient and may not be presented in infinispan cache at the moment of method return
    */
   public Node getTransientLeafNode(Fqn nodeFqn);

   /**
    * @param nodeFqn FQN, which acts as a key
    * @return Node object related to cache value under given FQN
    */
   public Node getNode(Fqn nodeFqn);

   /**
    * Remove node from cache and all it's subnodes (In case that node is path node, it's not removed but only all it's children are removed)
    *
    * @param nodeFqn
    * @return true if node was successfully removed
    */
   public boolean removeNode(Fqn nodeFqn);

   /**
    * See {@link #removeNode(Fqn)}
    */
   public boolean removeNode(String fqnString);

   /**
    * @return String with whole cache printed in nice tree
    */
   public String printTree();

   /**
    * @return underlying infinispan cache
    */
   public Cache getCache();
}
