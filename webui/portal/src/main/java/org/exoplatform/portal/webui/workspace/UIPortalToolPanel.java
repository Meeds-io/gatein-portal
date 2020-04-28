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

package org.exoplatform.portal.webui.workspace;

import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minhdv81@yahoo.com Jun 12, 2006
 */
@ComponentConfig(template = "system:/groovy/portal/webui/workspace/UIPortalToolPanel.gtmpl")
public class UIPortalToolPanel extends UIComponentDecorator {
    private boolean showMaskLayer = false;

    public UIPortalToolPanel() {
    }

    public <T extends UIComponent> void setWorkingComponent(Class<T> clazz, String id) throws Exception {
        UIComponent component = createUIComponent(clazz, null, id);
        setUIComponent(component);
    }

    public void setWorkingComponent(UIComponent component) {
        setUIComponent(component);
    }

    public void processRender(WebuiRequestContext context) throws Exception {
        JavascriptManager jsmanager = context.getJavascriptManager();

        super.processRender(context);
        if (showMaskLayer) {
            jsmanager.require("SHARED/UIMaskLayer", "mask").addScripts("mask.createMask('UIPortalToolPanel', null, 10) ;");
        }
    }

    public boolean isShowMaskLayer() {
        return showMaskLayer;
    }

    public void setShowMaskLayer(boolean showMaskLayer) {
        this.showMaskLayer = showMaskLayer;
    }
}
