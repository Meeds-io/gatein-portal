package org.exoplatform.web.application;

import java.util.List;

import org.exoplatform.container.PortalContainer;

/**
 * This is a single application lifecycle that is added in
 * webui-configuration.xml as point of extension to trigger real listeners
 * injected via Kernel configuration on service
 * {@link ApplicationLifecycleExtension}
 */
public class ExtensiblePortalApplicationLifecycle implements ApplicationLifecycle<RequestContext> {

  private ApplicationLifecycleExtension applicationLifecycleExtension;

  public void onInit(Application app) throws Exception {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onInit(app);
    }
  }

  public void onStartRequest(final Application app, final RequestContext context) throws Exception {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onStartRequest(app, context);
    }
  }

  public void onFailRequest(Application app, RequestContext context, RequestFailure failureType) {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onFailRequest(app, context, failureType);
    }
  }

  public void onEndRequest(Application app, RequestContext context) throws Exception {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onEndRequest(app, context);
    }
  }

  public void onDestroy(Application app) throws Exception {
    for (ApplicationLifecycle<RequestContext> applicationLifecycle : getApplicationLifecycles()) {
      applicationLifecycle.onDestroy(app);
    }
  }

  private List<ApplicationLifecycle<RequestContext>> getApplicationLifecycles() {
    if (applicationLifecycleExtension == null) {
      applicationLifecycleExtension =
                                    PortalContainer.getInstance().getComponentInstanceOfType(ApplicationLifecycleExtension.class);
    }
    return applicationLifecycleExtension.getPortalApplicationLifecycles();
  }
}
