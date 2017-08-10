package org.exoplatform.portal.jdbc.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.api.event.EventManager;
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
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PortalData;

@Managed
@ManagedDescription("Portal migration sites from JCR to RDBMS.")
@NameTemplate({ @Property(key = "service", value = "portal"), @Property(key = "view", value = "migration-sites") })
public class SiteMigrationService extends AbstractMigrationService<PortalData> {
  public static final String EVENT_LISTENER_KEY = "PORTAL_SITES_MIGRATION";

  private POMDataStorage     pomStorage;

  private ModelDataStorage   modelStorage;

  private List<PortalData>   data;

  public SiteMigrationService(InitParams initParams,
                              POMDataStorage pomStorage,
                              ModelDataStorage modelStorage,
                              EventManager<PortalData, String> eventManager,
                              EntityManagerService entityManagerService) {

    super(initParams, eventManager, entityManagerService);
    this.pomStorage = pomStorage;
    this.modelStorage = modelStorage;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 1);
  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setSiteDone(false);
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of sites from JCR to RDBMS.")
  public void doMigration() {
    boolean begunTx = startTx();
    int offset = 0;

    long t = System.currentTimeMillis();
    try {
      LOG.info("| \\ START::Sites migration ---------------------------------");
      List<PortalData> sites = getPortalData();

      if (sites == null || sites.size() == 0) {
        return;
      }

      LIMIT_THRESHOLD = LIMIT_THRESHOLD > sites.size() ? sites.size() : LIMIT_THRESHOLD;
      for (PortalData site : sites) {
        if (forkStop) {
          break;
        }

        LOG.info(String.format("|  \\ START::site number: %s (%s site)", offset, site.getKey()));
        long t1 = System.currentTimeMillis();

        try {
          PortalData created = modelStorage.getPortalConfig(site.getKey());
          if (created == null) {
            modelStorage.create(site);
            created = modelStorage.getPortalConfig(site.getKey());
          } else {
            LOG.info("Ignoring, this site: {} already in JPA", created.getKey());
          }
          //
          offset++;
          if (offset % LIMIT_THRESHOLD == 0) {
            endTx(begunTx);
            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());
            begunTx = startTx();
          }

          broadcastListener(created, created.getKey().toString());
          LOG.info(String.format("|  / END::site number %s (%s site) consumed %s(ms)",
                                 offset - 1,
                                 site.getKey(),
                                 System.currentTimeMillis() - t1));
        } catch (Exception ex) {
          LOG.error("exception during migration site: ", site.getKey());
        }
      }

    } finally {
      endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("| / END::Site migration for (%s) site(s) consumed %s(ms)", offset, System.currentTimeMillis() - t));
    }
  }

  @Override
  protected void afterMigration() throws Exception {
    if (forkStop) {
      return;
    }
    MigrationContext.setSiteDone(true);
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
  
  private List<PortalData> getPortalData() {
    if (data == null) {
      data = new ArrayList<PortalData>();
      try {
        data.addAll(getPortalData(PortalConfig.PORTAL_TYPE));
        data.addAll(getPortalData(PortalConfig.GROUP_TYPE));
        data.addAll(getPortalData(PortalConfig.USER_TYPE));
      } catch (Exception ex) {
        LOG.error("Can't load sites in JCR for migration", ex);
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
