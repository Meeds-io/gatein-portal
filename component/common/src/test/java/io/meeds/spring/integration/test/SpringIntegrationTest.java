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
package io.meeds.spring.integration.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.jpa.CommonsDAOJPAImplTest;

import io.meeds.kernel.test.KernelExtension;
import io.meeds.spring.integration.model.TestModel;
import io.meeds.spring.integration.service.TestExcludedService;
import io.meeds.spring.integration.service.TestService;
import io.meeds.spring.integration.storage.TestStorage;
import io.meeds.spring.integration.test.dao.TestDao;
import io.meeds.spring.integration.test.entity.TestEntity;

@ExtendWith({ SpringExtension.class, KernelExtension.class })
@SpringBootApplication(scanBasePackages = {
                                            "io.meeds.spring.integration"
})
@EnableJpaRepositories(basePackages = "io.meeds.spring.integration")
@EnableConfigurationProperties(LiquibaseProperties.class)
@TestPropertySource(properties = {
                                   "spring.liquibase.change-log=classpath:db/changelog/test-rdbms.db.changelog.xml",
})
public class SpringIntegrationTest extends CommonsDAOJPAImplTest { // NOSONAR

  @Autowired
  private SettingService      settingService;

  @Autowired
  private TestDao             testDao;

  @Autowired
  private TestStorage         testStorage;

  @Autowired
  private TestService         testService;

  @Autowired
  private TestExcludedService testExcludedService;

  @Test
  public void beansInjected() {
    assertNotNull("Kernel Service not included in Spring context", settingService);
    assertNotNull("Spring @Repository Bean not found", testDao);
    assertNotNull("Spring @Component Bean not found", testStorage);
    assertNotNull("Spring @Service Bean not found", testService);
    assertNotNull("Spring @Service + @Exclude Bean not found", testExcludedService);
  }

  @Test
  public void daoBeanReady() {
    TestEntity testEntity = testDao.save(new TestEntity(null, "test"));
    assertNotNull(testEntity);
    assertTrue(testEntity.getId() > 0);
    assertEquals("test", testEntity.getText());
  }

  @Test
  public void serviceBeanReady() {
    TestModel testModel = testService.save(new TestModel(null, "test2"));
    assertNotNull(testModel);
    assertTrue(testModel.getId() > 0);
    assertEquals("test2", testModel.getText());
  }

  @Test
  public void daoNotRegisteredInKernel() {
    TestDao daoComponent = getContainer().getComponentInstanceOfType(TestDao.class);
    assertNull("DAO Layer shouldn't be accessible globally outside the curent module", daoComponent);
  }

  @Test
  public void storageNotRegisteredInKernel() {
    TestStorage testStorageComponent = getContainer().getComponentInstanceOfType(TestStorage.class);
    assertNull("Service Layer should be shared globally", testStorageComponent);
  }

  @Test
  public void excludedServiceNotRegisteredInKernel() {
    TestExcludedService testExcludedServiceComponent = getContainer().getComponentInstanceOfType(TestExcludedService.class);
    assertNull("Excluded Service Layer shouldn't be shared globally", testExcludedServiceComponent);
  }

  @Test
  public void serviceRegisteredInKernel() {
    TestService testServiceComponent = getContainer().getComponentInstanceOfType(TestService.class);
    assertNotNull("Service Layer should be shared globally", testServiceComponent);
  }

}
