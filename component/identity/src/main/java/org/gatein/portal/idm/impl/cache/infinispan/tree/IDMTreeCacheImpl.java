package org.gatein.portal.idm.impl.cache.infinispan.tree;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.batch.AutoBatchSupport;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.util.Immutables;
import org.infinispan.executors.DefaultScheduledExecutorFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simplified implementation of Infinispan tree cache where each node can be either path node or leaf node.
 *
 * Path nodes can't have any values attached to it, but they can have subnodes. They are saved in underlying infinispan
 * cache in form of {@link AtomicMap}, which contains references to subnodes.
 *
 * Leaf nodes can't have subnodes and they have only single value attached to them
 *
 *
 */
public class IDMTreeCacheImpl extends AutoBatchSupport implements TreeCache
{
   /** Underlying infinispan cache */
   private final AdvancedCache<Fqn, Object> cache;

   private static final Log log = LogFactory.getLog(IDMTreeCacheImpl.class);

   /** If true, then during put we will use specific lifespan value from parameter leafNodeLifespan */
   private final boolean attachLifespanToLeafNodes;

   /** will be attached for leaf nodes if attachLifespanToLeafNodes is true */
   private final long leafNodeLifespan;

   /**
    * @param cache underlying infinispan cache
    * @param attachLifespanToLeafNodes will be used for attachLifespanToLeafNodes
    * @param leafNodeLifespan will be used for leafNodeLifespan
    * @param staleNodesLinksCleanerDelay if non-negative value is used, then thread for cleaning stale records in structure of path nodes will be executed.
    *                                    Thread will be periodic task executed after each staleNodesLinksCleanerDelay milliseconds
    */
   public IDMTreeCacheImpl(Cache<?, ?> cache, boolean attachLifespanToLeafNodes, long leafNodeLifespan, long staleNodesLinksCleanerDelay)
   {
      this(cache.getAdvancedCache(), attachLifespanToLeafNodes, leafNodeLifespan, staleNodesLinksCleanerDelay);
   }

   private IDMTreeCacheImpl(AdvancedCache<?, ?> cache, boolean attachLifespanToLeafNodes, long leafNodeLifespan, long staleNodesLinksCleanerDelay)
   {
      this.cache = (AdvancedCache<Fqn, Object>)cache;
      this.batchContainer = cache.getBatchContainer();
      if (cache.getCacheConfiguration().indexing().index().isEnabled())
      {
         throw new CacheConfigurationException("TreeCache cannot be used with a Cache instance configured to use indexing!");
      }

      this.attachLifespanToLeafNodes = attachLifespanToLeafNodes;
      this.leafNodeLifespan = leafNodeLifespan;

      createRoot();

      // Skip start of cleaner if delay is negative
      if (staleNodesLinksCleanerDelay > 0)
      {
         startStaleNodesLinkCleaner(staleNodesLinksCleanerDelay);
      }
   }

   /**
    * Verify if item exists in cache
    *
    * @param f FQN, which acts as a key
    * @return true if item exists in cache
    */
   public boolean exists(Fqn f)
   {
      return cache.containsKey(f);
   }

   /**
    * Add leaf node and all it's supernodes needed for the path into underlying cache.
    * Also save the value under FQN of newly created node
    *
    * @param nodeFqn FQN of node to add to cache
    * @param value Value, which will be saved to cache under given FQN
    * @return newly created node
    */
   Node addLeafNode(Fqn nodeFqn, Object value)
   {
      startAtomic();
      try
      {
         createNodeInCache(nodeFqn, true);
         putValueToCacheLeafNode(nodeFqn, value);
         return new IDMNodeImpl(nodeFqn, cache, this, value);
      }
      finally
      {
         endAtomic();
      }
   }

   /**
    * {@inheritDoc}
    */
   public Node getTransientLeafNode(Fqn nodeFqn)
   {
      return new IDMTransientNodeImpl(nodeFqn, this);
   }

   /**
    *
    * @param nodeFqn FQN, which acts as a key
    * @return Node object related to cache value under given FQN, null if there is no value in cache under given FQN
    */
   public Node getNode(Fqn nodeFqn)
   {
      Object value = cache.get(nodeFqn);
      if (value != null)
      {
         return new IDMNodeImpl(nodeFqn, cache, this, value);
      }
      else
      {
         return null;
      }
   }

   /**
    * Remove node from cache and all it's subnodes (In case that node is path node, it's not removed but only all it's children are removed)
    *
    * @param nodeFqn
    * @return true if node was successfully removed
    */
   public boolean removeNode(Fqn nodeFqn)
   {
      if (nodeFqn.isRoot())
      {
         return false;
      }

      startAtomic();
      try
      {
         Object cacheObject = cache.get(nodeFqn);
         if (cacheObject != null && cacheObject instanceof AtomicMap)
         {
            // Don't remove node itself for now, but remove only it's child nodes
            Node myNode = getNode(nodeFqn);
            myNode.removeChildren();
            return true;
         }
         else
         {
            Node parentNode = getNode(nodeFqn.getParent());
            return (parentNode != null && parentNode.removeChild(nodeFqn.getLastElement()));
         }
      }
      finally
      {
         endAtomic();
      }
   }

   public boolean removeNode(String fqnString)
   {
      return removeNode(Fqn.fromString(fqnString));
   }

   public Cache getCache()
   {
      return cache;
   }

   private void createRoot()
   {
      if (!exists(Fqn.ROOT))
      {
         createNodeInCache(Fqn.ROOT, false);
      }
   }

