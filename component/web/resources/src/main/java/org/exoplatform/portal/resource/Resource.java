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

package org.exoplatform.portal.resource;

import java.io.Reader;


/**
 * Represents a resource.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class Resource {

    private final String contextPath;

    private final String parentPath;

    private final String fileName;

    public Resource(String path) {
        int index = path.indexOf("/", 2);
        String relativeCSSPath = path.substring(index);
        int index2 = relativeCSSPath.lastIndexOf("/") + 1;

        //
        this.contextPath = path.substring(0, index);
        this.parentPath = relativeCSSPath.substring(0, index2);
        this.fileName = relativeCSSPath.substring(index2);
    }

    public Resource(String contextPath, String parentPath, String fileName) {
        this.contextPath = contextPath;
        this.parentPath = parentPath;
        this.fileName = fileName;
    }

    public final String getPath() {
        return getContextPath() + getParentPath() + getFileName();
    }

    public final String getContextPath() {
        return contextPath;
    }

    public final String getParentPath() {
        return parentPath;
    }

    public final String getFileName() {
        return fileName;
    }

    public final String getResourcePath() {
        return getParentPath() + getFileName();
    }

    public abstract Reader read();
}
