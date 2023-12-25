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
package io.meeds.spring.integration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.api.settings.SettingService;

import io.meeds.spring.integration.model.TestModel;
import io.meeds.spring.integration.storage.TestStorage;

@Service
public class TestService {

  // Fake injection from Kernel for Testing Purpose
  @Autowired
  private SettingService settingService;

  @Autowired
  private TestStorage    storage;

  public TestModel save(TestModel model) {
    if (model == null) {
      throw new IllegalArgumentException("TestModel is mandatory");
    }
    if (settingService == null) {
      // Fake exception
      throw new IllegalStateException("SettingService is null");
    }
    return storage.save(model);
  }

}
