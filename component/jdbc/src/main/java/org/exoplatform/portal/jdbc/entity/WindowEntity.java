/**
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.json.simple.JSONObject;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity
@ExoEntity
@Table(name = "PORTAL_WINDOWS")
@NamedQueries({
  @NamedQuery(name = "WindowEntity.findByIds", query = "SELECT w FROM WindowEntity w WHERE w.id in (:ids)") })
public class WindowEntity extends ComponentEntity implements Serializable {

  private static final long serialVersionUID = 6633792468705838255L;

  @Column(name = "TITLE", length = 200)
  private String            title;

  @Column(name = "ICON", length = 200)
  private String            icon;

  @Column(name = "DESCRIPTION", length = 2000)
  private String            description;

  @Column(name = "SHOW_INFO_BAR")
  private boolean           showInfoBar;

  @Column(name = "SHOW_APP_STATE")
  private boolean           showApplicationState;

  @Column(name = "SHOW_APP_MODE")
  private boolean           showApplicationMode;

  @Column(name = "THEME", length = 200)
  private String            theme;

  @Column(name = "WIDTH", length = 20)
  private String            width;

  @Column(name = "HEIGHT", length = 20)
  private String            height;

  @Column(name = "PROPERTIES", length = 2000)
  private String            properties = new JSONObject().toJSONString();

  @Column(name = "APP_TYPE")
  private AppType           appType;

  @Column(name = "CONTENT_ID", length = 200)
  private String            contentId;

  @Lob
  @Column(name = "CUSTOMIZATION", length = 10000)
  private byte[]            customization;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isShowInfoBar() {
    return showInfoBar;
  }

  public void setShowInfoBar(boolean showInfoBar) {
    this.showInfoBar = showInfoBar;
  }

  public boolean isShowApplicationState() {
    return showApplicationState;
  }

  public void setShowApplicationState(boolean showApplicationState) {
    this.showApplicationState = showApplicationState;
  }

  public boolean isShowApplicationMode() {
    return showApplicationMode;
  }

  public void setShowApplicationMode(boolean showApplicationMode) {
    this.showApplicationMode = showApplicationMode;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public String getWidth() {
    return width;
  }

  public void setWidth(String width) {
    this.width = width;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  public AppType getAppType() {
    return appType;
  }

  public void setAppType(AppType appType) {
    this.appType = appType;
  }

  public String getContentId() {
    return contentId;
  }

  public void setContentId(String contentId) {
    this.contentId = contentId;
  }

  public byte[] getCustomization() {
    return customization;
  }

  public void setCustomization(byte[] customization) {
    this.customization = customization;
  }

  @Override
  public TYPE getType() {
    return TYPE.WINDOW;
  }

  public static enum AppType {
    PORTLET, GADGET, WSRP
  }
}
