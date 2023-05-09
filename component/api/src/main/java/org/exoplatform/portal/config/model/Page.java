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
    public static final String DEFAULT_PAGE = "Default";

    private PageKey pageKey;

    private String ownerType;

    private String ownerId;

    private String editPermission;

    private boolean showMaxWindow = false;

    @Getter
    @Setter
    private boolean hideSharedLayout = false;

    private transient boolean modifiable;

    private String           pageType;

    private String           link;

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
        this.pageType = data.getPageType();
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
            return ownerType + "::" + ownerId + "::" + name;
        }
    }

    public void setPageId(String pageId) {
        if (pageId == null) {
            ownerType = null;
            ownerId = null;
            name = null;
        } else {
            int i1 = pageId.indexOf("::");
            int i2 = pageId.indexOf("::", i1 + 2);
            String ownerType = pageId.substring(0, i1);
            String ownerId = pageId.substring(i1 + 2, i2);
            String name = pageId.substring(i2 + 2);
            this.ownerType = ownerType;
            this.ownerId = ownerId;
            this.name = name;
        }
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean b) {
        modifiable = b;
    }

    @Override
    public PageData build() {
        List<ComponentData> children = buildChildren();
        return new PageData(storageId, id, name, icon, template, factoryId, title, description, width, height, cssClass, profiles,
                Utils.safeImmutableList(accessPermissions), children, ownerType, ownerId, editPermission, showMaxWindow, hideSharedLayout,
                Utils.safeImmutableList(moveAppsPermissions), Utils.safeImmutableList(moveContainersPermissions), pageType, link);
    }

    public static class PageSet {
        private ArrayList<Page> pages;

        public PageSet() {
            pages = new ArrayList<Page>();
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
