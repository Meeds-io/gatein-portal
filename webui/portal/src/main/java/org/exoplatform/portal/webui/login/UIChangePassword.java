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

import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.StringLengthValidator;

/**
 * Created by The eXo Platform SARL Author : dang.tung tungcnw@gmail.com Jul 09, 2008
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIFormWithTitle.gtmpl", events = {
        @EventConfig(listeners = UIChangePassword.SaveActionListener.class),
        @EventConfig(phase = Phase.DECODE, listeners = UIMaskWorkspace.CloseActionListener.class) })
public class UIChangePassword extends UIForm {
    static final String USER_NAME = "username";

    static final String PASSWORD = "password";

    static final String NEW_PASSWORD = "newpassword";

    static final String CONFIRM_NEW_PASSWORD = "confirmnewpassword";

    static User user_;

    public UIChangePassword() throws Exception {
        addUIFormInput(new UIFormStringInput(USER_NAME, USER_NAME, null).setReadOnly(true));
        addUIFormInput(new UIFormStringInput(PASSWORD, PASSWORD, null).setType(UIFormStringInput.PASSWORD_TYPE).addValidator(
                MandatoryValidator.class));
        addUIFormInput(((UIFormStringInput) new UIFormStringInput(NEW_PASSWORD, NEW_PASSWORD, null))
                .setType(UIFormStringInput.PASSWORD_TYPE).addValidator(MandatoryValidator.class)
                .addValidator(StringLengthValidator.class, 6, 30));
        addUIFormInput(((UIFormStringInput) new UIFormStringInput(CONFIRM_NEW_PASSWORD, CONFIRM_NEW_PASSWORD, null))
                .setType(UIFormStringInput.PASSWORD_TYPE).addValidator(MandatoryValidator.class)
                .addValidator(StringLengthValidator.class, 6, 30));
    }

    public void setData(User user) {
        user_ = user;
        getUIStringInput(USER_NAME).setValue(user.getUserName());
    }

    @Override
    public void reset() {
        UIFormStringInput passwordForm = getUIStringInput(PASSWORD);
        passwordForm.reset();
        UIFormStringInput newPasswordForm = getUIStringInput(NEW_PASSWORD);
        newPasswordForm.reset();
        UIFormStringInput confirmPasswordForm = getUIStringInput(CONFIRM_NEW_PASSWORD);
        confirmPasswordForm.reset();
    }

    public static class SaveActionListener extends EventListener<UIChangePassword> {
        public void execute(Event<UIChangePassword> event) throws Exception {
            UIChangePassword uiForm = event.getSource();
            String password = uiForm.getUIStringInput(PASSWORD).getValue();
            String newpassword = uiForm.getUIStringInput(NEW_PASSWORD).getValue();
            String confirmnewpassword = uiForm.getUIStringInput(CONFIRM_NEW_PASSWORD).getValue();
            WebuiRequestContext request = event.getRequestContext();
            UIApplication uiApp = request.getUIApplication();
            OrganizationService orgService = uiForm.getApplicationComponent(OrganizationService.class);
            uiForm.reset();
            boolean isNew = true;
            if (!orgService.getUserHandler().authenticate(user_.getUserName(), password)) {
                uiApp.addMessage(new ApplicationMessage("UIResetPassword.msg.Invalid-account", null));
                isNew = false;
            }
            if (!newpassword.equals(confirmnewpassword)) {
                uiApp.addMessage(new ApplicationMessage("UIResetPassword.msg.password-is-not-match", null));
                isNew = false;
            }

            UIMaskWorkspace uiMaskWorkspace = uiApp.getChildById(UIPortalApplication.UI_MASK_WS_ID);
            if (isNew) {
                user_.setPassword(newpassword);
                orgService.getUserHandler().saveUser(user_, true);
                uiMaskWorkspace.createEvent("Close", Phase.DECODE, request).broadcast();
                uiApp.addMessage(new ApplicationMessage("UIResetPassword.msg.change-password-successfully", null));
            }
            request.addUIComponentToUpdateByAjax(uiMaskWorkspace);
        }
    }
}
