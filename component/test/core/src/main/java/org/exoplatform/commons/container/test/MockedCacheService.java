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
package org.exoplatform.commons.container.test;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.impl.CacheServiceImpl;

/**
 * A cache service mock to avoid defining ExoCacheFactory inherited from ISPN In
 * Test context when not needed
 */
public class MockedCacheService extends CacheServiceImpl {

  public MockedCacheService(InitParams params) throws Exception {
    super(params);
  }

}
