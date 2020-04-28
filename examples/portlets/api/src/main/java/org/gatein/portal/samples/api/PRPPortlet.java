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

package org.gatein.portal.samples.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;


/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class PRPPortlet extends GenericPortlet {

    @Override
    protected void doView(RenderRequest req, RenderResponse resp) throws PortletException, IOException {
        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();

        //
        writer.println("Public Render Parameters<br/>");
        writer.println("<table>");
        for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
            writer.print("<tr>");
            writer.print("<td>");
            writer.print(entry.getKey());
            writer.print("</td>");
            writer.print("<td>");
            writer.print(entry.getValue()[0]);
            writer.print("</td>");
            writer.print("</tr>");
        }
        writer.println("</table>");

        //
        writer.close();
    }
}
