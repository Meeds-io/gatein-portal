/*
* JBoss, a division of Red Hat
* Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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

package org.picketlink.idm.impl.model.hibernate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.picketlink.idm.common.exception.PolicyValidationException;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectCredentialType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Entity(name = "HibernateIdentityObject")
@Table(name = "jbid_io")
@NamedQuery(
    name = "HibernateIdentityObject.findIdentityObjectByNameAndTypeIgnoreCase",
    query = "SELECT o FROM HibernateIdentityObject o WHERE o.realm.name = :realmName AND lower(o.name) = :name AND o.identityType.name = :typeName")
@NamedQuery(
    name = "HibernateIdentityObject.findIdentityObjectByNameAndType",
    query = "SELECT o FROM HibernateIdentityObject o WHERE o.realm.name = :realmName AND o.name = :name AND o.identityType.name = :typeName")
@NamedQuery(
    name = "HibernateIdentityObject.countIdentityObjectByNameAndTypeIgnoreCase",
    query = "SELECT count(o) FROM HibernateIdentityObject o WHERE o.realm.name = :realmName AND lower(o.name) = :name AND o.identityType.name = :typeName")
@NamedQuery(
    name = "HibernateIdentityObject.countIdentityObjectByNameAndType",
    query = "SELECT count(o) FROM HibernateIdentityObject o" + " WHERE o.realm.name = :realmName" +
        " AND o.name = :name" + " AND o.identityType.name = :typeName")
@NamedQuery(
    name = "HibernateIdentityObject.countIdentityObjectsByType",
    query = "SELECT count(o.id) FROM HibernateIdentityObject o" + " WHERE o.realm.name = :realmName" +
        " AND o.identityType.name = :typeName")
@Data
public class HibernateIdentityObject implements IdentityObject {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator="JBID_IO_ID_SEQ")
  @SequenceGenerator(name = "JBID_IO_ID_SEQ", sequenceName = "JBID_IO_ID_SEQ", allocationSize = 1)
  @Column(name = "ID")
  private Long                                     id;

  @Column(name = "NAME",
      unique = true)
  private String                                   name;

  @ManyToOne(fetch = FetchType.EAGER)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "IDENTITY_TYPE",
      nullable = false)
  private HibernateIdentityObjectType              identityType;

  @OneToMany(orphanRemoval = true,
      cascade = { CascadeType.ALL },
      mappedBy = "fromIdentityObject",
      fetch = FetchType.LAZY)
  @Fetch(FetchMode.SUBSELECT)
  private Set<HibernateIdentityObjectRelationship> fromRelationships = new HashSet<>();

  @OneToMany(orphanRemoval = true,
      cascade = { CascadeType.ALL },
      mappedBy = "toIdentityObject",
      fetch = FetchType.LAZY)
  @Fetch(FetchMode.SUBSELECT)
  private Set<HibernateIdentityObjectRelationship> toRelationships   = new HashSet<>();

  @OneToMany(orphanRemoval = true,
      cascade = { CascadeType.ALL },
      mappedBy = "identityObject",
      fetch = FetchType.LAZY)
  @Fetch(FetchMode.SUBSELECT)
  private Set<HibernateIdentityObjectAttribute>    attributes        = new HashSet<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "jbid_io_props",
      joinColumns = { @JoinColumn(name = "PROP_ID",
          referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  private Map<String, String>                      properties        = new HashMap<>();

  @OneToMany(
      orphanRemoval = true,
      cascade = CascadeType.REMOVE,
      fetch = FetchType.LAZY,
      targetEntity = HibernateIdentityObjectCredential.class)
  @JoinColumn(name = "IDENTITY_OBJECT_ID")
  @Fetch(FetchMode.SUBSELECT)
  private Set<HibernateIdentityObjectCredential>   credentials       = new HashSet<>();

  @ManyToOne(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SELECT)
  @JoinColumn(name = "REALM",
      nullable = false)
  private HibernateRealm                           realm;

  public HibernateIdentityObject() {
  }

  public HibernateIdentityObject(String name, HibernateIdentityObjectType identityType, HibernateRealm realm) {
    this.name = name;
    this.identityType = identityType;
    this.realm = realm;
  }

  public String getId() {
    return id.toString();
  }

  public Long getIdLong() {
    return id;
  }

  public void setId(String id) {
    this.id = Long.parseLong(id);
  }

  public void addAttribute(HibernateIdentityObjectAttribute attribute) {
    attribute.setIdentityObject(this);
    this.attributes.add(attribute);
  }

  public Map<String, Collection> getAttributesAsMap() {
    Map<String, Collection> map = new HashMap<>();
    for (HibernateIdentityObjectAttribute attribute : attributes) {
      Collection values = attribute.getValues();
      map.put(attribute.getName(), values);
    }
    return Collections.unmodifiableMap(map);
  }

  public void addTextAttribute(String name, String[] values) {
    HibernateIdentityObjectAttribute attr = new HibernateIdentityObjectAttribute(this,
                                                                                 name,
                                                                                 HibernateIdentityObjectAttribute.TYPE_TEXT);
    List<String> list = Arrays.asList(values);
    Set<String> vals = new HashSet<>(list);
    attr.setTextValues(vals);
    attributes.add(attr);
  }

  public void removeAttribute(String name) {
    HibernateIdentityObjectAttribute attributeToRemove = null;

    for (HibernateIdentityObjectAttribute attribute : attributes) {
      if (attribute.getName().equals(name)) {
        attributeToRemove = attribute;
        break;
      }
    }

    if (attributeToRemove != null) {
      attributes.remove(attributeToRemove);
    }
  }

  public void addFromRelationship(HibernateIdentityObjectRelationship fromRelationship) {
    fromRelationship.setFromIdentityObject(this);
    fromRelationships.add(fromRelationship);
  }

  public void addToRelationship(HibernateIdentityObjectRelationship toRelationship) {
    toRelationship.setToIdentityObject(this);
    fromRelationships.add(toRelationship);
  }

  public void addCredential(HibernateIdentityObjectCredential credential) {
    credential.setIdentityObject(this);
    credentials.add(credential);
  }

  public boolean hasCredentials() {
    return credentials != null && !credentials.isEmpty();
  }

  public boolean hasCredential(IdentityObjectCredentialType type) {
    if (credentials != null) {
      for (HibernateIdentityObjectCredential credential : credentials) {
        if (credential.getType().getName().equals(type.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  public HibernateIdentityObjectCredential getCredential(IdentityObjectCredentialType type) {
    if (credentials != null) {
      for (HibernateIdentityObjectCredential credential : credentials) {
        if (credential.getType().getName().equals(type.getName())) {
          return credential;
        }
      }
    }
    return null;
  }

  public void validatePolicy() throws PolicyValidationException {
    // No validation
  }

}
