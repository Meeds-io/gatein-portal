package org.exoplatform.portal.jdbc.migration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.portal.mop.Described;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.description.DescriptionServiceImpl;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NavigationServiceImpl;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.navigation.SimpleDataCache;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.services.listener.ListenerService;

@Managed
@ManagedDescription("Portal migration navigations from JCR to RDBMS.")
@NameTemplate({ @Property(key = "service", value = "portal"), @Property(key = "view", value = "migration-navigations") })
public class NavigationMigrationService extends AbstractMigrationService<NavigationContext> {
  public static final String      EVENT_LISTENER_KEY = "PORTAL_NAVIGATIONS_MIGRATION";

  private NavigationService       navService;

  private DescriptionService      descriptionService;

  private DescriptionServiceImpl  jcrDescriptionService;

  private NavigationServiceImpl   jcrNavService;

  private List<NavigationContext> navigations;

  public NavigationMigrationService(InitParams initParams,
                                    POMDataStorage pomStorage,
                                    NavigationService navService,
                                    DescriptionService descriptionService,
                                    POMSessionManager manager,
                                    ListenerService listenerService,
                                    EntityManagerService entityManagerService) {

    super(initParams, pomStorage, listenerService, entityManagerService);
    this.navService = navService;

    SimpleDataCache cache = new SimpleDataCache();
    this.jcrNavService = new NavigationServiceImpl(manager, cache);

    this.descriptionService = descriptionService;
    this.jcrDescriptionService = new DescriptionServiceImpl(manager);

    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 1);
  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setNavigationDone(false);
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of navigations from JCR to RDBMS.")
  public void doMigration() {
    boolean begunTx = startTx();
    int offset = 0;

    long t = System.currentTimeMillis();
    try {
      LOG.info("| \\ START::navigations migration ---------------------------------");
      List<NavigationContext> navs = getNavigations();
      Iterator<NavigationContext> navItr = navs.iterator();
      while (navItr.hasNext()) {
        if (forkStop) {
          break;
        }
        offset++;
        NavigationContext jcrNav = navItr.next();

        LOG.info(String.format("|  \\ START::nav number: %s (%s nav)", offset, jcrNav.getKey()));
        long t1 = System.currentTimeMillis();

        try {
          SiteKey key = jcrNav.getKey();
          NavigationContext created = navService.loadNavigation(key);
          if (created == null) {
            NavigationContext nav = new NavigationContext(key, jcrNav.getState());
            navService.saveNavigation(nav);
            created = navService.loadNavigation(key);
          }

          //
          NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, created, Scope.ALL, null);
          NodeContext<?> jcrRoot = jcrNavService.loadNode(NodeModel.SELF_MODEL, jcrNav, Scope.ALL, null);
          migrateNode(root, jcrRoot);
          navService.saveNode(root, null);
          migrateDescription(root, jcrRoot);

          //
          offset++;
          if (offset % LIMIT_THRESHOLD == 0) {
            endTx(begunTx);
            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());
            begunTx = startTx();
          }

          broadcastListener(created, created.getKey().toString());
          LOG.info(String.format("|  / END::nav number %s (%s nav) consumed %s(ms)",
                                 offset - 1,
                                 jcrNav.getKey(),
                                 System.currentTimeMillis() - t1));
        } catch (Exception ex) {
          LOG.error("exception during migration nav: " + jcrNav.getKey(), ex);
        }
      }

    } finally {
      endTx(begunTx);
      RequestLifeCycle.end();
      RequestLifeCycle.begin(PortalContainer.getInstance());
      LOG.info(String.format("| / END::nav migration for (%s) nav(s) consumed %s(ms)", offset, System.currentTimeMillis() - t));
    }
  }

  private void migrateNode(NodeContext<?> parent, NodeContext<?> jcrParent) {
    for (int i = 0; i < jcrParent.getNodeCount(); i++) {
      NodeContext jcrChild = jcrParent.get(i);
      NodeContext child = parent.get(jcrChild.getName());
      if (child == null) {
        child = parent.add(null, jcrChild.getName());
      }
      child.setState(jcrChild.getState());
      child.setHidden(jcrChild.isHidden());
      migrateNode(child, jcrChild);
    }
  }

  private void migrateDescription(NodeContext<?> parent, NodeContext<?> jcrParent) {
    for (int i = 0; i < jcrParent.getNodeCount(); i++) {
      NodeContext jcrChild = jcrParent.get(i);
      NodeContext child = parent.get(i);
      migrateDescription(child, jcrChild);
    }
    Map<Locale, Described.State> descriptions = jcrDescriptionService.getDescriptions(jcrParent.getId());
    descriptionService.setDescriptions(parent.getId(), descriptions);
  }

  private List<NavigationContext> getNavigations() {
    if (navigations == null || navigations.isEmpty()) {
      navigations = new ArrayList<NavigationContext>();
      navigations.addAll(jcrNavService.loadNavigations(SiteType.PORTAL));
      navigations.addAll(jcrNavService.loadNavigations(SiteType.GROUP));
      navigations.addAll(jcrNavService.loadNavigations(SiteType.USER));
    }
    return navigations;
  }

  @Override
  protected void afterMigration() throws Exception {
    if (forkStop) {
      return;
    }
    MigrationContext.setNavigationDone(true);
  }

  public void doRemove() throws Exception {
    LOG.info("| \\ START::cleanup navigations ---------------------------------");
    long t = System.currentTimeMillis();
    long timePernav = System.currentTimeMillis();

    RequestLifeCycle.begin(PortalContainer.getInstance());
    int offset = 0;

    List<NavigationContext> navs = getNavigations();
    Iterator<NavigationContext> navItr = navs.iterator();
    try {
      while (navItr.hasNext()) {
        NavigationContext nav = navItr.next();
        LOG.info(String.format("|  \\ START::cleanup nav number: %s (%s nav)", offset, nav.getKey()));
        offset++;

        try {
          SiteKey key = nav.getKey();
          jcrNavService.destroyNavigation(nav);

          LOG.info(String.format("|  / END::cleanup (%s nav) consumed time %s(ms)",
                                 key,
                                 System.currentTimeMillis() - timePernav));

          timePernav = System.currentTimeMillis();
          if (offset % LIMIT_THRESHOLD == 0) {
            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());
          }
        } catch (Exception ex) {
          LOG.error("Can't remove nav" + nav.getKey(), ex);
        }
      }
      LOG.info(String.format("| / END::cleanup navigations migration for (%s) nav consumed %s(ms)",
                             offset,
                             System.currentTimeMillis() - t));
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of navigations from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