   private boolean createNodeInCache(Fqn fqn, boolean isLeafNode)
   {
      if (cache.containsKey(fqn))
      {
         return false;
      }

      Fqn parent = fqn.getParent();
      if (!fqn.isRoot())
      {
         if (!exists(parent))
         {
            createNodeInCache(parent, false);
         }
         AtomicMap<Object, Fqn> parentStructure = getStructure(parent);
         parentStructure.put(fqn.getLastElement(), fqn);
      }

      if (!isLeafNode)
      {
         getStructure(fqn);
      }

      if (log.isTraceEnabled())
      {
         log.tracef("Created node %s", fqn);
      }

      return true;
   }

   AtomicMap<Object, Fqn> getStructure(Fqn fqn)
   {
      return AtomicMapLookup.getAtomicMap(cache, fqn);
   }

   void putValueToCacheLeafNode(Fqn key, Object value)
   {
      if (attachLifespanToLeafNodes)
      {
         cache.put(key, value, leafNodeLifespan, TimeUnit.MILLISECONDS);
         if (log.isTraceEnabled())
         {
            log.tracef("Added record %s with leafNodeLifespan " + leafNodeLifespan + "ms", key);
         }
      }
      else
      {
         cache.put(key, value);
         if (log.isTraceEnabled())
         {
            log.tracef("Added record %s with infinite leafNodeLifespan", key);
         }
      }
   }

   /**
    * Visual representation of a tree
    *
    * @return String rep
    */
   public String printTree()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("\n\n");

      // walk tree
      sb.append("+ ").append(Fqn.SEPARATOR);
      Object rootNodeContent = cache.get(Fqn.ROOT);

      if (rootNodeContent == null)
      {
         sb.append("NULL_CONTENT\n\n");
         return sb.toString();
      }

      if (rootNodeContent instanceof AtomicMap)
      {
         sb.append("  NO_DATA");
      }
      else
      {
         sb.append("  ").append(rootNodeContent);
      }
      sb.append("\n");
      printChildren(getNode(Fqn.ROOT), 1, sb);
      return sb.toString();
   }

   private void printChildren(Node node, int depth, StringBuilder sb)
   {
      AtomicMap<Object, Fqn> structure = getStructure(node.getFqn());

      for (Fqn childFqn : structure.values())
      {
         for (int i = 0; i < depth; i++) sb.append("  "); // indentations
         sb.append("+ ");
         sb.append(childFqn.getLastElementAsString()).append(Fqn.SEPARATOR);

         Object cacheValue = cache.get(childFqn);
         if (cacheValue instanceof AtomicMap)
         {
            sb.append("  NO_DATA\n");
            Node n = new IDMNodeImpl(childFqn, cache, this, cacheValue);
            printChildren(n, depth + 1, sb);
         }
         else
         {
            sb.append("  ").append(cache.get(childFqn));
         }
         sb.append("\n");
      }
   }

   // Start scheduled cleaner task with given schedule delay
   private void startStaleNodesLinkCleaner(long staleNodesLinksCleanerDelay)
   {
      Properties props = new Properties();
      props.put("threadNamePrefix", "StaleNodesLinksCleaner");
      ScheduledExecutorService executorService = new DefaultScheduledExecutorFactory().getScheduledExecutor(props);
      executorService.scheduleWithFixedDelay(new StaleNodesLinksCleaner(), staleNodesLinksCleanerDelay, staleNodesLinksCleanerDelay, TimeUnit.MILLISECONDS);
      log.info("StaleNodesCleaner started successfully with delay " + staleNodesLinksCleanerDelay);
   }

   private class StaleNodesLinksCleaner implements Runnable
   {
      public void run()
      {
         Node root = getNode(Fqn.ROOT);
         if (root != null)
         {
            log.debug("Going to process root node in StaleNodesLinksCleaner");
            processNode(root);
         }
         else
         {
            // Clear whole cache if root is missing
            log.debug("Root is missing. Going to clear whole cache in StaleNodesLinksCleaner");
            cache.clear();

         }
      }

      private void processNode(Node node)
      {
         if (node.get("") instanceof AtomicMap)
         {
            Fqn nodeFqn = node.getFqn();

            // Do we really need to refresh the state like this? But better yes
            AtomicMap<Object, Fqn> structure = getStructure(node.getFqn());
            List<Node> childPathNodes = new LinkedList<Node>();
            Set<Object> structureCopy = Immutables.immutableSetCopy(structure.keySet());

            // Wrap processing of current node into single transaction
            startAtomic();
            try
            {
               for (Object key : structureCopy)
               {
                  Fqn childFqn = structure.get(key);
                  Object cacheValue = cache.get(childFqn);

                  if (cacheValue == null)
                  {
                     // If child node doesn't exist, we need to remove it from our own structure
                     if (log.isTraceEnabled())
                     {
                        log.tracef("Removing node link %s from parent structure", childFqn);
                     }
                     structure.remove(key);
                  }
                  else if (cacheValue instanceof AtomicMap)
                  {
                     // Create node object and add it to childPathNodes list. We need to recursively process all child nodes after finish processing of current node
                     Node child = new IDMNodeImpl(childFqn, cache, IDMTreeCacheImpl.this, cacheValue);
                     childPathNodes.add(child);
                  }
               }
            }
            finally
            {
               endAtomic();
            }

            // Now recursively process child path nodes
            for (Node child : childPathNodes)
            {
               processNode(child);
            }
         }
      }
   }

}
