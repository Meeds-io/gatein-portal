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

package org.exoplatform.account.webui.component;

import org.exoplatform.portal.webui.register.UIRegisterEditMode;
import org.exoplatform.portal.webui.register.UIRegisterForm;
import org.exoplatform.portal.webui.register.UISocialRegisterButtons;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 *
 * @author <a href="mailto:hoang281283@gmail.com">Minh Hoang TO</a>
 * @version $Id$
 *
 */

@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/groovy/organization/webui/component/UIRegisterPortlet.gtmpl")
public class UIRegisterPortlet extends UIPortletApplication {

    public UIRegisterPortlet() throws Exception {
        addChild(UIRegisterForm.class, null, null);
        addChild(UISocialRegisterButtons.class, null, null);
        addChild(UIRegisterEditMode.class, null, null).setRendered(false);
    }
}
