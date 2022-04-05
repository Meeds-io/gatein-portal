/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
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
package org.exoplatform.portal.config;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

public class NavigationCategoryPlugin extends BaseComponentPlugin {

  public static final String URI_PARAM_NAME            = "uri";

  public static final String URI_ORDER_PARAM_NAME      = "uri.order";

  public static final String CATEGORY_PARAM_NAME       = "category";

  public static final String CATEGORY_ORDER_PARAM_NAME = "category.order";

  private String             category;

  private String             uri;

  private int                categoryOrder;

  private int                uriOrder;

  public NavigationCategoryPlugin(InitParams params) {
    this.category = params.getValueParam(CATEGORY_PARAM_NAME).getValue();
    this.uri = params.getValueParam(URI_PARAM_NAME).getValue();

    if (params.containsKey(CATEGORY_ORDER_PARAM_NAME)) {
      this.categoryOrder = Integer.parseInt(params.getValueParam(CATEGORY_ORDER_PARAM_NAME).getValue());
    }
    if (params.containsKey(URI_ORDER_PARAM_NAME)) {
      this.uriOrder = Integer.parseInt(params.getValueParam(URI_ORDER_PARAM_NAME).getValue());
    }
  }

  public int getCategoryOrder() {
    return categoryOrder;
  }

  public int getUriOrder() {
    return uriOrder;
  }

  public String getCategory() {
    return category;
  }

  public String getUri() {
    return uri;
  }

}
