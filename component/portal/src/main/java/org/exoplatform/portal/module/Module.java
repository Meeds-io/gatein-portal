/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.portal.module;

import java.util.List;

/**
 * A Module of the platform. Modules are optional features sets that extend
 * the capabilities of eXo Platform.<br>
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * Jun 24, 2010
 */
public class Module {

  /**
   * Identifier of the module. All modules must have a different one
   */
  private String name;

  /**
   * Brief textual description of the module to help identifying it.
   */
  private String description;

  /**
   * List of active portlets per module.
   */
  private List<String> portlets;

  /**
   * List of webapps that are active in this module.
   */
  private List<String> webapps;

  /**
   * Cache the hash code for the name
   */
  private int hash;

  /**
   * indicates if the module is active or no. false by default
   */
  private boolean active = false;

  public Module() {}

  public Module(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<String> getPortlets() {
    return this.portlets;
  }

  public void setPortlets(List<String> portlets) {
    this.portlets = portlets;
  }

  public List<String> getWebapps() {
    return this.webapps;
  }

  public void setWebapps(List<String> webapps) {
    this.webapps = webapps;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj != null && obj instanceof Module) {
      return name.equals(((Module) obj).getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0 && name != null) {
      hash = name.hashCode();
    }
    return h;
  }
  
  @Override
  public String toString() {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("Module: [name =").append(name).append("],\r\n");
    stringBuffer.append("Module: [description =").append(description).append("],\r\n");
    stringBuffer.append("Module: [active =").append(active).append("],\r\n");
    stringBuffer.append("Module: [portlets =").append(portlets).append("],\r\n");
    stringBuffer.append("Module: [webapps =").append(webapps).append("].");
    return stringBuffer.toString();
  }

}
