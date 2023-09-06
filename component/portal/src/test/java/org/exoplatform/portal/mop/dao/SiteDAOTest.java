package org.exoplatform.portal.mop.dao;

import java.util.List;

import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

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
    SiteEntity siteEntity = createSiteEntity("test", SiteType.PORTAL);
    siteDAO.create(siteEntity);
    restartTransaction();

    SiteEntity result = siteDAO.find(siteEntity.getId());
    assertNotNull(result);
    assertSite(siteEntity, result);

    siteDAO.delete(siteEntity);
  }

  public void testFind() {
    SiteEntity siteEntity = createSiteEntity("test1", SiteType.PORTAL);
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

    SiteEntity siteEntity2 = createSiteEntity("test2", SiteType.PORTAL);
    siteEntity2.setDisplayed(false);
    siteEntity2.setDisplayOrder(4);
    siteDAO.create(siteEntity2);
    restartTransaction();
    
    SiteEntity siteEntity3 = createSiteEntity("test3", SiteType.GROUP);
    siteEntity3.setDisplayed(true);
    siteEntity3.setDisplayOrder(3);
    siteDAO.create(siteEntity3);
    restartTransaction();
    
    SiteEntity siteEntity4 = createSiteEntity("test4", SiteType.USER);
    siteEntity4.setDisplayed(true);
    siteEntity4.setDisplayOrder(2);
    siteDAO.create(siteEntity4);
    restartTransaction();
    
    SiteFilter filter = new SiteFilter();
    filter.setFilterByDisplayed(true);
    filter.setDisplayed(false);
    results = siteDAO.findSites(filter);
    assertEquals(1, results.size());

    filter.setDisplayed(true);
    filter.setSiteType(SiteType.PORTAL);
    results = siteDAO.findSites(filter);
    assertEquals(1, results.size());
    assertEquals(siteEntity.getName(), results.get(0).getName());
    
    filter.setExcludedSiteName("test1");
    results = siteDAO.findSites(filter);
    assertEquals(0, results.size());
    
    filter.setExcludedSiteName(null);
    filter.setSiteType(SiteType.GROUP);
    results = siteDAO.findSites(filter);
    assertEquals(1, results.size());
    assertEquals(siteEntity3.getName(), results.get(0).getName());

    filter.setSiteType(null);
    results = siteDAO.findSites(filter);
    assertEquals(3, results.size());
    
    filter.setExcludedSiteType(SiteType.PORTAL);
    results = siteDAO.findSites(filter);
    assertEquals(2, results.size());
    assertEquals(siteEntity4.getName(), results.get(1).getName());
    
//    filter.setSortByDisplayOrder(true);
//    results = siteDAO.findSites(filter);
//    assertEquals(2, results.size());
//    assertEquals(siteEntity3.getName(), results.get(1).getName());
//    
//    siteEntity3.setDisplayOrder(2);
//    siteDAO.update(siteEntity3);
//    results = siteDAO.findSites(filter);
//    assertEquals(2, results.size());
//    assertEquals(siteEntity4.getName(), results.get(1).getName());
    
    siteEntity3.setName("/spaces/test3");
    siteDAO.update(siteEntity3);
    filter.setExcludeSpaceSites(true);
    results = siteDAO.findSites(filter);
    assertEquals(1, results.size());
    assertEquals(siteEntity4.getName(), results.get(0).getName());
    
    filter.setExcludedSiteType(null);
    filter.setFilterByDisplayed(false);
    results = siteDAO.findSites(filter);
    assertEquals(3, results.size());
    
    filter.setExcludeSpaceSites(false);
    results = siteDAO.findSites(filter);
    assertEquals(4, results.size());
    
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

  private SiteEntity createSiteEntity(String siteName, SiteType siteType) {
    SiteEntity siteEntity = new SiteEntity();
    siteEntity.setDescription(siteName + "Desc");
    siteEntity.setLabel(siteName + "Label");
    siteEntity.setLocale(siteName + "Locale");
    siteEntity.setName(siteName);
    siteEntity.setSiteBody(siteName + "Body");
    siteEntity.setSiteType(siteType);
    siteEntity.setSkin(siteName + "Skin");
    siteEntity.setProperties(siteName + "Properties");
    return siteEntity;
  }
}
