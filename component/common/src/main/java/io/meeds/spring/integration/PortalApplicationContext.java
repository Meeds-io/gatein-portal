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
package io.meeds.spring.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer.PortalContainerPostInitTask;

import io.meeds.spring.integration.kernel.KernelIntegration;

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
  @SuppressWarnings("resource")
  protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    PortalContainer.addInitTask(servletContext, new PortalContainerPostInitTask() {
      @Override
      public void execute(ServletContext context, PortalContainer portalContainer) {
        // Register beans in Spring
        AbstractApplicationContext springApplicationContext = PortalApplicationContext.this;
        KernelIntegration.integrateSpringContext(portalContainer,
                                                 (BeanDefinitionRegistry) beanFactory,
                                                 springApplicationContext::getBeanDefinitionNames,
                                                 springApplicationContext::getBean,
                                                 () -> finishSpringContextStartup(beanFactory));
      }
    }, "portal");
  }

  @Override
  protected void finishRefresh() {
    // Override to not be invoked in Context startup time
  }

  private void finishSpringContextStartup(ConfigurableListableBeanFactory beanFactory) {
    long start = System.currentTimeMillis();
    LOG.info("Start Spring context initialization of webapp '{}'", servletContext.getServletContextName());
    PortalApplicationContext.super.finishBeanFactoryInitialization(beanFactory);
    PortalApplicationContext.super.finishRefresh();
    LOG.info("Spring context '{}' initialized in {}ms",
             servletContext.getServletContextName(),
             System.currentTimeMillis() - start);
  }

}
