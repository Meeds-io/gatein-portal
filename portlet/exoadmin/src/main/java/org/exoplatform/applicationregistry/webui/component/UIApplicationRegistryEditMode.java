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
package org.exoplatform.applicationregistry.webui.component;

import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;

/**
 * @author <a href="mailto:truong.le@exoplatform.com">Truong Le</a>
 * @version $Id$
 *
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIFormWithTitle.gtmpl", events = { @EventConfig(listeners = UIApplicationRegistryEditMode.SaveActionListener.class) })
@Serialized
public class UIApplicationRegistryEditMode extends UIForm {
    public static final String SHOW_IMPORT = "showImport";

    public UIApplicationRegistryEditMode() {
        PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
        PortletPreferences pref = pcontext.getRequest().getPreferences();
        boolean isShowImport = Boolean.parseBoolean(pref.getValue(SHOW_IMPORT, "true"));
        addUIFormInput(new UIFormCheckBoxInput<Boolean>(SHOW_IMPORT, SHOW_IMPORT, isShowImport).setValue(isShowImport));
    }

    public static class SaveActionListener extends EventListener<UIApplicationRegistryEditMode> {

        @Override
        public void execute(Event<UIApplicationRegistryEditMode> event) throws Exception {
            // TODO Auto-generated method stub
            UIApplicationRegistryEditMode uiForm = event.getSource();
            boolean isShowImport = uiForm.getUIFormCheckBoxInput(SHOW_IMPORT).isChecked();
            PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
            PortletPreferences pref = pcontext.getRequest().getPreferences();
            pref.setValue(SHOW_IMPORT, Boolean.toString(isShowImport));
            pref.store();
            UIPortalApplication portalApp = Util.getUIPortalApplication();
            if (portalApp.getModeState() == UIPortalApplication.NORMAL_MODE)
                pcontext.setApplicationMode(PortletMode.VIEW);

        }

    }
}
