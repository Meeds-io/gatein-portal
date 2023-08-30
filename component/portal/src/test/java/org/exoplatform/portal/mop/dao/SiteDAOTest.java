package org.exoplatform.portal.mop.dao;

import java.util.List;

import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.dao.SiteDAO;

public class SiteDAOTest extends AbstractDAOTest {
  private SiteDAO siteDAO;

  @Override
  protected void setUp() throws Exception {
    begin();
    super.setUp();
    this.siteDAO = getContainer().getComponentInstanceOfType(SiteDAO.class);
    this.siteDAO.deleteAll();
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
    restartTransaction();
    
    SiteEntity result = siteDAO.find(siteEntity.getId());
    assertNotNull(result);
    assertSite(siteEntity, result);

    siteDAO.delete(siteEntity);
  }
  
  public void testFind() {
    SiteEntity siteEntity = creatSiteEntity();
    siteEntity.setDisplayed(true);
    siteEntity.setDisplayOrder(1);
    siteDAO.create(siteEntity);
    restartTransaction();
    
    SiteEntity result = siteDAO.findByKey(new SiteKey(siteEntity.getSiteType(), siteEntity.getName()));
    assertNotNull(result);
    assertSite(siteEntity, result);
    
    List<SiteEntity> results = siteDAO.findByType(siteEntity.getSiteType());
    assertEquals(1, results.size());
    
    List<SiteKey> keys = siteDAO.findSiteKey(siteEntity.getSiteType());
    assertEquals(1, keys.size());

    SiteEntity siteEntity2 = creatSiteEntity();
    siteEntity2.setName("siteName");
    siteEntity2.setDisplayed(true);
    siteEntity2.setDisplayOrder(2);
    siteDAO.create(siteEntity2);
    restartTransaction();

    SiteFilter filter = new SiteFilter();
    filter.setDisplayed(true);
    filter.setSiteType(SiteType.PORTAL);
    filter.setOrderByDisplayOrder(true);
    results = siteDAO.getSitesByFilter(filter);
    assertEquals(2, results.size());
    assertEquals(1, results.get(0).getDisplayOrder());
    assertEquals(2, results.get(1).getDisplayOrder());

    filter.setDisplayed(false);
    results = siteDAO.getSitesByFilter(filter);
    assertEquals(0, results.size());

    filter.setDisplayed(true);
    filter.setOrderByDisplayOrder(false);
    filter.setOrderByName(true);
    results = siteDAO.getSitesByFilter(filter);
    assertEquals(2, results.size());
    assertEquals(siteEntity2.getName(), results.get(0).getName());
    assertEquals(siteEntity.getName(), results.get(1).getName());

    filter.setExcludedSiteName("siteName");
    results = siteDAO.getSitesByFilter(filter);
    assertEquals(1, results.size());
    assertEquals(siteEntity.getName(), results.get(0).getName());
    filter.setExcludedSiteName(null);

    filter.setSiteType(SiteType.GROUP);
    results = siteDAO.getSitesByFilter(filter);
    assertEquals(0, results.size());

    filter.setSiteType(null);
    results = siteDAO.getSitesByFilter(filter);
    assertEquals(2, results.size());

    siteDAO.delete(siteEntity);
    siteDAO.delete(siteEntity2);
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
