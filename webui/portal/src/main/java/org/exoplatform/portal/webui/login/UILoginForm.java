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

package org.exoplatform.portal.webui.login;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.login.recovery.PasswordRecoveryHandler;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by The eXo Platform SARL Author : Nhu Dinh Thuan nhudinhthuan@exoplatform.com Jul 11, 2006
 */
@ComponentConfig(lifecycle= Lifecycle.class, template = "system:/groovy/portal/webui/UILoginForm.gtmpl", events = {
        @EventConfig(phase = Phase.DECODE, listeners = UIMaskWorkspace.CloseActionListener.class),
        @EventConfig(phase = Phase.DECODE, listeners = UILoginForm.ForgetPasswordActionListener.class) })
public class UILoginForm extends UIContainer {

    public UILoginForm() throws Exception {
        addChild(UISocialLoginButtons.class, null, null);
    }

    public static class ForgetPasswordActionListener extends EventListener<UILoginForm> {
        public void execute(Event<UILoginForm> event) throws Exception {
            UILogin uiLogin = event.getSource().getParent();
            uiLogin.getChild(UILoginForm.class).setRendered(false);
            uiLogin.getChild(UIForgetPasswordWizard.class).setRendered(true);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiLogin);
        }
    }

    @Override
    public void processDecode(WebuiRequestContext context) throws Exception {
        super.processDecode(context);
        String action = context.getRequestParameter(context.getActionParameterName());
        Event<UIComponent> event = createEvent(action, Event.Phase.DECODE, context);
        if (event != null) {
            event.broadcast();
        }
    }

    public String getForgetPasswordURL() {
        PortalRequestContext pContext = Util.getPortalRequestContext();
        String contextPath = pContext.getRequestContextPath();
        String initURL = pContext.getInitialURI();

        Router router = this.getApplicationComponent(WebAppController.class).getRouter();
        Map<QualifiedName, String> params = new HashMap<QualifiedName, String>();
        params.put(WebAppController.HANDLER_PARAM, PasswordRecoveryHandler.NAME);
        params.put(PasswordRecoveryHandler.INIT_URL, initURL);

        return contextPath + router.render(params);
    }

}
