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

package org.exoplatform.portal.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.controller.resource.ResourceRequestHandler;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.portal.resource.SimpleSkin;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ResourceRequestFilter extends AbstractFilter {

  protected static Log       log               = ExoLogger.getLogger(ResourceRequestFilter.class);

  public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

  public static final String LAST_MODIFIED     = "Last-Modified";

  public static final String EXPIRES           = "Expires";

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                            ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String servletPath = httpRequest.getServletPath();
    if (servletPath.endsWith(".css")) {
      SkinService skinService = ExoContainerContext.getService(SkinService.class);
      String fileContentHash = httpRequest.getParameter(SimpleSkin.HASH_QUERY_PARAM);
      String orientation = httpRequest.getParameter(SimpleSkin.ORIENTATION_QUERY_PARAM);
      String compress = httpRequest.getParameter(SimpleSkin.MINIFY_QUERY_PARAM);
      String fileContent = skinService.getSkinModuleFile(httpRequest.getContextPath() + servletPath,
                                                         fileContentHash == null ? 0 : Integer.parseInt(fileContentHash),
                                                         orientation == null ? Orientation.LT : Orientation.valueOf(orientation),
                                                         StringUtils.equals("true", compress));
      if (fileContent != null) {
        byte[] bytes = fileContent.getBytes(StandardCharsets.UTF_8);
        httpResponse.setHeader("Content-Type", "text/css");
        httpResponse.setHeader("Content-Length", String.valueOf(bytes.length));
        httpResponse.setHeader("Cache-Control", "public, " + ResourceRequestHandler.MAX_AGE);
        httpResponse.setDateHeader(ResourceRequestFilter.LAST_MODIFIED, System.currentTimeMillis());
        httpResponse.setDateHeader(ResourceRequestFilter.EXPIRES,
                                   System.currentTimeMillis() + ResourceRequestHandler.MAX_AGE * 1000);
        IOUtils.write(bytes, httpResponse.getWriter(), StandardCharsets.UTF_8);
        httpResponse.getWriter().close();
        return;
      }
    }
    httpResponse.addHeader("Cache-Control", "public, " + ResourceRequestHandler.MAX_AGE);
    httpResponse.setDateHeader(ResourceRequestFilter.LAST_MODIFIED, System.currentTimeMillis());
    httpResponse.setDateHeader(ResourceRequestFilter.EXPIRES,
                               System.currentTimeMillis() + ResourceRequestHandler.MAX_AGE * 1000);
    chain.doFilter(request, response);
  }
}
