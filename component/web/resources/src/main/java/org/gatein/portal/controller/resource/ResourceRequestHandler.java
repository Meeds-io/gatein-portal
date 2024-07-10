/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.gatein.portal.controller.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.common.io.IOTools;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.WebAppEvent;
import org.gatein.wci.WebAppListener;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.ResourceRequestFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.application.javascript.JavascriptConfigService;
import org.exoplatform.web.controller.QualifiedName;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ResourceRequestHandler extends WebRequestHandler implements WebAppListener {

  public static final String  HANDLER_NAME             = "script";

  public static final String  IF_MODIFIED_SINCE        = "If-Modified-Since";

  public static final String  LAST_MODIFIED            = "Last-Modified";

  public static final String  SUPPORT_GATEIN_RESOURCES = "org.gatein.supports.gatein-resources.";

  /** . */
  private static final String PATH                     =
                                   "META-INF/maven/io.meeds.portal/portal.component.web.resources/pom.properties";

  /** . */
  private static final Log    LOG                      = ExoLogger.getLogger(ResourceRequestHandler.class);

  /** . */
  public static final String  VERSION;

  public static final String  VERSION_E_TAG;

  public static final long    MAX_AGE;

  static {
    // Detecting version from maven properties
    // empty value is ok
    String version = "";

    String property = PropertyManager.getProperty("gatein.assets.version");
    if (property != null && !property.isEmpty()) {
      version = property;
    } else {
      URL url = ResourceRequestHandler.class.getClassLoader().getResource(PATH);
      if (url != null) {
        LOG.debug("Loading resource serving version from " + url);
        InputStream in = null;
        try {
          in = url.openStream();
          Properties props = new Properties();
          props.load(in);
          version = props.getProperty("version");
        } catch (IOException e) {
          LOG.error("Could not read properties from " + url, e);
        } finally {
          IOTools.safeClose(in);
        }
      }
    }

    //
    LOG.info("Use version \"" + version + "\" for resource serving");
    VERSION = version;
    VERSION_E_TAG = "W/\"" + version.hashCode() + "\"";

    long seconds = 31536000L;
    String propValue = PropertyManager.getProperty("gatein.assets.script.max-age");
    if (StringUtils.isNotBlank(propValue)) {
      try {
        seconds = Long.valueOf(propValue);
      } catch (NumberFormatException e) {
        LOG.warn("The gatein.assets.script.max-age property is not set properly.");
      }
    }

    MAX_AGE = seconds;
  }

  /** . */
  public static final QualifiedName VERSION_QN     = QualifiedName.create("gtn", "version");

  /** . */
  public static final QualifiedName RESOURCE_QN    = QualifiedName.create("gtn", "resource");

  /** . */
  public static final QualifiedName SCOPE_QN       = QualifiedName.create("gtn", "scope");

  /** . */
  public static final QualifiedName COMPRESS_QN    = QualifiedName.create("gtn", "compress");

  /** . */
  public static final QualifiedName ORIENTATION_QN = QualifiedName.create("gtn", "orientation");

  /** . */
  public static final QualifiedName LANG_QN        = QualifiedName.create("gtn", "lang");

  @Override
  public String getHandlerName() {
    return HANDLER_NAME;
  }

  @Override
  public boolean execute(ControllerContext context) throws Exception {
    HttpServletResponse response = context.getResponse();
    String resourceParam = context.getParameter(RESOURCE_QN);
    String scopeParam = context.getParameter(SCOPE_QN);

    if (scopeParam != null && resourceParam != null) {
      String compressParam = context.getParameter(COMPRESS_QN);
      ScriptContent script = ExoContainerContext.getService(JavascriptConfigService.class)
                                                .getScriptContent(ResourceScope.valueOf(scopeParam), resourceParam, "true".equals(compressParam));

      if (script == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      } else {
        byte[] bytes = script.getContentAsBytes();
        response.setContentType("text/javascript");
        response.setContentLength(bytes.length);
        response.setHeader("Cache-Control", "public, " + MAX_AGE);
        response.setDateHeader(ResourceRequestFilter.LAST_MODIFIED, System.currentTimeMillis());
        response.setDateHeader(ResourceRequestFilter.EXPIRES,
                               System.currentTimeMillis() + ResourceRequestHandler.MAX_AGE * 1000);
        PrintWriter writer = response.getWriter();
        IOUtils.write(bytes, writer, StandardCharsets.UTF_8);
        writer.close();
      }
    } else {
      String msg = "Missing scope or resource param";
      LOG.error(msg);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }
    return true;
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return false;
  }

  @Override
  public void onInit(WebAppController controller, ServletConfig sConfig) throws Exception {
    super.onInit(controller, sConfig);
    LOG.debug("Registering ResourceRequestHandler for servlet container events");
    ServletContainerFactory.getServletContainer().addWebAppListener(this);
  }

  @Override
  public void onDestroy(WebAppController controller) {
    super.onDestroy(controller);
    LOG.debug("Unregistering ResourceRequestHandler for servlet container events");
    ServletContainerFactory.getServletContainer().removeWebAppListener(this);
  }

  @Override
  public void onEvent(WebAppEvent event) {
    // Nothing to do
  }
}
