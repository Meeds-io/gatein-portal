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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "HibernateIdentityObjectRelationshipName")
@Table(name = "jbid_io_rel_name")
@NamedQuery(
    name = "HibernateIdentityObjectRelationshipName.findIdentityObjectRelationshipNameByName",
    query = "select rn from HibernateIdentityObjectRelationshipName rn where rn.name like :name and rn.realm.name = :realmName")
public class HibernateIdentityObjectRelationshipName {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator="JBID_IO_REL_NAME_ID_SEQ")
  @SequenceGenerator(name = "JBID_IO_REL_NAME_ID_SEQ", sequenceName = "JBID_IO_REL_NAME_ID_SEQ", allocationSize = 1)
  @Column(name = "ID")
  private Long                id;

  @Column(name = "NAME",
      nullable = false)
  private String              name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REALM",
      nullable = false)
  @Fetch(FetchMode.SELECT)
  private HibernateRealm      realm;

  @ElementCollection(fetch = FetchType.LAZY)
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "jbid_io_rel_name_props",
      joinColumns = { @JoinColumn(name = "PROP_ID",
          referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  private Map<String, String> properties                               = new HashMap<>();

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
