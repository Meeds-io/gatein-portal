/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.config.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GridSection extends Container {

  private int colsCount;

  private int rowsCount;

  @Override
  public String getCssClass() {
    StringBuilder cssClasses = new StringBuilder();
    if (cssClass != null) {
      cssClasses.append(cssClass);
    }
    cssClasses.append(" d-flex flex-column d-md-grid");
    cssClasses.append(" grid-cols-md-").append(colsCount);
    cssClasses.append(" grid-cols-lg-").append(colsCount);
    cssClasses.append(" grid-cols-xl-").append(colsCount);
    cssClasses.append(" grid-rows-md-").append(rowsCount);
    cssClasses.append(" grid-rows-lg-").append(rowsCount);
    cssClasses.append(" grid-rows-xl-").append(rowsCount);
    return cssClasses.toString();
  }

  @Override
  public String getTemplate() {
    return "GridContainer";
  }

}
