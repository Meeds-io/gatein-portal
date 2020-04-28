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
package org.exoplatform.portal.application.state;

import java.util.Map;

import javax.xml.namespace.QName;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * @author <a href="mailto:hoang281283@gmail.com">Minh Hoang TO</a>
 */
public abstract class AbstractContextualPropertyProviderPlugin extends BaseComponentPlugin {

    /** . */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** . */
    protected final String namespaceURI;

    protected AbstractContextualPropertyProviderPlugin(InitParams params) {
        this.namespaceURI = params.getValueParam("namespaceURI").getValue();
    }

    public abstract void getProperties(UIPortlet portletWindow, Map<QName, String[]> properties);

    protected final void addProperty(Map<QName, String[]> map, QName qname, String value) {
        if (value != null) {
            map.put(qname, new String[] { value });
        }
    }
}
