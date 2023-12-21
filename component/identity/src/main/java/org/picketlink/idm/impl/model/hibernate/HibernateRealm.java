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
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Entity(name = "HibernateRealm")
@Table(name = "jbid_realm")
@NamedQuery(
    name = "HibernateRealm.findRealmByName",
    query = "SELECT o FROM HibernateRealm o WHERE o.name = :name"
)
@Data
public class HibernateRealm {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator="JBID_REALM_ID_SEQ")
  @SequenceGenerator(name = "JBID_REALM_ID_SEQ", sequenceName = "JBID_REALM_ID_SEQ", allocationSize = 1)
  @Column(name = "ID")
  private Long                id;

  @Column(name = "NAME", nullable = false)
  private String              name;

  @ElementCollection(fetch = FetchType.LAZY)
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "jbid_real_props", joinColumns = { @JoinColumn(name = "PROP_ID", referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  private Map<String, String> properties = new HashMap<>();

  public HibernateRealm() {
  }

  public HibernateRealm(String name) {
    this.name = name;
  }

}
