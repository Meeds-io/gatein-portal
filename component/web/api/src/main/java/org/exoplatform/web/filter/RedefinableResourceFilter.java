/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.PortalContainer;

/**
 * A filter enables resource overriding via extension mechanism.
 *
 * @author <a href="mailto:hoang281283@gmail.com">Minh Hoang TO</a> Sep 8, 2010
 */

public class RedefinableResourceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        HttpServletRequest upcastedRequest = (HttpServletRequest) request;

        PortalContainer portalContainer = PortalContainer.getInstance();
        ServletContext mergedContext = portalContainer.getPortalContext();

        String path = upcastedRequest.getRequestURI();
        String ctx = upcastedRequest.getContextPath();

        if (ctx != null && ctx.length() > 0 && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        if (StringUtils.contains(path, "favicon")) {
          HttpServletResponse upcastedResponse = (HttpServletResponse) response;
          upcastedResponse.setHeader("Cache-Control", "public,max-age=86400");
          long now = System.currentTimeMillis();
          upcastedResponse.setDateHeader("Expires", now + 86400000l);
          upcastedResponse.setDateHeader("Last-Modified", now);
          upcastedResponse.setContentType("image/png");
          upcastedResponse.setHeader("Content-Encoding", "UTF-8");
        }
        mergedContext.getRequestDispatcher(path).include(request, response);
    }

}
