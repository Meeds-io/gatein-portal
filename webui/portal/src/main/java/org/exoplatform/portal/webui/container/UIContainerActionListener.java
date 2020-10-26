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

package org.exoplatform.portal.webui.container;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minhdv81@yahoo.com Jun 13, 2006
 */
public class UIContainerActionListener {

    public static class EditContainerActionListener extends EventListener<UIContainer> {
        public void execute(Event<UIContainer> event) throws Exception {

            UIContainer uiContainer = event.getSource();
            UIPortalApplication uiApp = Util.getUIPortalApplication();
            UIMaskWorkspace uiMaskWS = uiApp.getChildById(UIPortalApplication.UI_MASK_WS_ID);
            UIContainerForm containerForm = uiMaskWS.createUIComponent(UIContainerForm.class, null, null);
            containerForm.setValues(uiContainer);
            uiMaskWS.setUIComponent(containerForm);
            uiMaskWS.setShow(true);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiMaskWS);
        }
    }
}
