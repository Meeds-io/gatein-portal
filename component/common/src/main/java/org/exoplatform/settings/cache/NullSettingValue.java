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
package org.exoplatform.settings.cache;

import org.exoplatform.commons.api.settings.SettingValue;

public class NullSettingValue extends SettingValue<Object> {
  private static final long serialVersionUID = 3106661756567727076L;
  private static final NullSettingValue INSTANCE = new NullSettingValue(null);

  private NullSettingValue(Object value) {
    super(null);
  }

  public static NullSettingValue getInstance() {
    return INSTANCE;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    return true;
  }
}
