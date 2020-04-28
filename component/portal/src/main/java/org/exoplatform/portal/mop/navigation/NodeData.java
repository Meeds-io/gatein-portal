/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.portal.mop.navigation;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang.StringUtils;

/**
 * An immutable node data class.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class NodeData implements Serializable {

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

    public NodeData(String parentId, String id, String name, NodeState state, String[] children) {
      this.parentId = parentId;
      this.id = id;
      this.name = name;
      this.state = state;
      this.children = children;
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

    public String getParentId() {
        return this.parentId;
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
