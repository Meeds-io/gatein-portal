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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.*;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.picketlink.idm.spi.model.IdentityObjectRelationship;
import org.picketlink.idm.spi.model.IdentityObjectRelationshipType;

@Entity(name = "HibernateIdentityObjectRelationship")
@Table(name = "JBID_IO_REL")
@NamedQueries(
  {
      @NamedQuery(
          name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipWithoutName",
          query = "SELECT r FROM HibernateIdentityObjectRelationship r"
              + " WHERE r.type.id = :typeId"
              + " AND r.fromIdentityObject.id = :fromIdentityObjectId"
              + " AND r.toIdentityObject.id = :toIdentityObjectId"
      ),
      @NamedQuery(
          name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByAttributes",
          query = "SELECT r FROM HibernateIdentityObjectRelationship r"
              + " WHERE r.type.id = :typeId"
              + " AND r.name.name = :name"
              + " AND r.fromIdentityObject.id = :fromIdentityObjectId"
              + " AND r.toIdentityObject.id = :toIdentityObjectId"
      ),
      @NamedQuery(
          name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipsByIdentities",
          query = "SELECT r FROM HibernateIdentityObjectRelationship r"
              + " WHERE"
              + " ("
              + "   r.fromIdentityObject.id = :identityId1"
              + "     AND"
              + "   r.toIdentityObject.id = :identityId2"
              + " ) OR ("
              + "   r.fromIdentityObject.id = :identityId2"
              + "     AND"
              + "   r.toIdentityObject.id = :identityId1"
              + " ) "
      ),
      @NamedQuery(
          name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByIdentityByType",
          query = "SELECT r FROM HibernateIdentityObjectRelationship r"
              + " WHERE r.type.name = :typeName"
              + " AND r.fromIdentityObject.id = :fromIdentityObjectId"
              + " AND r.toIdentityObject.id = :toIdentityObjectId"
      ),
      @NamedQuery(
          name = "HibernateIdentityObjectRelationship.findIdentityObjectRelationshipByIdentity",
          query = "SELECT r FROM HibernateIdentityObjectRelationship r"
              + " AND r.fromIdentityObject.id = :fromIdentityObjectId"
              + " AND r.toIdentityObject.id = :toIdentityObjectId"
      ),
  }
)
public class HibernateIdentityObjectRelationship implements IdentityObjectRelationship {
  public static final String                      findIdentityObjectRelationshipsByType     =
                                                                                        "select r from HibernateIdentityObjectRelationship r where r.type.name = :typeName";

  public static final String                      findIdentityObjectRelationshipNamesByType =
                                                                                            "select r.name from HibernateIdentityObjectRelationship r where r.type.name = :typeName";

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "ID")
  private Long                                    id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "NAME")
  @Fetch(FetchMode.JOIN)
  @LazyToOne(LazyToOneOption.PROXY)
  private HibernateIdentityObjectRelationshipName name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REL_TYPE", nullable = false)
  @Fetch(FetchMode.SELECT)
  @LazyToOne(LazyToOneOption.PROXY)
  private HibernateIdentityObjectRelationshipType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "FROM_IDENTITY", nullable = false)
  @Fetch(FetchMode.SELECT)
  @LazyToOne(LazyToOneOption.PROXY)
  private HibernateIdentityObject                 fromIdentityObject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_IDENTITY", nullable = false)
  @Fetch(FetchMode.SELECT)
  @LazyToOne(LazyToOneOption.PROXY)
  private HibernateIdentityObject                 toIdentityObject;

  @ElementCollection
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "JBID_IO_REL_PROPS", joinColumns = { @JoinColumn(name = "PROP_ID", referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  @LazyCollection(LazyCollectionOption.EXTRA)
  private Map<String, String>                     properties                                = new HashMap<String, String>();

  public HibernateIdentityObjectRelationship() {
  }

  public HibernateIdentityObjectRelationship(HibernateIdentityObjectRelationshipType type,
                                             HibernateIdentityObject fromIdentityObject,
                                             HibernateIdentityObject toIdentityObject) {
    this.type = type;
    this.fromIdentityObject = fromIdentityObject;
    fromIdentityObject.getFromRelationships().add(this);
    this.toIdentityObject = toIdentityObject;
    toIdentityObject.getToRelationships().add(this);
  }

  public HibernateIdentityObjectRelationship(HibernateIdentityObjectRelationshipType type,
                                             HibernateIdentityObject fromIdentityObject,
                                             HibernateIdentityObject toIdentityObject,
                                             HibernateIdentityObjectRelationshipName name) {
    this.type = type;
    this.fromIdentityObject = fromIdentityObject;
    fromIdentityObject.getFromRelationships().add(this);
    this.toIdentityObject = toIdentityObject;
    toIdentityObject.getToRelationships().add(this);
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IdentityObjectRelationshipType getType() {
    return type;
  }

  public void setType(HibernateIdentityObjectRelationshipType type) {
    this.type = type;
  }

  public HibernateIdentityObject getFromIdentityObject() {
    return fromIdentityObject;
  }

  public void setFromIdentityObject(HibernateIdentityObject fromIdentityObject) {
    this.fromIdentityObject = fromIdentityObject;
  }

  public HibernateIdentityObject getToIdentityObject() {
    return toIdentityObject;
  }

  public void setToIdentityObject(HibernateIdentityObject toIdentityObject) {
    this.toIdentityObject = toIdentityObject;
  }

  public String getName() {
    if (name != null) {
      return name.getName();
    }
    return null;
  }

  public void setName(HibernateIdentityObjectRelationshipName name) {
    this.name = name;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
