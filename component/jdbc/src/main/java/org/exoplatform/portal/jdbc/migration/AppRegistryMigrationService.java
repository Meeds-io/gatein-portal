package org.exoplatform.portal.jdbc.migration;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.application.registry.Application;
import org.exoplatform.application.registry.ApplicationCategory;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.application.registry.impl.ApplicationRegistryServiceImpl;
import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;

@Managed
@ManagedDescription("Portal migration applications from JCR to RDBMS.")
@NameTemplate({ @Property(key = "service", value = "portal"), @Property(key = "view", value = "migration-applications") })
public class AppRegistryMigrationService extends AbstractMigrationService<ApplicationCategory> {
  public static final String EVENT_LISTENER_KEY = "PORTAL_APPLICATIONS_MIGRATION";

  private ApplicationRegistryService appService;
  
  private ApplicationRegistryServiceImpl jcrAppService;

  private List<ApplicationCategory>   data;

  public AppRegistryMigrationService(InitParams initParams,
                              ApplicationRegistryService appService,
                              ApplicationRegistryServiceImpl jcrAppService,
                              EventManager<ApplicationCategory, String> eventManager,
                              EntityManagerService entityManagerService) {

    super(initParams, eventManager, entityManagerService);
    this.appService = appService;
    this.jcrAppService = jcrAppService;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 1);
  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setAppDone(false);
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of applications from JCR to RDBMS.")
  public void doMigration() {
    boolean begunTx = startTx();
    int offset = 0;

    long t = System.currentTimeMillis();
    try {
      LOG.info("| \\ START::Application Categories migration ---------------------------------");
      List<ApplicationCategory> categories = getAppCategory();

      if (categories == null || categories.size() == 0) {
        return;
      }

      LIMIT_THRESHOLD = LIMIT_THRESHOLD > categories.size() ? categories.size() : LIMIT_THRESHOLD;
      for (ApplicationCategory category : categories) {
        if (forkStop) {
          break;
        }

        LOG.info(String.format("|  \\ START::category number: %s (%s category)", offset, category.getName()));
        long t1 = System.currentTimeMillis();

        try {
          ApplicationCategory created = appService.getApplicationCategory(category.getName());
          if (created == null) {
            for (Application app : category.getApplications()) {
              appService.save(category, app);
            }
            
            created = appService.getApplicationCategory(category.getName());
          } else {
            LOG.info("Ignoring, this category: {} already in JPA", created.getName());
          }
          //
          offset++;
          if (offset % LIMIT_THRESHOLD == 0) {
            endTx(begunTx);
            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());
            begunTx = startTx();
          }

          broadcastListener(created, created.getName());
          LOG.info(String.format("|  / END::category number %s (%s category) consumed %s(ms)",
                                 offset - 1,
                                 category.getName(),
                                 System.currentTimeMillis() - t1));
        } catch (Exception ex) {
          LOG.error("exception during migration category: " + category.getName(), ex);
        }
      }

    } finally {
      endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("| / END::Category migration for (%s) categories consumed %s(ms)", offset, System.currentTimeMillis() - t));
    }
  }

  @Override
  protected void afterMigration() throws Exception {
    if (forkStop) {
      return;
    }
    MigrationContext.setAppDone(true);
  }

  public void doRemove() throws Exception {
    LOG.info("| \\ START::cleanup Application Categories ---------------------------------");
    long t = System.currentTimeMillis();
    long timePerSite = System.currentTimeMillis();

    RequestLifeCycle.begin(PortalContainer.getInstance());
    int offset = 0;

    try {
      List<ApplicationCategory> categories = getAppCategory();

      if (categories == null || categories.size() == 0) {
        return;
      }

      LIMIT_THRESHOLD = LIMIT_THRESHOLD > categories.size() ? categories.size() : LIMIT_THRESHOLD;
      for (ApplicationCategory category : categories) {
        LOG.info(String.format("|  \\ START::cleanup Category number: %s (%s category)", offset, category.getName()));
        offset++;

        try {
          jcrAppService.remove(category);

          LOG.info(String.format("|  / END::cleanup (%s category) consumed time %s(ms)",
                                 category.getName(),
                                 System.currentTimeMillis() - timePerSite));

          timePerSite = System.currentTimeMillis();
          if (offset % LIMIT_THRESHOLD == 0) {
            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());
          }
        } catch (Exception ex) {
          LOG.error("Can't remove category: " + category.getName(), ex);
        }
      }
      LOG.info(String.format("| / END::cleanup Categories migration for (%s) category consumed %s(ms)",
                             offset,
                             System.currentTimeMillis() - t));
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of applications from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
  
  private List<ApplicationCategory> getAppCategory() {
    if (data == null) {
      data = new ArrayList<ApplicationCategory>();
      try {
        data.addAll(jcrAppService.getApplicationCategories());
      } catch (Exception e) {
        LOG.error("Can't load application categories", e);
      }      
    }
    return data;
  }
}
