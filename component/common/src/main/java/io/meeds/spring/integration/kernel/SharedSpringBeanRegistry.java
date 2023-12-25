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
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.spring.integration.kernel;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public class SharedSpringBeanRegistry {

  private static final Map<String, String>         BEAN_NAMES       = new HashMap<>();

  private static final Map<String, BeanDefinition> BEAN_DEFINITIONS = new HashMap<>();

  private SharedSpringBeanRegistry() {
  }

  public static void registerBeanDefinition(String beanName, Class<?> beanClass, BeanDefinition beanDefinition) {
    BEAN_NAMES.put(beanName, beanClass.getName());
    BEAN_DEFINITIONS.put(beanClass.getName(), beanDefinition);
  }

  public static Map<String, String> getBeanNames() {
    return BEAN_NAMES;
  }

  public static BeanDefinition getBeanDefinition(String beanName) {
    return BEAN_DEFINITIONS.get(beanName);
  }

  public static boolean hasBeanDefinition(String beanName) {
    return BEAN_DEFINITIONS.containsKey(beanName);
  }

  public static void registerSpringBeansState(BeanDefinitionRegistry beanRegistry) {
    getBeanNames().forEach((beanName, beanClassName) -> beanRegistry.registerBeanDefinition(beanName,
                                                                                            getBeanDefinition(beanClassName)));
  }

}