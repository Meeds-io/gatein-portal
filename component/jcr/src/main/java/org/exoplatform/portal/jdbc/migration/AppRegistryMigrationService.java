package org.exoplatform.portal.jdbc.migration;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.application.registry.*;
import org.exoplatform.application.registry.impl.ApplicationRegistryServiceImpl;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class AppRegistryMigrationService {
  public static final String             EVENT_LISTENER_KEY = "PORTAL_APPLICATIONS_MIGRATION";

  private static final Log               LOG                = ExoLogger.getExoLogger(AppRegistryMigrationService.class);

  private ApplicationRegistryService     appService;

  private ApplicationRegistryServiceImpl jcrAppService;

  private ListenerService                listenerService;

  private List<ApplicationCategory>      data;

  public AppRegistryMigrationService(ApplicationRegistryService appService,
                                     ApplicationRegistryServiceImpl jcrAppService,
                                     ListenerService listenerService) {
    this.appService = appService;
    this.jcrAppService = jcrAppService;
    this.listenerService = listenerService;
  }

  public void doMigration() {
    long t = System.currentTimeMillis();
    LOG.info("| \\ START::Application Categories migration ---------------------------------");
    List<ApplicationCategory> categories = getAppCategory();

    if (categories == null || categories.isEmpty()) {
      MigrationContext.setAppDone();
      return;
    }

    for (ApplicationCategory category : categories) {
      if (MigrationContext.isForceStop()) {
        break;
      }

      long t1 = System.currentTimeMillis();
      try {
        ApplicationCategory created = appService.getApplicationCategory(category.getName());
        if (created == null) {
          LOG.info("|  \\ START::migrate category {}", category.getName());
          for (Application app : category.getApplications()) {
            if (app == null || !ApplicationType.PORTLET.equals(app.getType())) {
              continue;
            }
            appService.save(category, app);
          }

          broadcastListener(category, category.getName());
          LOG.info("|  / END::migrate category {} in {}ms",
                   category.getName(),
                   System.currentTimeMillis() - t1);
        } else {
          LOG.info("|  / IGNORE::already existing category {} in {}ms",
                   category.getName(),
                   System.currentTimeMillis() - t1);
        }
      } catch (Exception ex) {
        LOG.error("|  / END::migrate category {} in {}ms",
                  category.getName(),
                  System.currentTimeMillis() - t1,
                  ex);
      } finally {
        MigrationContext.restartTransaction();
      }
    }
    MigrationContext.setAppDone();
    LOG.info("| / END::Category migration in {}ms",
             System.currentTimeMillis() - t);
  }

  public void doRemove() {
    LOG.info("| \\ START::cleanup Application Categories ---------------------------------");
    long t = System.currentTimeMillis();
    List<ApplicationCategory> categories = getAppCategory();
    if (categories.isEmpty()) {
      MigrationContext.setAppCleanupDone();
      return;
    }

    for (ApplicationCategory category : categories) {
      LOG.info("|  \\ START::cleanup Category number: {} ({} category)", category.getName());
      try {
        jcrAppService.remove(category);
        LOG.info("|  \\ END::cleanup Category {}", category.getName());
      } catch (Exception ex) {
        LOG.error("|  \\ END::cleanup Category {}", category.getName(), ex);
      } finally {
        MigrationContext.restartTransaction();
      }
    }
    LOG.info("| / END::cleanup Application categories in {}ms",
             System.currentTimeMillis() - t);
    MigrationContext.setAppCleanupDone();
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
        LOG.error("Can't load application categories", e);
      }
    }
    return data;
  }

  protected void broadcastListener(Object t, String newId) {
    try {
      listenerService.broadcast(new Event<>(getListenerKey(), t, newId));
    } catch (Exception e) {
      LOG.error("Failed to broadcast event", e);
    }
  }

}
