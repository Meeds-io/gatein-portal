/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
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
package org.exoplatform.portal.config;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Test;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

public class NavigationCategoryServiceTest {

  @Test
  public void testEmptyCategories() {
    NavigationCategoryService navigationCategoryService = new NavigationCategoryService();
    assertNotNull(navigationCategoryService.getNavigationCategories());
    assertTrue(navigationCategoryService.getNavigationCategories().isEmpty());
  }

  @Test
  public void testAddCategoryPlugin() {
    String catName = "cat1";
    String uriValue = "uri1";

    NavigationCategoryService navigationCategoryService = new NavigationCategoryService();
    InitParams params = new InitParams();
    addParam(params, NavigationCategoryPlugin.CATEGORY_PARAM_NAME, catName);
    addParam(params, NavigationCategoryPlugin.URI_PARAM_NAME, uriValue);
    NavigationCategoryPlugin plugin = new NavigationCategoryPlugin(params);
    navigationCategoryService.addPlugin(plugin);

    assertNotNull(navigationCategoryService.getNavigationCategories());
    assertEquals(1, navigationCategoryService.getNavigationCategories().size());
    assertEquals(catName, navigationCategoryService.getNavigationCategories().get(uriValue));

    assertEquals(1, navigationCategoryService.getNavigationCategoriesOrder().get(catName).intValue());
    assertEquals(2, navigationCategoryService.getNavigationUriOrder().get(uriValue).intValue());
  }

  @Test
  public void testAddSortedCategories() {
    NavigationCategoryService navigationCategoryService = new NavigationCategoryService();

    addNavigationPlugin(navigationCategoryService, "cat1", "6", "uri1-2");
    addNavigationPlugin(navigationCategoryService, "cat1", "6", "uri1-3");
    addNavigationPlugin(navigationCategoryService, "cat1", "6", "uri1-1");
    addNavigationPlugin(navigationCategoryService, "cat2", "4", "uri2-2");
    addNavigationPlugin(navigationCategoryService, "cat2", "4", "uri2-2");
    addNavigationPlugin(navigationCategoryService, "cat2", "4", "uri2-1");

    assertNotNull(navigationCategoryService.getNavigationCategories());
    assertEquals("URI must be injected only oncve in one single category",
                 5,
                 navigationCategoryService.getNavigationCategories().size());
    Iterator<Entry<String, String>> iterator = navigationCategoryService.getNavigationCategories().entrySet().iterator();
    Entry<String, String> firstEntry = iterator.next();
    Entry<String, String> secondEntry = iterator.next();
    Entry<String, String> thirdEntry = iterator.next();
    Entry<String, String> fourthEntry = iterator.next();
    Entry<String, String> fifthEntry = iterator.next();

    assertEquals("cat2", firstEntry.getValue());
    assertEquals("cat2", secondEntry.getValue());
    assertEquals("cat1", thirdEntry.getValue());
    assertEquals("cat1", fourthEntry.getValue());
    assertEquals("cat1", fifthEntry.getValue());

    assertEquals("uri2-2", firstEntry.getKey());
    assertEquals("uri2-1", secondEntry.getKey());
    assertEquals("uri1-2", thirdEntry.getKey());
    assertEquals("uri1-3", fourthEntry.getKey());
    assertEquals("uri1-1", fifthEntry.getKey());
  }

  private void addNavigationPlugin(NavigationCategoryService navigationCategoryService,
                                   String categoryName,
                                   String categoryOrder,
                                   String uriValue) {
    InitParams params = new InitParams();
    addParam(params, NavigationCategoryPlugin.CATEGORY_PARAM_NAME, categoryName);
    addParam(params, NavigationCategoryPlugin.CATEGORY_ORDER_PARAM_NAME, categoryOrder);
    addParam(params, NavigationCategoryPlugin.URI_PARAM_NAME, uriValue);
    NavigationCategoryPlugin plugin = new NavigationCategoryPlugin(params);
    navigationCategoryService.addPlugin(plugin);
  }

  private void addParam(InitParams params, String name, String value) {
    ValueParam param = new ValueParam();
    param.setName(name);
    param.setValue(value);
    params.addParameter(param);
  }

}
