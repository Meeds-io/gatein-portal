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

package org.exoplatform.portal.webui.component;

import javax.portlet.PortletPreferences;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.CacheUserProfileFilter;
import org.exoplatform.web.security.sso.SSOHelper;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.organization.OrganizationUtils;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform October 2, 2009
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/groovy/portal/webui/component/UILogoPortlet.gtmpl")
public class UILogoPortlet extends UIPortletApplication {
    private final SSOHelper ssoHelper;

    public UILogoPortlet() throws Exception {
        addChild(UILogoEditMode.class, null, null);
        ssoHelper = getApplicationComponent(SSOHelper.class);
    }

    public String getURL() {
        PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
        PortletPreferences pref = pcontext.getRequest().getPreferences();
        return pref.getValue("url", "");
    }

    public String getNavigationTitle() throws Exception {
        UserNode navPath = Util.getUIPortal().getNavPath();
        UserNavigation nav = navPath.getNavigation();
        if (nav.getKey().getType().equals(SiteType.GROUP)) {
            return OrganizationUtils.getGroupLabel(nav.getKey().getName());
        } else if (nav.getKey().getType().equals(SiteType.USER)) {
            ConversationState state = ConversationState.getCurrent();
            User user = (User) state.getAttribute(CacheUserProfileFilter.USER_PROFILE);
            return user.getFullName();
        }
        return "";
    }

    public String renderLoginLink(String signInAction, String signInLocalizedText) {
        // If SSO is enabled, we need to redirect to "/portal/sso" instead of showing login window
        if (ssoHelper.isSSOEnabled()) {
            PortalRequestContext pContext = Util.getPortalRequestContext();
            String ssoRedirectURL = pContext.getRequest().getContextPath() + ssoHelper.getSSORedirectURLSuffix();
            return "<a href=\"" + ssoRedirectURL + "\">" + signInLocalizedText + "</a>";
        } else {
            return "<a href=\"javascript:;\" onclick=\"" + signInAction + "\">" + signInLocalizedText + "</a>";
        }
    }
}
