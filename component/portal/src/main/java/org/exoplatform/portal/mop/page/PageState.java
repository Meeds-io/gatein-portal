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
package org.exoplatform.portal.mop.page;

import java.io.Serializable;
import java.util.*;

import org.exoplatform.commons.utils.Safe;

/**
 * An immutable page state class, modifying an existing state should use the
 * {@link Builder} builder class to rebuild a new immutable state object.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class PageState implements Serializable {

  private static final long serialVersionUID = 7874166775312871923L;

  /** . */
  final String              editPermission;

  /** . */
  final boolean             showMaxWindow;

  /** . */
  final String              factoryId;

  /** . */
  final String              displayName;

  /** . */
  final String              description;

  /** . */
  final List<String>        accessPermissions;

  final List<String>        moveAppsPermissions;

  final List<String>        moveContainersPermissions;

  public PageState(String displayName,
                   String description,
                   boolean showMaxWindow,
                   String factoryId,
                   List<String> accessPermissions,
                   String editPermission,
                   List<String> moveAppsPermissions,
                   List<String> moveContainersPermissions) {
    this.editPermission = editPermission;
    this.showMaxWindow = showMaxWindow;
    this.factoryId = factoryId;
    this.displayName = displayName;
    this.description = description;
    this.accessPermissions = accessPermissions;
    this.moveAppsPermissions = moveAppsPermissions;
    this.moveContainersPermissions = moveContainersPermissions;
  }

  public String getEditPermission() {
    return editPermission;
  }

  public boolean getShowMaxWindow() {
    return showMaxWindow;
  }

  public String getFactoryId() {
    return factoryId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getAccessPermissions() {
    return accessPermissions;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PageState)) {
      return false;
    }
    PageState that = (PageState) o;
    return Safe.equals(editPermission, that.editPermission) && showMaxWindow == that.showMaxWindow
        && Safe.equals(factoryId, that.factoryId) && Safe.equals(displayName, that.displayName)
        && Safe.equals(description, that.description) && Safe.equals(accessPermissions, that.accessPermissions);
  }

  @Override
  public int hashCode() {
    int result = editPermission != null ? editPermission.hashCode() : 0;
    result = 31 * result + (showMaxWindow ? 1 : 0);
    result = 31 * result + (factoryId != null ? factoryId.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (accessPermissions != null ? accessPermissions.hashCode() : 0);
    return result;
  }

  public Builder builder() {
    return new Builder(editPermission,
                       showMaxWindow,
                       factoryId,
                       displayName,
                       description,
                       accessPermissions,
                       moveAppsPermissions,
                       moveContainersPermissions);
  }

  public static class Builder {

    /** . */
    private String       editPermission;

    /** . */
    private boolean      showMaxWindow;

    /** . */
    private String       factoryId;

    /** . */
    private String       displayName;

    /** . */
    private String       description;

    /** . */
    private List<String> accessPermissions;

    private List<String> moveAppsPermissions;

    private List<String> moveContainersPermissions;

    private Builder(String editPermission,
                    boolean showMaxWindow,
                    String factoryId,
                    String displayName,
                    String description,
                    List<String> accessPermissions,
                    List<String> moveAppsPermissions,
                    List<String> moveContainersPermissions) {
      this.editPermission = editPermission;
      this.showMaxWindow = showMaxWindow;
      this.factoryId = factoryId;
      this.displayName = displayName;
      this.description = description;
      this.accessPermissions = accessPermissions;
      this.moveAppsPermissions = moveAppsPermissions;
      this.moveContainersPermissions = moveContainersPermissions;
    }

    public Builder editPermission(String editPermission) {
      this.editPermission = editPermission;
      return this;
    }

    public Builder accessPermissions(List<String> accessPermissions) {
      this.accessPermissions = accessPermissions;
      return this;
    }

    public Builder accessPermissions(String... accessPermissions) {
      this.accessPermissions = new ArrayList<String>(Arrays.asList(accessPermissions));
      return this;
    }

    public Builder showMaxWindow(boolean showMaxWindow) {
      this.showMaxWindow = showMaxWindow;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder factoryId(String factoryId) {
      this.factoryId = factoryId;
      return this;
    }

    public PageState build() {
      return new PageState(displayName,
                           description,
                           showMaxWindow,
                           factoryId,
                           accessPermissions,
                           editPermission,
                           moveAppsPermissions,
                           moveContainersPermissions);
    }
  }
}
