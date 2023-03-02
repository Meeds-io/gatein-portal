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

package org.exoplatform.portal.webui.navigation;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIRepeater;
import org.exoplatform.webui.core.UIVirtualList;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/*
 * Created by The eXo Platform SAS
 * Author : tam.nguyen
 *          tamndrok@gmail.com
 * May 28, 2009
 */
@ComponentConfigs({
        @ComponentConfig(template = "system:/groovy/portal/webui/navigation/UIAddGroupNavigation.gtmpl", events = {
                @EventConfig(listeners = UIMaskWorkspace.CloseActionListener.class),
                @EventConfig(listeners = UIAddGroupNavigation.AddNavigationActionListener.class) }),
        @ComponentConfig(id = "UIAddGroupNavigationGrid", type = UIRepeater.class, template = "system:/groovy/portal/webui/navigation/UIGroupGrid.gtmpl") })
public class UIAddGroupNavigation extends UIContainer {

    public UIAddGroupNavigation() throws Exception {
        UIVirtualList virtualList = addChild(UIVirtualList.class, null, "AddGroupNavList");
        UIRepeater repeater = createUIComponent(UIRepeater.class, "UIAddGroupNavigationGrid", null);
        virtualList.setUIComponent(repeater);
        addChild(UIPopupWindow.class, null, "EditGroup");
    }

    public void loadGroups() throws Exception {

        PortalRequestContext pContext = Util.getPortalRequestContext();
        UserPortalConfigService dataService = getApplicationComponent(UserPortalConfigService.class);
        UserACL userACL = getApplicationComponent(UserACL.class);
        OrganizationService orgService = getApplicationComponent(OrganizationService.class);
        final List<String> listGroup = new ArrayList<String>();

        // get all group that user has permission
        if (userACL.isUserInGroup(userACL.getAdminGroups()) && !userACL.getSuperUser().equals(pContext.getRemoteUser())) {
            Collection<?> temp = orgService.getGroupHandler().findGroupsOfUser(pContext.getRemoteUser());
            if (temp != null) {
                for (Object group : temp) {
                    Group m = (Group) group;
                    String groupId = m.getId().trim();
                    listGroup.add(groupId);
                }
            }
        } else {
            listGroup.addAll(dataService.getMakableNavigations(pContext.getRemoteUser(), false));
        }

        // Filter all groups having navigation
        NavigationService navigationService = getApplicationComponent(NavigationService.class);
        List<String> groupsHavingNavigation = new ArrayList<String>();
        for (String groupName : listGroup) {
            NavigationContext navigation = navigationService.loadNavigation(SiteKey.group(groupName));
            if (navigation != null && navigation.getState() != null) {
                groupsHavingNavigation.add(groupName);
            }
        }
        listGroup.removeAll(groupsHavingNavigation);

        UIVirtualList virtualList = getChild(UIVirtualList.class);
        final int pageSize = 6;
        Iterator<List<?>> source = new Iterator<List<?>>() {
            int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < listGroup.size();
            }

            @Override
            public List<String> next() {
                if (hasNext()) {
                    List<String> list = new ArrayList<String>(pageSize);
                    for (int i = currentIndex; i < currentIndex + pageSize; i++) {
                        if (i < listGroup.size()) {
                            list.add(listGroup.get(i));
                        } else {
                            break;
                        }
                    }

                    //
                    currentIndex += pageSize;
                    return list;
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        virtualList.dataBind(source);
    }

    public static class AddNavigationActionListener extends EventListener<UIAddGroupNavigation> {
        public void execute(Event<UIAddGroupNavigation> event) throws Exception {
            WebuiRequestContext ctx = event.getRequestContext();
            UIAddGroupNavigation uicomp = event.getSource();

            // get navigation id
            String ownerId = event.getRequestContext().getRequestParameter(OBJECTID);
            ownerId = URLDecoder.decode(ownerId);

            UIPortalApplication uiPortalApp = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class);

            // ensure this navigation does not exist
            NavigationService navigationService = uicomp.getApplicationComponent(NavigationService.class);
            NavigationContext navigation = navigationService.loadNavigation(SiteKey.group(ownerId));
            if (navigation != null && navigation.getState() != null) {
                uiPortalApp.addMessage(new ApplicationMessage("UIPageNavigationForm.msg.existPageNavigation",
                        new String[] { ownerId }));
            } else {
                // Create portal config of the group when it does not exist
                LayoutService layoutService = uicomp.getApplicationComponent(LayoutService.class);
                if (layoutService.getPortalConfig("group", ownerId) == null) {
                    UserPortalConfigService configService = uicomp.getApplicationComponent(UserPortalConfigService.class);
                    configService.createGroupSite(ownerId);
                }

                // create navigation for group
                SiteKey key = SiteKey.group(ownerId);
                NavigationContext existing = navigationService.loadNavigation(key);
                if (existing == null) {
                    navigationService.saveNavigation(new NavigationContext(key, new NavigationState(0)));
                }
            }

            // Update group navigation list
            ctx.addUIComponentToUpdateByAjax(uicomp);

            UIWorkingWorkspace uiWorkingWS = uiPortalApp.getChild(UIWorkingWorkspace.class);
            uiWorkingWS.updatePortletsByName("GroupNavigationPortlet");
            uiWorkingWS.updatePortletsByName("UserToolbarGroupPortlet");
        }
    }
}
