/*
 * Copyright (C) 2016 eXo Platform SAS.
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

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public interface NodeStore {

    NodeData loadNode(Long nodeId);

    /**
     * Load all Navigation node which refer to a page
     * @param pageRef
     * @return
     */
    NodeData[] loadNodes(String pageRef);

    NodeData[] createNode(Long parentId, Long previousId, String name, NodeState state);

    NodeData destroyNode(Long targetId);

    NodeData updateNode(Long targetId, NodeState state);

    NodeData[] moveNode(Long targetId, Long fromId, Long toId, Long previousId);

    NodeData[] renameNode(Long targetId, Long parentId, String name);

}
