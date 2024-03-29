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
package org.exoplatform.webui.ext.filter.impl;

import java.util.List;
import java.util.Map;

import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Aug 18, 2009  
 * 7:08:32 AM
 */
public class FileFilter implements UIExtensionFilter {

  protected List<String> mimeTypes;
  
  public boolean accept(Map<String, Object> context) throws Exception {
    if (mimeTypes == null || mimeTypes.isEmpty()) {
      return true;
    }
    if(context != null && context.containsKey("mimeType")) {
      Object mimeType = context.get("mimeType");
      return mimeType != null && mimeTypes.contains(mimeType.toString());
    }
    return false;
  }

  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }

  public void onDeny(Map<String, Object> context) throws Exception {
    
  }

}
