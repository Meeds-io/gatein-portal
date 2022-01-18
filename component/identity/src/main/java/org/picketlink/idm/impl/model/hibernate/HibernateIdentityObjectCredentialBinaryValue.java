/*
* JBoss, a division of Red Hat
* Copyright 2009, Red Hat Middleware, LLC, and individual contributors as indicated
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity(name = "HibernateIdentityObjectCredentialBinaryValue")
@Table(name = "jbid_creden_bin_value")
public class HibernateIdentityObjectCredentialBinaryValue {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "BIN_VALUE_ID")
  private Long   id;

  @Column(name = "VALUE", length = 10240000)
  @Type(type = "org.hibernate.type.BinaryType")
  private byte[] value = null;

  public HibernateIdentityObjectCredentialBinaryValue() {
  }

  public HibernateIdentityObjectCredentialBinaryValue(byte[] value) {
    this.value = value;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public byte[] getValue() {
    return value;
  }

  public void setValue(byte[] value) {
    this.value = value;
  }
}
