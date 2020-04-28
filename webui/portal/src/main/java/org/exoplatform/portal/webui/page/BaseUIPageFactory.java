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

package org.exoplatform.portal.webui.page;

import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * This factory is used to create the base UIPage component
 *
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public class BaseUIPageFactory extends UIPageFactory {
    @Override
    public String getType() {
        return DEFAULT_FACTORY_ID;
    }

    @Override
    public UIPage createUIPage(WebuiRequestContext context) throws Exception {
        if (context == null) {
            context = WebuiRequestContext.getCurrentInstance();
        }
        WebuiApplication app = (WebuiApplication) context.getApplication();
        UIPage uiPage = app.createUIComponent(UIPage.class, null, null, context);
        return uiPage;
    }
}
