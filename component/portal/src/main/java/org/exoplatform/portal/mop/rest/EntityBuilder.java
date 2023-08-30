/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2023 Meeds Association
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
package org.exoplatform.portal.mop.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.rest.model.SiteRestEntity;
import org.exoplatform.portal.mop.rest.model.UserNodeRestEntity;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.HttpUserPortalContext;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

public class EntityBuilder {

  private static final Log               LOG   = ExoLogger.getLogger(EntityBuilder.class);

  public static final String             GROUP = "group";

  private static LayoutService           layoutService;

  private static OrganizationService     organizationService;

  private static UserACL                 userACL;

  private static UserPortalConfigService userPortalConfigService;

  private EntityBuilder() { // NOSONAR
  }

  public static List<UserNodeRestEntity> toUserNodeRestEntity(Collection<UserNode> nodes,
                                                              boolean expand,
                                                              OrganizationService organizationService,
                                                              LayoutService layoutService,
                                                              UserACL userACL) {
    if (nodes == null) {
      return Collections.emptyList();
    }
    List<UserNodeRestEntity> result = new ArrayList<>();
    for (UserNode userNode : nodes) {
      if (userNode == null) {
        continue;
      }
      UserNodeRestEntity resultNode = new UserNodeRestEntity(userNode);
      if (expand && userNode.getPageRef() != null) {
        Page userNodePage = layoutService.getPage(userNode.getPageRef());
        if (PageType.LINK.equals(PageType.valueOf(userNodePage.getType()))) {
          resultNode.setPageLink(userNodePage.getLink());
        }
        if (!StringUtils.isBlank(userNodePage.getEditPermission())) {
          resultNode.setCanEditPage(userACL.hasEditPermission(userNodePage));
          Map<String, Object> editPermission = new HashMap<>();
          try {
            editPermission.put("membershipType", userNodePage.getEditPermission().split(":")[0]);
            editPermission.put(GROUP,
                               organizationService.getGroupHandler()
                                                  .findGroupById(userNodePage.getEditPermission().split(":")[1]));
          } catch (Exception e) {
            LOG.warn("Error when getting group with id {}", userNodePage.getEditPermission().split(":")[1], e);
          }
          resultNode.setPageEditPermission(editPermission);
        }
        if (userNodePage.getAccessPermissions() != null) {
          List<Map<String, Object>> accessPermissions = new ArrayList<>();
          if (userNodePage.getAccessPermissions().length == 1 && userNodePage.getAccessPermissions()[0].equals("Everyone")) {
            Map<String, Object> accessPermission = new HashMap<>();
            accessPermission.put("membershipType", userNodePage.getAccessPermissions()[0]);
            accessPermissions.add(accessPermission);
          } else {
            accessPermissions = Arrays.stream(userNodePage.getAccessPermissions()).map(permission -> {
              Map<String, Object> accessPermission = new HashMap<>();
              try {
                accessPermission.put("membershipType", permission.split(":")[0]);
                accessPermission.put(GROUP, organizationService.getGroupHandler().findGroupById(permission.split(":")[1]));
              } catch (Exception e) {
                LOG.warn("Error when getting group with id {}", permission.split(":")[1], e);
              }
              return accessPermission;
            }).collect(Collectors.toList());
          }
          resultNode.setPageAccessPermissions(accessPermissions);
        }
      }
      resultNode.setChildren(toUserNodeRestEntity(userNode.getChildren(), expand, organizationService, layoutService, userACL));
      result.add(resultNode);
    }
    return result;
  }


  public static SiteRestEntity toSiteRestEntity(PortalConfig site, HttpServletRequest request, SiteFilter siteFilter) {
    if (site == null) {
      return null;
    }
    SiteType siteType = SiteType.valueOf(site.getType().toUpperCase());
    String displayName = site.getLabel();
    Identity userIdentity = ConversationState.getCurrent().getIdentity();
    if (SiteType.GROUP.equals(siteType)) {
      try {
        Group siteGroup = getOrganizationService().getGroupHandler().findGroupById(site.getName());
        if (siteGroup == null || !userIdentity.isMemberOf(siteGroup.getId())) {
          return null;
        } else if (org.apache.commons.lang3.StringUtils.isBlank(displayName)) {
          displayName = siteGroup.getLabel();
        }
      } catch (Exception e) {
        LOG.error("Error while retrieving group with name ", site.getName(), e);
      }
    }
    List<Map<String, Object>> accessPermissions = computePermissions(site.getAccessPermissions());
    Map<String, Object> editPermission = computePermission(site.getEditPermission());

    UserNode rootNode = null;
    if (siteFilter.isExpandNavigations()) {
      String currentUser = userIdentity.getUserId();
      try {
        HttpUserPortalContext userPortalContext = new HttpUserPortalContext(request);
        UserPortalConfig userPortalCfg = getUserPortalConfigService().getUserPortalConfig(site.getName(), currentUser, userPortalContext);
        UserPortal userPortal = userPortalCfg.getUserPortal();
        UserNavigation navigation = userPortal.getNavigation(new SiteKey(siteType.getName(), site.getName()));
        rootNode = userPortal.getNode(navigation, Scope.ALL, UserNodeFilterConfig.builder().build(), null);
      } catch (Exception e) {
        LOG.error("Error while getting site {} navigations for user {}", site.getName(), currentUser, e);
      }
    }
    return new SiteRestEntity(siteType,
            site.getName(),
            !StringUtils.isBlank(displayName) ? displayName : site.getName(),
            site.getDescription(),
            accessPermissions,
            editPermission,
            isDefaultSite(site.getName()) || site.isDisplayed(),
            site.getDisplayOrder(),
            isDefaultSite(site.getName()),
            rootNode == null ? null : toUserNodeRestEntity(rootNode.getChildren(), true, getOrganizationService(), getLayoutService(), getUserACL()));

  }

  public static List<SiteRestEntity> toSiteRestEntities(List<PortalConfig> sites,
                                                        HttpServletRequest request,
                                                        SiteFilter siteFilter) {
    return sites.stream().map(site -> toSiteRestEntity(site, request, siteFilter)).filter(Objects::nonNull).toList();
  }

  private static Map<String, Object> computePermission(String permission) {
    Map<String, Object> accessPermission = new HashMap<>();
    try {
      accessPermission.put("membershipType", permission.split(":")[0]);
      accessPermission.put(GROUP, getOrganizationService().getGroupHandler().findGroupById(permission.split(":")[1]));
    } catch (Exception e) {
      LOG.error("Error while computing user permission ", permission, e);
    }
    return accessPermission;
  }

  private static List<Map<String, Object>> computePermissions(String[] permissions) {
    return Arrays.stream(permissions).map(EntityBuilder::computePermission).toList();
  }

  private static OrganizationService getOrganizationService() {
    if (organizationService == null) {
      organizationService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationService.class);
    }
    return organizationService;
  }

  private static LayoutService getLayoutService() {
    if (layoutService == null) {
      layoutService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(LayoutService.class);
    }
    return layoutService;
  }

  private static UserACL getUserACL() {
    if (userACL == null) {
      userACL = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserACL.class);
    }
    return userACL;
  }

  private static UserPortalConfigService getUserPortalConfigService() {
    if (userPortalConfigService == null) {
      userPortalConfigService =
                              ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserPortalConfigService.class);
    }
    return userPortalConfigService;
  }

  public static boolean isDefaultSite(String siteName) {
    return getUserPortalConfigService().getDefaultPortal().equals(siteName);
  }
}
