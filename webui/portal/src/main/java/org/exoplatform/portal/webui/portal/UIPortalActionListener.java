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

package org.exoplatform.portal.webui.portal;

import javax.servlet.http.HttpServletRequest;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Author : Tran The Trong trongtt@gmail.com June 3, 2008
 */
public class UIPortalActionListener {

    public static class PingActionListener extends EventListener<UIPortal> {
        public void execute(Event<UIPortal> event) throws Exception {
            PortalRequestContext pContext = (PortalRequestContext) event.getRequestContext();
            HttpServletRequest request = pContext.getRequest();
            pContext.ignoreAJAXUpdateOnPortlets(false);
            pContext.setResponseComplete(true);
            pContext.getWriter().write("" + request.getSession().getMaxInactiveInterval());
        }
    }
}
