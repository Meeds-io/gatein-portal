/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.application;

import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.StaleModelException;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.PortalHttpServletResponseWrapper;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.application.ApplicationRequestPhaseLifecycle;
import org.exoplatform.web.application.Phase;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.RequestFailure;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.webui.core.UIApplication;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Created by The eXo Platform SAS Dec 9, 2006<br>
 * This is 'portal' handler, it handle the request of URLs that are routed by
 * navigation controller (using urls parameter). This handler is registered to
 * WebAppController by xml configuration.
 */
@SuppressWarnings("rawtypes")
public class PortalRequestHandler extends WebRequestHandler {

  protected static final Log                    LOG               = ExoLogger.getLogger("portal:PortalRequestHandler");

  /** . */
  public static final QualifiedName             REQUEST_PATH      = QualifiedName.create("gtn", "path");

  /** . */
  public static final QualifiedName             REQUEST_SITE_TYPE = QualifiedName.create("gtn", "sitetype");

  /** . */
  public static final QualifiedName             REQUEST_SITE_NAME = QualifiedName.create("gtn", "sitename");

  /** . */
  public static final QualifiedName             LANG              = QualifiedName.create("gtn", "lang");

  private static final PortalApplicationFactory APPP_ROVIDER      = ServiceLoader.load(PortalApplicationFactory.class)
                                                                                 .findFirst()
                                                                                 .orElse(null);

  private static final String                   PORTAL_PUBLIC_PAGE_NOT_FOUND = "/portal/public/page-not-found";

  public String getHandlerName() {
    return "portal";
  }

  /**
   * Dispatched from WebAppController, after the portal servlet init function
   * called, this method create and register PortalApplication to
   * WebAppController PortalApplication creation can be customized by
   * registering PortalApplicationFactory implementation using ServiceLoader
   *
   * @see PortalApplication
   */
  @Override
  public void onInit(WebAppController controller, ServletConfig sConfig) throws Exception {
    PortalApplication application;
    if (APPP_ROVIDER != null) {
      application = APPP_ROVIDER.createApplication(sConfig);
    } else {
      application = new PortalApplication(sConfig);
    }
    application.onInit();
    controller.addApplication(application);
  }

