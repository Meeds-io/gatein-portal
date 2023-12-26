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
package io.meeds.spring.kernel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import org.exoplatform.container.BaseContainerLifecyclePlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;

import io.meeds.spring.kernel.annotation.Exclude;
import io.meeds.spring.kernel.model.SpringBeanComponentAdapter;

public class KernelContainerLifecyclePlugin extends BaseContainerLifecyclePlugin {

  private static final Logger                        LOG                  =
                                                         LoggerFactory.getLogger(KernelContainerLifecyclePlugin.class);

  private static Map<String, BeanDefinitionRegistry> springBeanRegistries = new ConcurrentHashMap<>();

  private static Map<String, ApplicationContext>     springContexts       = new ConcurrentHashMap<>();

  private static Map<String, Map<String, Object>>    springBeans          = new ConcurrentHashMap<>();

  private static boolean                             kernelAlreadyBooted  = false;

  @Override
  public void initContainer(ExoContainer container) throws Exception {
    if (!(container instanceof PortalContainer portalContainer)) {
      return;
    }
    kernelAlreadyBooted = true; // NOSONAR
    if (LOG.isDebugEnabled()) {
      LOG.debug("Start Spring Bean definitions of contexts [{}] injection into Kernel Container",
                StringUtils.join(springContexts.keySet(), ","));
    }

    // Inject Spring contexts included for Integration with Kernel Container
    springContexts.forEach((servletContextName, applicationContext) -> {
      BeanDefinitionRegistry beanDefinitionRegistry = springBeanRegistries.get(servletContextName);
      registerSpringBeanDefinitionAsKernelComponentAdapter(portalContainer,
                                                           applicationContext,
                                                           beanDefinitionRegistry,
                                                           servletContextName);
    });

    // Inject Early retrieved beans
    springBeans.forEach((servletContextName,
                         beanMap) -> beanMap.forEach((beanName,
                                                      bean) -> registerSpringBeanDefinitionAsKernelComponentAdapter(portalContainer,
                                                                                                                    servletContextName,
                                                                                                                    beanName,
                                                                                                                    bean)));

  }

  public static void addSpringContext(String servletContextName,
                                      PortalApplicationContext applicationContext,
                                      BeanDefinitionRegistry beanDefinitionRegistry) {
    if (kernelAlreadyBooted) {
      LOG.warn("Adding Spring context '{}' happened too late in Server startup, spring beans of current contexts will not be injected",
               servletContextName);
      return;
    }
    LOG.info("Add Spring context '{}' to inject its Beans in Kernel as available components", servletContextName);
    springContexts.put(servletContextName, applicationContext);
    springBeanRegistries.put(servletContextName, beanDefinitionRegistry);
  }

  public static void addSpringBean(String servletContextName, String beanName, Object bean) {
    if (kernelAlreadyBooted) {
      return;
    }
    LOG.info("Add Registered Spring Bean '{}' to be able to inject into Kernel", beanName);
    springBeans.computeIfAbsent(servletContextName, key -> new ConcurrentHashMap<>())
               .put(beanName, bean);
  }

  private void registerSpringBeanDefinitionAsKernelComponentAdapter(PortalContainer portalContainer,
                                                                    String servletContextName,
                                                                    String beanName,
                                                                    Object bean) {
    registerSpringBeanDefinitionAsKernelComponentAdapter(servletContextName,
                                                         portalContainer,
                                                         beanName,
                                                         bean.getClass()
                                                             .getName(),
                                                         () -> {
                                                           LOG.trace("Retrieve Bean with name '{}' from Spring to Kernel using early injected beans",
                                                                     beanName);
                                                           return springContexts.get(servletContextName)
                                                                                .getBean(beanName);
                                                         });
  }

