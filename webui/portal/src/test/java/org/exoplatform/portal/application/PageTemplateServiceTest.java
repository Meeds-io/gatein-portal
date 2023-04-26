/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.portal.application;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.portal.page.PageTemplatePlugin;
import org.exoplatform.portal.page.PageTemplateService;
import org.exoplatform.webui.core.model.SelectItemCategory;
import org.exoplatform.webui.core.model.SelectItemOption;

public class PageTemplateServiceTest {

  @Test
  public void testAddPageTemplatePlugin() {
    PageTemplateService pageTemplateService = new PageTemplateService();
    assertNotNull(pageTemplateService.getPageTemplateCategories());
    assertTrue(pageTemplateService.getPageTemplateCategories().isEmpty());

    PageTemplatePlugin pageTemplatePlugin = new PageTemplatePlugin(null);
    assertNull(pageTemplatePlugin.getCategory());
    pageTemplateService.addPageTemplate(pageTemplatePlugin);

    assertNotNull(pageTemplateService.getPageTemplateCategories());
    assertTrue(pageTemplateService.getPageTemplateCategories().isEmpty());

    InitParams params = new InitParams();
    ObjectParameter categoryParameter = new ObjectParameter();
    categoryParameter.setName("category");
    SelectItemCategory<String> category = new SelectItemCategory<String>("testPageCategory");
    categoryParameter.setObject(category);
    params.addParameter(categoryParameter);

    pageTemplatePlugin = new PageTemplatePlugin(params);
    assertNotNull(pageTemplatePlugin.getCategory());
    assertEquals(category, pageTemplatePlugin.getCategory());
    pageTemplateService.addPageTemplate(pageTemplatePlugin);

    assertNotNull(pageTemplateService.getPageTemplateCategories());
    assertTrue(pageTemplateService.getPageTemplateCategories().size() == 1);
    assertEquals(category, pageTemplateService.getPageTemplateCategories().get(0));
  }
  
  @Test
  public void testGetPageTemplates() {
    PageTemplateService pageTemplateService = new PageTemplateService();
    
    InitParams params = new InitParams();
    ObjectParameter category1Parameter = new ObjectParameter();
    category1Parameter.setName("category");
    SelectItemCategory<String> category1 = new SelectItemCategory<String>("category1");
    category1.addSelectItemOption(new SelectItemOption<String>("page template 11", "pageTemplate11"));
    category1.addSelectItemOption(new SelectItemOption<String>("page template 12", "pageTemplate12"));
    category1.addSelectItemOption(new SelectItemOption<String>("page template 13", "pageTemplate13"));
    category1Parameter.setObject(category1);
    params.addParameter(category1Parameter);
    PageTemplatePlugin pageTemplatePlugin = new PageTemplatePlugin(params);
    pageTemplateService.addPageTemplate(pageTemplatePlugin);
    
    params = new InitParams();
    ObjectParameter category2Parameter = new ObjectParameter();
    category2Parameter.setName("category");
    SelectItemCategory<String> category2 = new SelectItemCategory<String>("category2");
    category2.addSelectItemOption(new SelectItemOption<String>("page template 21", "pageTemplate21"));
    category2.addSelectItemOption(new SelectItemOption<String>("page template 22", "pageTemplate22"));
    category2Parameter.setObject(category2);
    params.addParameter(category2Parameter);
    pageTemplatePlugin = new PageTemplatePlugin(params);
    pageTemplateService.addPageTemplate(pageTemplatePlugin);
    
    List<SelectItemOption<String>> pageTemplates = pageTemplateService.getPageTemplates();

    assertNotNull(pageTemplates);
    assertEquals(5, pageTemplates.size());
    assertEquals("pageTemplate11", pageTemplates.get(0).getValue());
    assertEquals("page template 12", pageTemplates.get(1).getLabel());
    assertEquals("pageTemplate13", pageTemplates.get(2).getValue());
    assertEquals("page template 21", pageTemplates.get(3).getLabel());
    assertEquals("pageTemplate22", pageTemplates.get(4).getValue());
  }
}
