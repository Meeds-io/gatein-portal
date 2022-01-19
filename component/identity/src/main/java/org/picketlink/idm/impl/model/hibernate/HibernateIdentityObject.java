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

import java.util.*;

import javax.persistence.*;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.picketlink.idm.common.exception.PolicyValidationException;
import org.picketlink.idm.spi.model.IdentityObject;
import org.picketlink.idm.spi.model.IdentityObjectCredentialType;

@Entity(name = "HibernateIdentityObject")
@Table(name = "jbid_io")
@NamedQueries(
  {
      @NamedQuery(
          name = "HibernateIdentityObject.findIdentityObjectByNameAndTypeIgnoreCase",
          query = "SELECT o FROM HibernateIdentityObject o WHERE o.realm.name = :realmName AND lower(o.name) = :name AND o.identityType.name = :typeName"
      ),
      @NamedQuery(
          name = "HibernateIdentityObject.findIdentityObjectByNameAndType",
          query = "SELECT o FROM HibernateIdentityObject o WHERE o.realm.name = :realmName AND o.name = :name AND o.identityType.name = :typeName"
      ),
      @NamedQuery(
          name = "HibernateIdentityObject.countIdentityObjectByNameAndTypeIgnoreCase",
          query = "SELECT count(o) FROM HibernateIdentityObject o WHERE o.realm.name = :realmName AND lower(o.name) = :name AND o.identityType.name = :typeName"
      ),
      @NamedQuery(
          name = "HibernateIdentityObject.countIdentityObjectByNameAndType",
          query = "SELECT count(o) FROM HibernateIdentityObject o"
              + " WHERE o.realm.name = :realmName"
              + " AND o.name = :name"
              + " AND o.identityType.name = :typeName"
      ),
      @NamedQuery(
          name = "HibernateIdentityObject.countIdentityObjectsByType",
          query = "SELECT count(o.id) FROM HibernateIdentityObject o"
              + " WHERE o.realm.name = :realmName"
              + " AND o.identityType.name = :typeName"
      ),
  }
)
public class HibernateIdentityObject implements IdentityObject {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "ID")
  private Long                                     id;

  @Column(name = "NAME", unique = true)
  private String                                   name;

  @ManyToOne(fetch = FetchType.EAGER)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "IDENTITY_TYPE", nullable = false)
  private HibernateIdentityObjectType              identityType;

  @OneToMany(orphanRemoval = true, cascade = { CascadeType.ALL }, mappedBy = "fromIdentityObject")
  @Fetch(FetchMode.SUBSELECT)
  @LazyCollection(LazyCollectionOption.EXTRA)
  private Set<HibernateIdentityObjectRelationship> fromRelationships =
                                                                     new HashSet<HibernateIdentityObjectRelationship>();

  @OneToMany(orphanRemoval = true, cascade = { CascadeType.ALL }, mappedBy = "toIdentityObject")
  @Fetch(FetchMode.SUBSELECT)
  @LazyCollection(LazyCollectionOption.EXTRA)
  private Set<HibernateIdentityObjectRelationship> toRelationships   =
                                                                   new HashSet<HibernateIdentityObjectRelationship>();

  @OneToMany(orphanRemoval = true, cascade = { CascadeType.ALL }, mappedBy = "identityObject")
  @Fetch(FetchMode.SUBSELECT)
  @LazyCollection(LazyCollectionOption.EXTRA)
  private Set<HibernateIdentityObjectAttribute>    attributes        =
                                                              new HashSet<HibernateIdentityObjectAttribute>();

  @ElementCollection
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "jbid_io_props", joinColumns = { @JoinColumn(name = "PROP_ID", referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  @LazyCollection(LazyCollectionOption.EXTRA)
  private Map<String, String>                      properties        = new HashMap<String, String>();

  @OneToMany(
      orphanRemoval = true,
      cascade = CascadeType.REMOVE,
      fetch = FetchType.LAZY,
      targetEntity = HibernateIdentityObjectCredential.class
  )
  @JoinColumn(name = "IDENTITY_OBJECT_ID")
  @Fetch(FetchMode.SUBSELECT)
  @LazyCollection(LazyCollectionOption.EXTRA)
  private Set<HibernateIdentityObjectCredential>   credentials       =
                                                               new HashSet<HibernateIdentityObjectCredential>();

  @ManyToOne(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SELECT)
  @JoinColumn(name = "REALM", nullable = false)
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public HibernateIdentityObjectType getIdentityType() {
    return identityType;
  }

  public void setIdentityType(HibernateIdentityObjectType identityType) {
    this.identityType = identityType;
  }

  public String getFQDN() {
    return null;
  }

  public Set<HibernateIdentityObjectAttribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(Set<HibernateIdentityObjectAttribute> attributes) {
    this.attributes = attributes;
  }

  public void addAttribute(HibernateIdentityObjectAttribute attribute) {
    attribute.setIdentityObject(this);
    this.attributes.add(attribute);
  }

  public Map<String, Collection> getAttributesAsMap() {
    Map<String, Collection> map = new HashMap<String, Collection>();

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
    Set<String> vals = new HashSet<String>(list);
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

  public Set<HibernateIdentityObjectRelationship> getFromRelationships() {
    return fromRelationships;
  }

  public void setFromRelationships(Set<HibernateIdentityObjectRelationship> fromRelationships) {
    this.fromRelationships = fromRelationships;
  }

  public void addFromRelationship(HibernateIdentityObjectRelationship fromRelationship) {
    fromRelationship.setFromIdentityObject(this);
    fromRelationships.add(fromRelationship);
  }

  public Set<HibernateIdentityObjectRelationship> getToRelationships() {
    return toRelationships;
  }

  public void setToRelationships(Set<HibernateIdentityObjectRelationship> toRelationships) {
    this.toRelationships = toRelationships;
  }

  public void addToRelationship(HibernateIdentityObjectRelationship toRelationship) {
    toRelationship.setToIdentityObject(this);
    fromRelationships.add(toRelationship);
  }

  public HibernateRealm getRealm() {
    return realm;
  }

  public void setRealm(HibernateRealm realm) {
    this.realm = realm;
  }

  public Set<HibernateIdentityObjectCredential> getCredentials() {
    return credentials;
  }

  public void setCredentials(Set<HibernateIdentityObjectCredential> credentials) {
    this.credentials = credentials;
  }

  public void addCredential(HibernateIdentityObjectCredential credential) {
    credential.setIdentityObject(this);
    credentials.add(credential);
  }

  public boolean hasCredentials() {
    if (credentials != null && credentials.size() > 0) {
      return true;
    }
    return false;
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

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public void validatePolicy() throws PolicyValidationException {

  }

  @Override
  public String toString() {
    return "IdentityObject[id=" + getId() + "; name=" + getName() + "; type=" + getIdentityType().getName() + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IdentityObject)) {
      return false;
    }

    IdentityObject that = (IdentityObject) o;

    if (name != null ? !name.equals(that.getName()) : that.getName() != null) {
      return false;
    }
    if (identityType != null ? !identityType.equals(that.getIdentityType()) : that.getIdentityType() != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (identityType != null ? identityType.hashCode() : 0);
    return result;
  }
}
