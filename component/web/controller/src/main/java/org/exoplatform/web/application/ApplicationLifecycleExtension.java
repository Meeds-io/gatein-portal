package org.exoplatform.web.application;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.component.ComponentPlugin;

/**
 * This is a service that will holds all WebUI application lifecycles to inject
 * on PortalApplication instance using Kernel configuration instead of defining
 * it inside webui-configuration.xml
 */
public class ApplicationLifecycleExtension {
  private List<ApplicationLifecycle<RequestContext>> appLifecycles = new ArrayList<>();

  @SuppressWarnings("unchecked")
  public void addPortalApplicationLifecycle(ComponentPlugin componentPlugin) {
    if (componentPlugin instanceof ApplicationLifecycle) {
      this.appLifecycles.add((ApplicationLifecycle<RequestContext>) componentPlugin);
    }
  }

  public List<ApplicationLifecycle<RequestContext>> getPortalApplicationLifecycles() {
    return appLifecycles;
  }
}
