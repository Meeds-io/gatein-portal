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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.services.listener.ListenerService;

@Entity(name = "GateInNavigationNode")
@ExoEntity
@Table(name = "PORTAL_NAVIGATION_NODES")
@NamedQueries({
        @NamedQuery(name = "NodeEntity.findByPage", query = "SELECT n FROM GateInNavigationNode n INNER JOIN n.page p WHERE p.id = :pageId")
})
public class NodeEntity implements Serializable {

  private static final long serialVersionUID = 8630708630711337929L;

  @Id
  @Column(name = "NODE_ID", length = 200)
  private String             id;

  @Column(name = "NAME", length = 200)
  private String            name;

  @Column(name = "LABEL", length = 200)
  private String            label;

  @Column(name = "ICON", length = 200)
  private String            icon;

  @Column(name = "START_TIME")
  private long              startTime;

  @Column(name = "END_TIME")
  private long              endTime;

  @Column(name = "VISIBILITY")
  private Visibility        visibility       = Visibility.DISPLAYED;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "PAGE_ID", nullable = true)
  private PageEntity    page;

  @Column(name = "NODE_INDEX")
  private int               index;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "parent", orphanRemoval = true)
  @OrderBy("index ASC")
  private List<NodeEntity>  children         = new ArrayList<NodeEntity>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_ID")
  private NodeEntity        parent;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public Visibility getVisibility() {
    return visibility;
  }

  public void setVisibility(Visibility visibility) {
    this.visibility = visibility;
  }

  public PageEntity getPage() {
    return page;
  }

  public void setPage(PageEntity page) {
    this.page = page;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public List<NodeEntity> getChildren() {
    return children;
  }

  public void setChildren(List<NodeEntity> children) {
    if (children != null) {      
      for (int i = 0; i < children.size(); i++) {
        children.get(i).setIndex(i);
      }
    }
    this.children = children;
  }

  public NodeEntity getParent() {
    return parent;
  }

  public void setParent(NodeEntity parent) {
    this.parent = parent;
  }
  
  public static final String REMOVED_EVENT = "org.exoplatform.portal.jdbc.entity.NodeEntity.removed";
  
  @PreRemove
  public void preRemove() throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    if (container != null) {
      ListenerService listenerService = container.getComponentInstanceOfType(ListenerService.class); 
      if (listenerService != null) {
        listenerService.broadcast(REMOVED_EVENT, this, this.getId());
      }
    }
  }
}
