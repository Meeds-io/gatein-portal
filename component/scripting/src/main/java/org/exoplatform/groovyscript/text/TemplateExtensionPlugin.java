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
package org.exoplatform.groovyscript.text;

import java.util.List;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

/**
 * Component plugin of Template extension. Component name is identified as the
 * parent Template name to extend.
 */
public class TemplateExtensionPlugin extends BaseComponentPlugin {

  /**
   * List of templates to include to parent gtmpl
   */
  private List<String> templates = null;

  public TemplateExtensionPlugin(InitParams params) {
    templates = params.getValuesParam("templates").getValues();
  }

  public List<String> getTemplates() {
    return templates;
  }

  public void setTemplates(List<String> templates) {
    this.templates = templates;
  }

}
