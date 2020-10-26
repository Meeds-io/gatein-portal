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

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.validator.ExpressionValidator;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.NameValidator;
import org.exoplatform.webui.form.validator.NotHTMLTagValidator;
import org.exoplatform.webui.form.validator.StringLengthValidator;
import org.exoplatform.webui.organization.UIListPermissionSelector;
import org.exoplatform.webui.organization.UIListPermissionSelector.EmptyIteratorValidator;
import org.exoplatform.webui.organization.UIPermissionSelector;

/**
 * Author : Dang Van Minh minhdv81@yahoo.com Jun 8, 2006
 */
@ComponentConfigs({
        @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIFormTabPane.gtmpl", events = {
                @EventConfig(listeners = UIContainerForm.SaveActionListener.class),
                @EventConfig(listeners = UIMaskWorkspace.CloseActionListener.class, phase = Phase.DECODE) }),
        @ComponentConfig(id = "UIContainerPermission", type = UIFormInputSet.class, template = "system:/groovy/webui/core/UITabSelector.gtmpl", events = { @EventConfig(listeners = UIFormInputSet.SelectComponentActionListener.class) }) })
// initParams = {
// @ParamConfig(
// name = "ContainerTemplateOption",
// value = "app:/WEB-INF/conf/uiconf/portal/webui/component/customization/ContainerTemplateOption.groovy"
// )
// },
public class UIContainerForm extends UIFormTabPane {

    private UIContainer uiContainer_;

    private UIComponent backComponent_;

    public UIContainerForm() throws Exception { // InitParams initParams
        super("UIContainerForm");
        UIFormInputSet infoInputSet = new UIFormInputSet("ContainerSetting");
        infoInputSet
                .addUIFormInput(
                        new UIFormStringInput("id", "id", null).addValidator(MandatoryValidator.class)
                                .addValidator(StringLengthValidator.class, 3, 30).addValidator(NameValidator.class))
                .addUIFormInput(
                        new UIFormStringInput("title", "title", null).addValidator(StringLengthValidator.class, 50)
                                .addValidator(NotHTMLTagValidator.class, "UIContainerForm.msg.InvalidContainerTitle"))
                .addUIFormInput(
                        new UIFormStringInput("width", "width", null).addValidator(ExpressionValidator.class,
                                "(^([1-9]\\d*)(px|%)$)?", "UIContainerForm.msg.InvalidWidthHeight"))
                .addUIFormInput(
                        new UIFormStringInput("height", "height", null).addValidator(ExpressionValidator.class,
                                "(^([1-9]\\d*)(px|%)$)?", "UIContainerForm.msg.InvalidWidthHeight"));
        addChild(infoInputSet);
        setSelectedTab(infoInputSet.getId());

        PortalRequestContext prc = Util.getPortalRequestContext();
        if (prc.getSiteType() != SiteType.USER) {
            UIFormInputSet uiPermissionSetting = createUIComponent(UIFormInputSet.class, "UIContainerPermission", null);

            UIListPermissionSelector uiAccessPermissionSelector = createUIComponent(UIListPermissionSelector.class, null, null);
            uiAccessPermissionSelector.configure(WebuiRequestContext.generateUUID("UIListPermissionSelector"), "accessPermissions");
            uiAccessPermissionSelector.addValidator(EmptyIteratorValidator.class);
            uiPermissionSetting.addChild(uiAccessPermissionSelector);
            uiPermissionSetting.setSelectedComponent(uiAccessPermissionSelector.getId());

            ExoContainer container = ExoContainerContext.getCurrentContainer();
            UserACL acl = container.getComponentInstanceOfType(UserACL.class);
            if(acl.isSuperUser() || acl.isUserInGroup(acl.getAdminGroups())) {
                //Move permission
                UIListPermissionSelector uiMoveAppsPermissionSelector = createUIComponent(UIListPermissionSelector.class, null, "MoveAppsPermissions");
                uiMoveAppsPermissionSelector.configure(WebuiRequestContext.generateUUID("UIListMoveAppsPermissionSelector"), "moveAppsPermissions");
                uiPermissionSetting.addChild(uiMoveAppsPermissionSelector);

                //MoveContainers permission
                UIListPermissionSelector uiMoveContainersPermissionSelector = createUIComponent(UIListPermissionSelector.class, null, "MoveContainersPermissions");
                uiMoveContainersPermissionSelector.configure(WebuiRequestContext.generateUUID("UIListMoveContainersPermissionSelector"), "moveContainersPermissions");
                uiPermissionSetting.addChild(uiMoveContainersPermissionSelector);
            }

            addUIFormInput(uiPermissionSetting);
        }
    }

    public void setValues(final UIContainer uiContainer) throws Exception {
        uiContainer_ = uiContainer;
        Container container = (Container) PortalDataMapper.buildModelObject(uiContainer);
        getUIStringInput("id").setReadOnly(true);
        invokeGetBindingBean(container);
    }

    public UIContainer getContainer() {
        return uiContainer_;
    }

    public UIComponent getBackComponent() {
        return backComponent_;
    }

    public void setBackComponent(final UIComponent uiComp) {
        backComponent_ = uiComp;
    }

    public static class SaveActionListener extends EventListener<UIContainerForm> {
        public void execute(final Event<UIContainerForm> event) throws Exception {
            UIContainerForm uiForm = event.getSource();
            UIContainer uiContainer = uiForm.getContainer();
            uiForm.invokeSetBindingBean(uiContainer);
            PortalRequestContext pcontext = (PortalRequestContext) event.getRequestContext();

            UIMaskWorkspace uiMaskWorkspace = uiForm.getParent();
            uiMaskWorkspace.createEvent("Close", Phase.DECODE, pcontext).broadcast();

            UIPortalApplication uiPortalApp = Util.getUIPortalApplication();
            UIWorkingWorkspace uiWorkingWS = uiPortalApp.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
            pcontext.getJavascriptManager().require("SHARED/portalComposer", "portalComposer")
                    .addScripts("portalComposer.toggleSaveButton();");
            pcontext.addUIComponentToUpdateByAjax(uiWorkingWS);
            pcontext.ignoreAJAXUpdateOnPortlets(true);
        }
    }
}
