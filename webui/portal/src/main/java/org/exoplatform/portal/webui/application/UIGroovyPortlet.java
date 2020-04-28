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

package org.exoplatform.portal.webui.application;

import javax.portlet.PortletRequest;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPortletApplication;

@ComponentConfig()
public class UIGroovyPortlet extends UIPortletApplication {

    private static final String DEFAULT_TEMPLATE = "system:/groovy/portal/webui/application/UIGroovyPortlet.gtmpl";

    private final String template_;

    private final String windowId;

    private final String id;

    public UIGroovyPortlet() throws Exception {
        PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
        PortletRequest prequest = context.getRequest();
        template_ = prequest.getPreferences().getValue("template", DEFAULT_TEMPLATE);
        windowId = prequest.getWindowID();
        id = windowId + "-portlet";
    }

    public String getId() {
        return id;
    }

    public String getTemplate() {
        return template_;
    }

    public UIComponent getViewModeUIComponent() {
        return null;
    }

}
