package org.exoplatform.portal.jdbc.migration;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestHandler;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.filter.Filter;

/**
 * This filter will be used to prioritize accessed portals migration
 */
public class MigrationFilter implements Filter {
  private static final Log LOG               = ExoLogger.getLogger(MigrationFilter.class);

  private static final int MAX_WAIT_ATTEMPTS = 30;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (!(request instanceof HttpServletRequest)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    WebAppController controller = PortalContainer.getInstance().getComponentInstanceOfType(WebAppController.class);
    String portalPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
    Router router = controller.getRouter();
    if (router != null) {
      try {
        Iterator<Map<QualifiedName, String>> matcher = router.matcher(portalPath, httpRequest.getParameterMap());
        WebRequestHandler handler = null;
        while (handler == null && matcher.hasNext()) {
          Map<QualifiedName, String> parameters = matcher.next();
          String handlerKey = parameters.get(WebAppController.HANDLER_PARAM);
          if (handlerKey != null) {
            handler = controller.getHandler(handlerKey);
            if (handler instanceof PortalRequestHandler) {
              String requestSiteType = parameters.get(PortalRequestHandler.REQUEST_SITE_TYPE);
              String requestSiteName = parameters.get(PortalRequestHandler.REQUEST_SITE_NAME);

              PortalKey portalKey = new PortalKey(requestSiteType, requestSiteName);
              if (!MigrationContext.isMigrated(portalKey)) {
                MigrationContext.addPriorizedSitesToMigrate(portalKey);
                try {
                  int i = 0;
                  do {
                    LOG.info("Wait until site {} / {} migration finishes", requestSiteType, requestSiteName);
                    Thread.sleep(500);
                    i++;
                  } while (!MigrationContext.isMigrated(portalKey) && i < MAX_WAIT_ATTEMPTS);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return;
                }
              }
            }
          }
        }
      } catch (Exception e) {
        LOG.warn("Error while checking Portal RDBMS migration status when a user requests a page", e);
      }
    }
    chain.doFilter(request, response);
  }

}
