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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.exoplatform.commons.cache.future.FutureMap;
import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.utils.Safe;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.ResourceRequestFilter;
import org.exoplatform.portal.resource.AbstractResourceDeployer;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;

import org.apache.commons.lang3.StringUtils;
import org.gatein.common.io.IOTools;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.wci.ServletContainerFactory;
import org.gatein.wci.WebAppEvent;
import org.gatein.wci.WebAppLifeCycleEvent;
import org.gatein.wci.WebAppListener;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ResourceRequestHandler extends WebRequestHandler implements WebAppListener {

    public static final String HANDLER_NAME = "script";

    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    public static final String LAST_MODIFIED = "Last-Modified";

    public static final String SUPPORT_GATEIN_RESOURCES = "org.gatein.supports.gatein-resources.";

    /** . */
    private static String PATH = "META-INF/maven/io.meeds.portal/portal.component.web.resources/pom.properties";

    /** . */
    private static final Log log                      = ExoLogger.getLogger(ResourceRequestHandler.class);

    /** . */
    public static final String VERSION;

    public static final String VERSION_E_TAG;

    public static final long MAX_AGE;

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
                log.debug("Loading resource serving version from " + url);
                InputStream in = null;
                try {
                    in = url.openStream();
                    Properties props = new Properties();
                    props.load(in);
                    version = props.getProperty("version");
                } catch (IOException e) {
                    log.error("Could not read properties from " + url, e);
                } finally {
                    IOTools.safeClose(in);
                }
            }
        }

        //
        log.info("Use version \"" + version + "\" for resource serving");
        VERSION = version;
        VERSION_E_TAG = "W/\"" + Math.abs(version.hashCode()) + "\"";

        long seconds = 31536000L;
        String propValue = PropertyManager.getProperty("gatein.assets.script.max-age");
        if (StringUtils.isNotBlank(propValue)) {
            try {
                seconds = Long.valueOf(propValue);
            } catch (NumberFormatException e) {
                log.warn("The gatein.assets.script.max-age property is not set properly.");
            }
        }

        MAX_AGE = seconds;
    }

    /** . */
    public static final QualifiedName VERSION_QN = QualifiedName.create("gtn", "version");

    /** . */
    public static final QualifiedName RESOURCE_QN = QualifiedName.create("gtn", "resource");

    /** . */
    public static final QualifiedName SCOPE_QN = QualifiedName.create("gtn", "scope");

    /** . */
    public static final QualifiedName COMPRESS_QN = QualifiedName.create("gtn", "compress");

    /** . */
    public static final QualifiedName ORIENTATION_QN = QualifiedName.create("gtn", "orientation");

    /** . */
    public static final QualifiedName LANG_QN = QualifiedName.create("gtn", "lang");

    /** . */
    private final FutureMap<ScriptKey, ScriptResult, ControllerContext> cache;

    /** . */
    private final ScriptLoader loader = new ScriptLoader();

    public ResourceRequestHandler() {
        this.cache = new FutureMap<ScriptKey, ScriptResult, ControllerContext>(loader);
    }

    @Override
    public String getHandlerName() {
        return HANDLER_NAME;
    }

    @Override
    public boolean execute(ControllerContext context) throws Exception {
        String resourceParam = context.getParameter(RESOURCE_QN);
        String scopeParam = context.getParameter(SCOPE_QN);

        //
        if (scopeParam != null && resourceParam != null) {
            String compressParam = context.getParameter(COMPRESS_QN);
            String lang = context.getParameter(LANG_QN);

            //
            Locale locale = null;
            if (lang != null && lang.length() > 0) {
                locale = I18N.parseTagIdentifier(lang);
            }

            //
            ResourceScope scope;
            try {
                scope = ResourceScope.valueOf(ResourceScope.class, scopeParam);
            } catch (IllegalArgumentException e) {
                HttpServletResponse response = context.getResponse();
                String msg = "Unrecognized scope " + scopeParam;
                log.error(msg);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
                return true;
            }

            //
            ResourceId resource = new ResourceId(scope, resourceParam);

            ScriptKey key = new ScriptKey(resource, "min".equals(compressParam), locale);

            //
            ScriptResult result;
            if (PropertyManager.isDevelopping()) {
                result = loader.retrieve(context, key);
            } else {
                result = cache.get(context, key);
            }
            HttpServletResponse response = context.getResponse();
            HttpServletRequest request = context.getRequest();

            //
            if (result instanceof ScriptResult.Resolved) {
                ScriptResult.Resolved resolved = (ScriptResult.Resolved) result;

                long ifModifiedSince = request.getDateHeader(IF_MODIFIED_SINCE);
                if (resolved.isModified(ifModifiedSince)) {
                    response.setHeader("Cache-Control", "public, " + MAX_AGE);
                    response.setDateHeader(ResourceRequestFilter.EXPIRES, System.currentTimeMillis() + MAX_AGE * 1000);
                    response.setDateHeader(ResourceRequestFilter.LAST_MODIFIED, resolved.lastModified);
                    // Content type + charset
                    response.setContentType("text/javascript");
                    response.setHeader("Etag", ResourceRequestHandler.VERSION_E_TAG);

                    // Set content length
                    response.setContentLength(resolved.bytes.length);
                    // Send bytes
                    ServletOutputStream out = response.getOutputStream();
                    try {
                        out.write(resolved.bytes);
                    } finally {
                        Safe.close(out);
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
            } else if (result instanceof ScriptResult.Error) {
                ScriptResult.Error error = (ScriptResult.Error) result;
                log.error("Could not render script " + key + "\n:" + error.message);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                cache.remove(key);
            } else {
                String msg = "Resource " + key + " cannot be found";
                log.error(msg);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            }
        } else {
            HttpServletResponse response = context.getResponse();
            String msg = "Missing scope or resource param";
            log.error(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }

        //
        return true;
    }

    @Override
    protected boolean getRequiresLifeCycle() {
        return false;
    }

    @Override
    public void onInit(WebAppController controller, ServletConfig sConfig) throws Exception {
        super.onInit(controller, sConfig);
        log.debug("Registering ResourceRequestHandler for servlet container events");
        ServletContainerFactory.getServletContainer().addWebAppListener(this);
    }

    @Override
    public void onDestroy(WebAppController controller) {
        super.onDestroy(controller);
        log.debug("Unregistering ResourceRequestHandler for servlet container events");
        ServletContainerFactory.getServletContainer().removeWebAppListener(this);
    }

    @Override
    public void onEvent(WebAppEvent event) {
        if (event instanceof WebAppLifeCycleEvent) {
            WebAppLifeCycleEvent lifeCycleEvent = (WebAppLifeCycleEvent) event;
            ServletContext servletContext = lifeCycleEvent.getWebApp().getServletContext();

            if (WebAppLifeCycleEvent.ADDED == lifeCycleEvent.getType()) {
                InputStream is = servletContext.getResourceAsStream(AbstractResourceDeployer.GATEIN_CONFIG_RESOURCE);
                if (is != null) {
                    servletContext.setAttribute(SUPPORT_GATEIN_RESOURCES, true);
                }
            } else if (servletContext.getAttribute(SUPPORT_GATEIN_RESOURCES) != null
                    && WebAppLifeCycleEvent.REMOVED == lifeCycleEvent.getType()) {
                cache.clear();
                servletContext.removeAttribute(SUPPORT_GATEIN_RESOURCES);
            }
        }
    }

    /**
     * Get cached Javascript Modules ids
     */
    public Collection<String> getJavascriptModules() {
      List<String> keys = new ArrayList<>();

      Set<ScriptKey> cacheKeys = cache.getKeys();
      for (ScriptKey scriptKey : cacheKeys) {
        keys.add(scriptKey.id.toString());
      }
      return keys;
    }

    /**
     * Reload Javascript Modules by clearing cache
     */
    public void reloadJavascriptModules() {
      cache.clear();
    }

    /**
     * Reload Javascript Module by its id like 'SHARED/jquery'
     * 
     * @param jsModule
     */
    public void reloadJavascriptModule(String jsModule) {
      String[] scriptIdParts = jsModule.split("/");
      if(scriptIdParts.length != 2) {
        throw new IllegalArgumentException("js module have to be identified by 'SCOPE/id' like 'SHARED/jquery' ");
      }

      ResourceScope scope = ResourceScope.valueOf(ResourceScope.class, scriptIdParts[0]);
      ResourceId resource = new ResourceId(scope, scriptIdParts[1]);
      LocaleConfigService localeService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(LocaleConfigService.class);
      Collection<LocaleConfig> localConfigs = localeService.getLocalConfigs();
      for (LocaleConfig localeConfig : localConfigs) {
        ScriptKey key = new ScriptKey(resource, true, localeConfig.getLocale());
        cache.remove(key);
      }
      ScriptKey key = new ScriptKey(resource, true, null);
      cache.remove(key);
    }
}
