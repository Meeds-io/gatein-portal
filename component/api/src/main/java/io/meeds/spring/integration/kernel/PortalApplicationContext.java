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
package io.meeds.spring.integration.kernel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostInitTask;
import org.exoplatform.container.spi.ComponentAdapter;

import jakarta.servlet.ServletContext;

/**
 * A class to let Spring context initialized once the
 * {@link PortalContainer}finishes startup to bring all defined Kernel Services
 * into Spring Context in order to be able to use annotations to inject Kernel
 * services
 */
public class PortalApplicationContext extends AnnotationConfigServletWebServerApplicationContext {

  private static final Logger                      LOG               = LoggerFactory.getLogger(PortalApplicationContext.class);

  private static final Set<String>                 KERNEL_BEAN_NAMES = new HashSet<>();

  private static final Map<String, String>         BEAN_NAMES        = new HashMap<>();

  private static final Map<String, BeanDefinition> BEAN_DEFINITIONS  = new HashMap<>();

  private ServletContext                           servletContext;

  public PortalApplicationContext(ServletContext servletContext, DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
    this.servletContext = servletContext;
  }

  @Override
  protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    PortalContainer.addInitTask(servletContext, new PortalContainerPostInitTask() {
      @Override
      public void execute(ServletContext context, PortalContainer portalContainer) {
        // Register beans in Spring
        BeanDefinitionRegistry beanRegistry = (BeanDefinitionRegistry) beanFactory;
        registerKernelComponentsAsBeans(beanRegistry, portalContainer);
        registerSharedBeans(beanRegistry);
        // Continue startup of Spring
        initSpringContext(beanFactory);
        // Register Spring Beans in Kernel Container
        registerBeansInKernelContainer(portalContainer);
      }

    }, "portal");
  }

  @Override
  protected void finishRefresh() {
    // Override to not be invoked in Context startup time
  }

  private void initSpringContext(ConfigurableListableBeanFactory beanFactory) {
    long start = System.currentTimeMillis();
    LOG.info("Start Spring context initialization of webapp '{}'", servletContext.getServletContextName());
    PortalApplicationContext.super.finishBeanFactoryInitialization(beanFactory);
    PortalApplicationContext.super.finishRefresh();
    LOG.info("Spring context '{}' initialized in {}ms",
             servletContext.getServletContextName(),
             System.currentTimeMillis() - start);
  }

  private void registerBeansInKernelContainer(PortalContainer portalContainer) {
    Stream.of(getBeanDefinitionNames())
          .filter(beanName -> portalContainer.getComponentAdapter(beanName) == null)
          .filter(beanName -> !KERNEL_BEAN_NAMES.contains(beanName))
          .forEach(beanName -> {
            Object bean = getBean(beanName);
            BeanDefinition beanDefinition = getBeanDefinition(beanName);
            String beanClassName = beanDefinition.getBeanClassName();
            Class<?> beanClass = getBeanClass(portalContainer, beanClassName, bean);
            if (beanClass != null) {
              LOG.debug("Register Spring Bean '{}' as Kernel service", beanClass.getName());
              portalContainer.registerComponentInstance(beanClass, bean);
              BEAN_NAMES.put(beanName, beanClass.getName());
              BEAN_DEFINITIONS.put(beanClass.getName(), beanDefinition);
            }
          });
  }

  private void registerSharedBeans(BeanDefinitionRegistry beanRegistry) {
    BEAN_NAMES.forEach((beanName, beanClassName) -> beanRegistry.registerBeanDefinition(beanName,
                                                                                        BEAN_DEFINITIONS.get(beanClassName)));
  }

  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  private List<?> registerKernelComponentsAsBeans(BeanDefinitionRegistry beanRegistry, PortalContainer portalContainer) {
    List<Class> beanClasses = portalContainer.getComponentAdapters()
                                             .stream()
                                             .filter(adapter -> !BEAN_DEFINITIONS.containsKey(adapter.getComponentKey()))
                                             .map(adapter -> this.beanNameToClass(adapter, portalContainer))
                                             .filter(Objects::nonNull)
                                             .distinct()
                                             .toList();

    return beanClasses.stream()
                      // Avoid having two instances inheriting from the same API
                      // interface to not get NoUniqueBeanDefinitionException
                      .filter(c -> {
                        Class ec = beanClasses.stream()
                                              .filter(oc -> !oc.equals(c) && oc.isAssignableFrom(c))
                                              .findFirst()
                                              .orElse(null);
                        if (ec != null || KERNEL_BEAN_NAMES.contains(c.getName())) {
                          LOG.debug("Kernel Service '{}' is already defined by other Service with Key {}. Registration will be ignored.",
                                    c.getName(),
                                    ec.getName());
                          return false;
                        } else {
                          KERNEL_BEAN_NAMES.add(c.getName());
                          return true;
                        }
                      })
                      .map(beanClassName -> registerBean(portalContainer, beanRegistry, beanClassName))
                      .filter(Objects::nonNull)
                      .toList();
  }

  private Class<?> registerBean(PortalContainer portalContainer, BeanDefinitionRegistry beanRegistry, Class<?> beanClass) {
    RootBeanDefinition beanDefinition = createBeanDefinition(beanClass, portalContainer);
    beanRegistry.registerBeanDefinition(beanClass.getName(), beanDefinition);
    LOG.debug("Kernel service '{}' injected in Spring context", beanClass.getName());
    return beanClass;
  }

  private <T> RootBeanDefinition createBeanDefinition(Class<T> keyClass, PortalContainer portalContainer) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(keyClass,
                                                               () -> {
                                                                 T instance =
                                                                            portalContainer.getComponentInstanceOfType(keyClass);
                                                                 LOG.debug("Getting Kernel Service '{}' to inject in Spring context. Found = {}",
                                                                           keyClass,
                                                                           instance != null);
                                                                 return instance;
                                                               });
    beanDefinition.setLazyInit(true);
    beanDefinition.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
    return beanDefinition;
  }

  private Class<?> getBeanClass(PortalContainer portalContainer,
                                String beanClassName,
                                Object bean) {
    try {
      if (StringUtils.isBlank(beanClassName) || bean == null) {
        return null;
      }
      Class<?> beanClass = portalContainer.getPortalClassLoader().loadClass(beanClassName);
      if (bean.getClass().isAssignableFrom(beanClass)
          && (isBeanClassValid(beanClass) || isBeanClassValid(bean.getClass()))
          && !isConfigurationBean(beanClass)
          && !isConfigurationBean(bean.getClass())
          && (isServiceBean(beanClass) || isServiceBean(bean.getClass()))) {
        if (isServiceBean(bean.getClass())) {
          return bean.getClass();
        } else {
          return beanClass;
        }
      } else {
        LOG.debug("Ignore registering Spring Bean '{}/{}' as Kernel service",
                  beanClassName,
                  bean.getClass().getName());
        return null;
      }
    } catch (ClassNotFoundException e) {
      LOG.debug("Ignore registering Spring Bean '{}' as Kernel service since it's class is not found in shared ClassLoader",
                beanClassName);
      return null;
    }
  }

  private boolean isConfigurationBean(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Configuration.class) || beanClass.isAnnotationPresent(SpringBootApplication.class);
  }

  private boolean isBeanClassValid(Class<?> beanClass) {
    return !StringUtils.contains(beanClass.getName(), "org.springframework")
        && !StringUtils.startsWith(beanClass.getName(), "java")
        && !StringUtils.startsWith(beanClass.getName(), "jdk")
        && !StringUtils.contains(beanClass.getName(), "$");
  }

  private boolean isServiceBean(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Component.class) || beanClass.isAnnotationPresent(Service.class);
  }

  @SuppressWarnings("rawtypes")
  private Class beanNameToClass(ComponentAdapter adapter, PortalContainer portalContainer) {
    Object key = adapter.getComponentKey();
    if (key instanceof Class<?> keyClass) {
      return keyClass;
    } else {
      if (key instanceof String keyString) {
        try {
          return portalContainer.getPortalClassLoader().loadClass(keyString);
        } catch (ClassNotFoundException e) {
          return adapter.getComponentImplementation();
        }
      } else {
        return key.getClass();
      }
    }
  }

}