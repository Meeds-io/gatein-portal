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
@Table(name = "PORTAL_SETTINGS")
@NamedQueries({ @NamedQuery(name = "SettingEntity.findByName", query = "SELECT s FROM SettingEntity s WHERE s.name = :name") })
public class SettingEntity implements Serializable {

  private static final long serialVersionUID = -44730129666361277L;

  @Id
  @SequenceGenerator(name = "SEQ_PORTAL_SETTINGS_ID", sequenceName = "SEQ_PORTAL_SETTINGS_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_PORTAL_SETTINGS_ID")
  @Column(name = "SETTING_ID")
  private Long              id;

  @Column(name = "NAME", length = 200)
  private String            name;

  @Column(name = "VALUE", length = 2000)
  private String            value;

  @Column(name = "CREATED_DATE")
  private long              createdDate      = System.currentTimeMillis();

  @Column(name = "MODIFIED_DATE")
  private long              modifiedDate     = System.currentTimeMillis();

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

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
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
}
