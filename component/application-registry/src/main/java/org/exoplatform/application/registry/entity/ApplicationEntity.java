/**
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
package org.exoplatform.application.registry.entity;

import java.io.Serializable;

import jakarta.persistence.*;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "GateInApplication")
@ExoEntity
@Table(name = "PORTAL_APPLICATIONS")
@NamedQueries({
    @NamedQuery(name = "ApplicationEntity.find", query = "SELECT app FROM GateInApplication app WHERE app.category.name = :catName AND app.applicationName = :name") })
public class ApplicationEntity implements Serializable {

  private static final long serialVersionUID = 4955770436068594917L;

  @Id
  @SequenceGenerator(name="SEQ_GTN_APPLICATION_ID", sequenceName="SEQ_GTN_APPLICATION_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_GTN_APPLICATION_ID")
  @Column(name = "ID")
  private Long            id;

  @Column(name = "DISPLAY_NAME", length = 200)
  private String            displayName;

  @Column(name = "DESCRIPTION", length = 2000)
  private String            description;

  @Column(name = "CREATED_DATE")
  private long              createdDate;

  @Column(name = "MODIFIED_DATE")
  private long              modifiedDate;

  @Column(name = "APP_NAME", length = 200)
  private String            applicationName;

  @Column(name = "TYPE", length = 50)
  private String            type;

  @Column(name = "CONTENT_ID", length = 200)
  private String            contentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CATEGORY_ID")
  private CategoryEntity    category;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getContentId() {
    return contentId;
  }

  public void setContentId(String contentId) {
    this.contentId = contentId;
  }

  public CategoryEntity getCategory() {
    return category;
  }

  public void setCategory(CategoryEntity category) {
    this.category = category;
  }

}
