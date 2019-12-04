/*
 * Copyright (C) 2016 eXo Platform SAS.
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
package org.exoplatform.portal.jdbc.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity
@ExoEntity
@Table(name = "PORTAL_PERMISSIONS")
@NamedQueries({
  @NamedQuery(name = "PermissionEntity.deleteByRefId", query = "DELETE PermissionEntity p WHERE p.referenceId = :refId"),
  @NamedQuery(name = "PermissionEntity.getPermissions", query = "SELECT p FROM PermissionEntity p WHERE p.referenceId = :refId AND type = :type") })
public class PermissionEntity implements Serializable {

  private static final long serialVersionUID = 1173817577220348267L;

  @Id
  @SequenceGenerator(name = "SEQ_PORTAL_PERMISSIONS_ID", sequenceName = "SEQ_PORTAL_PERMISSIONS_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_PORTAL_PERMISSIONS_ID")
  @Column(name = "PERMISSION_ID")
  private Long              id;

  @Column(name = "REF_ID", length = 200)
  private String            referenceId;

  @Column(name = "PERMISSION", length = 200)
  private String            permission;

  @Column(name = "TYPE")
  private TYPE              type;

  public PermissionEntity() {
  }

  public PermissionEntity(String referenceId, String permission, TYPE type) {
    this.referenceId = referenceId;
    this.permission = permission;
    this.type = type;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public TYPE getType() {
    return type;
  }

  public void setType(TYPE type) {
    this.type = type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((permission == null) ? 0 : permission.hashCode());
    result = prime * result + ((referenceId == null) ? 0 : referenceId.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
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
    PermissionEntity other = (PermissionEntity) obj;
    if (permission == null) {
      if (other.permission != null)
        return false;
    } else if (!permission.equals(other.permission))
      return false;
    if (referenceId == null) {
      if (other.referenceId != null)
        return false;
    } else if (!referenceId.equals(other.referenceId))
      return false;
    if (type != other.type)
      return false;
    return true;
  }

  public static enum TYPE {
    ACCESS, EDIT, MOVE_APP, MOVE_CONTAINER
  }
}
