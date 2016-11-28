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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.application.replication.ApplicationState;
import org.exoplatform.webui.application.ConfigurationManager;
import org.exoplatform.webui.application.StateManager;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

public class PortalStateManager extends StateManager {

    /** . */
    private static final String APPLICATION_ATTRIBUTE_PREFIX = "psm.";

    /** . */
    private static final Logger log = LoggerFactory.getLogger(PortalStateManager.class);

    @Override
    public UIApplication restoreUIRootComponent(WebuiRequestContext context) throws Exception {
        context.setStateManager(this);

        //
        WebuiApplication app = (WebuiApplication) context.getApplication();

        //
        ApplicationState appState = null;
        HttpSession session = getSession(context);
        String key = getKey(context);
        if (session != null) {
            appState = (ApplicationState) session.getAttribute(APPLICATION_ATTRIBUTE_PREFIX + key);
        }

        UIApplication uiapp = null;
        if (appState != null) {
            if (log.isDebugEnabled()) {
                log.debug("Found application " + key + " :" + appState.getApplication());
            }
            if (Safe.equals(context.getRemoteUser(), appState.getUserName())) {
                uiapp = appState.getApplication();
            }
        } else {
            log.debug("Application " + key + " not found");
        }

        //
        if (uiapp == null) {
            ConfigurationManager cmanager = app.getConfigurationManager();
            String uirootClass = cmanager.getApplication().getUIRootComponent().trim();
            Class<? extends UIApplication> type = (Class<UIApplication>) Class.forName(uirootClass, true, Thread.currentThread().getContextClassLoader());
            uiapp = app.createUIComponent(type, null, null, context);
        }

        //
        return uiapp;
    }

    @Override
    public void storeUIRootComponent(final WebuiRequestContext context) throws Exception {
        UIApplication uiapp = context.getUIApplication();

        //
        if (uiapp != null) {
            HttpSession session = getSession(context);

            // At this point if it returns null it means that it was not possible to create a session
            // because the session might be invalidated and the response is already commited to the client.
            // That situation happens during a logout that invalidates the HttpSession
            if (session != null) {
                String key = getKey(context);
                log.debug("Storing application " + key);
                session.setAttribute(APPLICATION_ATTRIBUTE_PREFIX + key, new ApplicationState(uiapp, context.getRemoteUser()));
            }
        }
    }

    @Override
    public void expire(String sessionId, WebuiApplication app) {
        // For now do nothing....
    }

    private String getKey(WebuiRequestContext webuiRC) {
        if (webuiRC instanceof PortletRequestContext) {
            PortletRequestContext portletRC = (PortletRequestContext) webuiRC;
            return portletRC.getApplication().getApplicationId() + "/" + portletRC.getWindowId();
        } else {
            return PortalApplication.PORTAL_APPLICATION_ID;
        }
    }

    private HttpSession getSession(WebuiRequestContext webuiRC) {
        PortalRequestContext portalRC;
        if (webuiRC instanceof PortletRequestContext) {
            PortletRequestContext portletRC = (PortletRequestContext) webuiRC;
            portalRC = (PortalRequestContext) portletRC.getParentAppRequestContext();
        } else {
            portalRC = (PortalRequestContext) webuiRC;
        }
        HttpServletRequest req = portalRC.getRequest();
        return req.getSession(false);
    }
}
