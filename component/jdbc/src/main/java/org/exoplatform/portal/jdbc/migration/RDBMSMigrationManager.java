package org.exoplatform.portal.jdbc.migration;

import java.lang.reflect.Field;

import org.picocontainer.Startable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class RDBMSMigrationManager implements Startable {
  private static final Log            LOG                          = ExoLogger.getLogger(RDBMSMigrationManager.class);

  public static final String          MIGRATION_SETTING_GLOBAL_KEY = "MIGRATION_SETTING_GLOBAL";

  private Thread                      migrationThread;

  private SiteMigrationService        siteMigrationService;

  private PageMigrationService        pageMigrationService;

  private NavigationMigrationService  navMigrationService;

  private AppRegistryMigrationService appMigrationService;

  private SettingService              settingService;

  public RDBMSMigrationManager(SiteMigrationService siteMigrationService,
                               PageMigrationService pageMigrationService,
                               NavigationMigrationService navMigrationService,
                               AppRegistryMigrationService appMigrationService,
                               SettingService settingService) {
    this.siteMigrationService = siteMigrationService;
    this.pageMigrationService = pageMigrationService;
    this.navMigrationService = navMigrationService;
    this.appMigrationService = appMigrationService;
    this.settingService = settingService;
  }

  @Override
  public void start() {
    initMigrationSetting();

    Runnable migrateTask = new Runnable() {
      @Override
      public void run() {
        //
        Field field = null;
        try {
          if (MigrationContext.isDone()) {
            LOG.info("Overall Portal JCR to RDBMS migration already finished, ignore it.");
          } else {
            field = SessionImpl.class.getDeclaredField("FORCE_USE_GET_NODES_LAZILY");
            if (field != null) {
              field.setAccessible(true);
              field.set(null, true);
            }

            //
            long startTime = System.currentTimeMillis();
            //
            LOG.info("START ASYNC MIGRATION---------------------------------------------------");

            if (MigrationContext.isAppDone()) {
              LOG.info("APPLICATION REGISTRY migration already finished, ignore it.");
            } else {
              appMigrationService.start();
              updateSettingValue(MigrationContext.PORTAL_RDBMS_APP_MIGRATION_KEY, MigrationContext.isAppDone());
            }

            if (MigrationContext.isSiteDone()) {
              LOG.info("SITES migration already finished, ignore it.");
            } else {
              siteMigrationService.start();
              updateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_MIGRATION_KEY, MigrationContext.isSiteDone());
            }

            if (MigrationContext.isPageDone()) {
              LOG.info("PAGES migration already finished, ignore it.");
            } else {
              pageMigrationService.start();
              updateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_MIGRATION_KEY, MigrationContext.isPageDone());
            }

            if (MigrationContext.isNavDone()) {
              LOG.info("Sites NAVIGATIONS migration already finished, ignore it.");
            } else {
              navMigrationService.start();
              updateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_MIGRATION_KEY, MigrationContext.isNavDone());
            }
            //
            LOG.info("END ASYNC MIGRATION in {}ms-----------------------------------------------------",
                     System.currentTimeMillis() - startTime);

            startTime = System.currentTimeMillis();
            LOG.info("START CLEANUP PORTAL DATA ---------------------------------------------------");
            // cleanup
            if (MigrationContext.isAppDone() && !MigrationContext.isAppCleanupDone()) {
              appMigrationService.doRemove();
              updateSettingValue(MigrationContext.PORTAL_RDBMS_APP_CLEANUP_KEY, Boolean.TRUE);
            }

            // cleanup
            if (MigrationContext.isNavDone() && !MigrationContext.isNavCleanupDone()) {
              navMigrationService.doRemove();
              updateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_CLEANUP_KEY, Boolean.TRUE);
            }

            // Page
            if (MigrationContext.isPageDone() && !MigrationContext.isPageCleanupDone()) {
              pageMigrationService.doRemove();
              updateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_CLEANUP_KEY, Boolean.TRUE);
            }

            // Site
            if (MigrationContext.isSiteDone() && !MigrationContext.isSiteCleanupDone()) {
              siteMigrationService.doRemove();
              updateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_CLEANUP_KEY, Boolean.TRUE);
            }

            LOG.info("END CLEANUP PORTAL DATA in {}ms -----------------------------------------------------",
                     System.currentTimeMillis() - startTime);
          }

          if (MigrationContext.isSiteCleanupDone() && MigrationContext.isPageCleanupDone() && MigrationContext.isNavCleanupDone()
              && MigrationContext.isAppCleanupDone()) {
            updateSettingValue(MigrationContext.PORTAL_RDBMS_MIGRATION_STATUS_KEY, Boolean.TRUE);
            MigrationContext.setDone(true);
            settingService.remove(AbstractMigrationService.CONTEXT);
          }

        } catch (Exception e) {
          LOG.error("Failed to running Migration data from JCR to RDBMS", e);
        } finally {
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
    this.migrationThread = new Thread(migrateTask);
    this.migrationThread.setPriority(Thread.NORM_PRIORITY);
    this.migrationThread.setName("PORTAL-MIGRATION-RDBMS");
    this.migrationThread.start();
  }

  private void initMigrationSetting() {
    MigrationContext.setDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_MIGRATION_STATUS_KEY));
    //
    MigrationContext.setSiteDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_MIGRATION_KEY));
    MigrationContext.setSiteCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_CLEANUP_KEY));

    MigrationContext.setPageDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_MIGRATION_KEY));
    MigrationContext.setPageCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_CLEANUP_KEY));

    MigrationContext.setNavDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_MIGRATION_KEY));
    MigrationContext.setNavCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_CLEANUP_KEY));

    MigrationContext.setAppDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_APP_MIGRATION_KEY));
    MigrationContext.setAppCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_APP_CLEANUP_KEY));
  }

  private boolean getOrCreateSettingValue(String key) {
    SettingValue<?> setting = this.settingService.get(Context.GLOBAL, Scope.GLOBAL.id(null), key);
    if (setting != null) {
      return Boolean.parseBoolean(setting.getValue().toString());
    } else {
      updateSettingValue(key, Boolean.FALSE);
      return false;
    }
  }

  private void updateSettingValue(String key, Boolean status) {
    settingService.set(Context.GLOBAL, Scope.GLOBAL.id(null), key, SettingValue.create(status));
  }

  @Override
  public void stop() {
    AbstractMigrationService.forceStop = true; // NOSONAR
    this.migrationThread.interrupt();
  }
}
