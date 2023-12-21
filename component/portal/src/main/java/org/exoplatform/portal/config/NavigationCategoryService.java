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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

/**
 * A service to manage categories of Administration Menu
 */
public class NavigationCategoryService {

  private Map<String, Integer> navigationCategoriesOrder = new HashMap<>();

  private Map<String, Integer> navigationUriOrder        = new HashMap<>();

  private Map<String, String>  navigationCategories      = new HashMap<>();

  private AtomicInteger        orderIndex                = new AtomicInteger(1);

  public void addPlugin(NavigationCategoryPlugin navigationCategoryPlugin) {
    String category = navigationCategoryPlugin.getCategory();
    int categoryOrder = navigationCategoryPlugin.getCategoryOrder();
    if (categoryOrder > 0) {
      navigationCategoriesOrder.put(category, categoryOrder);
    } else {
      navigationCategoriesOrder.computeIfAbsent(category, key -> orderIndex.getAndIncrement());
    }
    String uri = navigationCategoryPlugin.getUri();
    int uriOrder = navigationCategoryPlugin.getUriOrder();
    if (uriOrder > 0) {
      navigationUriOrder.put(uri, uriOrder);
    } else {
      navigationUriOrder.computeIfAbsent(uri, key -> orderIndex.getAndIncrement());
    }

    navigationCategories.put(uri, category);
  }

  /**
   * @return a {@link Map} of Administration navigation categories
   */
  public SortedMap<String, String> getNavigationCategories() {
    TreeMap<String, String> treeMap = new TreeMap<>(this::sortCategories);
    treeMap.putAll(navigationCategories);
    return treeMap;
  }

  /**
   * @return a {@link Map} of Administration navigation categories order
   */
  public Map<String, Integer> getNavigationCategoriesOrder() {
    return Collections.unmodifiableMap(navigationCategoriesOrder);
  }

  /**
   * @return a {@link Map} of Administration navigation uri order
   */
  public Map<String, Integer> getNavigationUriOrder() {
    return Collections.unmodifiableMap(navigationUriOrder);
  }

  private int sortCategories(String key1, String key2) {
    if (StringUtils.equals(key1, key2)) {
      return 0;
    } else if (StringUtils.isBlank(key1)) {
      return -1;
    } else if (StringUtils.isBlank(key2)) {
      return 1;
    }
    String cat1 = navigationCategories.get(key1);
    String cat2 = navigationCategories.get(key2);
    if (StringUtils.equals(cat1, cat2)) {
      int key1Order = navigationUriOrder.containsKey(key1) ? navigationUriOrder.get(key1) : 0;
      int key2Order = navigationUriOrder.containsKey(key2) ? navigationUriOrder.get(key2) : 0;
      int order = key1Order - key2Order;
      return order == 0 ? key1.compareTo(key2) : order;
    } else if (StringUtils.isBlank(cat1)) {
      return -1;
    } else if (StringUtils.isBlank(cat2)) {
      return 1;
    } else {
      int diff = navigationCategoriesOrder.get(cat1) - navigationCategoriesOrder.get(cat2);
      if (diff == 0) {
        return cat1.compareTo(cat2);
      } else {
        return diff;
      }
    }
  }

}
