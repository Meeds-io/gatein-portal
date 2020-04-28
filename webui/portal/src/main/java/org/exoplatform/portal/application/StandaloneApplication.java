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

import javax.servlet.ServletConfig;


public class StandaloneApplication extends PortalApplication {
    private String webuiConfigPath;

    public static final String STANDALONE_APPLICATION_ID = "StandaloneApplication";

    public StandaloneApplication(ServletConfig config) throws Exception {
        super(config);
    }

    public void setWebUIConfigPath(String path) {
        webuiConfigPath = path;
    }

    public String getApplicationId() {
        return STANDALONE_APPLICATION_ID;
    }

    public String getApplicationInitParam(String name) {
        if ("webui.configuration".equals(name)) {
            return webuiConfigPath;
        }
        return super.getApplicationInitParam("standalone." + name);
    }
}
