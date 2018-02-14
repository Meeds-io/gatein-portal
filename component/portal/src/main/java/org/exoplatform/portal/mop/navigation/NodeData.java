/*
 * Copyright (C) 2010 eXo Platform SAS.
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

package org.exoplatform.portal.mop.navigation;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.mop.Described;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.Visible;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.gatein.mop.api.Attributes;
import org.gatein.mop.api.workspace.Navigation;
import org.gatein.mop.api.workspace.Site;
import org.gatein.mop.api.workspace.link.Link;
import org.gatein.mop.api.workspace.link.PageLink;

/**
 * An immutable node data class.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class NodeData implements Serializable {

    /** . */
    final String parentId;

    /** . */
    final String id;

    /** . */
    final String name;

    /** . */
    final NodeState state;

    /** . */
    final String[] children;

    NodeData(Navigation navigation) {
        List<String> children = null;
        List<Navigation> _children = navigation.getChildren();
        if (_children == null) {
            children = Collections.emptyList();
        } else {
            children = new ArrayList<>();
            for (Navigation child : _children) {
              children.add(child.getObjectId());
            }
        }

        //
        String label = null;
        if (navigation.isAdapted(Described.class)) {
            Described described = navigation.adapt(Described.class);
            label = described.getName();
        }

        //
        Visibility visibility = Visibility.DISPLAYED;
        Date startPublicationDate = null;
        Date endPublicationDate = null;
        if (navigation.isAdapted(Visible.class)) {
            Visible visible = navigation.adapt(Visible.class);
            visibility = visible.getVisibility();
            startPublicationDate = visible.getStartPublicationDate();
            endPublicationDate = visible.getEndPublicationDate();
        }

        //
        PageKey pageRef = null;
        Link link = navigation.getLink();
        if (link instanceof PageLink) {
            PageLink pageLink = (PageLink) link;
            org.gatein.mop.api.workspace.Page target = pageLink.getPage();
            if (target != null) {
                Site site = target.getSite();
                pageRef = Utils.siteType(site.getObjectType()).key(site.getName()).page(target.getName());
            }
        }

        //
        Attributes attrs = navigation.getAttributes();

        //
        NodeState state = new NodeState(label, attrs.getValue(MappedAttributes.ICON),
                startPublicationDate != null ? startPublicationDate.getTime() : -1,
                endPublicationDate != null ? endPublicationDate.getTime() : -1, visibility, pageRef);

        //
        String parentId;
        Navigation parent = navigation.getParent();
        if (parent != null) {
            parentId = parent.getObjectId();
        } else {
            parentId = null;
        }

        //
        this.parentId = parentId;
        this.id = navigation.getObjectId();
        this.name = navigation.getName();
        this.state = state;
        this.children = children.toArray(new String[0]);
    }

    NodeData(NodeContext<?> context) {
        int size = 0;
        for (NodeContext<?> current = context.getFirst(); current != null; current = current.getNext()) {
            size++;
        }
        String[] children = new String[size];
        for (NodeContext<?> current = context.getFirst(); current != null; current = current.getNext()) {
            children[children.length - size--] = current.handle;
        }
        String parentId = context.getParent() != null ? context.getParent().handle : null;
        String id = context.handle;
        String name = context.getName();
        NodeState state = context.getState();

        //
        this.parentId = parentId;
        this.id = id;
        this.name = name;
        this.state = state;
        this.children = children;
    }

    NodeData(String parentId, String id, String name, NodeState state, String[] children) {
        this.parentId = parentId;
        this.id = id;
        this.name = name;
        this.state = state;
        this.children = children;
    }

    public Iterator<String> iterator(boolean reverse) {
        if (reverse) {
            return new Iterator<String>() {
                int index = children.length;

                public boolean hasNext() {
                    return index > 0;
                }

                public String next() {
                    if (index > 0) {
                        return children[--index];
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        } else {
            return new Iterator<String>() {
                int index = 0;

                public boolean hasNext() {
                    return index < children.length;
                }

                public String next() {
                    if (index < children.length) {
                        return children[index++];
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public NodeState getState() {
        return state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeData)) return false;

        NodeData nodeData = (NodeData) o;

        return StringUtils.equals(parentId, nodeData.parentId) && StringUtils.equals(id, nodeData.id)
                && StringUtils.equals(name, nodeData.name);
    }

    @Override
    public int hashCode() {
        int result = parentId != null ? parentId.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NodeData[id=" + id + ",name=" + name + ",state=" + state + ",children=" + Arrays.asList(children) + "]";
    }
}
