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

package org.exoplatform.portal.webui.page;

import java.util.ArrayList;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Utils;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.portal.PageNodeEvent;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Just a class that contains the Page related action listeners
 *
 * @author <a href="mailto:trongtt@gmail.com">Tran The Trong</a>
 * @version $Revision$
 */
public class UIPageActionListener {

    public static class ChangeNodeActionListener extends EventListener<UIPortalApplication> {
        public void execute(Event<UIPortalApplication> event) throws Exception {
            PortalRequestContext pcontext = PortalRequestContext.getCurrentInstance();
            UserPortal userPortal = pcontext.getUserPortalConfig().getUserPortal();
            UIPortalApplication uiPortalApp = event.getSource();
            UIPortal showedUIPortal = uiPortalApp.getCurrentSite();

            UserNodeFilterConfig.Builder builder = UserNodeFilterConfig.builder();
            builder.withReadCheck();

            PageNodeEvent<UIPortalApplication> pageNodeEvent = (PageNodeEvent<UIPortalApplication>) event;
            String nodePath = pageNodeEvent.getTargetNodeUri();

            UserNode targetNode = null;
            SiteKey siteKey = pageNodeEvent.getSiteKey();
            if (siteKey != null) {
                if (pcontext.getRemoteUser() == null
                        && (siteKey.getType().equals(SiteType.GROUP) || siteKey.getType().equals(SiteType.USER))) {
                    NavigationService service = uiPortalApp.getApplicationComponent(NavigationService.class);
                    NavigationContext navContext = service.loadNavigation(siteKey);
                    if (navContext != null) {
                        uiPortalApp.setLastRequestNavData(null);
                        pcontext.requestAuthenticationLogin();
                        return;
                    }
                }

                UserNavigation navigation = userPortal.getNavigation(siteKey);
                if (navigation != null) {
                    targetNode = userPortal.resolvePath(navigation, builder.build(), nodePath);
                    if (targetNode == null) {
                        // If unauthenticated users have no permission on PORTAL node and URL is valid, they will be required to
                        // login
                        if (pcontext.getRemoteUser() == null && siteKey.getType().equals(SiteType.PORTAL)) {
                            targetNode = userPortal.resolvePath(navigation, null, nodePath);
                            if (targetNode != null) {
                                uiPortalApp.setLastRequestNavData(null);
                                pcontext.requestAuthenticationLogin();
                                return;
                            }
                        } else {
                            // If path to node is invalid, get the default node instead of.
                            targetNode = userPortal.getDefaultPath(navigation, builder.build());
                        }
                    }
                }
            }

            if (targetNode == null) {
                targetNode = userPortal.getDefaultPath(builder.build());
                if (targetNode == null) {
                    if (showedUIPortal != null) {
                        UIPageBody uiPageBody = showedUIPortal.findFirstComponentOfType(UIPageBody.class);
                        uiPageBody.setUIComponent(null);
                    }
                    return;
                }
            }

            UserNode currentNavPath = null;
            if (showedUIPortal != null) {
                currentNavPath = showedUIPortal.getNavPath();
            }

            if (currentNavPath != null && currentNavPath.getNavigation().getKey().equals(siteKey)) {
                // Case 1: Both navigation type and id are not changed, but current page node is changed and it is not a first
                // request.
                if (!currentNavPath.getURI().equals(targetNode.getURI())) {
                    showedUIPortal.setNavPath(targetNode);
                }
            } else {
                // Case 2: Either navigation type or id has been changed
                // First, we try to find a cached UIPortal
                UIWorkingWorkspace uiWorkingWS = uiPortalApp.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
                uiWorkingWS.setRenderedChild(UIPortalApplication.UI_VIEWING_WS_ID);
                uiPortalApp.setModeState(UIPortalApplication.NORMAL_MODE);
                showedUIPortal = uiPortalApp.getCachedUIPortal(siteKey);
                if (showedUIPortal != null) {
                    showedUIPortal.setNavPath(targetNode);
                    uiPortalApp.setCurrentSite(showedUIPortal);

                    PortalConfig associatedPortalConfig = pcontext.getDynamicPortalConfig();
                    UserPortalConfig userPortalConfig = pcontext.getUserPortalConfig();

                    // Update layout-related data on UserPortalConfig
                    userPortalConfig.setPortalConfig(associatedPortalConfig);
                } else {
                    showedUIPortal = buildUIPortal(uiPortalApp, pcontext);
                    if (showedUIPortal == null) {
                        return;
                    }
                    showedUIPortal.setNavPath(targetNode);
                    uiPortalApp.setCurrentSite(showedUIPortal);
                    uiPortalApp.putCachedUIPortal(showedUIPortal);
                }
            }

            showedUIPortal.refreshUIPage();
            pcontext.ignoreAJAXUpdateOnPortlets(!pcontext.useAjax());
        }

        private UIPortal buildUIPortal(UIPortalApplication uiPortalApp, PortalRequestContext pcontext)
                throws Exception {
            PortalConfig portalConfig = pcontext.getDynamicPortalConfig();
            Container layout = portalConfig.getPortalLayout();
            if (layout != null) {
                pcontext.getUserPortalConfig().setPortalConfig(portalConfig);
            }
            UIPortal uiPortal = uiPortalApp.createUIComponent(UIPortal.class, null, null);

            // Reset selected navigation on userPortalConfig
            PortalDataMapper.toUIPortalWithMetaLayout(uiPortal, pcontext.getDynamicPortalConfig());
            return uiPortal;
        }

    }
}
