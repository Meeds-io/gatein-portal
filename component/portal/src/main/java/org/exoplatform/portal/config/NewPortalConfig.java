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

package org.exoplatform.portal.config;

import java.util.HashSet;


/**
 * Author : Nhu Dinh Thuan nhudinhthuan@yahoo.com May 23, 2006
 */
public final class NewPortalConfig {

    private HashSet<String> predefinedOwner = new HashSet<String>(5);

    private String ownerType;

    private String templateName;

    private String location;

    private String label;

    private String description;

    private String importMode;

    /*
     * Object type is used for override attribute in order to support both string and boolean types of the corresponding configuration field for backward compatibility reasons. 
     */
    private Object override;

    private boolean useDefaultPortalLayout = false;

    /**
     * @deprecated use the location instead
     */
    @Deprecated
    private String templateLocation;

    public NewPortalConfig() {
    }

    public NewPortalConfig(NewPortalConfig cfg) {
        this.ownerType = cfg.ownerType;
        this.templateLocation = cfg.templateLocation;
        this.location = cfg.location;
        this.label = cfg.label;
        this.description = cfg.description;
        this.templateName = cfg.templateName;
        this.predefinedOwner = new HashSet<String>(cfg.predefinedOwner);
        this.importMode = cfg.importMode;
        this.override = cfg.override;
        this.useDefaultPortalLayout = cfg.useDefaultPortalLayout;
    }

    public NewPortalConfig(String path) {
        this.location = path;
    }

    public HashSet<String> getPredefinedOwner() {
        return predefinedOwner;
    }

    public void setPredefinedOwner(HashSet<String> s) {
        this.predefinedOwner = s;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    public String getTemplateLocation() {
        if (location != null)
            return location;
        else
            return templateLocation;
    }

    public void setTemplateLocation(String s) {
        this.location = s;
        this.templateLocation = s;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String s) {
        this.templateName = s;
    }

    public boolean isPredefinedOwner(String user) {
        return predefinedOwner.contains(user);
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImportMode() {
        return importMode;
    }

    public void setImportMode(String importMode) {
        this.importMode = importMode;
    }

    public Boolean getOverrideMode() {
      if (override != null) {
        return override instanceof String ? "true".equals((String) override) : (Boolean) override;
      }
      return null;
    }

    public void setOverrideMode(boolean overrideMode) {
      this.override = overrideMode;
    }

    public void setUseDefaultPortalLayout(boolean useDefaultPortalLayout) {
      this.useDefaultPortalLayout = useDefaultPortalLayout;
    }

    public boolean isUseDefaultPortalLayout() {
      return useDefaultPortalLayout;
    }

    @Override
    public String toString() {
        return "PortalConfig[predefinedOwner=" + predefinedOwner + ",ownerType=" + ownerType + ",templateName=" + templateName
                + ",label=" + label + ",description=" + description + ",location=" + location + ",templateLocation=" + templateLocation + "]";
    }
}
