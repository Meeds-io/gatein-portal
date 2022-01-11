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

import org.exoplatform.commons.api.persistence.ExoEntity;

@ExoEntity
@Entity(name = "HibernateIdentityObjectRelationshipName")
@Table(name = "JBID_IO_REL_NAME")
public class HibernateIdentityObjectRelationshipName {

  public static final String  findIdentityObjectRelationshipNameByName                             =
                                                                       "select rn from HibernateIdentityObjectRelationshipName rn where rn.name like :name and rn.realm.name = :realmName";

  public static final String  findIdentityObjectRelationshipNames                                  =
                                                                  "select rn.name from HibernateIdentityObjectRelationshipName rn where rn.name like :nameFilter and rn.realm.name = :realmName";

  public static final String  findIdentityObjectRelationshipNamesOrderedByNameAsc                  =
                                                                                  "select rn.name from HibernateIdentityObjectRelationshipName rn where rn.name like :nameFilter and rn.realm.name = :realmName "
                                                                                      +
                                                                                      "order by rn.name asc";

  public static final String  findIdentityObjectRelationshipNamesOrderedByNameDesc                 =
                                                                                   "select rn.name from HibernateIdentityObjectRelationshipName rn where rn.name like :nameFilter and rn.realm.name = :realmName "
                                                                                       +
                                                                                       "order by rn.name desc";

  public static final String  findIdentityObjectRelationshipNamesForIdentityObject                 =
                                                                                   "select r.name.name from HibernateIdentityObjectRelationship r where "
                                                                                       +
                                                                                       "r.fromIdentityObject = :identityObject or r.toIdentityObject = :identityObject";

  public static final String  findIdentityObjectRelationshipNamesForIdentityObjectOrderedByNameAsc =
                                                                                                   "select r.name.name from HibernateIdentityObjectRelationship r where "
                                                                                                       +
                                                                                                       "r.fromIdentityObject = :identityObject or r.toIdentityObject = :identityObject "
                                                                                                       +
                                                                                                       "order by r.name.name asc";

  public static final String  findIdentityObjectRelationshipNamesForIdentityObjectOrdereByNameDesc =
                                                                                                   "select r.name.name from HibernateIdentityObjectRelationship r where "
                                                                                                       +
                                                                                                       "r.fromIdentityObject = :identityObject or r.toIdentityObject = :identityObject "
                                                                                                       +
                                                                                                       "order by r.name.name desc";

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "ID")
  private Long                id;

  @Column(name = "NAME", nullable = false)
  private String              name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REALM", nullable = false)
  @Fetch(FetchMode.SELECT)
  @LazyToOne(LazyToOneOption.PROXY)
  private HibernateRealm      realm;

  @ElementCollection
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "JBID_IO_REL_NAME_PROPS", joinColumns = { @JoinColumn(name = "PROP_ID", referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  @LazyCollection(LazyCollectionOption.EXTRA)
  private Map<String, String> properties                                                           =
                                         new HashMap<String, String>();

  public HibernateIdentityObjectRelationshipName() {
  }

  public HibernateIdentityObjectRelationshipName(String name, HibernateRealm realm) {
    this.name = name;
    this.realm = realm;
  }

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

  public HibernateRealm getRealm() {
    return realm;
  }

  public void setRealm(HibernateRealm realm) {
    this.realm = realm;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
