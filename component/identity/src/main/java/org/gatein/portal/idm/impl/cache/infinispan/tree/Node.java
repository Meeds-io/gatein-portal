package org.gatein.portal.idm.impl.cache.infinispan.tree;

/**
 * Representation of Node in cache. Node can be either path node (node which contains other subnodes) or leaf node (node without subnodes).
 * Leaf node can contain either single value or multiple values (currently our impl supports only single value)
 *
 */
public interface Node
{
   /**
    * Put value to cache
    *
    * @param key parameter is defacto unused for leaf nodes with single value
    * @param value Value to be added to cache under FQN of this node
    */
   public void put(String key, Object value);

   /**
    *
    * @param key parameter is defacto unused for leaf nodes with single value
    * @return value from cache, which is saved under FQN of this node
    */
   public Object get(String key);

   /**
    * Remove child node of this node. Method is useful only for path nodes
    *
    * @param childName name of child to remove
    * @return true of child was successfully removed
    */
   boolean removeChild(Object childName);

   /**
    * Remove all children of this node. Method is useful only for path nodes
    */
   void removeChildren();

   /**
    * @return FQN of this node
    */
   Fqn getFqn();
}
