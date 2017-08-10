package org.exoplatform.portal.jdbc.dao;

import java.util.List;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.portal.jdbc.test.configuration.xml") })
public class SiteDAOTest extends AbstractKernelTest {
  private SiteDAO siteDAO;

  @Override
  protected void setUp() throws Exception {
    begin();
    super.setUp();
    this.siteDAO = getContainer().getComponentInstanceOfType(SiteDAO.class);
  }

  @Override
  protected void tearDown() throws Exception {
    siteDAO.deleteAll();
    super.tearDown();
    end();
  }
  
  public void testCreate() {
    SiteEntity siteEntity = creatSiteEntity();
    siteDAO.create(siteEntity);
    end();
    begin();
    
    SiteEntity result = siteDAO.find(siteEntity.getId());
    assertNotNull(result);
    assertSite(siteEntity, result);
  }
  
  public void testFind() {
    SiteEntity siteEntity = creatSiteEntity();
    siteDAO.create(siteEntity);
    end();
    begin();
    
    SiteEntity result = siteDAO.findByKey(new SiteKey(siteEntity.getSiteType(), siteEntity.getName()));
    assertNotNull(result);
    assertSite(siteEntity, result);
    
    List<SiteEntity> results = siteDAO.findByType(siteEntity.getSiteType());
    assertEquals(1, results.size());
    assertSite(siteEntity, results.get(0));
    
    List<SiteKey> keys = siteDAO.findSiteKey(siteEntity.getSiteType());
    assertEquals(1, keys.size());
    assertEquals(siteEntity.getName(), keys.get(0).getName());
  }

  private void assertSite(SiteEntity expected, SiteEntity siteEntity) {
    assertEquals(expected.getDescription(), siteEntity.getDescription());
    assertEquals(expected.getId(), siteEntity.getId());
    assertEquals(expected.getLabel(), siteEntity.getLabel());
    assertEquals(expected.getName(), siteEntity.getName());
    assertEquals(expected.getSiteBody(), siteEntity.getSiteBody());
    assertEquals(expected.getSkin(), siteEntity.getSkin());
    assertEquals(expected.getSiteType(), siteEntity.getSiteType());
    assertEquals(expected.getProperties(), siteEntity.getProperties());
  }

  private SiteEntity creatSiteEntity() {
    SiteEntity siteEntity = new SiteEntity();
    siteEntity.setDescription("testDesc");
    siteEntity.setLabel("testLbl");
    siteEntity.setLocale("testLocale");
    siteEntity.setName("testName");
    siteEntity.setSiteBody("tesBody");
    siteEntity.setSiteType(SiteType.PORTAL);
    siteEntity.setSkin("testSkin");
    siteEntity.setProperties("testProperties");
    return siteEntity;
  }
}
