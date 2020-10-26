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

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 * Created by The eXo Platform SARL Author : Tran The Trong trongtt@gmail.com August 14, 2007
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/groovy/portal/webui/component/UIIFramePortlet.gtmpl"
// events = {
// @EventConfig(listeners = UIIFramePortlet.MyEventPubActionListener.class, phase = Phase.PROCESS)
// }
)
public class UIIFramePortlet extends UIPortletApplication {
    public UIIFramePortlet() throws Exception {
        addChild(UIIFrameEditMode.class, null, null);
    }

    public String getURL() {
        PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
        PortletPreferences pref = pcontext.getRequest().getPreferences();
        return pref.getValue("url", "http://www.exoplatform.org");
    }

    // static public class MyEventPubActionListener extends EventListener<UIIFramePortlet> {
    // public void execute(Event<UIIFramePortlet> event) throws Exception {
    // PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance() ;
    // System.out.println("in MyEventActionListener");
    // System.out.println(pcontext.getAttribute("portletEventValue"));
    // }
    // }

}
