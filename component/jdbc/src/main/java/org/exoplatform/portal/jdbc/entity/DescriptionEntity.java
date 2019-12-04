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
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity
@ExoEntity
@Table(name = "PORTAL_DESCRIPTIONS")
@NamedQueries({
  @NamedQuery(name = "DescriptionEntity.getByRefId", query = "SELECT d FROM DescriptionEntity d WHERE d.referenceId = :refId") })
public class DescriptionEntity implements Serializable {

  private static final long serialVersionUID = 1173817577220348267L;

  @Id
  @SequenceGenerator(name = "SEQ_PORTAL_DESCRIPTIONS_ID", sequenceName = "SEQ_PORTAL_DESCRIPTIONS_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_PORTAL_DESCRIPTIONS_ID")
  @Column(name = "DESCRIPTION_ID")
  private Long              id;

  @Column(name = "REF_ID", length = 200)
  private String            referenceId;

  @Embedded
  private DescriptionState state;
  
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name="PORTAL_DESCRIPTION_LOCALIZED", joinColumns = @JoinColumn(name = "DESCRIPTION_ID"))
  @MapKeyColumn(name="LOCALE")
  private Map<String, DescriptionState> localized = new HashMap<String, DescriptionState>();
  
  public DescriptionEntity() {
  }

  public DescriptionEntity(String referenceId) {
    this.referenceId = referenceId;
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

  public DescriptionState getState() {
    return state;
  }

  public void setState(DescriptionState state) {
    this.state = state;
  }

  public Map<String, DescriptionState> getLocalized() {
    return localized;
  }

  public void setLocalized(Map<String, DescriptionState> localized) {
    this.localized = localized;
  }
  
}
