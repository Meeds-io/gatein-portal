package org.exoplatform.web.pwa;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.web.AbstractHttpServlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ManifestServlet extends AbstractHttpServlet {

  private static final long              serialVersionUID       = -4952267603509561950L;

  private static final Log               LOG                    = ExoLogger.getLogger(ManifestServlet.class);

  private static final String            MANIFEST_ENABLED_PARAM = "pwa.manifest.enabled";

  private static final String            MANIFEST_PATH_PARAM    = "pwa.manifest.path";

  private static AtomicBoolean           manifestEnabled        = new AtomicBoolean(true);

  private static AtomicReference<String> manifestContent        = new AtomicReference<>();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (!manifestEnabled.get()) {
      LOG.debug("Service worker disabled");
      resp.setStatus(404);
      return;
    }

    if (manifestContent.get() == null) {
      String manifestEnabledString = PropertyManager.getProperty(MANIFEST_ENABLED_PARAM);
      boolean enabled = manifestEnabledString == null || Boolean.parseBoolean(manifestEnabledString.trim());
      manifestEnabled.set(enabled);
    }

    if (manifestContent.get() == null || PropertyManager.isDevelopping()) {
      String manifestPath = PropertyManager.getProperty(MANIFEST_PATH_PARAM);
      try {
        ConfigurationManager configurationManager = PortalContainer.getInstance()
                                                                   .getComponentInstanceOfType(ConfigurationManager.class);

        URL resourceURL = configurationManager.getResource(manifestPath);
        String filePath = resourceURL.getPath();
        String content = IOUtil.getFileContentAsString(filePath, "UTF-8");
        manifestContent.set(content);
      } catch (Exception e) {
        LOG.warn("Can't find service worker path: {}", manifestPath);
        if (!PropertyManager.isDevelopping()) {
          // Turn off once for all reading file
          manifestEnabled.set(false);
        }
        resp.setStatus(404);
        return;
      }
    }

    try {
      resp.setHeader("Cache-Control", "max-age=31536000");
      resp.setHeader("Content-Type", "text/javascript");

      PrintWriter writer = resp.getWriter();
      writer.append(manifestContent.get());
      writer.flush();
    } catch (Exception e) {
      LOG.warn("Error retrieving service worker content", e);
      resp.setStatus(500);
    }
  }

}
