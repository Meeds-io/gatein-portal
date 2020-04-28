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
package org.exoplatform.application.registry.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "GateInApplicationCategory")
@ExoEntity
@Table(name = "PORTAL_APP_CATEGORIES")
@NamedQueries({
  @NamedQuery(name = "CategoryEntity.findByName", query = "SELECT cat FROM GateInApplicationCategory cat WHERE cat.name = :name")})
public class CategoryEntity implements Serializable {

  private static final long      serialVersionUID = 8772040309317091459L;

  @Id
  @SequenceGenerator(name="SEQ_GTN_APPLICATION_CAT_ID", sequenceName="SEQ_GTN_APPLICATION_CAT_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_GTN_APPLICATION_CAT_ID")
  @Column(name = "ID")
  private Long                 id;

  @Column(name = "NAME", length = 200)
  private String                 name;

  @Column(name = "DISPLAY_NAME", length = 200)
  private String                 displayName;

  @Column(name = "DESCRIPTION", length = 2000)
  private String                 description;

  @Column(name = "CREATED_DATE")
  private long                   createdDate      = System.currentTimeMillis();

  @Column(name = "MODIFIED_DATE")
  private long                   modifiedDate;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ApplicationEntity> applications     = new HashSet<ApplicationEntity>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(long createdDate) {
    this.createdDate = createdDate;
  }

  public long getModifiedDate() {
    return modifiedDate;
  }

  public void setModifiedDate(long modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  public Set<ApplicationEntity> getApplications() {
    return applications;
  }

  public void setApplications(Set<ApplicationEntity> applications) {
    this.applications = applications;
  }

}
