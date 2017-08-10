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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import java.io.Serializable;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.portal.mop.SiteType;

@Entity
@ExoEntity
@Table(name = "PORTAL_NAVIGATIONS")
@NamedQueries({
  @NamedQuery(name = "NavigationEntity.findByOwner", query = "SELECT nav FROM NavigationEntity nav INNER JOIN nav.owner s WHERE s.siteType = :ownerType AND s.name = :ownerId") })
public class NavigationEntity implements Serializable {

  private static final long serialVersionUID = 3811683620903785319L;

  @Id
  @Column(name = "NAVIGATION_ID", length = 200)
  private String             id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "SITE_ID")
  private SiteEntity owner;

  @Column(name = "PRIORITY")
  private int               priority = 1;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
  @JoinColumn(name = "NODE_ID")
  private NodeEntity        rootNode;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SiteEntity getOwner() {
    return owner;
  }

  public void setOwner(SiteEntity owner) {
    this.owner = owner;
  }
  
  public SiteType getOwnerType() {
    if (getOwner() != null) {
      return getOwner().getSiteType();
    } else {
      return null;
    }
  }
  
  public String getOwnerId() {
    if (getOwner() != null) {
      return getOwner().getName();
    } else {
      return null;
    }
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public NodeEntity getRootNode() {
    return rootNode;
  }

  public void setRootNode(NodeEntity rootNode) {
    this.rootNode = rootNode;
  }

}
