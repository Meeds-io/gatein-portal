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
package io.meeds.spring.integration.persistence;

import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.persistence.impl.LiquibaseDataInitializer;
import org.exoplatform.container.PortalContainer;

import jakarta.persistence.EntityManagerFactory;
import liquibase.integration.spring.SpringLiquibase;

@Configuration
public class PersistenceUnitIntegration {

  @Bean
  public EntityManagerFactory entityManagerFactory() {
    EntityManagerService entityManagerService = PortalContainer.getInstance()
                                                               .getComponentInstanceOfType(EntityManagerService.class);
    return entityManagerService.getEntityManagerFactory();
  }

  @Bean
  public SpringLiquibase liquibase(LiquibaseProperties liquibaseProperties) {
    LiquibaseDataInitializer liquibaseDataInitializer = PortalContainer.getInstance()
                                                                       .getComponentInstanceOfType(LiquibaseDataInitializer.class);
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(liquibaseDataInitializer.getDatasource());
    liquibase.setChangeLog(liquibaseProperties.getChangeLog());
    liquibase.setContexts(liquibaseProperties.getContexts());
    liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
    liquibase.setDropFirst(liquibaseProperties.isDropFirst());
    liquibase.setShouldRun(liquibaseProperties.isEnabled());
    return liquibase;
  }

}
