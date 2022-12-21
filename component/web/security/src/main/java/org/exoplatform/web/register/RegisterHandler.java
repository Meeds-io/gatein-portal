/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.web.register;

import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.json.JSONObject;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.JspBasedWebHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.login.UIParamsExtension;

public class RegisterHandler extends JspBasedWebHandler {

  public static final String  REGISTER_EXTENSION_NAME       = "RegisterExtension";

  public static final String  REGISTER_ENABLED              = "registerEnabled";

  public static final String  REGISTER_ERROR_PARAM          = "errorCode";

  private static final Log    LOG                           = ExoLogger.getLogger(RegisterHandler.class);

  private static final String REGISTER_JSP_PATH_PARAM       = "register.jsp.path";

  private static final String REGISTER_EXTENSION_JS_MODULES = "RegisterExtension";

  private PortalContainer     container;

  private ServletContext      servletContext;

  private String              registerJspPath;

  public RegisterHandler(PortalContainer container,
                         LocaleConfigService localeConfigService,
                         BrandingService brandingService,
                         JavascriptConfigService javascriptConfigService,
                         SkinService skinService,
                         InitParams params) {
    super(localeConfigService, brandingService, javascriptConfigService, skinService);
    this.container = container;
    if (params != null && params.containsKey(REGISTER_JSP_PATH_PARAM)) {
      this.registerJspPath = params.getValueParam(REGISTER_JSP_PATH_PARAM).getValue();
    }
  }

  @Override
  public String getHandlerName() {
    return "register";
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  @Override
  public void onInit(WebAppController controller, ServletConfig servletConfig) {
    this.servletContext = container.getPortalContext();
  }

  @Override
  public boolean execute(ControllerContext controllerContext) throws Exception {
    HttpServletRequest request = controllerContext.getRequest();
    HttpServletResponse response = controllerContext.getResponse();

    List<String> additionalJSModules = getExtendedJSModules(controllerContext, request);
    List<String> additionalCSSModules = Collections.singletonList("portal/login");

    super.prepareDispatch(controllerContext,
                          "PORTLET/social-portlet/Register",
                          additionalJSModules,
                          additionalCSSModules,
                          params -> extendUIParameters(controllerContext, params));

    servletContext.getRequestDispatcher(registerJspPath).include(request, response);
    return true;
  }

  private List<String> getExtendedJSModules(ControllerContext controllerContext, HttpServletRequest request) throws Exception {
    List<String> additionalJSModules = new ArrayList<>();
    JSONObject jsConfig = javascriptConfigService.getJSConfig(controllerContext, request.getLocale());
    if (jsConfig.has(JS_PATHS_PARAM)) {
      JSONObject jsConfigPaths = jsConfig.getJSONObject(JS_PATHS_PARAM);
      Iterator<String> keys = jsConfigPaths.keys();
      while (keys.hasNext()) {
        String module = keys.next();
        if (module.contains(REGISTER_EXTENSION_JS_MODULES)) {
          additionalJSModules.add(module);
        }
      }
    }
    return additionalJSModules;
  }

  private void extendUIParameters(ControllerContext controllerContext, JSONObject params) {
    try {
      Object errorCode = controllerContext.getRequest().getAttribute(REGISTER_ERROR_PARAM);
      if (errorCode != null) {
        params.put(REGISTER_ERROR_PARAM, errorCode);
      }
      List<UIParamsExtension> paramsExtensions = this.container.getComponentInstancesOfType(UIParamsExtension.class);
      if (CollectionUtils.isNotEmpty(paramsExtensions)) {
        paramsExtensions.stream()
                        .filter(extension -> extension.getExtensionNames().contains(REGISTER_EXTENSION_NAME))
                        .forEach(paramsExtension -> {
                          Map<String, Object> extendedParams = paramsExtension.extendParameters(controllerContext,
                                                                                                REGISTER_EXTENSION_NAME);
                          if (MapUtils.isNotEmpty(extendedParams)) {
                            extendedParams.forEach((key, value) -> {
                              try {
                                params.put(key, value);
                              } catch (Exception e) {
                                LOG.warn("Error while adding {}/{} in register params map", key, value, e);
                              }
                            });
                          }
                        });
      }
    } catch (Exception e) {
      LOG.warn("Error while computing Register UI parameters", e);
    }
  }

}
