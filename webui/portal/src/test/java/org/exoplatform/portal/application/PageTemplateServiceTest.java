package org.exoplatform.portal.application;

import static org.junit.Assert.*;

import org.junit.Test;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.portal.page.PageTemplatePlugin;
import org.exoplatform.portal.page.PageTemplateService;
import org.exoplatform.webui.core.model.SelectItemCategory;

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
}
