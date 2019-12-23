package org.exoplatform.portal.jdbc.migration;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;

@Managed
@ManagedDescription("Portal migration sites from JCR to RDBMS.")
@NameTemplate({ @Property(key = "service", value = "portal"), @Property(key = "view", value = "migration-sites") })
public class SiteMigrationService extends AbstractMigrationService<PortalData> {
  public static final String EVENT_LISTENER_KEY = "PORTAL_SITES_MIGRATION";

  private ModelDataStorage   modelStorage;

  public SiteMigrationService(InitParams initParams,
                              POMDataStorage pomStorage,
                              ModelDataStorage modelStorage,
                              ListenerService listenerService,
                              RepositoryService repoService,
                              EntityManagerService entityManagerService) {

    super(initParams, pomStorage, listenerService, repoService, entityManagerService);
    this.modelStorage = modelStorage;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 100);
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of sites from JCR to RDBMS.")
  public void doMigration() throws Exception {
    long t = System.currentTimeMillis();

    long total = 0;
    long count = 0;
    Set<PortalKey> sitesFailed = new HashSet<>();

    LOG.info("|\\ START::migrate sites");

    LOG.info("|  \\ START::migrate site of type " + SiteType.PORTAL.getName());
    count = doMigrate(SiteType.PORTAL, sitesFailed);
    LOG.info("|  // END::migrate site of type " + SiteType.PORTAL.getName() + ", migrated for " + count + " site(s)");
    total += count;

    LOG.info("|  \\ START::migrate site of type " + SiteType.GROUP.getName());
    count = doMigrate(SiteType.GROUP, sitesFailed);
    LOG.info("|  // END::migrate site of type " + SiteType.GROUP.getName() + ", migrated for " + count + " site(s)");
    total += count;

    LOG.info("|  \\ START::migrate site of type " + SiteType.USER.getName());
    count = doMigrate(SiteType.USER, sitesFailed);
    LOG.info("|  // END::migrate site of type " + SiteType.USER.getName() + ", migrated for " + count + " site(s)");
    total += count;

    LOG.info("|// END::migrated for "+ total + " site(s) in " + (System.currentTimeMillis() - t) + "ms");

    MigrationContext.setSitesMigrateFailed(sitesFailed);
    restartTransaction();
  }

  private long doMigrate(SiteType type, Set<PortalKey> failed) {
    long offset = 0;
    long limit = LIMIT_THRESHOLD;
    long count = 0;

    boolean hasNext = true;
    while (hasNext) {
      if (forkStop) {
        LOG.info("|  \\ Stop requested!!!");
        break;
      }

      Set<PortalKey> keys = findSites(type, offset, limit);
      hasNext = keys != null && !keys.isEmpty();
      if (hasNext) {
        for (PortalKey key : keys) {
          offset++;
          count ++;

          long t1 = System.currentTimeMillis();
          LOG.info(String.format("|  \\ START::migrate site number: %s (%s site) (type: %s)", offset, key.toString(), type.getName()));
          try {
            PortalData toMigrateSite = pomStorage.getPortalConfig(key);
            ContainerData portalLayoutContainer = this.migrateContainer(toMigrateSite.getPortalLayout());

            PortalData created = modelStorage.getPortalConfig(key);
            if (created == null) {
              LOG.info("Creating site {} in JPA", toMigrateSite.getKey());
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
              LOG.info("Updating layout for site {} in JPA", created.getKey());
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

            restartTransaction();

            created = modelStorage.getPortalConfig(key);
            broadcastListener(created, created.getKey().toString());

          } catch (Exception ex) {
            LOG.error("Error during migration site: " + key.toString(), ex);
            failed.add(key);
            count --;
          } finally {
            restartTransaction();
            LOG.info(String.format("|  / END::migrate site number: %s (%s site) (type: %s) consumed %s(ms)",
                    offset,
                    key.toString(),
                    type.getName(),
                    System.currentTimeMillis() - t1));
          }
        }
      }
    }
    return count;
  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setSiteDone(false);
  }

  @Override
  protected void afterMigration() throws Exception {
    if (forkStop) {
      return;
    }
    if (MigrationContext.getSitesMigrateFailed().isEmpty()) {
      MigrationContext.setSiteDone(true);
    }
  }

  public void doRemove() throws Exception {
    LOG.info("|\\ START::cleanup Sites ---------------------------------");
    long t = System.currentTimeMillis();
    long total = 0;
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      total += doRemove(SiteType.PORTAL);
      total += doRemove(SiteType.GROUP);
      total += doRemove(SiteType.USER);
    } finally {
      LOG.info(String.format("|// END::Cleanup sites for (%s) site(s) consumed %s(ms)",
              total,
              System.currentTimeMillis() - t));
      RequestLifeCycle.end();

      // Clean up success
      Set<PortalKey> portals = findSites(SiteType.PORTAL, 0, 1);
      Set<PortalKey> groups = findSites(SiteType.GROUP, 0, 1);
      Set<PortalKey> users = findSites(SiteType.USER, 0, 1);
      boolean isDone = (portals == null || portals.isEmpty())
              && (groups == null || groups.isEmpty())
              && (users == null || users.isEmpty());

      MigrationContext.setSiteCleanupDone(isDone);
    }
  }

  private long doRemove(SiteType type) {
    long offset = 0;
    long limit = LIMIT_THRESHOLD;
    long count = 0;

    boolean hasNext = true;
    while (hasNext) {
      if (forkStop) {
        LOG.info("|  \\ Stop requested!!!");
        break;
      }

      Set<PortalKey> keys = findSites(type, offset, limit);
      hasNext = keys != null && !keys.isEmpty();
      if (hasNext) {
        for (PortalKey key : keys) {
          count ++;
          long t1 = System.currentTimeMillis();
          LOG.info(String.format("|  \\ START::Clean up site number: %s (%s site) (type: %s)", count, key.toString(), type.getName()));

          try {
            PortalData data = pomStorage.getPortalConfig(key);
            if (data != null) {
              pomStorage.remove(data);
            }
            pomStorage.save();
          } catch (Exception ex) {
            LOG.error("Error during clean up site: " + key.toString(), ex);
            count --;
          } finally {
            restartTransaction();
            LOG.info(String.format("|  // END::Clean up site number: %s (%s site) (type: %s) consumed %s(ms)",
                    offset,
                    key.toString(),
                    type.getName(),
                    System.currentTimeMillis() - t1));
          }
        }
      }
    }
    return count;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of sites from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
