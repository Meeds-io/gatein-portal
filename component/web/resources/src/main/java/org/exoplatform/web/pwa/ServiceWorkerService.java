package org.exoplatform.web.pwa;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.gatein.portal.controller.resource.ResourceRequestHandler;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ServiceWorkerService implements Startable {

  private static final Log                 LOG                                    =
                                               ExoLogger.getLogger(ServiceWorkerService.class);

  private static final String              EXTENDED_SERVICE_WORKER_PARTS_VARIABLE = "@extended-service-worker-parts@";

  private static final String              DEVELOPMENT_VARIABLE                   = "@development@";

  private static final String              ASSETS_VERSION_VARIABLE                = "@assets-version@";

  private static final String              SITE_NAME_VARIABLE                     = "@site-name@";

  private Map<String, ServiceWorkerPlugin> plugins                                = new HashMap<>();

  private ConfigurationManager             configurationManager;

  private UserPortalConfigService          portalConfigService;

  private boolean                          enabled                                = true;

  private String                           filePath;

  private String                           content                                = null;

  public ServiceWorkerService(ConfigurationManager configurationManager,
                              UserPortalConfigService portalConfigService,
                              InitParams initParams) {
    this.configurationManager = configurationManager;
    this.portalConfigService = portalConfigService;
    if (initParams != null) {
      if (initParams.containsKey("filePath")) {
        this.filePath = initParams.getValueParam("filePath").getValue();
      }
      if (initParams.containsKey("enabled")) {
        this.enabled = Boolean.parseBoolean(initParams.getValueParam("enabled").getValue());
      }
    }
  }

  @Override
  public void start() {
    computeContent();
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  public void addContentPlugin(ServiceWorkerPlugin plugin) {
    this.plugins.put(plugin.getName(), plugin);
  }

  public String getContent() {
    if (content == null || PropertyManager.isDevelopping()) {
      computeContent();
    }
    return content;
  }

  public boolean isEnabled() {
    return enabled;
  }

  private void computeContent() {
    try {
      String fileContent = getContent(filePath);
      String fileExtendedContent = plugins.values().stream().map(plugin -> {
        try {
          return getContent(plugin.getFilePath());
        } catch (Exception e) {
          return "";
        }
      }).reduce("", String::concat);
      this.content = fileContent.replace(EXTENDED_SERVICE_WORKER_PARTS_VARIABLE, fileExtendedContent);
    } catch (Exception e) {
      LOG.warn("Can't find service worker path: {}", filePath);
      if (!PropertyManager.isDevelopping()) {
        // Turn off once for all reading file
        enabled = false;
      }
    }
  }

  private String getContent(String path) throws Exception {
    if (path == null) {
      return StringUtils.EMPTY;
    } else {
      URL resourceURL = configurationManager.getResource(path);
      String absolutePath = resourceURL.getPath();
      String fileContent = IOUtil.getFileContentAsString(absolutePath, "UTF-8");
      return replaceVariables(fileContent);
    }
  }

  private String replaceVariables(String content) {
    content = content.replace(SITE_NAME_VARIABLE, portalConfigService.getDefaultPortal());
    content = content.replace(ASSETS_VERSION_VARIABLE, ResourceRequestHandler.VERSION);
    return content.replace(DEVELOPMENT_VARIABLE, String.valueOf(PropertyManager.isDevelopping()));
  }

}
