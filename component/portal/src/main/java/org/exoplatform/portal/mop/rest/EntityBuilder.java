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

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.rest.model.UserNodeBreadcrumbItem;
import org.exoplatform.portal.mop.rest.model.UserNodeRestEntity;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;

public class EntityBuilder {
  private static final Log   LOG   = ExoLogger.getLogger(EntityBuilder.class);

  public static final String GROUP = "group";
  
  private EntityBuilder() { // NOSONAR
  }

  public static List<UserNodeRestEntity> toUserNodeRestEntity(Collection<UserNode> nodes,
                                                              boolean expand,
                                                              OrganizationService organizationService,
                                                              LayoutService layoutService,
                                                              UserACL userACL,
                                                              UserPortal userPortal,
                                                              boolean expandBreadcrumb) {
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
      if (expandBreadcrumb) {
        List<UserNodeBreadcrumbItem> userNodeBreadcrumbItemList = getUserNodeBreadcrumbList(userPortal, userNode);
        Collections.reverse(userNodeBreadcrumbItemList);
        resultNode.setUserNodeBreadcrumbItemList(userNodeBreadcrumbItemList);
      }


      resultNode.setChildren(toUserNodeRestEntity(userNode.getChildren(),
              expand,
              organizationService,
              layoutService,
              userACL,
              userPortal,
              expandBreadcrumb));
      result.add(resultNode);
    }
    return result;
  }

  public static List<UserNodeBreadcrumbItem> getUserNodeBreadcrumbList(UserPortal userPortal, UserNode node) {
    UserNavigation navigation = userPortal.getNavigation(node.getNavigation().getKey());
    UserNode rootNode = userPortal.getNode(navigation, Scope.ALL, UserNodeFilterConfig.builder().build(), null);
    node = findTargetNode(node.getName(), rootNode.getChildren());
    return node != null ? computeUserNodeBreadcrumbList(node) : Collections.emptyList();
  }

  public static List<UserNodeBreadcrumbItem> computeUserNodeBreadcrumbList(UserNode node) {
    List<UserNodeBreadcrumbItem> userNodeBreadcrumbItemList = new ArrayList<>();
    UserNodeBreadcrumbItem breadcrumbItem = new UserNodeBreadcrumbItem(node.getId(),
                                                                       node.getName(),
                                                                       node.getResolvedLabel(),
                                                                       node.getPageRef() != null ? node.getURI() : null);
    userNodeBreadcrumbItemList.add(breadcrumbItem);
    if (node.getParent() != null && !node.getParent().getName().equals("default")) {
      userNodeBreadcrumbItemList.addAll(computeUserNodeBreadcrumbList(node.getParent()));
    }
    return userNodeBreadcrumbItemList;
  }

  private static UserNode findTargetNode(String nodeName, Collection<UserNode> userNodes) {
    if (userNodes.isEmpty()) {
      return null;
    }
    UserNode targetUserNode = null;
    for (Iterator<UserNode> i = userNodes.iterator(); i.hasNext();) {
      UserNode userNode = i.next();

      if (userNode.getName().equals(nodeName)) {
        targetUserNode = userNode;
      } else if (userNode.getChildren() != null && !userNode.getChildren().isEmpty() ) {
        if (userNode.getChild(nodeName) != null) {
          targetUserNode = userNode.getChild(nodeName);
        } else {
          targetUserNode = findTargetNode(nodeName, userNode.getChildren());
        }
      }
      if (targetUserNode != null) {
        break;
      }
    }
    return targetUserNode;
  }
}
