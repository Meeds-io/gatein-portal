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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import org.exoplatform.container.PortalContainer;

import io.meeds.spring.kernel.utils.KernelIntegration;

@Component
public class SpringBeanFactoryInterceptor implements BeanFactoryPostProcessor, BeanPostProcessor, ApplicationContextAware {

  private static final Logger    LOG = LoggerFactory.getLogger(SpringBeanFactoryInterceptor.class);

  private PortalContainer        container;

  private ApplicationContext     applicationContext;

  private BeanDefinitionRegistry beanRegistry;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    LOG.info("Integrating Spring Context with Container. Application name = {}", applicationContext.getApplicationName());
    this.container = PortalContainer.getInstance();
    this.beanRegistry = (BeanDefinitionRegistry) beanFactory;
    KernelIntegration.registerKernelComponentsAsSpringBeans(container, beanRegistry);
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    KernelIntegration.registerSpringBeanAsKernelComponent(container,
                                                          beanRegistry,
                                                          beanName,
                                                          bean);
    return bean;
  }

}
