/*
 * Copyright (C) 2018 eXo Platform SAS.
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
package org.exoplatform.portal.gadget.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.security.proxy.ProxyFilterService;

/**
 *
 */
public class IfrServletFilter implements Filter {

    /** . */
    private ServletContext ctx;

    /** . */
    private static final Log logger = ExoLogger.getLogger(IfrServletFilter.class);

    public void init(FilterConfig cfg) throws ServletException {
        this.ctx = cfg.getServletContext();
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest hreq = (HttpServletRequest) req;
        HttpServletResponse hresp = (HttpServletResponse) resp;

        // Get URL
        String url = hreq.getParameter("url");
        if (url == null) {
            hresp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No URL");
        } else {
            CharResponseWrapper responseWrapper = new CharResponseWrapper((HttpServletResponse) resp);

            chain.doFilter(req, responseWrapper);

            // Always return the same type of error if an error if sent by the proxy servlet to avoid
            // disclosing the reason of the failure (which can lead to know if the port used in the url
            // is opened or not)
            if(resp != null && hresp.getStatus() >= HttpServletResponse.SC_BAD_REQUEST) {
                logger.debug("The error response code sent by gadget iframe servlet is {}", hresp.getStatus());
                hresp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                hresp.setContentLength(0);
                hresp.getOutputStream().write("".getBytes());
            } else {
                PrintWriter responseWrapperWriter = responseWrapper.getWriter();
                responseWrapperWriter.flush();
                byte[] bytes = responseWrapper.getByteArray();
                hresp.setContentLength(bytes.length);
                hresp.getWriter().write(new String(bytes));
            }
        }
    }

    public void destroy() {
    }
}
