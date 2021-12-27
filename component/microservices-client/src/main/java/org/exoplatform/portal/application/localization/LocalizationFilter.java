/**
 * Copyright (C) 2021 eXo Platform SAS. This is free software; you can
 * redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version. This software is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or
 * see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.portal.application.localization;

import java.io.IOException;

import javax.servlet.*;

import org.exoplatform.container.web.AbstractFilter;

public class LocalizationFilter extends AbstractFilter {

  @Override
  protected void afterInit(FilterConfig config) throws ServletException {
    // TODO Implement User Locale Detection switch JWT Token properties that has
    // to be computed in Monolith
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }

}
