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

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.exoplatform.commons.api.persistence.ExoEntity;
import org.exoplatform.portal.mop.SiteType;

@Entity
@ExoEntity
@Table(name = "PORTAL_SITES")
@NamedQueries({
    @NamedQuery(name = "SiteEntity.findByKey", query = "SELECT s FROM SiteEntity s WHERE s.siteType = :siteType AND s.name = :name"),
    @NamedQuery(name = "SiteEntity.findByType", query = "SELECT s FROM SiteEntity s WHERE s.siteType = :siteType"),
    @NamedQuery(name = "SiteEntity.findSiteKey", query = "SELECT s.name FROM SiteEntity s WHERE s.siteType = :siteType")
})
public class SiteEntity extends ComponentEntity {

  private static final long     serialVersionUID = 3036823700771832314L;

  @Column(name = "TYPE")
  private SiteType              siteType;

  @Column(name = "NAME", length = 200)
  private String                name;

  @Column(name = "LOCALE", length = 20)
  private String                locale;

  @Column(name = "SKIN", length = 200)
  private String                skin;

  @Column(name = "LABEL", length = 200)
  private String                label;

  @Column(name = "DESCRIPTION", length = 2000)
  private String                description;

  @Column(name = "PROPERTIES", length = 2000)
  private String                properties = new JSONObject().toJSONString();

  @Column(name = "SITE_BODY", length = 5000)
  private String                siteBody         = new JSONArray().toJSONString();

  @Transient
  private List<ComponentEntity> children         = new LinkedList<ComponentEntity>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getSkin() {
    return skin;
  }

  public void setSkin(String skin) {
    this.skin = skin;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSiteBody() {
    return siteBody;
  }

  public void setSiteBody(String siteBody) {
    this.siteBody = siteBody;
  }

  public SiteType getSiteType() {
    return siteType;
  }

  public void setSiteType(SiteType siteType) {
    this.siteType = siteType;
  }

  public List<ComponentEntity> getChildren() {
    return children;
  }

  public void setChildren(List<ComponentEntity> children) {
    this.children = children;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  @Override
  public JSONObject toJSON() {
    JSONObject obj = super.toJSON();

    JSONArray jChildren = new JSONArray();
    for (ComponentEntity child : getChildren()) {
      jChildren.add(child.toJSON());
    }
    obj.put("children", jChildren);

    return obj;
  }

  @Override
  public TYPE getType() {
    return TYPE.SITE;
  }

}
