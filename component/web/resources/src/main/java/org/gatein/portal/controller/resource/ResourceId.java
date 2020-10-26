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

package org.gatein.portal.controller.resource;

import java.io.Serializable;


/**
 * Identify a resource.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ResourceId implements Serializable {

    /** . */
    private final ResourceScope scope;

    /** . */
    private final String name;

    /** . */
    private boolean isFullId;

    public ResourceId(ResourceScope scope, String name) {
        this(scope, name, true);
    }

    public ResourceId(ResourceScope scope, String name, boolean isFullId) {
        this.scope = scope;
        this.name = name;
        this.isFullId = isFullId;
    }

    public ResourceScope getScope() {
        return scope;
    }

    public String getName() {
        return name;
    }

    public void setFullId(boolean isFullId) {
        this.isFullId = isFullId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResourceId) {
            ResourceId that = (ResourceId) obj;
            return scope == that.scope && name.equals(that.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return scope.hashCode() ^ name.hashCode();
    }

    @Override
    public String toString() {
        if (!isFullId)
            return name;
        return scope + "/" + name;
    }
}
