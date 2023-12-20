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

package org.exoplatform.web;

import jakarta.servlet.ServletConfig;

import org.exoplatform.container.component.BaseComponentPlugin;

/**
 * Abstract calss that one must implement if it want to provide a dedicated handler for serving custom requests. The execute
 * method must be impemented and the serving logic should be handled here.
 */
public abstract class WebRequestHandler extends BaseComponentPlugin {
    /**
     * Init callback.
     *
     * @param controller the controller
     * @param sConfig the servlet config
     * @throws Exception any exception
     */
    public void onInit(WebAppController controller, ServletConfig sConfig) throws Exception {
    }

    /**
     * Returns the handler name.
     *
     * @return the handler name
     */
    public abstract String getHandlerName();

    /**
     * Execute a service.
     *
     * @param context the controller context
     * @throws Exception any exception
     * @return true if the handler was able to handle the request
     */
    public abstract boolean execute(ControllerContext context) throws Exception;

    /**
     * Destroy callback.
     *
     * @param controller the controller
     */
    public void onDestroy(WebAppController controller) {
    }

    /**
     * Flag if particular handler requires lifecycle.
     *
     * @return true if processing of particular handler requires to be wrapped within
     *         {@link org.exoplatform.container.component.RequestLifeCycle} block.
     */
    protected abstract boolean getRequiresLifeCycle();
}
