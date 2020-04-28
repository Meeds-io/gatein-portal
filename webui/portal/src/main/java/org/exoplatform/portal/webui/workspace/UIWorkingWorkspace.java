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

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.page.UISiteBody;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minhdv81@yahoo.com Jun 12, 2006
 */

@ComponentConfig(id = "UIWorkingWorkspace", template = "system:/groovy/portal/webui/workspace/UIWorkingWorkspace.gtmpl", events = {
        @EventConfig(listeners = UIMainActionListener.CreatePortalActionListener.class, csrfCheck = false),
        @EventConfig(listeners = UIMainActionListener.PageCreationWizardActionListener.class, csrfCheck = false),
        @EventConfig(listeners = UIMainActionListener.EditBackgroundActionListener.class),
        @EventConfig(listeners = UIMainActionListener.EditInlineActionListener.class, csrfCheck = false),
        @EventConfig(listeners = UIMainActionListener.EditPageInFullPreviewActionListener.class)})
public class UIWorkingWorkspace extends UIContainer {

    private UIPortal backupUIPortal = null;

    public UIPortal getBackupUIPortal() {
        return backupUIPortal;
    }

    public void setBackupUIPortal(UIPortal uiPortal) {
        backupUIPortal = uiPortal;
    }

    public UIPortal restoreUIPortal() {
        UIPortal result = backupUIPortal;
        if (result == null) {
            throw new IllegalStateException("backupUIPortal not available");
        } else {
            UISiteBody siteBody = findFirstComponentOfType(UISiteBody.class);
            siteBody.setUIComponent(result);
            return result;
        }
    }

    public void updatePortletByWindowId(String windowId) {
        List<UIPortlet> portletInstancesInPage = new ArrayList<UIPortlet>();
        findComponentOfType(portletInstancesInPage, UIPortlet.class);

        for (UIPortlet portlet : portletInstancesInPage) {
            if (portlet.getWindowId().equals(windowId)) {
                Util.getPortalRequestContext().addUIComponentToUpdateByAjax(portlet);
            }
        }
    }

    public void updatePortletsByName(String portletName) {
        List<UIPortlet> portletInstancesInPage = new ArrayList<UIPortlet>();
        findComponentOfType(portletInstancesInPage, UIPortlet.class);

        for (UIPortlet portlet : portletInstancesInPage) {
            String applicationId = portlet.getApplicationId();
            ApplicationType<?> type = portlet.getState().getApplicationType();
            if (type == ApplicationType.PORTLET) {
                String[] chunks = Utils.split("/", applicationId);
                if (chunks[1].equals(portletName)) {
                    Util.getPortalRequestContext().addUIComponentToUpdateByAjax(portlet);
                }
            } else {
                throw new AssertionError("Need to handle wsrp case later");
            }
        }
    }
}
