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

package org.exoplatform.portal.config.model;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.PageData;

import lombok.Getter;
import lombok.Setter;

/**
 * May 13, 2004
 **/
public class Page extends Container {
  public static final String DEFAULT_PAGE     = "Default";

  private PageKey            pageKey;

  private String             ownerType;

  private String             ownerId;

  private String             editPermission;

  private boolean            showMaxWindow    = false;

  @Getter
  @Setter
  private boolean            hideSharedLayout = false;

  private String             type;

  private String             link;

  public Page() {
  }

  public Page(String ownerType, String ownerId, String name) {
    this.ownerType = ownerType;
    this.ownerId = ownerId;
    this.name = name;
  }

  public Page(PageData data) {
    super(data);

    //
    this.ownerType = data.getOwnerType();
    this.ownerId = data.getOwnerId();
    this.editPermission = data.getEditPermission();
    this.showMaxWindow = data.isShowMaxWindow();
    this.type = data.getType();
    this.link = data.getLink();
  }

  public Page(String storageId) {
    super(storageId);
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerType() {
    return ownerType;
  }

  public void setOwnerType(String ownerType) {
    this.ownerType = ownerType;
  }

  public String getEditPermission() {
    return editPermission;
  }

  public void setEditPermission(String editPermission) {
    this.editPermission = editPermission;
  }

  public boolean isShowMaxWindow() {
    return showMaxWindow;
  }

  public void setShowMaxWindow(Boolean showMaxWindow) {
    this.showMaxWindow = showMaxWindow.booleanValue();
  }

  public PageKey getPageKey() {
    if (pageKey == null) {
      pageKey = PageKey.parse(getPageId());
    }
    return pageKey;
  }

  public String getPageId() {
    if (ownerType == null || ownerId == null || name == null) {
      return null;
    } else {
      return String.format("%s::%s::%s", ownerType, ownerId, name);
    }
  }

  public void setPageId(String pageId) {
    if (pageId == null) {
      ownerType = null;
      ownerId = null;
      name = null;
    } else {
      String[] pageIdParts = pageId.split("::");
      this.ownerType = pageIdParts[0];
      this.ownerId = pageIdParts[1];
      this.name = pageIdParts[2];
    }
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  @Override
  public PageData build() {
    List<ComponentData> children = buildChildren();
    return new PageData(storageId,
                        id,
                        name,
                        icon,
                        template,
                        factoryId,
                        title,
                        description,
                        width,
                        height,
                        cssClass,
                        profiles,
                        Utils.safeImmutableList(accessPermissions),
                        children,
                        ownerType,
                        ownerId,
                        editPermission,
                        showMaxWindow,
                        hideSharedLayout,
                        type,
                        link);
  }

  public static class PageSet {
    private ArrayList<Page> pages;

    public PageSet() {
      pages = new ArrayList<>();
    }

    public ArrayList<Page> getPages() {
      return pages;
    }

    public void setPages(ArrayList<Page> list) {
      pages = list;
    }
  }

  @Override
  public String toString() {
    return "Page[ownerType=" + ownerType + ",ownerId=" + ownerId + ",name=" + name + "]";
  }
}
