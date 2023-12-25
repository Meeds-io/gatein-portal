/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
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
package io.meeds.spring.integration.web.configuration;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.meeds.spring.integration.web.filter.PortalIdentityFilter;
import io.meeds.spring.integration.web.filter.PortalTransactionFilter;

@Configuration
public class WebConfiguration {

  @Bean
  public FilterRegistrationBean<PortalIdentityFilter> identityFilter() {
    FilterRegistrationBean<PortalIdentityFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new PortalIdentityFilter());
    registrationBean.addUrlPatterns("/rest/*");
    registrationBean.setOrder(1);
    return registrationBean;
  }

  @Bean
  public FilterRegistrationBean<PortalTransactionFilter> transactionFilter() {
    FilterRegistrationBean<PortalTransactionFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new PortalTransactionFilter());
    registrationBean.addUrlPatterns("/rest/*");
    registrationBean.setOrder(2);
    return registrationBean;
  }

}