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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.application.replication.ApplicationState;
import org.exoplatform.webui.application.ConfigurationManager;
import org.exoplatform.webui.application.StateManager;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

public class StandaloneAppStateManager extends StateManager {

    /** . */
    protected static final String APPLICATION_KEY = "StandaloneApp";

    /** . */
    private static final Log    log             = ExoLogger.getLogger(StandaloneAppStateManager.class);

    @Override
    public UIApplication restoreUIRootComponent(WebuiRequestContext context) throws Exception {
        context.setStateManager(this);

        //
        WebuiApplication app = (WebuiApplication) context.getApplication();

        //
        ApplicationState appState = null;
        HttpSession session = getSession(context);
        if (session != null) {
            appState = (ApplicationState) session.getAttribute(APPLICATION_KEY);
        }

        //

        //
        UIApplication uiapp = null;
        if (appState != null) {
            if (Safe.equals(context.getRemoteUser(), appState.getUserName())) {
                uiapp = appState.getApplication();
            }
        }

        //
        if (appState != null) {
            log.debug("Found application " + APPLICATION_KEY + " :" + appState.getApplication());
        } else {
            log.debug("Application " + APPLICATION_KEY + " not found");
        }

        //
        if (uiapp == null) {
            ConfigurationManager cmanager = app.getConfigurationManager();
            String uirootClass = cmanager.getApplication().getUIRootComponent().trim();
            Class<? extends UIApplication> type = (Class<UIApplication>) Thread.currentThread().getContextClassLoader()
                    .loadClass(uirootClass);
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
                log.debug("Storing application " + APPLICATION_KEY);
                session.setAttribute(APPLICATION_KEY, new ApplicationState(uiapp, context.getRemoteUser()));
            }
        }
    }

    @Override
    public void expire(String sessionId, WebuiApplication app) {
        // For now do nothing....
    }

    protected HttpSession getSession(WebuiRequestContext webuiRC) {
        StandaloneAppRequestContext staRC = (StandaloneAppRequestContext) webuiRC;
        HttpServletRequest req = staRC.getRequest();
        return req.getSession(false);
    }
}
