package org.exoplatform.commons.utils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.PortalContainerInfo;

public class Utils {

  public static final String CONFIGURED_DOMAIN_URL_KEY = "gatein.email.domain.url";

  private Utils() {
    // private constructor for static class
  }

  public static String getRestContextName() {
    PortalContainerConfig portalContainerConfig = ExoContainerContext.getService(PortalContainerConfig.class);
    PortalContainerInfo containerInfo = ExoContainerContext.getService(PortalContainerInfo.class);
    return portalContainerConfig.getRestContextName(containerInfo.getContainerName());
  }

  /**
   * Get the current domain name by configuration
   * 
   * @return the current domain name.
   */
  public static String getCurrentDomain() {
    String sysDomain = System.getProperty(CONFIGURED_DOMAIN_URL_KEY);
    if (sysDomain == null || sysDomain.length() == 0) {
      throw new NullPointerException("Get the domain is unsuccessfully. Please, add configuration domain on configuration.properties file with key: "
          +
          CONFIGURED_DOMAIN_URL_KEY);
    }
    //
    return sysDomain;
  }

}
