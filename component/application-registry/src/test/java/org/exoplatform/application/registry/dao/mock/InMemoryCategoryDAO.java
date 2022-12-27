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
package org.exoplatform.application.registry.dao.mock;

import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.application.registry.dao.CategoryDAO;
import org.exoplatform.application.registry.entity.ApplicationEntity;
import org.exoplatform.application.registry.entity.CategoryEntity;
import org.exoplatform.jpa.mock.AbstractInMemoryDAO;

public class InMemoryCategoryDAO extends AbstractInMemoryDAO<CategoryEntity> implements CategoryDAO {

  private InMemoryApplicationDAO applicationDAO;

  public InMemoryCategoryDAO(InMemoryApplicationDAO applicationDAO) {
    this.applicationDAO = applicationDAO;
  }

  @Override
  public List<CategoryEntity> findAll() {
    List<CategoryEntity> categories = super.findAll();
    return categories.stream().map(entity -> this.findByName(entity.getName())).toList();
  }

  @Override
  public CategoryEntity findByName(String name) {
    CategoryEntity categoryEntity = entities.values()
                                            .stream()
                                            .filter(entity -> StringUtils.equals(name, entity.getName()))
                                            .findFirst()
                                            .orElse(null);
    if (categoryEntity != null) {
      List<ApplicationEntity> applications = getCategoryApplications(name);
      categoryEntity.setApplications(new HashSet<>(applications));
    }
    return categoryEntity;
  }

  @Override
  public void delete(CategoryEntity entity) {
    super.delete(entity);
    List<ApplicationEntity> applications = getCategoryApplications(entity.getName());
    applications.forEach(applicationDAO::delete);
  }

  private List<ApplicationEntity> getCategoryApplications(String name) {
    return this.applicationDAO.findAll()
                              .stream()
                              .filter(appEntity -> appEntity.getCategory() != null
                                  && StringUtils.equals(appEntity.getCategory().getName(),
                                                        name))
                              .toList();
  }
}
