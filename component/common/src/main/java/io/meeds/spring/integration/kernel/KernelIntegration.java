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

import static io.meeds.spring.integration.kernel.SharedSpringBeanRegistry.hasBeanDefinition;
import static io.meeds.spring.integration.kernel.SharedSpringBeanRegistry.registerSpringBeansState;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.spi.ComponentAdapter;

import io.meeds.spring.integration.Exclude;

/**
 * A class to let Spring context initialized once the
 * {@link PortalContainer}finishes startup to bring all defined Kernel Services
 * into Spring Context in order to be able to use annotations to inject Kernel
 * services
 */
public class KernelIntegration {

  private static final Logger      LOG               = LoggerFactory.getLogger(KernelIntegration.class);

  private static final Set<String> KERNEL_BEAN_NAMES = new HashSet<>();

  private KernelIntegration() {
  }

  public static void integrateSpringContext(PortalContainer portalContainer,
                                            BeanDefinitionRegistry beanRegistry,
                                            Supplier<String[]> getBeanDefinitionNamesSupplier,
                                            Function<String, Object> getBeanFunction,
                                            Runnable finishSpringContextStartup) {
    registerKernelComponentsAsSpringBeans(portalContainer, beanRegistry);
    if (finishSpringContextStartup != null) {
      LOG.info("Continue Spring context startup with integrated Kernel Services");
      // Continue startup of Spring
      finishSpringContextStartup.run();
      // Register Spring Beans in Kernel Container
      LOG.info("Inject Spring Beans into Kernel after finishing startup into kernel");
      registerSpringBeansAsKernelComponents(portalContainer,
                                            beanRegistry,
                                            getBeanDefinitionNamesSupplier,
                                            getBeanFunction);
    }
  }

  public static void registerKernelComponentsAsSpringBeans(PortalContainer portalContainer,
                                                           BeanDefinitionRegistry beanRegistry) {
    LOG.info("Injecting Kernel components into Spring context as beans");
    injectKernelComponentsAsBeans(beanRegistry, portalContainer);
    registerSpringBeansState(beanRegistry);
  }

  public static void registerSpringBeanAsKernelComponent(PortalContainer container,
                                                         BeanDefinitionRegistry beanRegistry,
                                                         String beanName,
                                                         Object bean) {
    registerSpringBeansAsKernelComponents(container, beanRegistry, new String[] { beanName }, name -> bean);
  }

  private static void registerSpringBeansAsKernelComponents(PortalContainer portalContainer,
                                                            BeanDefinitionRegistry beanRegistry,
                                                            Supplier<String[]> getBeanDefinitionNamesSupplier,
                                                            Function<String, Object> getBeanFunction) {
    String[] springBeanDefinitionNames = getBeanDefinitionNamesSupplier.get();
    registerSpringBeansAsKernelComponents(portalContainer, beanRegistry, springBeanDefinitionNames, getBeanFunction);
  }

  private static void registerSpringBeansAsKernelComponents(PortalContainer portalContainer,
                                                            BeanDefinitionRegistry beanRegistry,
                                                            String[] beanDefinitionNames,
                                                            Function<String, Object> getBeanFunction) {
    Stream.of(beanDefinitionNames)
          .filter(beanName -> portalContainer.getComponentAdapter(beanName) == null)
          .filter(beanName -> !isBeanRegisteredInKernel(beanName))
          .forEach(beanName -> {
            BeanDefinition beanDefinition = getBeanDefinition(beanRegistry, beanName);
            if (beanDefinition == null) {
              LOG.debug("Bean {} seems to have an empty definition name", beanName);
            } else {
              String beanClassName = beanDefinition.getBeanClassName();
              Object bean = getBeanFunction.apply(beanName);
              Class<?> beanClass = getBeanClass(portalContainer, beanClassName, bean);
              if (beanClass != null && portalContainer.getComponentInstanceOfType(beanClass) == null) {
                LOG.atLevel(PropertyManager.isDevelopping() ? Level.INFO : Level.DEBUG)
                   .log("Register Spring Bean '{}' as Kernel service", beanClass.getName());
                portalContainer.registerComponentInstance(beanClass, bean);
                SharedSpringBeanRegistry.registerBeanDefinition(beanName, beanClass, beanDefinition);
              }
            }
          });
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void injectKernelComponentsAsBeans(BeanDefinitionRegistry beanRegistry, PortalContainer portalContainer) {
    List<Class> beanClasses = portalContainer.getComponentAdapters()
                                             .stream()
                                             .filter(adapter -> !hasBeanDefinition(adapter.getComponentKey().toString()))
                                             .map(adapter -> beanNameToClass(adapter, portalContainer))
                                             .filter(Objects::nonNull)
                                             .distinct()
                                             .toList();

    beanClasses.stream()
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
               .forEach(beanClassName -> registerBean(portalContainer, beanRegistry, beanClassName));
  }

  private static Class<?> registerBean(PortalContainer portalContainer, BeanDefinitionRegistry beanRegistry, Class<?> beanClass) {
    RootBeanDefinition beanDefinition = createBeanDefinition(beanClass, portalContainer);
    beanRegistry.registerBeanDefinition(beanClass.getName(), beanDefinition);
    LOG.debug("Kernel service '{}' injected in Spring context", beanClass.getName());
    return beanClass;
  }

  private static <T> RootBeanDefinition createBeanDefinition(Class<T> keyClass, PortalContainer portalContainer) {
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

  private static Class<?> getBeanClass(PortalContainer portalContainer,
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
          && (isServiceBean(beanClass) || isServiceBean(bean.getClass()))
          && (!isServiceBeanExcluded(beanClass) || !isServiceBeanExcluded(bean.getClass()))) {
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
      LOG.warn("Ignore registering Spring Bean '{}' as Kernel service since it's class is not found in shared ClassLoader",
               beanClassName);
      return null;
    }
  }

  private static BeanDefinition getBeanDefinition(BeanDefinitionRegistry beanRegistry, String beanName) {
    try {
      return beanRegistry.getBeanDefinition(beanName);
    } catch (NoSuchBeanDefinitionException e) {
      LOG.debug("Can't find Bean with name {}. Ignore adding it into Kernel Container", beanName);
      return null;
    }
  }

  private static boolean isConfigurationBean(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Configuration.class) || beanClass.isAnnotationPresent(SpringBootApplication.class);
  }

  private static boolean isBeanClassValid(Class<?> beanClass) {
    return !StringUtils.contains(beanClass.getName(), "org.springframework")
           && !StringUtils.startsWith(beanClass.getName(), "java")
           && !StringUtils.startsWith(beanClass.getName(), "jdk")
           && !StringUtils.contains(beanClass.getName(), "$");
  }

  private static boolean isServiceBean(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Service.class);
  }

  private static boolean isServiceBeanExcluded(Class<?> beanClass) {
    return beanClass.isAnnotationPresent(Exclude.class);
  }

  @SuppressWarnings("rawtypes")
  private static Class beanNameToClass(ComponentAdapter adapter, PortalContainer portalContainer) {
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

  private static boolean isBeanRegisteredInKernel(String beanName) {
    return KERNEL_BEAN_NAMES.contains(beanName);
  }

}
