/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.spring.integration.storage;

import static io.meeds.spring.integration.mapper.EntityMapper.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.meeds.spring.integration.model.TestModel;
import io.meeds.spring.integration.test.dao.TestDao;
import io.meeds.spring.integration.test.entity.TestEntity;

@Component
public class TestStorage {

  @Autowired
  private TestDao dao;

  public TestModel save(TestModel model) {
    TestEntity entity = toEntity(model);
    entity = dao.save(entity);
    return fromEntity(entity);
  }

}
