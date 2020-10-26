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
package org.exoplatform.portal.config;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

/**
 * Listener to update the default portal config in UserPortalConfigService service when the portal config is updated
 */
public class DefaultPortalConfigUpdateListener extends Listener {

  @Override
  public void onEvent(Event event) throws Exception {
    if(event.getData() instanceof PortalConfig) {
      PortalConfig portalConfig = (PortalConfig) event.getData();
      // update only when the updated site is the default site
      UserPortalConfigService userPortalConfigService = ExoContainerContext.getCurrentContainer()
              .getComponentInstanceOfType(UserPortalConfigService.class);
      if(StringUtils.equals(userPortalConfigService.getDefaultPortal(), portalConfig.getName())) {
        userPortalConfigService.setDefaultPortalConfig(portalConfig);
      }
    }
  }
  
}
