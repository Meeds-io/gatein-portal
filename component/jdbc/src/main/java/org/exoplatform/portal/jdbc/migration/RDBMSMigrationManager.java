package org.exoplatform.portal.jdbc.migration;

import java.lang.reflect.Field;
import java.util.*;

import org.picocontainer.Startable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.*;
import org.exoplatform.portal.jdbc.migration.MigrationContext.PortalEntityType;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.filter.*;

public class RDBMSMigrationManager implements Startable {
  private static final Log            LOG                          = ExoLogger.getLogger(RDBMSMigrationManager.class);

  public static final String          MIGRATION_SETTING_GLOBAL_KEY = "MIGRATION_SETTING_GLOBAL";

  private PortalContainer             container;

  private SiteMigrationService        siteMigrationService;

  private PageMigrationService        pageMigrationService;

  private NavigationMigrationService  navMigrationService;

  private AppRegistryMigrationService appMigrationService;

  private SettingService              settingService;

  private ExtensibleFilter            extensibleFilter;

  public RDBMSMigrationManager(PortalContainer container,
                               SiteMigrationService siteMigrationService,
                               PageMigrationService pageMigrationService,
                               NavigationMigrationService navMigrationService,
                               AppRegistryMigrationService appMigrationService,
                               ExtensibleFilter extensibleFilter,
                               SettingService settingService) {
    this.container = container;
    this.siteMigrationService = siteMigrationService;
    this.pageMigrationService = pageMigrationService;
    this.navMigrationService = navMigrationService;
    this.appMigrationService = appMigrationService;
    this.settingService = settingService;
    this.extensibleFilter = extensibleFilter;
  }

  @Override
  public void start() {
    if (MigrationContext.isDone()) {
      LOG.info("Overall Portal JCR to RDBMS migration already finished, ignore it.");
      return;
    }

    installMigrationFilter();

    Runnable migrateTask = new Runnable() {
      @Override
      public void run() {
        //
        Field field = null;
        ExoContainerContext.setCurrentContainer(container);
        RequestLifeCycle.begin(container);
        try {
          field = SessionImpl.class.getDeclaredField("FORCE_USE_GET_NODES_LAZILY");
          if (field != null) {
            field.setAccessible(true);
            field.set(null, true);
          }

          List<PortalKey> sitesToMigrate = siteMigrationService.getSitesToMigrate();

          migrateAppRegistry();
          Set<PortalKey> failedSitesToMigrate = migratePortals();
          removeAppRegistry();
          Set<PortalKey> failedSitesToRemove = removePortals();
          setMigrationAsDone(failedSitesToMigrate, failedSitesToRemove);

          LOG.info("END PORTAL Migration for {} sites, failed sites to migrate = {}, failed sites to cleanup = {}. Overall migration finished successfully= {}",
                   sitesToMigrate.size(),
                   failedSitesToMigrate.isEmpty() ? "'0 sites'" : failedSitesToMigrate,
                   failedSitesToRemove.isEmpty() ? "'0 sites'" : failedSitesToRemove,
                   MigrationContext.isAppDone(),
                   MigrationContext.isAppCleanupDone(),
                   MigrationContext.isDone());
        } catch (Exception e) {
          LOG.error("Failed to run Portal Migration data from JCR to RDBMS", e);
        } finally {
          RequestLifeCycle.end();
          if (field != null) {
            try {
              field.set(null, false);
            } catch (Exception e) {
              LOG.warn(e.getMessage(), e);
            }
          }
        }
      }
    };
    Thread migrationThread = new Thread(migrateTask);
    migrationThread.setPriority(Thread.NORM_PRIORITY);
    migrationThread.setName("PORTAL-MIGRATION-RDBMS");
    migrationThread.start();
  }

  private void installMigrationFilter() {
    InitParams params = new InitParams();
    ObjectParameter objectParameter = new ObjectParameter();
    objectParameter.setName("filter");
    FilterDefinition filterDefinition = new FilterDefinition(new MigrationFilter(), Collections.singletonList("/.*"));
    objectParameter.setObject(filterDefinition);
    params.addParameter(objectParameter);
    this.extensibleFilter.addFilterDefinitions(new FilterDefinitionPlugin(params));
  }

  @Override
  public void stop() {
    MigrationContext.setForceStop();
  }

