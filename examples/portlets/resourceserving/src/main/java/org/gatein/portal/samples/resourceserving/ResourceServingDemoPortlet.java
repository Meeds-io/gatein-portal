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

package org.gatein.portal.samples.resourceserving;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceURL;


/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ResourceServingDemoPortlet extends GenericPortlet {

    /** . */
    private static final String HTML = "html", EXCEPTION = "exception", NOT_FOUND = "404", NO_OP = "noop";

    /** . */
    private static String[] msg = { "HTML", "Exception", "Not Found", "No op" };

    /** . */
    private static String[] behaviors = { HTML, EXCEPTION, NOT_FOUND, NO_OP };

    @Override
    protected void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        ResourceURL url = response.createResourceURL();
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.print("<p>This portlet shows how a resource serving portlet can modify the response</p>");
        writer.print("<ul>");
        for (int i = 0; i < msg.length; i++) {
            url.setParameter("behavior", behaviors[i]);
            writer.print("<li><a href='" + url + "'>" + msg[i] + "</a></li>");
        }
        writer.print("</ul>");
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        String behavior = request.getParameter("behavior");
        if (HTML.equals(behavior)) {
            response.setContentType("text/html");
            PrintWriter writer = response.getWriter();
            writer.print("<html><body>Hello World</body><html>");
        } else if (EXCEPTION.equals(behavior)) {
            throw new PortletException("Don't freak out");
        } else if (NOT_FOUND.equals(behavior)) {
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "404");
            PrintWriter writer = response.getWriter();
            writer.print("<html><body>Not Found</body><html>");
        } else {
            // Do nothing
        }
    }
}
