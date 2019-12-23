package org.exoplatform.portal.jdbc.migration;

import java.util.*;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.*;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;

public class PageMigrationService extends AbstractMigrationService<PageContext> {
  public static final String EVENT_LISTENER_KEY = "PORTAL_PAGES_MIGRATION";

  private ModelDataStorage   modelStorage;

  private PageService        pageService;

  private PageServiceImpl    jcrPageService;

  public PageMigrationService(InitParams initParams,
                              POMDataStorage pomStorage,
                              ModelDataStorage modelStorage,
                              PageService pageService,
                              PageServiceImpl jcrPageService,
                              ListenerService listenerService,
                              RepositoryService repoService,
                              SettingService settingService,
                              EntityManagerService entityManagerService) {
    super(initParams, pomStorage, listenerService, repoService, settingService, entityManagerService);
    this.modelStorage = modelStorage;
    this.pageService = pageService;
    this.jcrPageService = jcrPageService;
  }

  @Override
  protected void beforeMigration() {
    MigrationContext.setPageDone(false);
  }

  public void doMigration() {
    long t = System.currentTimeMillis();
    Set<String> failedPages = new HashSet<>();
    int offset = 0;

    try {
      log.info("| \\ START::pages migration ---------------------------------");
      QueryResult<PageContext> pages;
      do {
        pages = jcrPageService.findPages(offset, limitThreshold, null, null, null, null);

        Iterator<PageContext> pageItr = pages.iterator();
        while (pageItr.hasNext()) {
          if (forceStop) {
            break;
          }
          offset++;
          PageContext page = pageItr.next();
          PageKey key = page.getKey();
          if (isPageMigrated(key) || !isSiteMigrated(key.getSite())) {
            continue;
          }

          log.info("|  \\ START::page number: {} ({} page)", offset, page.getKey().format());
          long t1 = System.currentTimeMillis();

          try {
            SiteKey siteKey = key.getSite();
            PageContext created = pageService.loadPage(key);

            org.exoplatform.portal.pom.data.PageKey pomPageKey =
                                                               new org.exoplatform.portal.pom.data.PageKey(siteKey.getTypeName(),
                                                                                                           siteKey.getName(),
                                                                                                           key.getName());
            PageData pageData = pomStorage.getPage(pomPageKey);

            if (created == null) {
              log.info("Creating page: {} already in JPA", key);
              pageService.savePage(page);
              PageData migrate = new PageData(
                                              null,
                                              pageData.getId(),
                                              pageData.getName(),
                                              pageData.getIcon(),
                                              pageData.getTemplate(),
                                              pageData.getFactoryId(),
                                              pageData.getTitle(),
                                              pageData.getDescription(),
                                              pageData.getWidth(),
                                              pageData.getHeight(),
                                              pageData.getAccessPermissions(),
                                              this.migrateComponents(pageData.getChildren()),
                                              pageData.getOwnerType(),
                                              pageData.getOwnerId(),
                                              pageData.getEditPermission(),
                                              pageData.isShowMaxWindow(),
                                              pageData.getMoveAppsPermissions(),
                                              pageData.getMoveContainersPermissions());

              modelStorage.save(migrate);
            } else {
              log.info("Updating layout for page: {} already in JPA", created.getKey());
              PageData data = modelStorage.getPage(pomPageKey);
              PageData migrate = new PageData(
                                              data.getStorageId(),
                                              data.getId(),
                                              data.getName(),
                                              data.getIcon(),
                                              data.getTemplate(),
                                              data.getFactoryId(),
                                              data.getTitle(),
                                              data.getDescription(),
                                              data.getWidth(),
                                              data.getHeight(),
                                              data.getAccessPermissions(),
                                              this.migrateComponents(pageData.getChildren()),
                                              data.getOwnerType(),
                                              data.getOwnerId(),
                                              data.getEditPermission(),
                                              data.isShowMaxWindow(),
                                              data.getMoveAppsPermissions(),
                                              data.getMoveContainersPermissions());
              modelStorage.save(migrate);
            }
            setPageMigrated(key);

            //
            if (offset % limitThreshold == 0) {
              restartTransaction();
            }

            created = pageService.loadPage(page.getKey());
            broadcastListener(created, created.getKey().toString());

            log.info("|  / END::page number {} ({} page) consumed {}(ms)",
                     offset,
                     page.getKey(),
                     System.currentTimeMillis() - t1);
          } catch (Exception ex) {
            log.error("Exception during migration page: " + page.getKey(), ex);
            failedPages.add(page.getKey().format());
          }
        }
        restartTransaction();
      } while (pages.getSize() > 0);
    } finally {
      MigrationContext.setPagesMigrateFailed(failedPages);
      restartTransaction();
      log.info("| / END::page migration for ({}) page(s) consumed {}(ms)", offset, System.currentTimeMillis() - t);
    }
  }

  @Override
  protected void afterMigration() {
    if (forceStop || !MigrationContext.getPagesMigrateFailed().isEmpty()) {
      return;
    }
    MigrationContext.setPageDone(MigrationContext.isSiteDone());
  }

  public void doRemove() {
    log.info("| \\ START::cleanup pages ---------------------------------");
    long t = System.currentTimeMillis();
    long timePerpage = System.currentTimeMillis();

    int offset = 0;
    QueryResult<PageContext> pages;

    int errors = 0;
    do {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      pages = jcrPageService.findPages(offset, limitThreshold, null, null, null, null);

      try {
        Iterator<PageContext> pageItr = pages.iterator();
        while (pageItr.hasNext()) {
          PageContext page = pageItr.next();
          log.info("|  \\ START::cleanup page number: {} ({} page)", offset, page.getKey());
          offset++;

          try {
            jcrPageService.destroyPage(page.getKey());

            log.info("|  / END::cleanup ({} page) consumed time {}(ms)",
                     page.getKey(),
                     System.currentTimeMillis() - timePerpage);

            timePerpage = System.currentTimeMillis();
          } catch (Exception ex) {
            log.error("Can't remove page", ex);
            errors++;
          }
        }
        log.info("| / END::cleanup pages migration for ({}) page consumed {}(ms)",
                 offset,
                 System.currentTimeMillis() - t);
      } finally {
        RequestLifeCycle.end();
      }
    } while (pages.getSize() > 0);

    boolean pageCleanupDone = errors == 0;
    MigrationContext.setPageCleanupDone(pageCleanupDone);
    if (pageCleanupDone) {
      settingService.remove(CONTEXT, org.exoplatform.commons.api.settings.data.Scope.PAGE);
    }
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
