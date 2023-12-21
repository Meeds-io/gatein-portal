package org.exoplatform.application.registry.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "GateInApplicationCategory")
@ExoEntity
@Table(name = "PORTAL_APP_CATEGORIES")
@NamedQueries({
  @NamedQuery(name = "CategoryEntity.findByName", query = "SELECT cat FROM GateInApplicationCategory cat WHERE cat.name = :name")})
public class CategoryEntity implements Serializable {

  private static final long      serialVersionUID = 8772040309317091459L;

  @Id
  @SequenceGenerator(name="SEQ_GTN_APPLICATION_CAT_ID", sequenceName="SEQ_GTN_APPLICATION_CAT_ID", allocationSize = 1)
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
