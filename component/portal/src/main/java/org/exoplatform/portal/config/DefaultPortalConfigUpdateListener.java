package org.exoplatform.portal.config;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

/**
 * Listener to update the default portal config in UserPortalConfigService service when the portal config is updated
 */
public class DefaultPortalConfigUpdateListener extends Listener {

  private UserPortalConfigService userPortalConfigService;

  public DefaultPortalConfigUpdateListener(UserPortalConfigService userPortalConfigService) {
    this.userPortalConfigService = userPortalConfigService;
  }

  @Override
  public void onEvent(Event event) throws Exception {
    if(event.getData() instanceof PortalConfig) {
      PortalConfig portalConfig = (PortalConfig) event.getData();
      // update only when the updated site is the default site
      if(StringUtils.equals(userPortalConfigService.getDefaultPortal(), portalConfig.getName())) {
        userPortalConfigService.setDefaultPortalConfig(portalConfig);
      }
    }
  }
  
}