  /**
   * This method will handle incoming portal request. It gets a reference to the
   * WebAppController Here are the steps done in the method: <br>
   * 1) Get the PortalApplication reference from the controller <br>
   * 2) Create a PortalRequestContext object that is a convenient wrapper on all
   * the request information <br>
   * 3) Get the collection of ApplicationLifecycle referenced in the
   * PortalApplication and defined in the webui-configuration.xml of the portal
   * application <br>
   * 4) Set that context in a ThreadLocal to easily access it <br>
   * 5) Check if user have permission to access portal, if not, send 403 status
   * code, if user has not login, redirect to login page <br>
   * 6) dispatch to processRequest method, this is protected method, we can
   * extend and override this method to write a new requestHandler base on
   * PortalRequestHandler <br>
   */
  @Override
  public boolean execute(ControllerContext controllerContext) throws Exception {
    HttpServletRequest req = controllerContext.getRequest();
    HttpServletResponse res = controllerContext.getResponse();

    res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    res.setHeader("Pragma", "no-cache");
    res.setHeader("Expires", "0");

    Locale requestLocale = getRequestLocale(controllerContext);
    String requestSiteName = controllerContext.getParameter(REQUEST_SITE_NAME);
    String requestSiteType = controllerContext.getParameter(REQUEST_SITE_TYPE);
    String requestPath = controllerContext.getParameter(REQUEST_PATH);

    if (requestSiteName == null) {
      res.sendRedirect(req.getContextPath());
      return true;
    }

    UserPortalConfigService portalConfigService = ExoContainerContext.getService(UserPortalConfigService.class);
    requestPath = computeRequestPath(requestPath, requestSiteName, requestSiteType, portalConfigService, req);
    PortalApplication app = controllerContext.getController().getApplication(PortalApplication.PORTAL_APPLICATION_ID);
    PortalRequestContext context = new PortalRequestContext(app,
                                                            controllerContext,
                                                            requestSiteType,
                                                            requestSiteName,
                                                            requestPath,
                                                            requestLocale);
    try {
      PortalConfig persistentPortalConfig = context.getDynamicPortalConfig();
      if (context.getUserPortalConfig() == null) {
        if (persistentPortalConfig == null
            || StringUtils.equals(persistentPortalConfig.getName(), portalConfigService.getGlobalPortal())) {
          return false;
        } else if (req.getRemoteUser() == null) {
          context.requestAuthenticationLogin();
        } else {
          String metaPageNotFound = "/portal/" + portalConfigService.getMetaPortal() + "/page-not-found";
          if (!StringUtils.equals(req.getRequestURI(), metaPageNotFound)) {
            if (StringUtils.equals(req.getRequestURI(), PORTAL_PUBLIC_PAGE_NOT_FOUND)) {
              // In case page-not-found can't be displayed in 'public' or 'meta' sites
              // If logged in => redirect to /
              // If Anonymous => redirect to Login page
              if (StringUtils.isNotBlank(req.getRemoteUser())) {
                context.sendRedirect("/");
              } else {
                context.requestAuthenticationLogin();
              }
            } else {
              context.sendRedirect(metaPageNotFound);
            }
          } else {
            context.sendRedirect(PORTAL_PUBLIC_PAGE_NOT_FOUND);
          }
        }
      } else if (persistentPortalConfig != null
                 && StringUtils.equals(persistentPortalConfig.getName(), portalConfigService.getGlobalPortal())) {
        return false;
      } else {
        processRequest(context, app);
      }
      return true;
    } finally {
      context.onRequestEnd();
    }
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  /**
   * This method do the main job on processing a portal request: 1) Call
   * onStartRequest() on each ApplicationLifecycle object <br>
   * 2) Get the StateManager object from the PortalApplication (also referenced
   * in the XML file) <br>
   * 3) Use the StateManager to get a reference on the root UI component:
   * UIApplication; the method used is restoreUIRootComponent(context) <br>
   * 4) If the UI component is not the current one in used in the
   * PortalContextRequest, then replace it <br>
   * 5) Process decode on the PortalApplication <br>
   * 6) Process Action on the PortalApplication <br>
   * 7) Process Render on the UIApplication UI component <br>
   * 8) call onEndRequest on all the ApplicationLifecycle <br>
   * 9) Release the context from the thread
   *
   * @param context
   * @param app
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  protected void processRequest(PortalRequestContext context, PortalApplication app) throws Exception {
    RequestContext.setCurrentInstance(context);
    PortalRequestImpl.createInstance(context);
    List<ApplicationLifecycle> lifecycles = app.getApplicationLifecycle();
    try {
      if (context.getResponse() instanceof PortalHttpServletResponseWrapper responseWrapper) {
        responseWrapper.setWrapMethods(true);
      }
      UIApplication uiApp = app.getStateManager().restoreUIRootComponent(context);
      if (context.getUIApplication() != uiApp)
        context.setUIApplication(uiApp);
      for (ApplicationLifecycle lifecycle : lifecycles)
        lifecycle.onStartRequest(app, context);

      if (uiApp != null) {
        uiApp.processDecode(context);
      }

      if (!context.isResponseComplete() && !context.getProcessRender()) {
        startRequestPhaseLifecycle(app, context, lifecycles, Phase.ACTION);
        uiApp.processAction(context);
        endRequestPhaseLifecycle(app, context, lifecycles, Phase.ACTION);
      }

      if (!context.isResponseComplete()) {
        startRequestPhaseLifecycle(app, context, lifecycles, Phase.RENDER);
        uiApp.processRender(context);
        endRequestPhaseLifecycle(app, context, lifecycles, Phase.RENDER);
      }

      if (uiApp != null)
        uiApp.setLastAccessApplication(System.currentTimeMillis());

      // Store ui root
      app.getStateManager().storeUIRootComponent(context);
    } catch (StaleModelException e) {
      // Minh Hoang TO:
      // At the moment, this catch block is never reached, as the
      // StaleModelException is intercepted temporarily
      // in UI-related code
      for (ApplicationLifecycle lifecycle : lifecycles) {
        lifecycle.onFailRequest(app, context, RequestFailure.CONCURRENCY_FAILURE);
      }
    } catch (Exception e) {
      // We want to ignore the ClientAbortException since this is caused by the
      // users
      // browser closing the connection and is not something we should be
      // logging.
      if (!e.getClass().toString().contains("ClientAbortException")) {
        LOG.error("Error while handling request", e);
      }
    } finally {
      try {
        if (context.getResponse() instanceof PortalHttpServletResponseWrapper responseWrapper) {
          responseWrapper.commit();
          responseWrapper.setWrapMethods(false);
        }

        // We flush the writer here for all
        context.getWriter().flush();
        //
        try {
          for (ApplicationLifecycle lifecycle : lifecycles)
            lifecycle.onEndRequest(app, context);
        } catch (Exception exception) {
          LOG.error("Error while ending request on all ApplicationLifecycle", exception);
        }
      } finally {
        // To avoid memory leak, the ThreadLocal instances have to be purged all
        // time, even if an error occurs
        RequestContext.setCurrentInstance(null);
        PortalRequestImpl.clearInstance();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void startRequestPhaseLifecycle(PortalApplication app,
                                          PortalRequestContext context,
                                          List<ApplicationLifecycle> lifecycles,
                                          Phase phase) {
    for (ApplicationLifecycle lifecycle : lifecycles) {
      if (lifecycle instanceof ApplicationRequestPhaseLifecycle requestLifecycle)
        requestLifecycle.onStartRequestPhase(app, context, phase);
    }
  }

  @SuppressWarnings("unchecked")
  private void endRequestPhaseLifecycle(PortalApplication app,
                                        PortalRequestContext context,
                                        List<ApplicationLifecycle> lifecycles,
                                        Phase phase) {
    for (ApplicationLifecycle lifecycle : lifecycles) {
      if (lifecycle instanceof ApplicationRequestPhaseLifecycle applicationRequestPhaseLifecycle)
        applicationRequestPhaseLifecycle.onEndRequestPhase(app, context, phase);
    }
  }

  private String computeRequestPath(String path,
                                    String portalName,
                                    String requestSiteType,
                                    UserPortalConfigService portalConfigService,
                                    HttpServletRequest context) throws Exception {
    if (path.isBlank()) {
      if (SiteType.GROUP.getName().equals(requestSiteType)) {
        return path;
      }
      String newPath = portalConfigService.computePortalSitePath(portalName, context);
      return newPath == null ? path : newPath.substring(("/" + requestSiteType + "/" + portalName + "/").length());
    }
    return portalConfigService.getFirstAllowedPageNode(portalName, requestSiteType, path, context);
  }

  private Locale getRequestLocale(ControllerContext controllerContext) {
    String lang = controllerContext.getParameter(LANG);
    if (StringUtils.isBlank(lang)) {
      return null;
    } else {
      return I18N.parseTagIdentifier(lang);
    }
  }

}
