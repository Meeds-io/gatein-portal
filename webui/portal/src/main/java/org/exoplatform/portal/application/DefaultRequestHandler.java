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

package org.exoplatform.portal.application;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.*;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.url.PortalURLContext;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.url.URLFactoryService;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class DefaultRequestHandler extends WebRequestHandler {

    /** . */
    private final UserPortalConfigService configService;

    /** . */
    private final URLFactoryService urlFactory;

    public DefaultRequestHandler(UserPortalConfigService configService, URLFactoryService urlFactory) {
        this.configService = configService;
        this.urlFactory = urlFactory;
    }

    @Override
    public String getHandlerName() {
        return "default";
    }

    @Override
    public boolean execute(ControllerContext context) throws Exception {
        String defaultUri = null;

        String currentUser = context.getRequest().getRemoteUser();
        if (StringUtils.isNotBlank(currentUser)) {
          defaultUri = configService.getUserHomePage(currentUser);
        }

        if (StringUtils.isBlank(defaultUri)) {
          String defaultPortal = configService.getDefaultPortal();
          List<String> allPortalNames = configService.getAllPortalNames();
          boolean emptyPortalList = allPortalNames == null || allPortalNames.isEmpty();
          boolean canAccessDefaultPortal = allPortalNames != null && allPortalNames.contains(defaultPortal);
          if (!emptyPortalList && !canAccessDefaultPortal) {
            defaultPortal = allPortalNames.get(0);
          } else if (emptyPortalList) {
            HttpServletResponse resp = context.getResponse();
            String currentPortalContainerName = PortalContainer.getCurrentPortalContainerName();
            resp.sendRedirect("/" + currentPortalContainerName + "/login");
            return true;
          }

          PortalURLContext urlContext = new PortalURLContext(context, SiteKey.portal(defaultPortal));
          NodeURL url = urlFactory.newURL(NodeURL.TYPE, urlContext);
          defaultUri = url.setResource(new NavigationResource(SiteType.PORTAL, defaultPortal, "")).toString();
        }

        HttpServletResponse resp = context.getResponse();
        resp.sendRedirect(resp.encodeRedirectURL(defaultUri));
        return true;
    }

    @Override
    protected boolean getRequiresLifeCycle() {
        return true;
    }
}
