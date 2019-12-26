package org.exoplatform.portal.jdbc.migration;

import java.util.*;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.*;
import org.exoplatform.portal.mop.page.*;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;

public class PageMigrationService extends AbstractMigrationService {
  public static final String EVENT_LISTENER_KEY = "PORTAL_PAGES_MIGRATION";

  private PageService        pageService;

  private PageServiceImpl    jcrPageService;

  public PageMigrationService(InitParams initParams,
                              POMDataStorage pomStorage,
                              ModelDataStorage modelStorage,
                              PageService pageService,
                              PageServiceImpl jcrPageService,
                              ListenerService listenerService,
                              RepositoryService repoService,
                              SettingService settingService) {
    super(initParams, pomStorage, modelStorage, listenerService, repoService, settingService);
    this.pageService = pageService;
    this.jcrPageService = jcrPageService;
  }

  @Override
  public void doMigrate(PortalKey siteToMigrateKey) throws Exception {
    Set<String> failedPages = new HashSet<>();
    int offset = 0;

    QueryResult<PageContext> pages;
    do {
      if (MigrationContext.isForceStop()) {
        throw new InterruptedException();
      }
      pages = jcrPageService.findPages(offset,
                                       limitThreshold,
                                       SiteType.valueOf(siteToMigrateKey.getType().toUpperCase()),
                                       siteToMigrateKey.getId(),
                                       null,
                                       null);
      Iterator<PageContext> pageItr = pages.iterator();
      while (pageItr.hasNext()) {
        if (MigrationContext.isForceStop()) {
          throw new InterruptedException();
        }

        offset++;
        PageContext page = pageItr.next();
        PageKey key = page.getKey();
        if (MigrationContext.isPageMigrated(key)) {
          continue;
        }

        try {
          SiteKey siteKey = key.getSite();
          String siteType = siteKey.getTypeName();
          String siteId = siteKey.getName();

          PageContext created = pageService.loadPage(key);

          org.exoplatform.portal.mop.PageKey pomPageKey =
                                                             new org.exoplatform.portal.mop.PageKey(siteKey.getTypeName(),
                                                                                                         siteKey.getName(),
                                                                                                         key.getName());
          PageData pageDataToMigrate = pomStorage.getPage(pomPageKey);
          String storageId = null;
          if (created == null) {
            pageService.savePage(page);
          } else {
            PageData data = modelStorage.getPage(pomPageKey);
            storageId = data.getStorageId();
          }

          log.info("|  ---- | {}::page {}::{}::{}",
                   storageId == null ? "CREATE" : "UPDATE",
                   siteType,
                   siteId,
                   page.getKey().getName());

          List<ComponentData> pageLayout = this.migrateComponents(pageDataToMigrate.getChildren());

          PageData migratedPage = new PageData(storageId,
                                               pageDataToMigrate.getId(),
                                               pageDataToMigrate.getName(),
                                               pageDataToMigrate.getIcon(),
                                               pageDataToMigrate.getTemplate(),
                                               pageDataToMigrate.getFactoryId(),
                                               pageDataToMigrate.getTitle(),
                                               pageDataToMigrate.getDescription(),
                                               pageDataToMigrate.getWidth(),
                                               pageDataToMigrate.getHeight(),
                                               pageDataToMigrate.getAccessPermissions(),
                                               pageLayout,
                                               pageDataToMigrate.getOwnerType(),
                                               pageDataToMigrate.getOwnerId(),
                                               pageDataToMigrate.getEditPermission(),
                                               pageDataToMigrate.isShowMaxWindow(),
                                               pageDataToMigrate.getMoveAppsPermissions(),
                                               pageDataToMigrate.getMoveContainersPermissions());
          modelStorage.save(migratedPage);
          MigrationContext.setPageMigrated(key);

          //
          if (offset % limitThreshold == 0) {
            MigrationContext.restartTransaction();
          }

          created = pageService.loadPage(page.getKey());
          broadcastListener(created, created.getKey().toString());
        } catch (Exception ex) {
          log.error("Exception during migration page: " + page.getKey(), ex);
          failedPages.add(page.getKey().format());
        }
      }
      MigrationContext.restartTransaction();
    } while (pages.getSize() > 0);

    if (!failedPages.isEmpty()) {
      throw new IllegalStateException("Some errors was encountered while migrating pages " + failedPages);
    }
  }

  @Override
  public void doRemove(PortalKey siteToMigrateKey) throws Exception {
    int errors = 0;
    int offset = 0;
    QueryResult<PageContext> pages;
    Set<PageKey> deletedPages = new HashSet<>();
    do {
      pages = jcrPageService.findPages(offset,
                                       limitThreshold,
                                       SiteType.valueOf(siteToMigrateKey.getType().toUpperCase()),
                                       siteToMigrateKey.getId(),
                                       null,
                                       null);

      Iterator<PageContext> pageItr = pages.iterator();
      while (pageItr.hasNext()) {
        PageContext page = pageItr.next();
        PageKey key = page.getKey();
        String siteType = key.getSite().getTypeName();
        String siteId = key.getSite().getName();
        try {
          if (deletedPages.contains(key)) {
            log.info("|  ---- | IGNORE::page {}::{}::{} (already deleted)",
                     siteType,
                     siteId,
                     key.getName());
            continue;
          }
          deletedPages.add(key);
          log.info("|  ---- | REMOVE::page {}::{}::{}",
                   siteType,
                   siteId,
                   key.getName());
          jcrPageService.destroyPage(key);
        } catch (Exception ex) {
          log.error("Can't remove page {}::{}::{}",
                    siteType,
                    siteId,
                    key.getName(),
                    ex);
          errors++;
        }
      }
      MigrationContext.restartTransaction();
    } while (pages.getSize() > 0 && errors < limitThreshold);
    if (errors > 0) {
      throw new IllegalStateException("Some errors (" + errors + ") was encountered while removing pages of site "
          + siteToMigrateKey.getType() + "/" + siteToMigrateKey.getId() + "");
    }
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
