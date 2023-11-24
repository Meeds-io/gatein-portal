/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.portal.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.URIWriter;
import org.exoplatform.web.url.MimeType;

import lombok.Getter;
import lombok.Setter;

import org.exoplatform.services.log.ExoLogger;

import org.apache.commons.lang3.StringUtils;
import org.gatein.portal.controller.resource.ResourceRequestHandler;

/**
 * An implementation of the skin config. Created by The eXo Platform SAS Jan 19,
 * 2007
 */
class SimpleSkin implements SkinConfig {

  private final String       module;

  private final String       name;

  private final String       cssPath;

  private final String       id;

  private final int          priority;

  private final boolean      filtered;

  private final List<String> additionalModules;

  private String             type;

  public SimpleSkin(SkinService service, String module, String name, String cssPath) {
    this(service, module, name, cssPath, Integer.MAX_VALUE);
  }

  public SimpleSkin(SkinService service, String module, String name, String cssPath, int cssPriority) {
    this.module = module;
    this.name = name;
    this.cssPath = cssPath;
    this.id = module.replace('/', '_');
    priority = cssPriority;
    additionalModules = null;
    filtered = false;
  }

  public SimpleSkin(String module, String name, String cssPath, int cssPriority, List<String> additionalModules) {
    this.module = module;
    this.name = name;
    this.cssPath = cssPath;
    this.id = module.replace('/', '_');
    this.priority = cssPriority;
    this.additionalModules = additionalModules;
    this.filtered = false;
  }

  public SimpleSkin(String module, String name, String cssPath, int cssPriority, boolean filtered) {
    this.module = module;
    this.name = name;
    this.cssPath = cssPath;
    this.id = module.replace('/', '_');
    this.priority = cssPriority;
    this.additionalModules = null;
    this.filtered = filtered;
  }

  @Override
  public int getCSSPriority() {
    return priority;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getModule() {
    return this.module;
  }

  @Override
  public String getCSSPath() {
    return this.cssPath;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean isFiltered() {
    return filtered;
  }

  @Override
  public List<String> getAdditionalModules() {
    return additionalModules;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  public String toString() {
    return "SimpleSkin[id=" + this.id + ",module=" + this.module + ",name=" + this.name + ",cssPath=" + this.cssPath +
        ", priority=" + priority +
        "]";
  }

  public SkinURL createURL(final ControllerContext context) {
    if (context == null) {
      throw new NullPointerException("No controller context provided");
    }
    if (StringUtils.isBlank(this.cssPath)) {
      return null;
    }
    return new SkinURL() {

      Orientation orientation = null;

      boolean     compress    = !PropertyManager.isDevelopping();

      public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
      }

      @Override
      public String toString() {
        try {
          String resource = cssPath.substring(1, cssPath.length() - ".css".length());

          //
          Map<QualifiedName, String> params = new HashMap<QualifiedName, String>();
          params.put(ResourceRequestHandler.VERSION_QN, ResourceRequestHandler.VERSION);
          params.put(ResourceRequestHandler.ORIENTATION_QN, orientation == Orientation.RT ? "rt" : "lt");
          params.put(ResourceRequestHandler.COMPRESS_QN, compress ? "min" : "");
          params.put(WebAppController.HANDLER_PARAM, "skin");
          params.put(ResourceRequestHandler.RESOURCE_QN, resource);
          StringBuilder url = new StringBuilder();
          context.renderURL(params, new URIWriter(url, MimeType.PLAIN));

          //
          return url.toString();
        } catch (IOException e) {
          ExoLogger.getLogger(this.getClass()).error(e.getMessage(), e);
          return null;
        }
      }
    };
  }
}
