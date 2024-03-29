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

import java.util.Map;

/**
 * This class is used to add custom filters on an UI Extension in order to force the
 * UIExtensionManager to hide the extension if the filter 
 * 
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * May 04, 2009  
 */
public interface UIExtensionFilter {

  /**
   * Indicates whether the given context is accepted by this filter
   * @param context the context to check
   * @return <code>true</code> if the context is accepted <code>false</code> otherwise
   * @throws Exception if an error occurs
   */
  public boolean accept(Map<String, Object> context) throws Exception;
  
  /**
   * Allows to execute some code when the filter rejects the given context
   * @param context the context
   * @throws Exception if an error occurs
   */
  public void onDeny(Map<String, Object> context) throws Exception;
  
  /**
   * Indicates the type of the current filter
   * @return the type of the filter
   */
  public UIExtensionFilterType getType();
}
