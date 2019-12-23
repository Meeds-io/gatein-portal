package org.exoplatform.portal.jdbc.migration;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.application.registry.*;
import org.exoplatform.application.registry.impl.ApplicationRegistryServiceImpl;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;

public class AppRegistryMigrationService extends AbstractMigrationService<ApplicationCategory> {
  public static final String             EVENT_LISTENER_KEY = "PORTAL_APPLICATIONS_MIGRATION";

  private ApplicationRegistryService     appService;

  private ApplicationRegistryServiceImpl jcrAppService;

  private List<ApplicationCategory>      data;

  public AppRegistryMigrationService(InitParams initParams,
                                     POMDataStorage pomStorage,
                                     ApplicationRegistryService appService,
                                     ApplicationRegistryServiceImpl jcrAppService,
                                     ListenerService listenerService,
                                     RepositoryService repoService,
                                     SettingService settingService,
                                     EntityManagerService entityManagerService) {
    super(initParams, pomStorage, listenerService, repoService, settingService, entityManagerService);
    this.appService = appService;
    this.jcrAppService = jcrAppService;
  }

  @Override
  protected void beforeMigration() {
    MigrationContext.setAppDone(false);
  }

  @Override
  public void doMigration() {
    int offset = 0;

    long t = System.currentTimeMillis();
    try {
      log.info("| \\ START::Application Categories migration ---------------------------------");
      List<ApplicationCategory> categories = getAppCategory();

      if (categories == null || categories.isEmpty()) {
        return;
      }

      for (ApplicationCategory category : categories) {
        if (forceStop) {
          break;
        }

        log.info("|  \\ START::category number: {} ({} category)", offset, category.getName());
        long t1 = System.currentTimeMillis();

        try {
          ApplicationCategory created = appService.getApplicationCategory(category.getName());
          if (created == null) {
            for (Application app : category.getApplications()) {
              if (ApplicationType.WSRP_PORTLET.equals(app.getType())) {
                continue;
              }
              appService.save(category, app);
            }

            created = appService.getApplicationCategory(category.getName());
          } else {
            log.info("Ignoring, this category: {} already in JPA", created.getName());
          }
          //
          offset++;
          if (offset % limitThreshold == 0) {
            restartTransaction();
          }

          broadcastListener(created, created.getName());
          log.info("|  / END::category number {} ({} category) consumed {}(ms)",
                   offset - 1,
                   category.getName(),
                   System.currentTimeMillis() - t1);
        } catch (Exception ex) {
          log.error("exception during migration category: " + category.getName(), ex);
        }
      }
    } finally {
      restartTransaction();
      log.info("| / END::Category migration for ({}) categories consumed {}(ms)",
               offset,
               System.currentTimeMillis() - t);
    }
  }

  @Override
  protected void afterMigration() {
    if (forceStop) {
      return;
    }
    MigrationContext.setAppDone(true);
  }

  public void doRemove() {
    log.info("| \\ START::cleanup Application Categories ---------------------------------");
    long t = System.currentTimeMillis();
    long timePerSite = System.currentTimeMillis();

    RequestLifeCycle.begin(PortalContainer.getInstance());
    int offset = 0;

    try {
      List<ApplicationCategory> categories = getAppCategory();
      if (categories.isEmpty()) {
        return;
      }

      for (ApplicationCategory category : categories) {
        log.info("|  \\ START::cleanup Category number: {} ({} category)", offset, category.getName());
        offset++;

        try {
          jcrAppService.remove(category);

          log.info("|  / END::cleanup ({} category) consumed time {}(ms)",
                   category.getName(),
                   System.currentTimeMillis() - timePerSite);

          timePerSite = System.currentTimeMillis();
          if (offset % limitThreshold == 0) {
            restartTransaction();
          }
        } catch (Exception ex) {
          log.error("Can't remove category: " + category.getName(), ex);
        }
      }
      restartTransaction();
      log.info("| / END::cleanup Categories migration for ({}) category consumed {}(ms)",
               offset,
               System.currentTimeMillis() - t);
    } finally {
      RequestLifeCycle.end();
    }
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }

  private List<ApplicationCategory> getAppCategory() {
    if (data == null) {
      data = new ArrayList<>();
      try {
        data.addAll(jcrAppService.getApplicationCategories());
      } catch (Exception e) {
        log.error("Can't load application categories", e);
      }
    }
    return data;
  }
}
