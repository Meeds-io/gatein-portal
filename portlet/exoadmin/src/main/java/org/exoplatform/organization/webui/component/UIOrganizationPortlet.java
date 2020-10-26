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

package org.exoplatform.organization.webui.component;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.portal.webui.portal.UIPortalComponentActionListener.ViewChildActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL Author : chungnv nguyenchung136@yahoo.com Jun 23, 2006 10:07:15 AM
 */

@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/groovy/organization/webui/component/UIOrganizationPortlet.gtmpl", events = {
        @EventConfig(listeners = UIOrganizationPortlet.NewAccountAddedActionListener.class),
        @EventConfig(listeners = ViewChildActionListener.class) }

)
@Serialized
public class UIOrganizationPortlet extends UIPortletApplication {

    public UIOrganizationPortlet() throws Exception {
        // setMinWidth(730) ;
        // addChild(UIViewMode.class, null, UIPortletApplication.VIEW_MODE);
        addChild(UIUserManagement.class, null, null);
        addChild(UIGroupManagement.class, null, null).setRendered(false);
        addChild(UIMembershipManagement.class, null, null).setRendered(false);
    }

    // @ComponentConfig(
    // template = "app:/groovy/organization/webui/component/UIViewMode.gtmpl",
    // events = {
    // @EventConfig (listeners = ViewChildActionListener.class)
    // }
    // )
    // static public class UIViewMode extends UIContainer {
    // public UIViewMode() throws Exception {
    // }
    // }
    //
    public static class NewAccountAddedActionListener extends EventListener<UIOrganizationPortlet> {
        public void execute(Event<UIOrganizationPortlet> event) throws Exception {
            // Actually we don't need to do anything here
            // UIListUsers will have the lasteast data in it's processRender method
            // UIListUsers uiListUsers = event.getSource().findFirstComponentOfType(UIListUsers.class);
            // uiListUsers.search(new Query());
        }
    }
}
