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

import java.util.LinkedList;
import java.util.List;

import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;

import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 * Author : Nhu Dinh Thuan nhudinhthuan@yahoo.com May 30, 2006
 *
 * @version:: $Id$
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class)
public class UIBreadcumbsPortlet extends UIPortletApplication {

    private final String template;

    public UIBreadcumbsPortlet() throws Exception {
        PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
        PortletRequest prequest = context.getRequest();
        PortletPreferences prefers = prequest.getPreferences();
        template = prefers.getValue("template", "app:/groovy/portal/webui/component/UIBreadcumbsPortlet.gtmpl");
    }

    public String getTemplate() {
        return template != null ? template : super.getTemplate();
    }

    public boolean isUseAjax() throws Exception {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        PortletRequest prequest = context.getRequest();
        PortletPreferences prefers = prequest.getPreferences();
        return Boolean.valueOf(prefers.getValue("useAJAX", "true"));
    }

    public List<UserNode> getSelectedPath() throws Exception {
        UserNode node = Util.getUIPortal().getSelectedUserNode();
        UserPortal userPortal = Util.getPortalRequestContext().getUserPortalConfig().getUserPortal();
        UserNavigation nav = userPortal.getNavigation(node.getNavigation().getKey());

        UserNode targetNode = userPortal.resolvePath(nav, null, node.getURI());
        LinkedList<UserNode> paths = new LinkedList<UserNode>();

        do {
            paths.addFirst(targetNode);
            targetNode = targetNode.getParent();
        } while (targetNode != null && targetNode.getParent() != null);

        return paths;
    }

}
