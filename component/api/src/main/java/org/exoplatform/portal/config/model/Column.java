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
public class Column extends Container {

  private int        colSpan = 1;

  @Override
  public String getCssClass() {
    StringBuilder cssClasses = new StringBuilder();
    if (cssClass != null) {
      cssClasses.append(cssClass);
    }
    cssClasses.append(" flex-cell");
    cssClasses.append(" grid-cell-colspan-md-").append(colSpan);
    cssClasses.append(" grid-cell-colspan-lg-").append(colSpan);
    cssClasses.append(" grid-cell-colspan-xl-").append(colSpan);
    cssClasses.append(" grid-cell-rowspan-md-1");
    cssClasses.append(" grid-cell-rowspan-lg-1");
    cssClasses.append(" grid-cell-rowspan-xl-1");
    return cssClasses.toString();
  }

  @Override
  public String getTemplate() {
    return "CellContainer";
  }

}
