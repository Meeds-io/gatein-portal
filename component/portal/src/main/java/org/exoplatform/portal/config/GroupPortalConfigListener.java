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

package org.exoplatform.portal.config;

import java.util.ArrayList;
import java.util.Collection;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;

/**
 * This listener is used to initialize/destroy the Group Site Config of a group when it is created/deleted
 *
 * Author : Nhu Dinh Thuan nhudinhthuan@exoplatform.com May 29, 2007
 */
public class GroupPortalConfigListener extends GroupEventListener {

    /** . */
    private final UserPortalConfigService portalConfigService;

    /** . */
    private final OrganizationService orgService;

    public GroupPortalConfigListener(UserPortalConfigService portalConfigService, OrganizationService orgService) {
        this.portalConfigService = portalConfigService;
        this.orgService = orgService;
    }

    public void preDelete(Group group) throws Exception {
        RequestLifeCycle.begin(PortalContainer.getInstance());
        try {
            String groupId = group.getId().trim();

            // Remove all descendant navigations
            removeGroupNavigation(group);

            portalConfigService.removeUserPortalConfig(PortalConfig.GROUP_TYPE, groupId);
        } finally {
            RequestLifeCycle.end();
        }
    }

    private void removeGroupNavigation(Group group) throws Exception {
        GroupHandler groupHandler = orgService.getGroupHandler();
        Collection<String> descendantGroups = getDescendantGroups(group, groupHandler);
        Collection<String> deletedNavigationGroups = new ArrayList<>();
        deletedNavigationGroups.addAll(descendantGroups);
        deletedNavigationGroups.add(group.getId());
        for (String childGroup : deletedNavigationGroups) {
            SiteKey key = SiteKey.group(childGroup);
            NavigationService navService = portalConfigService.getNavigationService();
            NavigationContext nav = navService.loadNavigation(key);
            if (nav != null) {
                navService.destroyNavigation(nav);
            }
        }
    }

    private Collection<String> getDescendantGroups(Group group, GroupHandler groupHandler) throws Exception {
        Collection<Group> groupCollection = groupHandler.findGroups(group);
        Collection<String> col = new ArrayList<String>();
        for (Group childGroup : groupCollection) {
            col.add(childGroup.getId());
            col.addAll(getDescendantGroups(childGroup, groupHandler));
        }
        return col;
    }
}
