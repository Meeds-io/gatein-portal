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

import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.URLValidator;

/**
 * Created by The eXo Platform SARL Author : Tran The Trong trongtt@gmail.com August 15, 2007 9:10:53 AM
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIFormWithTitle.gtmpl", events = { @EventConfig(listeners = UIIFrameEditMode.SaveActionListener.class) })
public class UIIFrameEditMode extends UIForm {

    private static final String FIELD_URL = "iframeUrl";

    public UIIFrameEditMode() {
        PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
        PortletPreferences pref = pcontext.getRequest().getPreferences();
        addUIFormInput(new UIFormStringInput(FIELD_URL, FIELD_URL, pref.getValue("url", "http://www.exoplatform.org")));
    }

    public void setValue() {
        UIIFramePortlet uiPortlet = getParent();
        getUIStringInput(FIELD_URL).setValue(uiPortlet.getURL());
    }

    public static class SaveActionListener extends EventListener<UIIFrameEditMode> {
        public void execute(Event<UIIFrameEditMode> event) throws Exception {

            UIIFrameEditMode uiForm = event.getSource();
            String url = uiForm.getUIStringInput(FIELD_URL).getValue();
            UIIFramePortlet uiPortlet = uiForm.getParent();
            if (url == null || url.length() == 0) {
                Object[] args = { uiForm.getLabel(uiForm.getUIStringInput(FIELD_URL).getId()) };
                uiPortlet.addMessage(new ApplicationMessage("EmptyFieldValidator.msg.empty-input", args));
                uiForm.getUIStringInput(FIELD_URL).setValue(uiPortlet.getURL());
                return;
            }
            if (!url.trim().matches(URLValidator.URL_REGEX)) {
                uiForm.getUIStringInput(FIELD_URL).setValue(uiPortlet.getURL());
                Object[] args = { FIELD_URL, "URL" };
                throw new MessageException(new ApplicationMessage("ExpressionValidator.msg.value-invalid", args));
            }
            PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
            PortletPreferences pref = pcontext.getRequest().getPreferences();
            pref.setValue("url", uiForm.getUIStringInput(FIELD_URL).getValue());
            pref.store();

            UIPortalApplication portalApp = Util.getUIPortalApplication();
            if (portalApp.getModeState() == UIPortalApplication.NORMAL_MODE)
                pcontext.setApplicationMode(PortletMode.VIEW);
        }
    }

}
