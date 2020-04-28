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

package org.exoplatform.portal.account;

/**
 * Created by The eXo Platform SARL
 * Author : dang.tung
 *          tungcnw@gmail.com
 */

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;

@ComponentConfig(template = "system:/groovy/portal/webui/portal/UIAccountSettingForm.gtmpl", events = { @EventConfig(listeners = UIMaskWorkspace.CloseActionListener.class) })
public class UIAccountSetting extends UIContainer {
    public static final String PARAM_ACTIVE_CHILD_ID = "accountSettingActiveTab";

    public static final String[] ACTIONS = { "Close" };

    private String activeChildId = null;

    public String[] getActions() {
        return ACTIONS;
    }

    public UIAccountSetting() throws Exception {
        addChild(UIAccountProfiles.class, null, null).setRendered(false);
        addChild(UIAccountChangePass.class, null, null).setRendered(false);
        addChild(UIAccountSocial.class, null, null).setRendered(false);
        setActiveChildId(getChild(0).getId());
    }

    public String getActiveChildId() {
        return activeChildId;
    }

    public void setActiveChildId(String activeChildId) {
        UIComponent child = this.getChildById(activeChildId);
        if (child != null) {
            if (this.activeChildId != null && !this.activeChildId.isEmpty()) {
                UIComponent ui = getChildById(activeChildId);
                if (ui != null) {
                    ui.setRendered(false);
                }
            }
            child.setRendered(true);
            this.activeChildId = activeChildId;
        }
    }

    @Override
    public void processRender(WebuiRequestContext context) throws Exception {
        PortalRequestContext prContext = Util.getPortalRequestContext();
        String childId = prContext.getRequestParameter(PARAM_ACTIVE_CHILD_ID);
        if (childId != null && !childId.isEmpty()) {
            setActiveChildId(childId);
        }
        super.processRender(context);
    }
}
