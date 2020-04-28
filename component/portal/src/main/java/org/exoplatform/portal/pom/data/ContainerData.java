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
package org.exoplatform.portal.pom.data;

import java.util.List;


/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class ContainerData extends ComponentData {

    private static final long serialVersionUID = 7328080023001711540L;

    /** . */
    private final String id;

    /** . */
    private final String name;

    /** . */
    private final String icon;

    /** . */
    private final String template;

    /** . */
    private final String factoryId;

    /** . */
    private final String title;

    /** . */
    private final String description;

    /** . */
    private final String width;

    /** . */
    private final String height;

    /** . */
    private final String cssClass;

    /** . */
    private final String profiles;

    /** . */
    private final List<String> accessPermissions;

    private final List<String> moveAppsPermissions;

    private final List<String> moveContainersPermissions;

    /** . */
    private final List<ComponentData> children;

    public ContainerData(String storageId, String id, String name, String icon, String template, String factoryId,
            String title, String description, String width, String height, List<String> accessPermissions,
            List<String> moveAppsPermissions, List<String> moveContainersPermissions,
            List<ComponentData> children) {
    this(storageId,
         id,
         name,
         icon,
         template,
         factoryId,
         title,
         description,
         width,
         height,
         null,
         null,
         accessPermissions,
         moveAppsPermissions,
         moveContainersPermissions,
         children);
    }

    public ContainerData(String storageId,
                         String id,
                         String name,
                         String icon,
                         String template,
                         String factoryId,
                         String title,
                         String description,
                         String width,
                         String height,
                         String cssClass,
                         String profiles,
                         List<String> accessPermissions,
                         List<String> moveAppsPermissions,
                         List<String> moveContainersPermissions,
                         List<ComponentData> children) {
        super(storageId, null);

        //
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.template = template;
        this.factoryId = factoryId;
        this.title = title;
        this.description = description;
        this.width = width;
        this.height = height;
        this.cssClass = cssClass;
        this.profiles = profiles;
        this.accessPermissions = accessPermissions;
        this.moveAppsPermissions = moveAppsPermissions;
        this.moveContainersPermissions = moveContainersPermissions;
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getTemplate() {
        return template;
    }

    public String getFactoryId() {
        return factoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    public List<String> getAccessPermissions() {
        return accessPermissions;
    }

    public List<ComponentData> getChildren() {
        return children;
    }

    /**
     * @return the moveAppsPermissions
     */
    public List<String> getMoveAppsPermissions() {
        return moveAppsPermissions;
    }

    /**
     * @return the moveContainersPermissions
     */
    public List<String> getMoveContainersPermissions() {
        return moveContainersPermissions;
    }
  
    public String getCssClass() {
      return cssClass;
    }

    public String getProfiles() {
      return profiles;
    }

}
