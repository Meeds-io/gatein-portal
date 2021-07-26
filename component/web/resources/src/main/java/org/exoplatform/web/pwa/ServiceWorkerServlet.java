package org.exoplatform.web.pwa;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.web.AbstractHttpServlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ServiceWorkerServlet extends AbstractHttpServlet {

  private static final long              serialVersionUID     = 3739991860557358896L;

  private static final Log               LOG                  = ExoLogger.getLogger(ServiceWorkerServlet.class);

  private static AtomicBoolean           serviceWorkerEnabled = new AtomicBoolean(true);

  private static AtomicReference<String> serviceWorkerContent = new AtomicReference<>();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!serviceWorkerEnabled.get()) {
      LOG.debug("Service worker disabled");
      resp.setStatus(404);
      return;
    }

    if (serviceWorkerContent.get() == null) {
      ServiceWorkerService serviceWorkerService = PortalContainer.getInstance()
                                                                 .getComponentInstanceOfType(ServiceWorkerService.class);
      boolean enabled = serviceWorkerService.isEnabled();
      serviceWorkerEnabled.set(enabled);
      if (!enabled) {
        LOG.debug("Service worker disabled");
        resp.setStatus(404);
        return;
      }
    }

    if (serviceWorkerContent.get() == null || PropertyManager.isDevelopping()) {
      ServiceWorkerService serviceWorkerService = PortalContainer.getInstance()
                                                                 .getComponentInstanceOfType(ServiceWorkerService.class);
      try {
        String content = serviceWorkerService.getContent();
        serviceWorkerContent.set(content);
      } catch (Exception e) {
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

}
