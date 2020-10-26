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

package org.exoplatform.web.application.javascript;

import org.gatein.portal.controller.resource.ResourceId;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class DependencyDescriptor {

    /** . */
    private final ResourceId resourceId;

    /** . */
    private final String alias;

    /** . */
    private String pluginRS;

    public DependencyDescriptor(ResourceId resourceId) throws NullPointerException {
        this(resourceId, null, null);
    }

    public DependencyDescriptor(ResourceId resourceId, String alias, String pluginRS) throws NullPointerException {
        if (resourceId == null) {
            throw new NullPointerException("No null resource id accepted");
        }

        //
        this.resourceId = resourceId;
        this.alias = alias;
        this.pluginRS = pluginRS;
    }

    public ResourceId getResourceId() {
        return resourceId;
    }

    public String getAlias() {
        return alias;
    }

    public String getPluginResource() {
        return pluginRS;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DependencyDescriptor) {
            DependencyDescriptor that = (DependencyDescriptor) obj;
            return resourceId.equals(that.resourceId);
        }
        return false;
    }
}
