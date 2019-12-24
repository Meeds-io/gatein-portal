package org.exoplatform.portal.jdbc.migration;

import java.util.*;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.*;
import org.exoplatform.portal.mop.page.*;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PortalKey;
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
          PageContext created = pageService.loadPage(key);

          org.exoplatform.portal.pom.data.PageKey pomPageKey =
                                                             new org.exoplatform.portal.pom.data.PageKey(siteKey.getTypeName(),
                                                                                                         siteKey.getName(),
                                                                                                         key.getName());
          PageData pageData = pomStorage.getPage(pomPageKey);

          if (created == null) {
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
    do {
      pages = jcrPageService.findPages(offset, limitThreshold, null, null, null, null);

      Iterator<PageContext> pageItr = pages.iterator();
      while (pageItr.hasNext()) {
        PageContext page = pageItr.next();
        try {
          jcrPageService.destroyPage(page.getKey());
        } catch (Exception ex) {
          log.error("Can't remove page", ex);
          errors++;
        }
      }
    } while (pages.getSize() > 0);
    if (errors > 0) {
      throw new IllegalStateException("Some errors (" + errors + ") was encountered while removing pages of site "
          + siteToMigrateKey.getType() + "/" + siteToMigrateKey.getId() + "");
    }
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
