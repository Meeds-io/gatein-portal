package org.exoplatform.portal.jdbc.dao;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.portal.jdbc.entity.ContainerEntity;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.portal.jdbc.test.configuration.xml")  
})
public class ContainerDAOTest extends AbstractKernelTest {
  private ContainerDAO containerDAO;
  
  private EntityTransaction transaction;
  
  @Override
  protected void setUp() throws Exception {    
    begin();
    super.setUp();
    this.containerDAO = getContainer().getComponentInstanceOfType(ContainerDAO.class);
    
    EntityManagerService managerService = getContainer().getComponentInstanceOfType(EntityManagerService.class);
    transaction = managerService.getEntityManager().getTransaction();
    transaction.begin();
  }

  @Override
  protected void tearDown() throws Exception {
    if (transaction.isActive()) {
      transaction.rollback();
    }
    super.tearDown();
    end();
  }
  
  public void testCreateContainer() {
    ContainerEntity entity = createInstance("testContainer", "testDesc");
    containerDAO.create(entity);
    end();
    begin();
    
    ContainerEntity result = containerDAO.find(entity.getId());
    assertNotNull(result);
    assertContainer(entity, result);
  }
  
  public void testFindByIds() {
    ContainerEntity entity1 = createInstance("testContainer1", "testDesc1");
    containerDAO.create(entity1);
    ContainerEntity entity2 = createInstance("testContainer2", "testDesc2");
    containerDAO.create(entity2);    
    end();
    begin();
    
    List<ContainerEntity> results = containerDAO.findByIds(Arrays.asList(entity1.getId(), entity2.getId()));
    assertEquals(2, results.size());
  }
  
  private ContainerEntity createInstance(String name, String description) {
    ContainerEntity entity = new ContainerEntity();
    entity.setContainerBody("testBody");
    entity.setDescription(description);
    entity.setFactoryId("testFactoriId");
    entity.setHeight("testHeight");
    entity.setIcon("testIcon");
    entity.setName(name);
    entity.setProperties("testProps");
    entity.setTemplate("testTemplate");
    entity.setTitle("testTitle");
    entity.setWidth("testWidth");
    return entity;
  }
  
  private void assertContainer(ContainerEntity expected, ContainerEntity result) {
    assertEquals(expected.getContainerBody(), result.getContainerBody());
    assertEquals(expected.getDescription(), result.getDescription());
    assertEquals(expected.getFactoryId(), result.getFactoryId());
    assertEquals(expected.getHeight(), result.getHeight());
    assertEquals(expected.getIcon(), result.getIcon());
    assertEquals(expected.getId(), result.getId());
    assertEquals(expected.getName(), result.getName());
    assertEquals(expected.getProperties(), result.getProperties());
    assertEquals(expected.getTemplate(), result.getTemplate());
    assertEquals(expected.getTitle(), result.getTitle());
    assertEquals(expected.getWidth(), result.getWidth());
  }
}
