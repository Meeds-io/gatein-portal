package org.exoplatform.web.pwa;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gatein.portal.controller.resource.ResourceRequestHandler;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.web.AbstractHttpServlet;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ServiceWorkerServlet extends AbstractHttpServlet {

  private static final String            EXTENDED_SERVICE_WORKER_PARTS_VARIABLE = "@extended-service-worker-parts@";

  private static final String            DEVELOPMENT_VARIABLE                   = "@development@";

  private static final String            ASSETS_VERSION_VARIABLE                = "@assets-version@";

  private static final String            SITE_NAME_VARIABLE                     = "@site-name@";

  private static final long              serialVersionUID                       = 3739991860557358896L;

  private static final Log               LOG                                    = ExoLogger.getLogger(ServiceWorkerServlet.class);

  private static final String            SERVICE_WORKER_ENABLED_PARAM           = "pwa.service.worker.enabled";

  private static final String            SERVICE_WORKER_PATH_PARAM              = "pwa.service.worker.path";

  private static AtomicBoolean           serviceWorkerEnabled                   = new AtomicBoolean(true);

  private static AtomicReference<String> serviceWorkerContent                   = new AtomicReference<>();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!serviceWorkerEnabled.get()) {
      LOG.debug("Service worker disabled");
      resp.setStatus(404);
      return;
    }

    if (serviceWorkerContent.get() == null) {
      String serviceWorkerEnabledString = PropertyManager.getProperty(SERVICE_WORKER_ENABLED_PARAM);
      boolean enabled = serviceWorkerEnabledString == null || Boolean.parseBoolean(serviceWorkerEnabledString.trim());
      serviceWorkerEnabled.set(enabled);
    }

    if (serviceWorkerContent.get() == null || PropertyManager.isDevelopping()) {
      String serviceWorkerPath = PropertyManager.getProperty(SERVICE_WORKER_PATH_PARAM);
      try {
        ConfigurationManager configurationManager = PortalContainer.getInstance()
                                                                   .getComponentInstanceOfType(ConfigurationManager.class);

        URL resourceURL = configurationManager.getResource(serviceWorkerPath);
        String filePath = resourceURL.getPath();
        String content = IOUtil.getFileContentAsString(filePath, "UTF-8");
        content = replaceVariables(content);
        serviceWorkerContent.set(content);
      } catch (Exception e) {
        LOG.warn("Can't find service worker path: {}", serviceWorkerPath);
        if (!PropertyManager.isDevelopping()) {
          // Turn off once for all reading file
          serviceWorkerEnabled.set(false);
        }
        resp.setStatus(404);
        return;
      }
    }

    try {
      resp.setHeader("Service-Worker-Allowed", "/");
      resp.setHeader("Cache-Control", "max-age=31536000");
      resp.setHeader("Content-Type", "text/javascript");

      PrintWriter writer = resp.getWriter();
      writer.append(serviceWorkerContent.get());
      writer.flush();
    } catch (Exception e) {
      LOG.warn("Error retrieving service worker content", e);
      resp.setStatus(500);
    }
  }

  private String replaceVariables(String content) {
    UserPortalConfigService portalConfigService = PortalContainer.getInstance()
                                                                 .getComponentInstanceOfType(UserPortalConfigService.class);
    String defaultSite = portalConfigService.getDefaultPortal();

    content = content.replaceAll(SITE_NAME_VARIABLE, defaultSite);
    content = content.replaceAll(ASSETS_VERSION_VARIABLE, ResourceRequestHandler.VERSION);
    content = content.replaceAll(DEVELOPMENT_VARIABLE, String.valueOf(PropertyManager.isDevelopping()));
    content = content.replaceAll(EXTENDED_SERVICE_WORKER_PARTS_VARIABLE, "");
    return content;
  }

}
