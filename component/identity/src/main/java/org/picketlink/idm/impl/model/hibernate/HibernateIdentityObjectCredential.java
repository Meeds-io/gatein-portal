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
import org.picketlink.idm.spi.model.IdentityObjectCredential;

import org.exoplatform.commons.api.persistence.ExoEntity;

@ExoEntity
@Entity(name = "HibernateIdentityObjectCredential")
@Table(name = "JBID_IO_CREDEN")
public class HibernateIdentityObjectCredential implements IdentityObjectCredential {

  public static final String                           findIdentityObjectCredentialByIOAndTypeNAme =
                                                                                                   "select c from HibernateIdentityObjectCredential c where c.type.name like :typeName and c.identityObject = :identityObject";

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "ID")
  private Long                                         id;

  @ManyToOne(fetch = FetchType.EAGER)
  @Fetch(FetchMode.JOIN)
  @JoinColumn(name = "CREDENTIAL_TYPE", nullable = false)
  private HibernateIdentityObjectCredentialType        type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "IDENTITY_OBJECT_ID", nullable = false)
  @Fetch(FetchMode.SELECT)
  @LazyToOne(LazyToOneOption.PROXY)
  private HibernateIdentityObject                      identityObject;

  @Column(name = "TEXT")
  private String                                       textValue;

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @Fetch(FetchMode.SELECT)
  @JoinColumn(name = "BIN_VALUE_ID")
  @LazyToOne(LazyToOneOption.PROXY)
  private HibernateIdentityObjectCredentialBinaryValue binaryValue;

  @ElementCollection
  @MapKeyColumn(name = "PROP_NAME")
  @Column(name = "PROP_VALUE")
  @CollectionTable(name = "JBID_IO_CREDEN_PROPS", joinColumns = { @JoinColumn(name = "PROP_ID", referencedColumnName = "ID") })
  @Fetch(FetchMode.SUBSELECT)
  @LazyCollection(LazyCollectionOption.EXTRA)
  private Map<String, String>                          properties                                  =
                                                                  new HashMap<String, String>();

  public HibernateIdentityObjectCredential() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTextValue() {
    return textValue;
  }

  public void setTextValue(String textValue) {
    this.textValue = textValue;
  }

  public HibernateIdentityObjectCredentialBinaryValue getBinaryValue() {
    return binaryValue;
  }

  public void setBinaryValue(HibernateIdentityObjectCredentialBinaryValue binaryValue) {
    this.binaryValue = binaryValue;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public HibernateIdentityObjectCredentialType getType() {
    return type;
  }

  public void setType(HibernateIdentityObjectCredentialType type) {
    this.type = type;
  }

  public HibernateIdentityObject getIdentityObject() {
    return identityObject;
  }

  public void setIdentityObject(HibernateIdentityObject identityObject) {
    this.identityObject = identityObject;
  }

  public Object getValue() {
    if (textValue != null) {
      return textValue;
    }
    return binaryValue.getValue();
  }

  public Object getEncodedValue() {
    return null;
  }
}