  private void setMigrationAsDone(Set<PortalKey> failedSitesToMigrate, Set<PortalKey> failedSitesToRemove) {
    if (MigrationContext.isAppCleanupDone() && failedSitesToMigrate.isEmpty() && failedSitesToRemove.isEmpty()) {
      settingService.remove(MigrationContext.CONTEXT);
      MigrationContext.restartTransaction();

      MigrationContext.setDone();
      MigrationContext.restartTransaction();
    }
  }

  private Set<PortalKey> removePortals() {
    Set<PortalKey> failedSitesToRemove = new HashSet<>();
    long startTime = System.currentTimeMillis();
    LOG.info("START CLEANUP PORTAL DATA ---------------------------------------------------");
    doRemove(failedSitesToRemove);
    LOG.info("END PORTAL DATA CLEANUP in {}ms-----------------------------------------------------",
             System.currentTimeMillis() - startTime);
    return failedSitesToRemove;
  }

  private void removeAppRegistry() {
    if (MigrationContext.isAppDone() && !MigrationContext.isAppCleanupDone()) {
      appMigrationService.doRemove();
      MigrationContext.restartTransaction();
    }
  }

  private Set<PortalKey> migratePortals() {
    Set<PortalKey> failedSitesToMigrate = new HashSet<>();
    long startTime = System.currentTimeMillis();
    LOG.info("START ASYNC PORTAL DATA MIGRATION---------------------------------------------------");
    doMigrate(failedSitesToMigrate);
    LOG.info("END PORTAL DATA MIGRATION in {}ms-----------------------------------------------------",
             System.currentTimeMillis() - startTime);
    return failedSitesToMigrate;
  }

  private void migrateAppRegistry() {
    if (MigrationContext.isAppDone()) {
      LOG.info("APPLICATION REGISTRY migration already finished, ignore it.");
    } else {
      appMigrationService.doMigration();
      MigrationContext.restartTransaction();
    }
  }

  private void doMigrate(Set<PortalKey> failedSitesToMigrate) {
    int totalSitesToMigrateCount = MigrationContext.getSitesCountToMigrate();
    for (int siteToMigrateIndex = 0; siteToMigrateIndex < totalSitesToMigrateCount; siteToMigrateIndex++) {
      if (MigrationContext.isForceStop()) {
        LOG.info("|  \\ FORCE STOPPING MIGRATION. Migrated {} / {} sites, failed = {}",
                 siteToMigrateIndex,
                 totalSitesToMigrateCount,
                 failedSitesToMigrate.size());
        return;
      }

      PortalKey siteToMigrateKey = MigrationContext.getNextSiteKeyToMigrate();
      boolean migrated = doMigrate(siteMigrationService,
                                   siteToMigrateKey,
                                   PortalEntityType.SITE,
                                   siteToMigrateIndex,
                                   totalSitesToMigrateCount,
                                   failedSitesToMigrate);
      if (!migrated || MigrationContext.isForceStop()) {
        continue;
      }
      migrated = doMigrate(pageMigrationService,
                           siteToMigrateKey,
                           PortalEntityType.PAGE,
                           siteToMigrateIndex,
                           totalSitesToMigrateCount,
                           failedSitesToMigrate);
      if (!migrated || MigrationContext.isForceStop()) {
        continue;
      }

      doMigrate(navMigrationService,
                siteToMigrateKey,
                PortalEntityType.NAVIGATION,
                siteToMigrateIndex,
                totalSitesToMigrateCount,
                failedSitesToMigrate);
    }
  }

  protected boolean doMigrate(AbstractMigrationService migrationService,
                              PortalKey siteToMigrateKey,
                              PortalEntityType entityType,
                              int siteToMigrateIndex,
                              int totalSitesToMigrateCount,
                              Set<PortalKey> failedSitesToMigrate) {
    boolean migrated = true;
    long t1 = System.currentTimeMillis();
    if (MigrationContext.isMigrated(siteToMigrateKey, entityType)) {
      LOG.info("|  ---- \\ IGNORE::ALREADY migrated {} {} / {} (site: {}::{})",
               entityType.getTitle(),
               siteToMigrateIndex,
               totalSitesToMigrateCount,
               siteToMigrateKey.getType(),
               siteToMigrateKey.getId());
    } else {
      LOG.info("|  ---- \\ START::migrate {} {} / {} (site: {}::{})",
               entityType.getTitle(),
               siteToMigrateIndex,
               totalSitesToMigrateCount,
               siteToMigrateKey.getType(),
               siteToMigrateKey.getId());
      try {
        migrationService.doMigrate(siteToMigrateKey);
        MigrationContext.setMigrated(siteToMigrateKey, entityType);

        LOG.info("|  ---- / END::migrate {} {} / {} (site: {}::{}) in {}ms",
                 entityType.getTitle(),
                 siteToMigrateIndex,
                 totalSitesToMigrateCount,
                 siteToMigrateKey.getType(),
                 siteToMigrateKey.getId(),
                 System.currentTimeMillis() - t1);
      } catch (Exception e) {
        migrated = false;
        failedSitesToMigrate.add(siteToMigrateKey);
        LOG.error("|  ---- / END::migrate {} {} / {} (site: {}::{}) in {}ms",
                  entityType.getTitle(),
                  siteToMigrateIndex,
                  totalSitesToMigrateCount,
                  siteToMigrateKey.getType(),
                  siteToMigrateKey.getId(),
                  System.currentTimeMillis() - t1,
                  e);
      } finally {
        MigrationContext.restartTransaction();
      }
    }
    return migrated;
  }

