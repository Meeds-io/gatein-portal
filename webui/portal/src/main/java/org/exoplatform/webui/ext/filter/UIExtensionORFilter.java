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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.webui.ext.filter;

import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 7 mai 2009  
 */
public abstract class UIExtensionORFilter extends UIExtensionAbstractFilter {

  /**
   * The sub filters to combine with the operator OR
   */
  private final List<UIExtensionFilter> filters;

  public UIExtensionORFilter(List<UIExtensionFilter> filters) {
    this.filters = filters; 
  }
  
  public UIExtensionORFilter(List<UIExtensionFilter> filters, String messageKey) {
    super(messageKey);
    this.filters = filters; 
  }
  
  public UIExtensionORFilter(List<UIExtensionFilter> filters, String messageKey, UIExtensionFilterType type) {
    super(messageKey, type);
    this.filters = filters; 
  }
  
  /**
   * {@inheritDoc}
   */
  public boolean accept(Map<String, Object> context) throws Exception {
    int length; 
    if (filters == null || (length = filters.size()) == 0) {
      return true;
    }
    for (int i = 0; i < length; i++) {
      UIExtensionFilter filter = filters.get(i);
      if (filter.accept(context)) {
        return true;
      }
    }
    return false;
  }
}
