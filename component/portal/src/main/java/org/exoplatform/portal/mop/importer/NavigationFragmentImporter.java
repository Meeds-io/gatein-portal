/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.mop.importer;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.portal.tree.diff.*;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class NavigationFragmentImporter {

    private static final ListAdapter<PageNodeContainer, String> PAGE_NODE_CONTAINER_ADAPTER = new ListAdapter<PageNodeContainer, String>() {
        public int size(PageNodeContainer list) {
            List<PageNode> nodes = list.getNodes();
            if (nodes == null) {
                return 0;
            } else {
                return nodes.size();
            }
        }

        public Iterator<String> iterator(PageNodeContainer list, boolean reverse) {
            List<PageNode> nodes = list.getNodes();
            if (nodes == null) {
                return Collections.<String> emptyList().iterator();
            } else {
                String[] names = new String[nodes.size()];
                int index = 0;
                for (PageNode child : nodes) {
                    names[index++] = child.getName();
                }
                return Adapters.<String> list().iterator(names, reverse);
            }
        }
    };

    private static final ListAdapter<NodeContext<?>, String> NODE_ADAPTER = new ListAdapter<NodeContext<?>, String>() {
        public int size(NodeContext<?> list) {
            return list.getNodeCount();
        }

        public Iterator<String> iterator(NodeContext<?> list, boolean reverse) {
            int size = list.getNodeCount();
            String[] names = new String[size];
            int index = 0;
            for (NodeContext<?> child = list.getFirst(); child != null; child = child.getNext()) {
                names[index++] = child.getName();
            }
            return Adapters.<String> list().iterator(names, reverse);
        }
    };

    /** . */
    private final String[] path;

    /** . */
    private final NavigationService navigationService;

    /** . */
    private final SiteKey navigationKey;

    /** . */
    private final Locale portalLocale;

    /** . */
    private final DescriptionService descriptionService;

    /** . */
    private final PageNodeContainer src;

    /** . */
    private final ImportConfig config;

    public NavigationFragmentImporter(String[] path, NavigationService navigationService, SiteKey navigationKey,
            Locale portalLocale, DescriptionService descriptionService, PageNodeContainer src, ImportConfig config) {
        this.path = path;
        this.navigationService = navigationService;
        this.navigationKey = navigationKey;
        this.portalLocale = portalLocale;
        this.descriptionService = descriptionService;
        this.src = src;
        this.config = config;
    }

    public ImportConfig getConfig() {
        return config;
    }

    public NodeContext<?> perform() {
        NavigationContext navigationCtx = navigationService.loadNavigation(navigationKey);

        //
        if (navigationCtx != null) {
            NodeContext root = navigationService.loadNode(NodeModel.SELF_MODEL, navigationCtx, GenericScope.branchShape(path),
                    null);

            //
            NodeContext from = root;
            for (String name : path) {
                NodeContext a = from.get(name);
                if (a != null) {
                    from = a;
                } else {
                    from = from.add(null, name);
                }
            }

            // Collect labels
            Map<NodeContext<?>, Map<Locale, org.exoplatform.portal.mop.State>> labelMap = new HashMap<NodeContext<?>, Map<Locale, org.exoplatform.portal.mop.State>>();

            // Perform save
            perform(src, from, labelMap);

            // Save the node
            navigationService.saveNode(root, null);

            //
            for (Map.Entry<NodeContext<?>, Map<Locale, org.exoplatform.portal.mop.State>> entry : labelMap.entrySet()) {
                String id = entry.getKey().getId();
                descriptionService.setDescriptions(id, entry.getValue());
            }

            //
            return from;
        } else {
            return null;
        }
    }

    private void perform(PageNodeContainer src, final NodeContext<?> dst,
            final Map<NodeContext<?>, Map<Locale, org.exoplatform.portal.mop.State>> labelMap) {
        navigationService.rebaseNode(dst, Scope.CHILDREN, null);

        //
        ListDiff<PageNodeContainer, NodeContext<?>, String> diff = new ListDiff<PageNodeContainer, NodeContext<?>, String>(
                PAGE_NODE_CONTAINER_ADAPTER, NODE_ADAPTER);

        //
        List<PageNode> srcChildren = src.getNodes();
        ListChangeIterator<PageNodeContainer, NodeContext<?>, String> it = diff.iterator(src, dst);

        class Change {
            final ListChangeType type;
            final String name;
            final int index1;
            final int index2;

            Change(ListChangeType type, String name, int index1, int index2) {
                this.type = type;
                this.name = name;
                this.index1 = index1;
                this.index2 = index2;
            }
        }

        // Buffer the changes in a list
        LinkedList<Change> changes = new LinkedList<Change>();
        while (it.hasNext()) {
            ListChangeType type = it.next();
            changes.add(new Change(type, it.getElement(), it.getIndex1(), it.getIndex2()));
        }
        changes.sort((change1, change2) -> {
          PageNode srcNode1 = src.getNode(change1.name);
          PageNode srcNode2 = src.getNode(change2.name);
          int index1 = srcNode1 == null ? Integer.MAX_VALUE : src.getNodes().indexOf(srcNode1);
          int index2 = srcNode2 == null ? Integer.MAX_VALUE : src.getNodes().indexOf(srcNode2);
          return index1 - index2;
        });

        // The last encountered child
        NodeContext<?> previousChild = null;

        // Replay the changes and apply them
        for (Change change : changes) {
            PageNode srcChild = src.getNode(change.name);
            NodeContext<?> dstChild = dst.get(change.name);

            //
            switch (change.type) {
                case SAME:
                    // Perform recursively
                    perform(srcChild, dstChild, labelMap);

                    //
                    if (config.updatedSame) {
                        update(src, dst, srcChild, dstChild, labelMap);
                    }

                    //
                    previousChild = dstChild;
                    break;
                case REMOVE:
                    if (dst.getNode(change.name) != null) {
                    } else {
                        if (config.createMissing) {
                            previousChild = add(srcChild, previousChild, dst, labelMap);
                        }
                    }
                    break;
                case ADD:
                    if (src.getNode(change.name) != null) {
                        if (config.updatedSame) {
                            update(src, dst, srcChild, dstChild, labelMap);
                        }
                        previousChild = dstChild;
                    } else {
                        if (config.destroyOrphan) {
                            dstChild.removeNode();
                        } else {
                            previousChild = dstChild;
                        }
                    }
                    break;
            }
        }
    }

    private NodeContext<?> add(PageNode target, NodeContext<?> previous, NodeContext<?> parent,
            Map<NodeContext<?>, Map<Locale, org.exoplatform.portal.mop.State>> labelMap) {
        I18NString labels = target.getLabels();

        //
        Map<Locale, org.exoplatform.portal.mop.State> description;
        if (labels.isSimple()) {
            description = null;
        } else if (labels.isEmpty()) {
            description = null;
        } else {
            description = new HashMap<Locale, org.exoplatform.portal.mop.State>();
            for (Map.Entry<Locale, String> entry : labels.getExtended(portalLocale).entrySet()) {
                description.put(entry.getKey(), new org.exoplatform.portal.mop.State(entry.getValue(), null));
            }
        }

        //
        String name = target.getName();
        int index;
        if (previous != null) {
            index = parent.get((previous).getName()).getIndex() + 1;
        } else {
            index = 0;
        }
        NodeContext<?> child = parent.add(index, name);
        NodeState state = target.getState();
        child.setState(state);

        //
        if (description != null) {
            labelMap.put(child, description);
        }

        // We recurse to create the descendants
        List<PageNode> targetChildren = target.getNodes();
        if (targetChildren != null) {
            NodeContext<?> targetPrevious = null;
            for (PageNode targetChild : targetChildren) {
                targetPrevious = add(targetChild, targetPrevious, child, labelMap);
            }
        }

        //
        return child;
    }

    private void update(PageNodeContainer srcParent, NodeContext dstParent, PageNode src, NodeContext<?> target, Map<NodeContext<?>, Map<Locale, org.exoplatform.portal.mop.State>> labelMap) {
        target.setState(src.getState());

        int srcIndex = srcParent.getNodes().indexOf(src);
        if (srcIndex <= 0) {
          dstParent.add(0, target);
        } else {
          PageNode previousSrcNode = srcParent.getNodes().get(srcIndex - 1);
          NodeContext previousTargetNode = dstParent.get(previousSrcNode.getName());
          if (previousTargetNode != null) {
            dstParent.add(previousTargetNode.getIndex() + 1, target);
          } else {
            dstParent.add(null, target);
          }
        }

        // Update extended labels if necessary
        I18NString labels = src.getLabels();
        Map<Locale, org.exoplatform.portal.mop.State> description;
        if (labels.isSimple()) {
            description = null;
        } else if (labels.isEmpty()) {
            description = null;
        } else {
            description = new HashMap<Locale, org.exoplatform.portal.mop.State>();
            for (Map.Entry<Locale, String> entry : labels.getExtended(portalLocale).entrySet()) {
                description.put(entry.getKey(), new org.exoplatform.portal.mop.State(entry.getValue(), null));
            }
        }

        if (description != null) {
            labelMap.put(target, description);
        } else {
            labelMap.put(target, Collections.<Locale, org.exoplatform.portal.mop.State> emptyMap());
        }
    }
}
