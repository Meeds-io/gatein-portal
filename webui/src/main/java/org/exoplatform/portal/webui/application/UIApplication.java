/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.webui.application;

import org.exoplatform.portal.config.model.Properties;
import org.exoplatform.portal.webui.portal.UIPortalComponent;

/**
 * Created by The eXo Platform SARL Author : Pham Dung Ha ha.pham@exoplatform.com Jun 14, 2007
 */
public class UIApplication extends UIPortalComponent {

    private Properties properties;

    private boolean showInfoBar = true;

    private boolean showWindowState = true;

    private String description;

    private String icon;

    public static final String locationX = "locationX";

    public static final String locationY = "locationY";

    public static final String zIndex = "zIndex";

    public static final String appWidth = "appWidth";

    public static final String appHeight = "appHeight";

    public static final String appStatus = "appStatus";

    public Properties getProperties() {
        if (properties == null)
            properties = new Properties();
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public boolean getShowWindowState() {
        return showWindowState;
    }

    public void setShowWindowState(Boolean b) {
        showWindowState = b;
    }

    public boolean getShowInfoBar() {
        return showInfoBar;
    }

    public void setShowInfoBar(Boolean b) {
        showInfoBar = b;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String s) {
        description = s;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String s) {
        icon = s;
    }
}
