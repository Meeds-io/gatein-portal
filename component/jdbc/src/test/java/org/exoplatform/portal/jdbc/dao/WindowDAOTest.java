package org.exoplatform.portal.jdbc.dao;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity.AppType;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.portal.jdbc.test.configuration.xml")  
})
public class WindowDAOTest extends AbstractKernelTest {
  private WindowDAO windowDAO;
  
  private EntityTransaction transaction;
  
  @Override
  protected void setUp() throws Exception {    
    begin();
    super.setUp();
    this.windowDAO = getContainer().getComponentInstanceOfType(WindowDAO.class);
    
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
    WindowEntity entity = createInstance("content1", AppType.WSRP);
    windowDAO.create(entity);
    end();
    begin();
    
    WindowEntity result = windowDAO.find(entity.getId());
    assertNotNull(result);
    assertContainer(entity, result);
  }
  
  public void testFindByIds() {
    WindowEntity entity1 = createInstance("content1", AppType.PORTLET);
    windowDAO.create(entity1);
    WindowEntity entity2 = createInstance("content2", AppType.GADGET);
    windowDAO.create(entity2);
    end();
    begin();
    
    List<WindowEntity> results = windowDAO.findByIds(Arrays.asList(entity1.getId(), entity2.getId()));
    assertEquals(2, results.size());
  }
  
  private WindowEntity createInstance(String contentId, AppType type) {
    WindowEntity entity = new WindowEntity();
    entity.setAppType(type);
    entity.setContentId(contentId);
    entity.setCustomization("testCustom".getBytes());
    entity.setHeight("testHeight");
    entity.setIcon("testIcon");
    entity.setProperties("testProps");
    entity.setShowApplicationMode(true);
    entity.setShowApplicationState(true);
    entity.setShowInfoBar(true);
    entity.setTheme("testTheme");
    entity.setTitle("testTitle");
    entity.setDescription("testDesc");
    entity.setTitle("testTitle");
    entity.setWidth("testWidth");
    return entity;
  }
  
  private void assertContainer(WindowEntity expected, WindowEntity result) {
    assertEquals(expected.getDescription(), result.getDescription());
    assertEquals(expected.getHeight(), result.getHeight());
    assertEquals(expected.getIcon(), result.getIcon());
    assertEquals(expected.getId(), result.getId());
    assertEquals(expected.getProperties(), result.getProperties());
    assertEquals(expected.getTitle(), result.getTitle());
    assertEquals(expected.getWidth(), result.getWidth());
    assertEquals(expected.getContentId(), result.getContentId());
    assertEquals(expected.getTheme(), result.getTheme());
    assertEquals(expected.isShowApplicationMode(), result.isShowApplicationMode());
    assertEquals(expected.isShowApplicationState(), result.isShowApplicationState());
    assertEquals(expected.isShowInfoBar(), result.isShowInfoBar());
    assertEquals(expected.getAppType(), result.getAppType());
    assertEquals(expected.getCustomization().length, result.getCustomization().length);
  }
}
