package org.exoplatform.portal.jdbc.migration;

import java.lang.reflect.Field;

import org.picocontainer.Startable;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.settings.impl.SettingServiceImpl;

public class RDBMSMigrationManager implements Startable {
  private static final Log     LOG                          = ExoLogger.getLogger(RDBMSMigrationManager.class);

  public static final String   MIGRATION_SETTING_GLOBAL_KEY = "MIGRATION_SETTING_GLOBAL";

  private Thread               migrationThread;

  private SiteMigrationService siteMigrationService;
  
  private PageMigrationService pageMigrationService;
  
  private NavigationMigrationService navMigrationService;
  
  private AppRegistryMigrationService appMigrationService;

  private SettingService       settingService;

  public RDBMSMigrationManager(SiteMigrationService siteMigrationService,
                               PageMigrationService pageMigrationService,
                               NavigationMigrationService navMigrationService,
                               AppRegistryMigrationService appMigrationService,
                               SettingService settingService,
                               DataInitializer initializer) {
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
          if (!MigrationContext.isDone()) {
            field = SessionImpl.class.getDeclaredField("FORCE_USE_GET_NODES_LAZILY");
            if (field != null) {
              field.setAccessible(true);
              field.set(null, true);
            }
            //
            LOG.info("START ASYNC MIGRATION---------------------------------------------------");
            //
            if (!MigrationContext.isDone()) {
              if (!MigrationContext.isSiteDone()) {
                siteMigrationService.start();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_MIGRATION_KEY, Boolean.TRUE);
              }
              // cleanup
              if (MigrationContext.isSiteDone() && !MigrationContext.isSiteCleanupDone()) {
                siteMigrationService.doRemove();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_CLEANUP_KEY, Boolean.TRUE);
              }
              
              if (MigrationContext.isSiteDone() && !MigrationContext.isPageDone()) {
                pageMigrationService.start();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_MIGRATION_KEY, Boolean.TRUE);
              }
              // cleanup
              if (MigrationContext.isPageDone() && !MigrationContext.isPageCleanupDone()) {
                pageMigrationService.doRemove();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_CLEANUP_KEY, Boolean.TRUE);
              }
              
              if (MigrationContext.isPageDone() && !MigrationContext.isNavDone()) {
                navMigrationService.start();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_MIGRATION_KEY, Boolean.TRUE);
              }
              // cleanup
              if (MigrationContext.isNavDone() && MigrationContext.isNavCleanupDone()) {
                navMigrationService.doRemove();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_CLEANUP_KEY, Boolean.TRUE);
              }
              
              if (MigrationContext.isNavDone() && !MigrationContext.isAppDone()) {
                appMigrationService.start();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_APP_MIGRATION_KEY, Boolean.TRUE);
              }
              // cleanup
              if (MigrationContext.isAppDone() && !MigrationContext.isAppCleanupDone()) {
                appMigrationService.doRemove();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_APP_CLEANUP_KEY, Boolean.TRUE);
                
                updateSettingValue(MigrationContext.PORTAL_RDBMS_MIGRATION_STATUS_KEY, Boolean.TRUE);
                MigrationContext.setDone(true);
              }         
            }
            
            //
            LOG.info("END ASYNC MIGRATION-----------------------------------------------------");
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
    
    MigrationContext.setSiteDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_MIGRATION_KEY));
    MigrationContext.setSiteCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_CLEANUP_KEY));
    
    MigrationContext.setSiteDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_MIGRATION_KEY));
    MigrationContext.setSiteCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_CLEANUP_KEY));
    
    MigrationContext.setSiteDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_APP_MIGRATION_KEY));
    MigrationContext.setSiteCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_APP_CLEANUP_KEY));
  }

  private boolean getOrCreateSettingValue(String key) {
    try {
      SettingValue<?> migrationValue = settingService.get(Context.GLOBAL, Scope.GLOBAL.id(MIGRATION_SETTING_GLOBAL_KEY), key);
      if (migrationValue != null) {
        return Boolean.parseBoolean(migrationValue.getValue().toString());
      } else {
        updateSettingValue(key, Boolean.FALSE);
        return false;
      }
    } finally {
      Scope.GLOBAL.id(null);
    }
  }

  private void updateSettingValue(String key, Boolean status) {
    SettingServiceImpl settingServiceImpl = CommonsUtils.getService(SettingServiceImpl.class);
    boolean created = settingServiceImpl.startSynchronization();
    try {
      settingService.set(Context.GLOBAL, Scope.GLOBAL.id(MIGRATION_SETTING_GLOBAL_KEY), key, SettingValue.create(status));
      try {
        CommonsUtils.getService(ChromatticManager.class).getLifeCycle("setting").getContext().getSession().save();
      } catch (Exception e) {
        LOG.warn(e);
      }
    } finally {
      Scope.GLOBAL.id(null);
      settingServiceImpl.stopSynchronization(created);
    }
  }

  @Override
  public void stop() {
    siteMigrationService.stop();
    try {
      this.migrationThread.join();
    } catch (InterruptedException e) {
      LOG.error(e);
    }
  }

}
