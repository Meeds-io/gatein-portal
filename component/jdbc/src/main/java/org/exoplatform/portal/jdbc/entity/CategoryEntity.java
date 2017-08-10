package org.exoplatform.portal.jdbc.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity
@ExoEntity
@Table(name = "PORTAL_APP_CATEGORIES")
@NamedQueries({
  @NamedQuery(name = "CategoryEntity.findByName", query = "SELECT cat FROM CategoryEntity cat WHERE cat.name = :name")})
public class CategoryEntity implements Serializable {

  private static final long      serialVersionUID = 8772040309317091459L;

  @Id
  @Column(name = "ID", length = 200)
  private String                 id;

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

  public String getId() {
    return id;
  }

  public void setId(String id) {
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
