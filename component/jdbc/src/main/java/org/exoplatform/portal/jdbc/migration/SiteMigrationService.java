package org.exoplatform.portal.jdbc.migration;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;

public class SiteMigrationService extends AbstractMigrationService<PortalData> {
  public static final String EVENT_LISTENER_KEY = "PORTAL_SITES_MIGRATION";

  private ModelDataStorage   modelStorage;

  public SiteMigrationService(InitParams initParams,
                              POMDataStorage pomStorage,
                              ModelDataStorage modelStorage,
                              ListenerService listenerService,
                              RepositoryService repoService,
                              SettingService settingService,
                              EntityManagerService entityManagerService) {
    super(initParams, pomStorage, listenerService, repoService, settingService, entityManagerService);
    this.modelStorage = modelStorage;
  }

  public void doMigration() {
    long t = System.currentTimeMillis();

    long total = 0;
    long count = 0;
    Set<PortalKey> sitesFailed = new HashSet<>();

    log.info("|\\ START::migrate sites");

    log.info("|  \\ START::migrate site of type " + SiteType.PORTAL.getName());
    count = doMigrate(SiteType.PORTAL, sitesFailed);
    log.info("|  // END::migrate site of type " + SiteType.PORTAL.getName() + ", migrated for " + count + " site(s)");
    total += count;

    log.info("|  \\ START::migrate site of type " + SiteType.GROUP.getName());
    count = doMigrate(SiteType.GROUP, sitesFailed);
    log.info("|  // END::migrate site of type " + SiteType.GROUP.getName() + ", migrated for " + count + " site(s)");
    total += count;

    log.info("|  \\ START::migrate site of type " + SiteType.USER.getName());
    count = doMigrate(SiteType.USER, sitesFailed);
    log.info("|  // END::migrate site of type " + SiteType.USER.getName() + ", migrated for " + count + " site(s)");
    total += count;

    log.info("|// END::migrated for " + total + " site(s) in " + (System.currentTimeMillis() - t) + "ms");

    MigrationContext.setSitesMigrateFailed(sitesFailed);
  }

  private long doMigrate(SiteType type, Set<PortalKey> failed) {
    long offset = 0;
    long limit = limitThreshold;
    long count = 0;

    boolean hasNext = true;
    while (hasNext) {
      if (forceStop) {
        log.info("|  \\ Stop requested!!!");
        break;
      }

      Set<PortalKey> keys = findSites(type, offset, limit);
      hasNext = keys != null && !keys.isEmpty();
      if (hasNext) {
        for (PortalKey key : keys) {
          offset++;
          count++;
          if (isSiteMigrated(key)) {
            continue;
          }

          long t1 = System.currentTimeMillis();
          log.info("|  \\ START::migrate site number: {} ({} site) (type: {})", offset, key.toString(), type.getName());
          try {
            PortalData toMigrateSite = pomStorage.getPortalConfig(key);
            ContainerData portalLayoutContainer = this.migrateContainer(toMigrateSite.getPortalLayout());

            PortalData created = modelStorage.getPortalConfig(key);
            if (created == null) {
              log.info("Creating site {} in JPA", toMigrateSite.getKey());
              PortalData migrate = new PortalData(null,
                                                  toMigrateSite.getName(),
                                                  toMigrateSite.getType(),
                                                  toMigrateSite.getLocale(),
                                                  toMigrateSite.getLabel(),
                                                  toMigrateSite.getDescription(),
                                                  toMigrateSite.getAccessPermissions(),
                                                  toMigrateSite.getEditPermission(),
                                                  toMigrateSite.getProperties(),
                                                  toMigrateSite.getSkin(),
                                                  portalLayoutContainer,
                                                  toMigrateSite.getRedirects());
              modelStorage.create(migrate);
            } else {
              log.info("Updating layout for site {} in JPA", created.getKey());
              PortalData migrate = new PortalData(created.getStorageId(),
                                                  created.getName(),
                                                  created.getType(),
                                                  created.getLocale(),
                                                  created.getLabel(),
                                                  created.getDescription(),
                                                  created.getAccessPermissions(),
                                                  created.getEditPermission(),
                                                  created.getProperties(),
                                                  created.getSkin(),
                                                  portalLayoutContainer,
                                                  created.getRedirects());
              modelStorage.save(migrate);
            }

            created = modelStorage.getPortalConfig(key);
            broadcastListener(created, created.getKey().toString());
            setSiteMigrated(key);
          } catch (Exception ex) {
            log.error("Error during migration site: " + key.toString(), ex);
            failed.add(key);
            count--;
          } finally {
            restartTransaction();
            log.info("|  / END::migrate site number: {} ({} site) (type: {}) consumed {}(ms)",
                     offset,
                     key.toString(),
                     type.getName(),
                     System.currentTimeMillis() - t1);
          }
        }
      }
    }
    return count;
  }

  @Override
  protected void beforeMigration() {
    MigrationContext.setSiteDone(false);
  }

  @Override
  protected void afterMigration() {
    if (forceStop) {
      return;
    }
    if (MigrationContext.getSitesMigrateFailed().isEmpty()) {
      MigrationContext.setSiteDone(true);
    }
  }

  public void doRemove() {
    log.info("|\\ START::cleanup Sites ---------------------------------");
    long t = System.currentTimeMillis();
    long total = 0;
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      total += doRemove(SiteType.PORTAL);
      total += doRemove(SiteType.GROUP);
      total += doRemove(SiteType.USER);
    } finally {
      log.info("|// END::Cleanup sites for ({}) site(s) consumed {}(ms)",
               total,
               System.currentTimeMillis() - t);
      RequestLifeCycle.end();

      // Clean up success
      Set<PortalKey> portals = findSites(SiteType.PORTAL, 0, 1);
      Set<PortalKey> groups = findSites(SiteType.GROUP, 0, 1);
      Set<PortalKey> users = findSites(SiteType.USER, 0, 1);
      boolean isDone = (portals == null || portals.isEmpty())
          && (groups == null || groups.isEmpty())
          && (users == null || users.isEmpty());

      MigrationContext.setSiteCleanupDone(isDone);
      if (isDone) {
        settingService.remove(CONTEXT, org.exoplatform.commons.api.settings.data.Scope.PORTAL);
      }
    }
  }

  private long doRemove(SiteType type) {
    long offset = 0;
    long limit = limitThreshold;
    long count = 0;

    boolean hasNext = true;
    while (hasNext) {
      if (forceStop) {
        log.info("|  \\ Stop requested!!!");
        break;
      }

      Set<PortalKey> keys = findSites(type, offset, limit);
      hasNext = keys != null && !keys.isEmpty();
      if (hasNext) {
        for (PortalKey key : keys) {
          count++;
          long t1 = System.currentTimeMillis();
          log.info("|  \\ START::Clean up site number: {} ({} site) (type: {})",
                   count,
                   key.toString(),
                   type.getName());

          try {
            PortalData data = pomStorage.getPortalConfig(key);
            if (data != null) {
              pomStorage.remove(data);
            }
            pomStorage.save();
          } catch (Exception ex) {
            log.error("Error during clean up site: " + key.toString(), ex);
            count--;
          } finally {
            restartTransaction();
            log.info("|  // END::Clean up site number: {} ({} site) (type: {}) consumed {}(ms)",
                     offset,
                     key.toString(),
                     type.getName(),
                     System.currentTimeMillis() - t1);
          }
        }
      }
    }
    return count;
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
