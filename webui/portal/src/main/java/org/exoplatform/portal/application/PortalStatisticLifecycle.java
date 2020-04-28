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

package org.exoplatform.portal.application;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.application.RequestFailure;
import org.exoplatform.webui.application.WebuiRequestContext;

public class PortalStatisticLifecycle implements ApplicationLifecycle<WebuiRequestContext> {

    private static final String ATTRIBUTE_NAME = "PortalStatistic";

    @SuppressWarnings("unused")
    public void onInit(Application app) {
    }

    @SuppressWarnings("unused")
    public void onStartRequest(Application app, WebuiRequestContext rcontext) throws Exception {
        app.setAttribute(ATTRIBUTE_NAME, System.currentTimeMillis());
    }

    @SuppressWarnings("unused")
    public void onFailRequest(Application app, WebuiRequestContext rcontext, RequestFailure failureType) {

    }

    @SuppressWarnings("unused")
    public void onEndRequest(Application app, WebuiRequestContext rcontext) throws Exception {
        PortalStatisticService service = (PortalStatisticService) PortalContainer.getInstance().getComponentInstanceOfType(
                PortalStatisticService.class);
        String portalOwner = ((PortalRequestContext) rcontext).getPortalOwner();
        if (portalOwner != null) {
            PortalStatistic appStatistic = service.getPortalStatistic(portalOwner);
            long startTime = Long.valueOf(app.getAttribute(ATTRIBUTE_NAME).toString());
            appStatistic.logTime(System.currentTimeMillis() - startTime);
        }
    }

    @SuppressWarnings("unused")
    public void onDestroy(Application app) {
    }

}
