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
package org.exoplatform.portal.mop.rest.model;

import java.util.List;
import java.util.Map;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.user.UserNode;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode
public class UserNodeRestEntity {

  private UserNode                  userNode;

  private List<UserNodeRestEntity>  subNodes;

  private boolean                   canEditPage;

  private Map<String, Object>       pageEditPermission;

  private List<Map<String, Object>> pageAccessPermissions;

  private String                    pageLink;

  private List<UserNodeBreadcrumbItem> userNodeBreadcrumbItemList;

  public UserNodeRestEntity(UserNode userNode) {
    this.userNode = userNode;
  }

  public void setChildren(List<UserNodeRestEntity> subNodes) {
    this.subNodes = subNodes;
  }

  public List<UserNodeRestEntity> getChildren() {
    return subNodes;
  }

  public String getLabel() {
    return userNode == null ? null : userNode.getResolvedLabel();
  }

  public String getLabelKey() {
    return userNode == null ? null : userNode.getLabel();
  }

  public String getIcon() {
    return userNode == null ? null : userNode.getIcon();
  }

  public String getId() {
    return userNode == null ? null : userNode.getId();
  }

  public String getUri() {
    return userNode == null ? null : userNode.getURI();
  }

  public Visibility getVisibility() {
    return userNode == null ? null : userNode.getVisibility();
  }

  public String getName() {
    return userNode == null ? null : userNode.getName();
  }

  public long getStartPublicationTime() {
    return userNode == null ? 0l : userNode.getStartPublicationTime();
  }

  public long getEndPublicationTime() {
    return userNode == null ? 0l : userNode.getEndPublicationTime();
  }

  public SiteKey getSiteKey() {
    return userNode == null ? null : userNode.getNavigation().getKey();
  }

  public PageKey getPageKey() {
    return userNode == null ? null : userNode.getPageRef();
  }

  public boolean isCanEditPage() {
    return canEditPage;
  }

  public void setCanEditPage(boolean canEditPage) {
    this.canEditPage = canEditPage;
  }

  public Map<String, Object> getPageEditPermission() {
    return pageEditPermission;
  }

  public void setPageEditPermission(Map<String, Object> pageEditPermission) {
    this.pageEditPermission = pageEditPermission;
  }

  public List<Map<String, Object>> getPageAccessPermissions() {
    return pageAccessPermissions;
  }

  public void setPageAccessPermissions(List<Map<String, Object>> pageAccessPermissions) {
    this.pageAccessPermissions = pageAccessPermissions;
  }

  public String getTarget() {
    return userNode == null ? null : userNode.getTarget();
  }

  public String getPageLink() {
    return pageLink;
  }

  public void setPageLink(String pageLink) {
    this.pageLink = pageLink;
  }

  public long getUpdatedDate() {
    return userNode == null ? 0l : userNode.getUpdatedDate();
  }

  public List<UserNodeBreadcrumbItem> getUserNodeBreadcrumbItemList() {
    return userNodeBreadcrumbItemList;
  }

  public void setUserNodeBreadcrumbItemList(List<UserNodeBreadcrumbItem> userNodeBreadcrumbItemList) {
    this.userNodeBreadcrumbItemList = userNodeBreadcrumbItemList;
  }

}
