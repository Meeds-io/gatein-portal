package org.exoplatform.portal.mop.jdbc.service;

import java.util.Comparator;
import java.util.List;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.mop.*;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.jdbc.dao.*;
import org.exoplatform.portal.mop.page.*;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PageKey;

public class CachedPageService implements PageService, ModelDataStorage {

  private PageServiceImpl      pageServiceImpl;

  private JDBCModelStorageImpl modelStorageImpl;

  public CachedPageService(PageDAO pageDAO,
                           ContainerDAO containerDAO,
                           WindowDAO windowDAO,
                           PermissionDAO permissionDAO,
                           SiteDAO siteDAO,
                           SettingService settingService,
                           ConfigurationManager confManager) {
    pageServiceImpl = new PageServiceImpl(pageDAO, containerDAO, windowDAO, permissionDAO, siteDAO);
    modelStorageImpl = new JDBCModelStorageImpl(siteDAO,
                                                pageDAO,
                                                windowDAO,
                                                containerDAO,
                                                permissionDAO,
                                                settingService,
                                                confManager);
  }

  @Override
  public void create(PortalData config) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void save(PortalData config) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public PortalData getPortalConfig(PortalKey key) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void remove(PortalData config) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public PageData getPage(PageKey key) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ModelChange> save(PageData page) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <S> String getId(ApplicationState<S> state) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <S> S load(ApplicationState<S> state, ApplicationType<S> type) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <S> ApplicationState<S> save(ApplicationState<S> state, S preferences) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> LazyPageList<T> find(Query<T> q) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Container getSharedLayout(String siteName) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void save() throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String[] getSiteInfo(String workspaceObjectId) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <S> ApplicationData<S> getApplicationData(String applicationStorageId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <A> A adapt(ModelData modelData, Class<A> type) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <A> A adapt(ModelData modelData, Class<A> type, boolean create) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Status getImportStatus() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void saveImportStatus(Status status) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public PageContext loadPage(org.exoplatform.portal.mop.page.PageKey key) throws NullPointerException, PageServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<PageContext> loadPages(SiteKey siteKey) throws NullPointerException, PageServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean savePage(PageContext page) throws NullPointerException, PageServiceException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean destroyPage(org.exoplatform.portal.mop.page.PageKey key) throws NullPointerException, PageServiceException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public PageContext clone(org.exoplatform.portal.mop.page.PageKey src,
                           org.exoplatform.portal.mop.page.PageKey dst) throws NullPointerException, PageServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public QueryResult<PageContext> findPages(int offset,
                                            int limit,
                                            SiteType siteType,
                                            String siteName,
                                            String pageName,
                                            String pageTitle) throws PageServiceException {
    // TODO Auto-generated method stub
    return null;
  }

  
}
