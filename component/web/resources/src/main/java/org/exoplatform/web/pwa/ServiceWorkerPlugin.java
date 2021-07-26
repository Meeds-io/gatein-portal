package org.exoplatform.web.pwa;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

public class ServiceWorkerPlugin extends BaseComponentPlugin {

  private String filePath;

  public ServiceWorkerPlugin(InitParams initParams) {
    if (initParams != null && initParams.containsKey("filePath")) {
      this.filePath = initParams.getValueParam("filePath").getValue();
    }
  }

  public String getFilePath() {
    return filePath;
  }

}