  private void registerSpringBeanDefinitionAsKernelComponentAdapter(PortalContainer portalContainer,
                                                                    ApplicationContext applicationContext,
                                                                    BeanDefinitionRegistry beanDefinitionRegistry,
                                                                    String servletContextName) {
    String[] beanDefinitionNames = beanDefinitionRegistry.getBeanDefinitionNames();
    Stream.of(beanDefinitionNames)
          .distinct()
          .forEach(beanName -> {
            BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanName);
            registerSpringBeanDefinitionAsKernelComponentAdapter(servletContextName,
                                                                 portalContainer,
                                                                 beanName,
                                                                 beanDefinition.getBeanClassName(),
                                                                 () -> {
                                                                   LOG.trace("Retrieve Bean with name '{}' from Spring to Kernel using Application context",
                                                                             beanName);
                                                                   return applicationContext.getBean(beanName);
                                                                 });
          });
  }

  @SuppressWarnings("unchecked")
  private void registerSpringBeanDefinitionAsKernelComponentAdapter(String servletContextName,
                                                                    PortalContainer portalContainer,
                                                                    String beanName,
                                                                    String beanClassName,
                                                                    Supplier<Object> getBeanFunction) {
    if (portalContainer == null || beanClassName == null) {
      return;
    }
    Class<Object> componentKey = getComponentKey(portalContainer, beanClassName, beanName);
    if (componentKey != null && portalContainer.getComponentAdapter(componentKey) == null) {
      Class<?>[] componentKeyInterfaces = componentKey.getInterfaces();
      if (ArrayUtils.isNotEmpty(componentKeyInterfaces)) {
        registerSpringBeanDefinitionAsKernelComponentAdapter(servletContextName,
                                                             portalContainer,
                                                             beanName,
                                                             getBeanFunction,
                                                             componentKey);
        Arrays.stream(componentKeyInterfaces)
              .forEach(componentKeyInterface -> registerSpringBeanDefinitionAsKernelComponentAdapter(servletContextName,
                                                                                                     portalContainer,
                                                                                                     beanName,
                                                                                                     getBeanFunction,
                                                                                                     (Class<Object>) componentKeyInterface));
      } else {
        registerSpringBeanDefinitionAsKernelComponentAdapter(servletContextName,
                                                             portalContainer,
                                                             beanName,
                                                             getBeanFunction,
                                                             componentKey);
      }
    }
  }

  private void registerSpringBeanDefinitionAsKernelComponentAdapter(String servletContextName,
                                                                    PortalContainer portalContainer,
                                                                    String beanName,
                                                                    Supplier<Object> getBeanFunction,
                                                                    Class<Object> componentKey) {
    LOG.debug("Register Spring Bean '{}' issued from context '{}' as Kernel service", componentKey.getName(), servletContextName);
    portalContainer.registerComponentAdapter(new SpringBeanComponentAdapter(servletContextName,
                                                                            beanName,
                                                                            componentKey,
                                                                            getBeanFunction));
  }

  private Class<Object> getComponentKey(PortalContainer portalContainer,
                                        String beanClassName,
                                        String beanName) {
    Stream<String> componentKeys = Stream.of(beanClassName, beanName);
    List<Class<Object>> componentClasses = componentKeys.filter(Objects::nonNull)
                                                        .map(className -> getClass(portalContainer, className))
                                                        .filter(Objects::nonNull)
                                                        .filter(className -> !Objects.equals(className, Object.class))
                                                        .distinct()
                                                        .toList();
    if (componentClasses.isEmpty()
        // Direct rejection of Services in some cases
        || componentClasses.stream().anyMatch(this::isBeanClassRejected)) {
      return null;
    } else {
      return componentClasses.stream()
                             .filter(this::isServiceBean)
                             .findFirst()
                             .orElse(null);
    }
  }

  private boolean isBeanClassRejected(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Exclude.class)
           || beanClass.isAnnotationPresent(Configuration.class)
           || beanClass.isAnnotationPresent(SpringBootApplication.class);
  }

  @SuppressWarnings("unchecked")
  private <T> Class<T> getClass(PortalContainer portalContainer, String beanClassName) {
    try {
      return (Class<T>) portalContainer.getPortalClassLoader().loadClass(beanClassName);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private boolean isServiceBean(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Service.class)
           && !StringUtils.contains(beanClass.getName(), "org.springframework")
           && !StringUtils.startsWith(beanClass.getName(), "java")
           && !StringUtils.startsWith(beanClass.getName(), "jdk")
           && !StringUtils.contains(beanClass.getName(), "$");
  }

}
