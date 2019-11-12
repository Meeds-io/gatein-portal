package org.exoplatform.portal.jdbc.migration;

import java.util.*;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.listener.ListenerService;

@Managed
@ManagedDescription("Portal migration sites from JCR to RDBMS.")
@NameTemplate({ @Property(key = "service", value = "portal"), @Property(key = "view", value = "migration-sites") })
public class SiteMigrationService extends AbstractMigrationService<PortalData> {
  public static final String EVENT_LISTENER_KEY = "PORTAL_SITES_MIGRATION";

  private ModelDataStorage   modelStorage;

  private List<PortalData>   data;

  public SiteMigrationService(InitParams initParams,
                              POMDataStorage pomStorage,
                              ModelDataStorage modelStorage,
                              ListenerService listenerService,
                              EntityManagerService entityManagerService) {

    super(initParams, pomStorage, listenerService, entityManagerService);
    this.modelStorage = modelStorage;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 100);
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of sites from JCR to RDBMS.")
  public void doMigration() throws Exception {
    boolean begunTx = startTx();

    long t = System.currentTimeMillis();

    int total = 0;
    int offset = 0;
    Set<PortalKey> sitesFailed = new HashSet<>();
    try {
      LOG.info("| \\ START::Sites migration ---------------------------------");
      List<PortalData> sites = getPortalData();

      if (sites == null || sites.size() == 0) {
        LOG.info("|  \\ NO SITE: There is no site to migrate");
        return;
      }

      total = sites.size();
      LOG.info("|  \\ There are " + total + " site(s)");

      final int limitThreshold = LIMIT_THRESHOLD > sites.size() ? sites.size() : LIMIT_THRESHOLD;
      for (PortalData site : sites) {
        if (forkStop) {
          LOG.info("|  \\ Force stop");
          break;
        }
        offset++;
        LOG.info(String.format("|  \\ START::site number: %s (%s site)", offset, site.getKey()));
        long t1 = System.currentTimeMillis();

        try {
          PortalData created = modelStorage.getPortalConfig(site.getKey());
          if (created == null) {
            PortalData migrate = new PortalData(null,
                    site.getName(),
                    site.getType(),
                    site.getLocale(),
                    site.getLabel(),
                    site.getDescription(),
                    site.getAccessPermissions(),
                    site.getEditPermission(),
                    site.getProperties(),
                    site.getSkin(),
                    this.migrateContainer(site.getPortalLayout()),
                    site.getRedirects());


            modelStorage.create(migrate);
            created = modelStorage.getPortalConfig(site.getKey());
          } else {
            LOG.info("Ignoring, this site: {} already in JPA", created.getKey());
          }

          //
          if (offset % limitThreshold == 0) {
            endTx(begunTx);
            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());
            begunTx = startTx();
          }

          broadcastListener(created, created.getKey().toString());

          LOG.info(String.format("|  / END::site number %s (%s site) consumed %s(ms)",
                                 offset,
                                 site.getKey(),
                                 System.currentTimeMillis() - t1));
        } catch (Exception ex) {
          LOG.error("exception during migration site: " + site.getKey(), ex);
          sitesFailed.add(site.getKey());
        }
      }
    } finally {
      MigrationContext.setSitesMigrateFailed(sitesFailed);
      endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("| / END::Site migration for (%s) site(s) consumed %s(ms)", offset, System.currentTimeMillis() - t));
    }
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
    LOG.info("| \\ START::cleanup Sites ---------------------------------");
    long t = System.currentTimeMillis();
    long timePerSite = System.currentTimeMillis();

    RequestLifeCycle.begin(PortalContainer.getInstance());
    int offset = 0;

    try {
      List<PortalData> sites = getPortalData();
      if (sites == null || sites.size() == 0) {
        return;
      }

      for (PortalData site : sites) {
        LOG.info(String.format("|  \\ START::cleanup Site number: %s (%s site)", offset, site.getKey()));
        offset++;

        try {
          pomStorage.remove(site);

          LOG.info(String.format("|  / END::cleanup (%s site) consumed time %s(ms)",
                                 site.getKey(),
                                 System.currentTimeMillis() - timePerSite));

          timePerSite = System.currentTimeMillis();
          if (offset % LIMIT_THRESHOLD == 0) {
            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());
          }
        } catch (Exception ex) {
          LOG.error("Can't remove site", ex);
        }
      }
      LOG.info(String.format("| / END::cleanup Sites migration for (%s) site consumed %s(ms)",
                             offset,
                             System.currentTimeMillis() - t));
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of sites from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }
  
  private List<PortalData> getPortalData() throws Exception {
    if (data == null || data.isEmpty()) {
      data = new ArrayList<>();
      try {
        data.addAll(getPortalData(PortalConfig.PORTAL_TYPE));
        data.addAll(getPortalData(PortalConfig.GROUP_TYPE));
        data.addAll(getPortalData(PortalConfig.USER_TYPE));
      } catch (Exception ex) {
        LOG.error("Can't load sites in JCR for migration", ex);
        throw ex;
      }
    }
    return data;
  }

  private Collection<? extends PortalData> getPortalData(String portalType) throws Exception {
    Query<PortalData> q = new Query<PortalData>(portalType, null, PortalData.class);
    return pomStorage.find(q).getAll();
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
