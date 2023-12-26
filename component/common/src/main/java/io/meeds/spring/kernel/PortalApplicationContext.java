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
package io.meeds.spring.kernel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostInitTask;
import org.exoplatform.container.spi.ComponentAdapter;

import io.meeds.spring.kernel.model.SpringBeanComponentAdapter;

import jakarta.servlet.ServletContext;

/**
 * A class to let Spring context initialized once the
 * {@link PortalContainer}finishes startup to bring all defined Kernel Services
 * into Spring Context in order to be able to use annotations to inject Kernel
 * services
 */
public class PortalApplicationContext extends AnnotationConfigServletWebServerApplicationContext {

  private static final Logger LOG = LoggerFactory.getLogger(PortalApplicationContext.class);

  private ServletContext      servletContext;

  public PortalApplicationContext(ServletContext servletContext, DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
    this.servletContext = servletContext;
  }

  @Override
  protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    BeanDefinitionRegistry beanRegistry = (BeanDefinitionRegistry) beanFactory;
    // Declare spring Context for integration on Kernel when it will start
    // ( Kernel container is always started after all Spring contexts are
    // started, see "PortalContainersCreator" in Meeds-io/meeds project )
    KernelContainerLifecyclePlugin.addSpringContext(servletContext.getServletContextName(), this, beanRegistry);

    // Delay Spring context finishing startup until the Kernel container is
    // fully started
    PortalContainer.addInitTask(servletContext, new PortalContainerPostInitTask() {
      @Override
      public void execute(ServletContext context, PortalContainer portalContainer) {
        // Register Kernel Components and other Spring contexts in current
        // Spring context
        LOG.info("Retrieving Kernel component adapters into Spring context '{}' as beans", servletContext.getServletContextName());
        injectKernelComponentsAsBeans(servletContext.getServletContextName(), beanRegistry, portalContainer);
        finishSpringContextStartup(beanFactory);
      }
    }, "portal");
  }

  @Override
  protected void finishRefresh() {
    // Override to not be invoked in Context startup time
  }

  private void finishSpringContextStartup(ConfigurableListableBeanFactory beanFactory) {
    long start = System.currentTimeMillis();
    LOG.info("Continue Spring context '{}' initialization", servletContext.getServletContextName());
    PortalApplicationContext.super.finishBeanFactoryInitialization(beanFactory);
    PortalApplicationContext.super.finishRefresh();
    LOG.info("Spring context '{}' initialized in {}ms",
             servletContext.getServletContextName(),
             System.currentTimeMillis() - start);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void injectKernelComponentsAsBeans(String servletContextName,
                                             BeanDefinitionRegistry beanRegistry,
                                             PortalContainer portalContainer) {
    List<Class> beanClasses = portalContainer.getComponentAdapters()
                                             .stream()
                                             .map(adapter -> {
                                               LOG.debug("Attempting to inject Kernel component '{}' with implementation class '{}' as Spring bean in content {}",
                                                         adapter.getComponentKey(),
                                                         adapter.getComponentImplementation(),
                                                         servletContextName);
                                               return adapter;
                                             })
                                             .filter(adapter -> !(adapter instanceof SpringBeanComponentAdapter springComponentAdapter)
                                                                || !springComponentAdapter.isIssuedFrom(servletContext.getServletContextName()))
                                             .map(adapter -> computeComponentBeanName(adapter, portalContainer))
                                             .filter(Objects::nonNull)
                                             .map(componentKey -> {
                                               LOG.debug("Attempting to inject Kernel component Class '{}' as Spring bean in context {}",
                                                         componentKey,
                                                         servletContextName);
                                               return componentKey;
                                             })
                                             .toList();
    // Avoid having two instances inheriting from
    // the same API interface to not get NoUniqueBeanDefinitionException
    List<String> kernelComponentClasses = new ArrayList<>();
    beanClasses.stream()
               .filter(c -> {
                 Class ec = beanClasses.stream()
                                       .filter(oc -> !oc.equals(c) && oc.isAssignableFrom(c))
                                       .findFirst()
                                       .orElse(null);
                 if (ec != null || kernelComponentClasses.contains(c.getName())) {
                   LOG.debug("Kernel Service '{}' is already defined by other Service with Key {}. Registration will be ignored.",
                             c.getName(),
                             ec.getName());
                   return false;
                 } else {
                   kernelComponentClasses.add(c.getName());
                   return true;
                 }
               })
               .forEach(beanClassName -> registerBean(portalContainer, beanRegistry, beanClassName, servletContextName));
  }

  private void registerBean(PortalContainer portalContainer,
                            BeanDefinitionRegistry beanRegistry,
                            Class<?> beanClass,
                            String servletContextName) {
    String beanName = beanClass.getName();
    try {
      if (beanRegistry.containsBeanDefinition(beanName)) {
        LOG.debug("Kernel service '{}' already injected in Spring context '{}', ignore it", beanName, servletContextName);
      } else {
        RootBeanDefinition beanDefinition = createBeanDefinition(beanClass, portalContainer);
        beanRegistry.registerBeanDefinition(beanName, beanDefinition);
        LOG.debug("Kernel component '{}' injected in Spring context '{}'", beanName, servletContextName);
      }
    } catch (BeanDefinitionStoreException e) {
      LOG.warn("Kernel component '{}' wasn't injected in Spring context '{}'", beanName, servletContextName, e);
    }
  }

  private <T> RootBeanDefinition createBeanDefinition(Class<T> keyClass, PortalContainer portalContainer) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(keyClass,
                                                               () -> {
                                                                 T instance =
                                                                            portalContainer.getComponentInstanceOfType(keyClass);
                                                                 LOG.trace("Getting Kernel Service '{}' requested by a Spring Bean. Instance was found = {}",
                                                                           keyClass,
                                                                           instance != null);
                                                                 return instance;
                                                               });
    beanDefinition.setLazyInit(true);
    beanDefinition.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
    return beanDefinition;
  }

  @SuppressWarnings("rawtypes")
  private Class computeComponentBeanName(ComponentAdapter adapter, PortalContainer portalContainer) { // NOSONAR
    Object key = adapter.getComponentKey();
    if (key instanceof Class<?> keyClass) {
      return keyClass;
    } else {
      if (key instanceof String keyString) {
        try {
          return portalContainer.getPortalClassLoader().loadClass(keyString);
        } catch (ClassNotFoundException e) {
          Class componentImplementation = adapter.getComponentImplementation();
          LOG.debug("Kernel component with key '{}' class wasn't found. Returning implementation class {}",
                    key,
                    componentImplementation == null ? null : componentImplementation.getName());
          return componentImplementation;
        }
      } else {
        LOG.debug("Kernel component key '{}' isn't of type *Class* neither *String*. Returning key class {}",
                  key,
                  key.getClass().getName());
        return key.getClass();
      }
    }
  }

}
