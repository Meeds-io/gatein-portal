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

package org.exoplatform.web.url;

import org.exoplatform.container.component.BaseComponentPlugin;

/**
 * A plugin for adding an url factory to the {@link URLFactoryService}.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class URLFactoryPlugin<R, U extends PortalURL<R, U>> extends BaseComponentPlugin {

    /**
     * Returns the resource type.
     *
     * @return the resource type
     */
    protected abstract ResourceType<R, U> getResourceType();

    /**
     * Returns a new url instance.
     *
     * @param context the context
     * @return a new url instance
     */
    protected abstract U newURL(URLContext context);

}