  private void doRemove(Set<PortalKey> failedSitesToRemove) {
    List<PortalKey> sitesToMigrate = siteMigrationService.getSitesToMigrate();
    int totalSitesToMigrateCount = sitesToMigrate.size();
    int siteToMigrateIndex = 0;
    for (PortalKey siteToMigrateKey : sitesToMigrate) {
      if (MigrationContext.isForceStop()) {
        LOG.info("|  \\ FORCE STOPPING CLEANUO. Cleaned up {} / {} sites, failed = {}",
                 siteToMigrateIndex,
                 totalSitesToMigrateCount,
                 failedSitesToRemove.size());
        return;
      }

      siteToMigrateIndex++;

      boolean removed = doRemove(navMigrationService,
                                 siteToMigrateKey,
                                 PortalEntityType.NAVIGATION,
                                 siteToMigrateIndex,
                                 totalSitesToMigrateCount,
                                 failedSitesToRemove);
      if (!removed || MigrationContext.isForceStop()) {
        continue;
      }
      removed = doRemove(pageMigrationService,
                         siteToMigrateKey,
                         PortalEntityType.PAGE,
                         siteToMigrateIndex,
                         totalSitesToMigrateCount,
                         failedSitesToRemove);
      if (!removed || MigrationContext.isForceStop()) {
        continue;
      }

      doRemove(siteMigrationService,
               siteToMigrateKey,
               PortalEntityType.SITE,
               siteToMigrateIndex,
               totalSitesToMigrateCount,
               failedSitesToRemove);
    }
  }

  protected boolean doRemove(AbstractMigrationService migrationService,
                             PortalKey siteToCleanupKey,
                             PortalEntityType entityType,
                             int siteToCleanupIndex,
                             int totalSitesToCleanupCount,
                             Set<PortalKey> failedSitesToCleanup) {
    boolean removed = true;
    long t1 = System.currentTimeMillis();
    if (MigrationContext.isMigrated(siteToCleanupKey, entityType)) {
      LOG.info("|  ---- \\ START::cleanup {} {} / {} (site: {}::{})",
               entityType.getTitle(),
               siteToCleanupIndex,
               totalSitesToCleanupCount,
               siteToCleanupKey.getType(),
               siteToCleanupKey.getId());
      try {
        migrationService.doRemove(siteToCleanupKey);

        LOG.info("|  ---- / END::cleanup {} {} / {} (site: {}::{}) in {}ms",
                 entityType.getTitle(),
                 siteToCleanupIndex,
                 totalSitesToCleanupCount,
                 siteToCleanupKey.getType(),
                 siteToCleanupKey.getId(),
                 System.currentTimeMillis() - t1);
      } catch (Exception e) {
        removed = false;
        failedSitesToCleanup.add(siteToCleanupKey);
        LOG.error("|  ---- / END::cleanup {} {} / {} (site: {}::{}) in {}ms",
                  entityType.getTitle(),
                  siteToCleanupIndex,
                  totalSitesToCleanupCount,
                  siteToCleanupKey.getType(),
                  siteToCleanupKey.getId(),
                  System.currentTimeMillis() - t1,
                  e);
      } finally {
        MigrationContext.restartTransaction();
      }
    } else {
      LOG.info("|  ---- \\ IGNORE::NOT migrated yet {} {} / {} (site: {}::{})",
               entityType.getTitle(),
               siteToCleanupIndex,
               totalSitesToCleanupCount,
               siteToCleanupKey.getType(),
               siteToCleanupKey.getId());
    }
    return removed;
  }

}
