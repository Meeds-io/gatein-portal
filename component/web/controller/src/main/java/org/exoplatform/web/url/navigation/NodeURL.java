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

package org.exoplatform.web.url.navigation;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.url.PortalURL;
import org.exoplatform.web.url.ResourceType;
import org.exoplatform.web.url.URLContext;

/**
 * An url for navigation.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class NodeURL extends PortalURL<NavigationResource, NodeURL> {

    /** . */
    public static final QualifiedName PATH = QualifiedName.create("gtn", "path");

    /** . */
    public static final QualifiedName REQUEST_SITE_TYPE = QualifiedName.create("gtn", "sitetype");

    /** . */
    public static final QualifiedName REQUEST_SITE_NAME = QualifiedName.create("gtn", "sitename");

    /** . */
    public static final ResourceType<NavigationResource, NodeURL> TYPE = new ResourceType<NavigationResource, NodeURL>() {
    };

    /** . */
    private static final Set<QualifiedName> PARAMETER_NAMES = new HashSet<QualifiedName>();

    static {
        PARAMETER_NAMES.add(PATH);
        PARAMETER_NAMES.add(REQUEST_SITE_TYPE);
        PARAMETER_NAMES.add(REQUEST_SITE_NAME);
    }

    /** . */
    private NavigationResource resource;

    public NodeURL(URLContext context) throws NullPointerException {
        super(context);
    }

    public Set<QualifiedName> getParameterNames() {
        return PARAMETER_NAMES;
    }

    public String getParameterValue(QualifiedName parameterName) {
        if (PATH.equals(parameterName)) {
            if (resource.getNodeURI() == null) {
                return "";
            } else {
                return resource.getNodeURI();
            }
        } else if (REQUEST_SITE_TYPE.equals(parameterName)) {
            return resource.getSiteType().getName();
        } else if (REQUEST_SITE_NAME.equals(parameterName)) {
            return resource.getSiteName();
        }
        return null;
    }

    public NavigationResource getResource() {
        return resource;
    }

    public NodeURL setResource(NavigationResource resource) {
        this.resource = resource;
        return this;
    }

    public NodeURL setNode(UserNode node) {
        this.resource = new NavigationResource(node);
        return this;
    }
}
