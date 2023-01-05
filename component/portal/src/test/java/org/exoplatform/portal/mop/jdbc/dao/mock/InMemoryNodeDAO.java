/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.portal.mop.jdbc.dao.mock;

import java.util.List;

import org.exoplatform.jpa.mock.AbstractInMemoryDAO;
import org.exoplatform.portal.jdbc.entity.NodeEntity;
import org.exoplatform.portal.mop.jdbc.dao.NodeDAO;

public class InMemoryNodeDAO extends AbstractInMemoryDAO<NodeEntity> implements NodeDAO {

  @Override
  public List<NodeEntity> findAllByPage(Long pageId) {
    return entities.values()
                   .stream()
                   .filter(node -> node.getPage() != null && node.getPage().getId().longValue() == pageId.longValue())
                   .toList();
  }

}
