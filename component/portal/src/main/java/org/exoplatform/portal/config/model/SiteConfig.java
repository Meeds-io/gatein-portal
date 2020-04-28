package org.exoplatform.portal.config.model;

import org.exoplatform.portal.pom.data.ModelData;

/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
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
public class SiteConfig extends ModelObject {

    public static final String USER_TYPE = "user";

    public static final String GROUP_TYPE = "group";

    public static final String PORTAL_TYPE = "portal";

    private String ownerType;

    private String ownerId;

    /** Access permissions on UI */
    private String[] accessPermissions;

    /** Edit permissions on UI */
    private String editPermission;

    /** Layout of the site */
    private Container siteLayout;

    private String siteSkin;

    public SiteConfig(String _ownerType, String _ownerId, String storageId) {
        super(storageId);
        this.ownerType = _ownerType;
        this.ownerId = _ownerId;
    }

    @Override
    public ModelData build() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setSiteLayout(Container _siteLayout) {
        this.siteLayout = _siteLayout;
    }

    public Container getSiteLayout() {
        return this.siteLayout;
    }

    public String getSiteSkin() {
        return this.siteSkin;
    }

    public void setSiteSkin(String _siteSkin) {
        this.siteSkin = _siteSkin;
    }
}
