package org.exoplatform.portal.mop.dao;

import org.gatein.api.page.PageQuery;

import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;

public class PageDAOTest extends AbstractDAOTest {
  private PageDAO pageDAO;
  private SiteDAO siteDAO;
  
  @Override
  protected void setUp() throws Exception {    
    super.setUp();
    this.pageDAO = getService(PageDAO.class);
    this.siteDAO = getService(SiteDAO.class);
    begin();
  }

  @Override
  protected void tearDown() throws Exception {
    pageDAO.deleteAll();
    siteDAO.deleteAll();
    super.tearDown();
    end();
  }

  public void testCreatePage() {
    PageEntity entity = createInstance("portal::b::c", "testCreatePage", "create page description");
    pageDAO.create(entity);
    restartTransaction();
    
    PageEntity result = pageDAO.find(entity.getId());
    assertNotNull(result);
    assertPage(entity, result);
  }
  
  public void testFindByKey() {
    
    PageEntity entity = createInstance("portal::b::c", "testPage", null);
    pageDAO.create(entity);
    restartTransaction();
    
    PageEntity result = pageDAO.findByKey(PageKey.parse("portal::b::c"));
    assertNotNull(result);
    assertPage(entity, result);
  }
  
  public void testFindByQuery() throws Exception {
    PageEntity page1 = createInstance("portal::b::c1", "aBc dEf", null);
    pageDAO.create(page1);    
    PageEntity page2 = createInstance("portal::b::c2", "Efg Hik", null);
    pageDAO.create(page2);    
    restartTransaction();
        
    PageQuery.Builder query1 = new PageQuery.Builder();    
    query1.withSiteType(org.gatein.api.site.SiteType.SITE).withSiteName("b").withDisplayName("ef");
    assertEquals(2, pageDAO.findByQuery(query1.build()).getSize());
    
    PageQuery.Builder query2 = new PageQuery.Builder();
    query2.withSiteType(org.gatein.api.site.SiteType.SITE).withSiteName("b").withDisplayName("hik");
    assertEquals(1, pageDAO.findByQuery(query2.build()).getSize());
  }

  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }

  private PageEntity createInstance(String key, String displayName, String description) {
    PageEntity entity = new PageEntity();
    PageKey pageKey = PageKey.parse(key);
    entity.setOwner(getOrCreateSite(pageKey.getSite().getName()));
    entity.setName(pageKey.getName());
    entity.setDisplayName(displayName);
    entity.setDescription(description);
    entity.setShowMaxWindow(true);
    entity.setHideSharedLayout(true);
    entity.setFactoryId("testFactoryId");
    entity.setPageBody("testPageBody");
    entity.setPageType(PageType.PAGE);
    return entity;
  }
  
  private void assertPage(PageEntity entity, PageEntity result) {
    assertEquals(entity.getId(), result.getId());
    assertEquals(entity.getDescription(), result.getDescription());
    assertEquals(entity.getDisplayName(), result.getDisplayName());
    assertEquals(entity.getFactoryId(), result.getFactoryId());
    assertEquals(entity.getOwnerId(), result.getOwnerId());
    assertEquals(entity.getOwnerType(), result.getOwnerType());
    assertEquals(entity.getName(), result.getName());
    assertEquals(entity.getFactoryId(), result.getFactoryId());
    assertEquals(entity.getPageBody(), result.getPageBody());
  }
  
  private SiteEntity getOrCreateSite(String name) {
    SiteEntity siteEntity = siteDAO.findByKey(SiteType.PORTAL.key(name));
    if (siteEntity == null) {
      siteEntity = new SiteEntity();
      siteEntity.setSiteType(SiteType.PORTAL);
      siteEntity.setName(name);
      siteDAO.create(siteEntity);
    }
    return siteEntity;
  }
}
