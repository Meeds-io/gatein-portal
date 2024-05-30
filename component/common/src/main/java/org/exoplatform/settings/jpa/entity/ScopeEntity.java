package org.exoplatform.settings.jpa.entity;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 07, 2017
 */
@Entity(name = "SettingsScopeEntity")
@Table(name = "STG_SCOPES")
@NamedQueries({
    @NamedQuery(name = "SettingsScopeEntity.getScope", query = "SELECT s FROM SettingsScopeEntity s " +
        "WHERE s.name = :scopeName " +
        "AND s.type = :scopeType "),
    @NamedQuery(name = "SettingsScopeEntity.getScopeWithNullName", query = "SELECT s FROM SettingsScopeEntity s " +
        "WHERE s.name IS NULL " +
        "AND s.type = :scopeType ")
})
public class ScopeEntity {
  @Id
  @Column(name = "SCOPE_ID")
  @SequenceGenerator(name="SEQ_STG_SCOPE_COMMON_ID", sequenceName="SEQ_STG_SCOPE_COMMON_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_STG_SCOPE_COMMON_ID")
  private Long id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "TYPE")
  private String type;

  @OneToMany(fetch=FetchType.LAZY, mappedBy = "scope")
  private Set<SettingsEntity> settings;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public ScopeEntity setName(String name) {
    this.name = name;
    return this;
  }

  public String getType() {
    return type;
  }

  public ScopeEntity setType(String type) {
    this.type = type;
    return this;
  }

  public Set<SettingsEntity> getSettings() {
    return settings;
  }

  public void setSettings(Set<SettingsEntity> settings) {
    this.settings = settings;
  }
}
