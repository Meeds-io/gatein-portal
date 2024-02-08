/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.exoplatform.portal.config;

import java.util.ArrayList;
import java.util.Collection;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
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
 * This listener is used to initialize/destroy the Group Site Config of a group
 * when it is created/deleted
 */
public class GroupPortalConfigListener extends GroupEventListener {

  /** . */
  private final UserPortalConfigService portalConfigService;

  /** . */
  private final LayoutService           layoutService;

  /** . */
  private final OrganizationService     orgService;

  private String                        groupNamePattern;

  private static Log                    LOG = ExoLogger.getLogger(GroupPortalConfigListener.class);

  public GroupPortalConfigListener(InitParams params,
                                   UserPortalConfigService portalConfigService,
                                   OrganizationService orgService,
                                   LayoutService layoutService) {
    this.portalConfigService = portalConfigService;
    this.orgService = orgService;
    this.layoutService = layoutService;
    if (params != null) {
      ValueParam groupPatternValueParam = params.getValueParam("group.name.pattern");
      if (groupPatternValueParam != null) {
        groupNamePattern = groupPatternValueParam.getValue();
      }
    }
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

  @Override
  public void postSave(Group group, boolean isNew) throws Exception {
    if (!isNew) {
      return;
    }

    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      String groupId = group.getId();
      
      if (layoutService.getPortalConfig(SiteKey.group(groupId)) == null) {
        if (groupNamePattern != null && groupId.startsWith(groupNamePattern)) {
          portalConfigService.createGroupSite(groupId);
        } else {
          LOG.debug("The group name doesn't match the pattern. Ignore creating from listener", groupId);
        }
      } else {
        LOG.debug("The group site {} already exists. Ignore creating from listener", groupId);
      }
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
