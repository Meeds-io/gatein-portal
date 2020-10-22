package org.exoplatform.web.pwa;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

/**
 * This filter will add a mandatory HTTP Header to allow service worker to
 * manage data retrieved outside /portal
 */
public class ServiceWorkerPathFilter implements Filter {

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (response instanceof HttpServletResponse) {
      ((HttpServletResponse) response).setHeader("Service-Worker-Allowed", "/");
    }
    chain.doFilter(request, response);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // NOTHING to do
  }

  @Override
  public void destroy() {
    // NOTHING to do
  }
}
