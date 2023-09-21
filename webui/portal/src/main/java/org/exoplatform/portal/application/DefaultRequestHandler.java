/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.application;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.*;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteFilter;
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
          SiteFilter siteFilter = new SiteFilter();
          siteFilter.setExcludedSiteName(configService.getGlobalPortal());
          siteFilter.setSiteType(SiteType.PORTAL);
          siteFilter.setSortByDisplayOrder(true);
          siteFilter.setFilterByDisplayed(true);
          siteFilter.setDisplayed(true);
          siteFilter.setLimit(1);
          siteFilter.setOffset(0);
          List<PortalConfig> portalConfigList = configService.getSites(siteFilter);
          if (portalConfigList != null && !portalConfigList.isEmpty()) {
            defaultPortal = portalConfigList.get(0).getName();
          } else {
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
