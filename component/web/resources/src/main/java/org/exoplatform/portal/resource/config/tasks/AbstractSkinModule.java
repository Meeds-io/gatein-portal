/**
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.portal.resource.config.tasks;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.resource.config.xml.SkinConfigParser;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;

import lombok.SneakyThrows;

/**
 * @author <a href="trong.tran@exoplatform.com">Trong Tran</a>
 * @version $Revision$
 */

public abstract class AbstractSkinModule {

  protected static final Log   LOG         = ExoLogger.getLogger(AbstractSkinModule.class);

  private static final boolean DEVELOPPING = PropertyManager.isDevelopping();

  protected String             skinName;

  protected String             cssPath;

  protected boolean            overwrite;

  protected String             cssPriority;

  AbstractSkinModule(String name) {
    skinName = name;
  }

  protected void bindingSkinName(Element element) {
    NodeList nodes = element.getElementsByTagName(SkinConfigParser.SKIN_NAME_TAG);
    if (nodes == null || nodes.getLength() < 1) {
      return;
    }
    this.skinName = nodes.item(0).getFirstChild().getNodeValue();
  }

  protected void bindingCSSPath(Element element) {
    NodeList nodes = element.getElementsByTagName(SkinConfigParser.CSS_PATH_TAG);
    if (nodes == null || nodes.getLength() < 1) {
      return;
    }
    this.cssPath = nodes.item(0).getFirstChild().getNodeValue();
  }

  protected void bindingOverwrite(Element element) {
    NodeList nodes = element.getElementsByTagName(SkinConfigParser.OVERWRITE);
    if (nodes == null || nodes.getLength() < 1) {
      return;
    }
    setOverwrite("true".equals(nodes.item(0).getFirstChild().getNodeValue()));
  }

  protected void bindingCSSPriority(Element element) {
    NodeList nodes = element.getElementsByTagName(SkinConfigParser.CSS_PRIORITY_TAG);
    if (nodes == null || nodes.getLength() < 1) {
      return;
    }
    this.cssPriority = nodes.item(0).getFirstChild().getNodeValue();
  }

  public void setSkinName(String name) {
    this.skinName = name;
  }

  public void setCSSPath(String cssPath) {
    this.cssPath = cssPath;
  }

  public void setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
  }

  public void setCSSPriority(String cssPriority) {
    this.cssPriority = cssPriority;
  }

}
