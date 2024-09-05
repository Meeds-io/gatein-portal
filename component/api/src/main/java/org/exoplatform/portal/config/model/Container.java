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

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.Tools;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.portal.pom.data.*;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Tuan Nguyen
 **/
public class Container extends ModelObject implements Cloneable {

  public static final String           EVERYONE                              = "Everyone";

  public static final List<String>     DEFAULT_ACCESS_PERMISSIONS            = Collections.singletonList(EVERYONE);

  public static final List<String>     DEFAULT_MOVE_APPLICATIONS_PERMISSIONS = Collections.singletonList(EVERYONE);

  public static final List<String>     DEFAULT_MOVE_CONTAINERS_PERMISSIONS   = Collections.singletonList(EVERYONE);

  protected String                     id;

  protected String                     name;

  protected String                     icon;

  protected String                     template;

  protected String                     factoryId;

  protected String                     title;

  protected String                     description;

  protected String                     profiles;

  protected String[]                   accessPermissions;

  protected String[]                   moveAppsPermissions;

  protected String[]                   moveContainersPermissions;

  protected ArrayList<ModelObject>     children;

  @Getter
  @Setter
  protected ApplicationBackgroundStyle appBackgroundStyle;

  public Container() {
    setDefaultPermissions();
    children = new ArrayList<>();
  }

  public Container(String storageId) {
    super(storageId);
    setDefaultPermissions();
    //
    this.children = new ArrayList<>();
  }

  public Container(ContainerData data) {
    super(data.getStorageId());

    //
    this.id = data.getId();
    this.name = data.getName();
    this.icon = data.getIcon();
    this.template = data.getTemplate();
    this.factoryId = data.getFactoryId();
    this.title = data.getTitle();
    this.description = data.getDescription();
    this.width = data.getWidth();
    this.height = data.getHeight();
    this.cssClass = data.getCssClass();
    this.cssStyle = data.getCssStyle();
    this.appBackgroundStyle = data.getAppBackgroundStyle();
    this.profiles = data.getProfiles();
    this.accessPermissions = data.getAccessPermissions().toArray(new String[data.getAccessPermissions().size()]);
    List<String> permisssions = data.getMoveAppsPermissions();
    this.moveAppsPermissions = permisssions != null ? permisssions.toArray(new String[permisssions.size()]) : null;
    permisssions = data.getMoveContainersPermissions();
    this.moveContainersPermissions = permisssions != null ? permisssions.toArray(new String[permisssions.size()]) : null;
    this.children = new ArrayList<>();
    for (ComponentData child : data.getChildren()) {
      ModelObject m = ModelObject.build(child);
      if (m != null) {
        children.add(ModelObject.build(child));
      }
    }
  }

  private void setDefaultPermissions() {
    List<String> permissions = DEFAULT_MOVE_APPLICATIONS_PERMISSIONS;
    this.moveAppsPermissions = permissions.toArray(new String[permissions.size()]);
    permissions = DEFAULT_MOVE_CONTAINERS_PERMISSIONS;
    this.moveContainersPermissions = permissions.toArray(new String[permissions.size()]);
  }

  public String getId() {
    return id;
  }

  public void setId(String s) {
    id = s;
  }

  public String getName() {
    return name;
  }

  public void setName(String s) {
    name = s;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public ArrayList<ModelObject> getChildren() {
    return children;
  }

  public void setChildren(ArrayList<ModelObject> children) {
    this.children = children;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String des) {
    description = des;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getFactoryId() {
    return factoryId;
  }

  public void setFactoryId(String factoryId) {
    this.factoryId = factoryId;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String[] getAccessPermissions() {
    return accessPermissions;
  }

  public void setAccessPermissions(String[] accessPermissions) {
    this.accessPermissions = accessPermissions;
  }

  public String[] getMoveAppsPermissions() {
    return moveAppsPermissions;
  }

  public void setMoveAppsPermissions(String[] moveAppsPermissions) {
    this.moveAppsPermissions = moveAppsPermissions;
  }

  public String[] getMoveContainersPermissions() {
    return moveContainersPermissions;
  }

  public void setMoveContainersPermissions(String[] moveContainersPermissions) {
    this.moveContainersPermissions = moveContainersPermissions;
  }

  public String getProfiles() {
    return profiles;
  }

  public void setProfiles(String profiles) {
    this.profiles = profiles;
  }

  @Override
  public ContainerData build() {
    return new ContainerData(getStorageId(),
                             getId(),
                             getName(),
                             getIcon(),
                             getTemplate(),
                             getFactoryId(),
                             getTitle(),
                             getDescription(),
                             getWidth(),
                             getHeight(),
                             getCssClass(),
                             getProfiles(),
                             getCssStyle(),
                             getAppBackgroundStyle(),
                             Utils.safeImmutableList(accessPermissions),
                             Utils.safeImmutableList(moveAppsPermissions),
                             Utils.safeImmutableList(moveContainersPermissions),
                             buildChildren());
  }

  @Override
  public void checkStorage() throws ObjectNotFoundException {
    super.checkStorage();
    if (getChildren() != null && !getChildren().isEmpty()) {
      for (ModelObject child : getChildren()) {
        child.checkStorage();
      }
    }
  }

  @Override
  public void resetStorage() throws ObjectNotFoundException {
    super.resetStorage();
    if (getChildren() != null && !getChildren().isEmpty()) {
      for (ModelObject child : getChildren()) {
        child.resetStorage();
      }
    }
  }

  @Override
  public Container clone() { // NOSONAR
    return new Container(build());
  }

  protected List<ComponentData> buildChildren() { // NOSONAR
    if (StringUtils.isNotBlank(getProfiles())) {
      Set<String> activeProfiles = Tools.parseCommaList(getProfiles());
      if (ExoContainer.getProfiles()
                      .stream()
                      .noneMatch(activeProfiles::contains)) {
        return Collections.emptyList();
      }
    }
    if (getChildren() != null && !getChildren().isEmpty()) {
      ArrayList<ComponentData> dataChildren = new ArrayList<>();
      for (int i = 0; i < children.size(); i++) {
        ModelObject node = children.get(i);
        if (node instanceof Container container) {
          String nodeProfiles = container.getProfiles();
          if (StringUtils.isNotBlank(nodeProfiles)) {
            Set<String> activeProfiles = Tools.parseCommaList(nodeProfiles);
            if (ExoContainer.getProfiles()
                            .stream()
                            .noneMatch(activeProfiles::contains)) {
              continue;
            }
          }
        } else if (node instanceof Application application) { // NOSONAR
          String nodeProfiles = application.getProfiles();
          if (StringUtils.isNotBlank(nodeProfiles)) {
            Set<String> activeProfiles = Tools.parseCommaList(nodeProfiles);
            if (ExoContainer.getProfiles()
                            .stream()
                            .noneMatch(activeProfiles::contains)) {
              continue;
            }
          }
        }
        ModelData data = node.build();
        ComponentData componentData = (ComponentData) data;
        dataChildren.add(componentData);
      }
      return Collections.unmodifiableList(dataChildren);
    } else {
      return Collections.emptyList();
    }
  }
}
