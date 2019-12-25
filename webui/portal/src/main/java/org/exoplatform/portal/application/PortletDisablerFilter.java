package org.exoplatform.portal.application;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.*;
import javax.portlet.filter.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.module.ModuleRegistry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * This portlet filter ensures that the portlet is active. To be active, a
 * portlet must have its context name declared as a dependency of the current
 * {@link PortalContainer}.
 */
public class PortletDisablerFilter implements RenderFilter {
  private static final Log                  LOG            = ExoLogger.getLogger(PortletDisablerFilter.class);

  private static final Map<String, Boolean> PORTLET_STATUS = new ConcurrentHashMap<>();

  private ModuleRegistry                    moduleRegistry = null;

  private String                            contextName    = null;

  public void init(FilterConfig filterConfig) throws PortletException {
    PortletContext context = filterConfig.getPortletContext();
    this.contextName = context.getPortletContextName();
  }

  /**
   * Serves a page with a message if the portlet is not a valid dependency of
   * the current portal container.
   */
  public void doFilter(RenderRequest request, RenderResponse response, FilterChain chain) throws IOException, PortletException {
    PortletConfig portletConfig = (PortletConfig) request.getAttribute("javax.portlet.config");
    if (portletConfig == null) {
      chain.doFilter(request, response);
      return;
    }
    String portletName = portletConfig.getPortletName();
    Boolean enabled = isPortletEnabled(portletName);
    if (enabled != null) {
      if (enabled.booleanValue()) {
        chain.doFilter(request, response);
      }
      return;
    }
    String configuredPortletProfiles = portletConfig.getInitParameter("exo.profiles");
    if (StringUtils.isNotBlank(configuredPortletProfiles)) {
      String[] portletProfiles = configuredPortletProfiles.trim().split(" *, *");
      Set<String> activeProfiles = ExoContainer.getProfiles();
      enabled = Arrays.stream(portletProfiles).anyMatch(activeProfiles::contains);
      if (Boolean.FALSE.equals(enabled)) {
        setPortletEnabled(portletName, enabled);
        LOG.debug("Portlet '{}' is disabled because none of profiles '{}' exists in portal container",
                  portletConfig.getPortletName(),
                  configuredPortletProfiles);
        return;
      } else {
        LOG.debug("Portlet '{}' is enable because one of profiles '{}' exists in portal container",
                  portletConfig.getPortletName(),
                  configuredPortletProfiles);
      }
    }
    String applicationId = this.contextName + "/" + portletName;
    enabled = getModuleRegistry().isPortletActive(applicationId);
    setPortletEnabled(portletName, enabled);
    if (enabled.booleanValue()) {
      chain.doFilter(request, response);
    } else {
      LOG.debug("Portlet '{}' is disabled", applicationId);
    }
  }

  private Boolean isPortletEnabled(String portletName) {
    return PORTLET_STATUS.get(portletName);
  }

  private void setPortletEnabled(String portletName, boolean enabled) {
    PORTLET_STATUS.put(portletName, enabled);
  }

  public void destroy() {
    // Nothing to destroy in this instance
  }

  /**
   * @return ModuleRegistry component
   */
  private ModuleRegistry getModuleRegistry() {
    if (moduleRegistry == null) {
      moduleRegistry = (ModuleRegistry) PortalContainer.getComponent(ModuleRegistry.class);
    }
    return moduleRegistry;
  }

}
