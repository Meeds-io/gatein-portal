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

import jakarta.persistence.*;

import org.picketlink.idm.spi.model.IdentityObjectRelationshipType;

@Entity(name = "HibernateIdentityObjectRelationshipType")
@Table(name = "jbid_io_rel_type")
@NamedQuery(
    name = "HibernateIdentityObjectRelationshipType.findIdentityRelationshipTypeByName",
    query = "SELECT t FROM HibernateIdentityObjectRelationshipType t"
        + " WHERE t.name = :name"
)
public class HibernateIdentityObjectRelationshipType implements IdentityObjectRelationshipType {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator="JBID_IO_REL_TYPE_ID_SEQ")
  @SequenceGenerator(name = "JBID_IO_REL_TYPE_ID_SEQ", sequenceName = "JBID_IO_REL_TYPE_ID_SEQ", allocationSize = 1)
  @Column(name = "ID")
  private Long               id;

  @Column(name = "NAME", nullable = false)
  private String             name;

  public HibernateIdentityObjectRelationshipType() {
  }

  public HibernateIdentityObjectRelationshipType(String name) {
    this.name = name;
  }

  public HibernateIdentityObjectRelationshipType(IdentityObjectRelationshipType type) {
    if (type == null) {
      throw new IllegalArgumentException("type is null");
    }
    if (type.getName() != null) {
      this.name = type.getName();
    }
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
}
