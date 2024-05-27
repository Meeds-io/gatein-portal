/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.portal.webui.container;

import java.util.List;

import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.NameValidator;
import org.exoplatform.webui.form.validator.StringLengthValidator;
import org.exoplatform.webui.organization.UIListPermissionSelector;
import org.exoplatform.webui.organization.UIListPermissionSelector.EmptyIteratorValidator;

@ComponentConfigs({
    @ComponentConfig(id = "UIContainerForm", lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIFormTabPane.gtmpl", events = {
            @EventConfig(listeners = UIContainerForm.SaveActionListener.class),
            @EventConfig(listeners = UIMaskWorkspace.CloseActionListener.class, phase = Phase.DECODE) }),
    @ComponentConfig(id = "UIContainerPermission", type = UIFormInputSet.class, template = "system:/groovy/webui/core/UITabSelector.gtmpl", events = { @EventConfig(listeners = UIFormInputSet.SelectComponentActionListener.class) }),
    @ComponentConfig(id = "UIAddonContainerPermission", type = UIFormInputSet.class, lifecycle = UIContainerLifecycle.class) })
public class UIAddOnContainerForm extends UIContainerForm {

    public UIAddOnContainerForm() throws Exception {
        super();
        UIFormInputSet infoInputSet = this.findComponentById("ContainerSetting");
        UIFormInputBase<String> input = new UIFormStringInput("name", "name", null);
        input.addValidator(MandatoryValidator.class);
        input.addValidator(StringLengthValidator.class, 3, 30);
        input.addValidator(NameValidator.class);
        input.setParent(infoInputSet);

        List<UIComponent> children = infoInputSet.getChildren();
        children.add(1, input);
        infoInputSet.setChildren(children);
        this.removeChildById("UIContainerPermission");

        UIFormInputSet uiPermissionSetting = createUIComponent(UIFormInputSet.class, "UIAddonContainerPermission", null);
        UIListPermissionSelector uiAccessPermissionSelector = createUIComponent(UIListPermissionSelector.class, null, null);
        uiAccessPermissionSelector.configure(WebuiRequestContext.generateUUID("UIListPermissionSelector"), "accessPermissions");
        uiAccessPermissionSelector.addValidator(EmptyIteratorValidator.class);
        uiPermissionSetting.addChild(uiAccessPermissionSelector);
        addUIFormInput(uiPermissionSetting);
    }

}