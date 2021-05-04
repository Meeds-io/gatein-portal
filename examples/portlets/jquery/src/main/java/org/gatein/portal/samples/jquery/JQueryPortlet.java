/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.gatein.portal.samples.jquery;

import java.io.IOException;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;


/**
 * @author <a href="hoang281283@gmail.com">Minh Hoang TO</a>
 */
public class JQueryPortlet extends GenericPortlet {
    @Override
    public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        String view = request.getParameter("view");
        if (view == null || !view.endsWith(".jsp")) {
            view = "index.jsp";
        }
        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/jsp/views/" + view);
        dispatcher.forward(request, response);
    }
}
