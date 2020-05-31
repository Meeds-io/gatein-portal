package org.exoplatform.portal.rest.model;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.organization.Group;

public class GroupRestEntity {

  private String                id;

  private String                parentId;

  private String                groupName;

  private String                label;

  private String                description;

  private List<GroupRestEntity> children;

  public GroupRestEntity() {
  }

  public GroupRestEntity(Group group) {
    this.id = group.getId();
    this.parentId = group.getParentId();
    this.groupName = group.getGroupName();
    this.label = group.getLabel();
    this.description = group.getDescription();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
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

  public List<GroupRestEntity> getChildren() {
    return children;
  }

  public void setChildren(List<GroupRestEntity> children) {
    this.children = children;
  }

  public void addChild(GroupRestEntity child) {
    if (this.children == null) {
      this.children = new ArrayList<>();
    }
    if (!this.children.contains(child)) {
      this.children.add(child);
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    GroupRestEntity other = (GroupRestEntity) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

}
