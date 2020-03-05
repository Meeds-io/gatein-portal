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

import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.portal.pom.data.*;

/**
 * @author Tuan Nguyen
 **/
public class Container extends ModelObject implements Cloneable {

  public static final String       EVERYONE                              = "Everyone";

  public static final List<String> DEFAULT_ACCESS_PERMISSIONS            = Collections.singletonList(EVERYONE);

  public static final List<String> DEFAULT_MOVE_APPLICATIONS_PERMISSIONS = Collections.singletonList(EVERYONE);

  public static final List<String> DEFAULT_MOVE_CONTAINERS_PERMISSIONS   = Collections.singletonList(EVERYONE);

  protected String                 id;

  protected String                 name;

  protected String                 icon;

  protected String                 template;

  protected String                 factoryId;

  protected String                 title;

  protected String                 description;

  protected String                 width;

  protected String                 height;

  // Here to please jibx binding but not used anymore
  protected String                 decorator;

  protected String[]               accessPermissions;

  protected String[]               moveAppsPermissions;

  protected String[]               moveContainersPermissions;

  protected ArrayList<ModelObject> children;

  public Container() {
    setDefaultPermissions();
    children = new ArrayList<ModelObject>();
  }

  public Container(String storageId) {
    super(storageId);
    setDefaultPermissions();
    //
    this.children = new ArrayList<ModelObject>();
  }

  public Container(ContainerData data) {
    super(data.getStorageId());

    //
    ArrayList<ModelObject> children = new ArrayList<ModelObject>();
    for (ComponentData child : data.getChildren()) {
      ModelObject m = ModelObject.build(child);
      if (m != null) {
        children.add(ModelObject.build(child));
      }
    }

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
    this.accessPermissions = data.getAccessPermissions().toArray(new String[data.getAccessPermissions().size()]);
    List<String> permisssions = data.getMoveAppsPermissions();
    this.moveAppsPermissions = permisssions != null ? permisssions.toArray(new String[permisssions.size()]) : null;
    permisssions = data.getMoveContainersPermissions();
    this.moveContainersPermissions = permisssions != null ? permisssions.toArray(new String[permisssions.size()]) : null;
    this.children = children;
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

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }

  public String getWidth() {
    return width;
  }

  public void setWidth(String width) {
    this.width = width;
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

  public String getDecorator() {
    // Here to please jibx binding but not used anymore
    return null;
  }

  // Here to please jibx binding but not used anymore
  public void setDecorator(String decorator) {
    // Here to please jibx binding but not used anymore
  }

  @Override
  public ContainerData build() {
    List<ComponentData> children = buildChildren();
    return new ContainerData(storageId,
                             id,
                             name,
                             icon,
                             template,
                             factoryId,
                             title,
                             description,
                             width,
                             height,
                             Utils.safeImmutableList(accessPermissions),
                             Utils.safeImmutableList(moveAppsPermissions),
                             Utils.safeImmutableList(moveContainersPermissions),
                             children);
  }

  @Override
  public void resetStorage() {
    super.resetStorage();
    if (children != null && !children.isEmpty()) {
      for (ModelObject child : children) {
        child.resetStorage();
      }
    }
  }

  @Override
  public Container clone() {
    try {
      return (Container) super.clone();
    } catch (CloneNotSupportedException e) {
      return new Container(build());
    }
  }

  protected List<ComponentData> buildChildren() {
    if (children != null && children.size() > 0) {
      ArrayList<ComponentData> dataChildren = new ArrayList<ComponentData>(children.size());
      for (int i = 0; i < children.size(); i++) {
        ModelObject node = children.get(i);
        ModelData data = node.build();
        dataChildren.add((ComponentData) data);
      }
      return Collections.unmodifiableList(dataChildren);
    } else {
      return Collections.emptyList();
    }
  }
}
