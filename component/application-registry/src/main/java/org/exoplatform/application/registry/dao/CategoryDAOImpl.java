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
package org.exoplatform.application.registry.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.application.registry.entity.CategoryEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class CategoryDAOImpl extends GenericDAOJPAImpl<CategoryEntity, Long> implements CategoryDAO {

  @Override
  public CategoryEntity findByName(String name) {
    TypedQuery<CategoryEntity> query = getEntityManager().createNamedQuery("CategoryEntity.findByName", CategoryEntity.class);
    query.setParameter("name", name);
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }
}
