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

import java.io.IOException;
import java.util.Enumeration;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostCreateTask;
import org.exoplatform.container.web.AbstractHttpServlet;
import org.exoplatform.web.WebAppController;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

/**
 * The PortalContainer servlet is the main entry point for the eXo Portal product.
 *
 * Both the init() and service() methods are implemented. The first one is used to configure all the portal resources to prepare
 * the platform to receive requests. The second one is used to handle them.
 *
 * Basically, this class is just dispatcher as the real business logic is implemented inside the WebAppController class.
 */
@SuppressWarnings("serial")
public class PortalController extends AbstractHttpServlet {

  protected static Log log = ExoLogger.getLogger("org.gatein.portal.application.PortalController");

    /**
     * The onInit() method is used to prepare the portal to receive requests.
     *
     * 1) Get the WebAppController component from the container and delegate the init task to this component
     * 2) WebAppController will then delegate to coressponsding handlers depends on the URL. For example:
     * PortalRequestHandler is in charged of creating and registering PortalApplication and handle the requests for portal
     */
    private void onInit(ServletConfig sConfig, PortalContainer portalContainer) {
        // Keep the old ClassLoader
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        boolean hasChanged = false;
        try {
            ServletConfig config = new PortalServletConfig(sConfig, portalContainer);
            WebAppController controller = (WebAppController) portalContainer.getComponentInstanceOfType(WebAppController.class);
            // Set the full classloader of the portal container
            Thread.currentThread().setContextClassLoader(portalContainer.getPortalClassLoader());
            hasChanged = true;

            controller.onHandlersInit(config);
            log.info("The WebAppController has been successfully initialized for the portal '" + portalContainer.getName()
                    + "'");
        } catch (Throwable t) {
            log.error("The WebAppController could not be initialized for the portal '" + portalContainer.getName() + "'", t);
        } finally {
            if (hasChanged) {
                // Re-set the old classloader
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        }
    }

    /**
     * Register onInit callback to PortalContainerPostCreateTask and register the servlet context with RootContainer.
     * So that portal.war don't need PortalContainerConfigOwner listener. (extension project will need this listener)
     * @see org.exoplatform.container.web.AbstractHttpServlet#afterInit(jakarta.servlet.ServletConfig)
     */
    public void afterInit(final ServletConfig config) throws ServletException {
        final PortalContainerPostCreateTask task = new PortalContainerPostCreateTask() {

            public void execute(ServletContext context, PortalContainer portalContainer) {
                onInit(config, portalContainer);
            }
        };
        ServletContext context = config.getServletContext();
        RootContainer rootContainer = RootContainer.getInstance();
        rootContainer.addInitTask(context, task);
        rootContainer.registerPortalContainer(context);
    }

    /**
     * This method simply delegates the incoming call to the WebAppController stored in the Portal Container object
     */
    @Override
    protected void onService(ExoContainer container, HttpServletRequest req, HttpServletResponse res) throws ServletException,
            IOException {
        try {
            WebAppController controller = (WebAppController) container.getComponentInstanceOfType(WebAppController.class);
            controller.service(req, res);
        } catch (Throwable t) {
            throw new ServletException(t);
        }
    }

    /**
     * @see org.exoplatform.container.web.AbstractHttpServlet#requirePortalEnvironment()
     */
    @Override
    protected boolean requirePortalEnvironment() {
        return true;
    }

    private static class PortalServletConfig implements ServletConfig {
        private final ServletConfig sConfig;

        private final PortalContainer portalContainer;

        public PortalServletConfig(ServletConfig sConfig, PortalContainer portalContainer) {
            this.sConfig = sConfig;
            this.portalContainer = portalContainer;
        }

        public String getServletName() {
            return sConfig.getServletName();
        }

        public ServletContext getServletContext() {
            return portalContainer.getPortalContext();
        }

        @SuppressWarnings("unchecked")
        public Enumeration getInitParameterNames() {
            return sConfig.getInitParameterNames();
        }

        public String getInitParameter(String name) {
            return sConfig.getInitParameter(name);
        }
    }
}
