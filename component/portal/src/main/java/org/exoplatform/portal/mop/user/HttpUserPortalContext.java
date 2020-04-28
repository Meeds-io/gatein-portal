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
package org.exoplatform.portal.mop.user;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.ResourceBundleManager;

public class HttpUserPortalContext implements UserPortalContext {

  private HttpServletRequest httpRequest;

  public HttpUserPortalContext(HttpServletRequest servletRequest) {
    this.httpRequest = servletRequest;
  }

  @Override
  public ResourceBundle getBundle(UserNavigation navigation) {
    ResourceBundleManager rbMgr = ExoContainerContext.getService(ResourceBundleManager.class);
    return rbMgr.getNavigationResourceBundle(getLocaleAsString(),
                                             getSiteType(navigation),
                                             getSiteName(navigation));
  }

  private String getSiteName(UserNavigation navigation) {
    return navigation
                     .getKey()
                     .getName();
  }

  private String getSiteType(UserNavigation navigation) {
    return navigation.getKey()
                     .getTypeName();
  }

  private String getLocaleAsString() {
    return LocaleContextInfo.getLocaleAsString(getUserLocale());
  }

  @Override
  public Locale getUserLocale() {
    return LocaleContextInfoUtils.computeLocale(httpRequest);
  }
}
