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
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.listener.ListenerService;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;

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
                                    RepositoryService repoService,
                                    EntityManagerService entityManagerService) {

    super(initParams, pomStorage, listenerService, repoService, entityManagerService);
    this.navService = navService;

    SimpleDataCache cache = new SimpleDataCache();
    this.jcrNavService = new NavigationServiceImpl(manager, cache);

    this.descriptionService = descriptionService;
    this.jcrDescriptionService = new DescriptionServiceImpl(manager);

    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 100);
  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setNavigationDone(false);
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of navigations from JCR to RDBMS.")
  public void doMigration() {

    long t = System.currentTimeMillis();

    long total = 0;
    long count = 0;
    Set<PortalKey> sitesFailed = new HashSet<>();

    LOG.info("|\\ START::migrate site navigations");

    LOG.info("|  \\ START::migrate navigation of site type " + SiteType.PORTAL.getName());
    count = doMigrate(SiteType.PORTAL, sitesFailed);
    LOG.info("|  // END::migrate navigation of site type " + SiteType.PORTAL.getName() + ", migrated for " + count + " site(s)");
    total += count;

    LOG.info("|  \\ START::migrate navigation of site type " + SiteType.GROUP.getName());
    count = doMigrate(SiteType.GROUP, sitesFailed);
    LOG.info("|  // END::migrate navigation of site type " + SiteType.GROUP.getName() + ", migrated for " + count + " site(s)");
    total += count;

    LOG.info("|  \\ START::migrate navigation of site type " + SiteType.USER.getName());
    count = doMigrate(SiteType.USER, sitesFailed);
    LOG.info("|  // END::migrate navigation of site type " + SiteType.USER.getName() + ", migrated for " + count + " site(s)");
    total += count;

    LOG.info("|// END::migrated navigation for "+ total + " site(s) in " + (System.currentTimeMillis() - t) + "ms");

    MigrationContext.setNavigationFailed(sitesFailed);
    RequestLifeCycle.end();
    RequestLifeCycle.begin(PortalContainer.getInstance());

//    boolean begunTx = startTx();
//    int offset = 0;
//
//    long t = System.currentTimeMillis();
//    try {
//      LOG.info("| \\ START::navigations migration ---------------------------------");
//      List<NavigationContext> navs = getNavigations();
//      Iterator<NavigationContext> navItr = navs.iterator();
//      while (navItr.hasNext()) {
//        if (forkStop) {
//          break;
//        }
//        offset++;
//        NavigationContext jcrNav = navItr.next();
//
//        LOG.info(String.format("|  \\ START::nav number: %s (%s nav)", offset, jcrNav.getKey()));
//        long t1 = System.currentTimeMillis();
//
//        try {
//          SiteKey key = jcrNav.getKey();
//          NavigationContext created = navService.loadNavigation(key);
//          if (created == null) {
//            NavigationContext nav = new NavigationContext(key, jcrNav.getState());
//            navService.saveNavigation(nav);
//            created = navService.loadNavigation(key);
//          }
//
//          //
//          NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, created, Scope.ALL, null);
//          NodeContext<?> jcrRoot = jcrNavService.loadNode(NodeModel.SELF_MODEL, jcrNav, Scope.ALL, null);
//          migrateNode(root, jcrRoot);
//          navService.saveNode(root, null);
//          migrateDescription(root, jcrRoot);
//
//          //
//          offset++;
//          if (offset % LIMIT_THRESHOLD == 0) {
//            endTx(begunTx);
//            RequestLifeCycle.end();
//            RequestLifeCycle.begin(PortalContainer.getInstance());
//            begunTx = startTx();
//          }
//
//          broadcastListener(created, created.getKey().toString());
//          LOG.info(String.format("|  / END::nav number %s (%s nav) consumed %s(ms)",
//                                 offset - 1,
//                                 jcrNav.getKey(),
//                                 System.currentTimeMillis() - t1));
//        } catch (Exception ex) {
//          LOG.error("exception during migration nav: " + jcrNav.getKey(), ex);
//        }
//      }
//
//    } finally {
//      endTx(begunTx);
//      RequestLifeCycle.end();
//      RequestLifeCycle.begin(PortalContainer.getInstance());
//      LOG.info(String.format("| / END::nav migration for (%s) nav(s) consumed %s(ms)", offset, System.currentTimeMillis() - t));
//    }
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

        boolean begunTx = startTx();

        for (PortalKey key : keys) {
          offset++;
          count ++;

          long t1 = System.currentTimeMillis();
          LOG.info(String.format("|  \\ START::Clean navigation for site number: %s (%s site) (type: %s)", offset, key.toString(), type.getName()));

          try {
            SiteKey siteKey = type.key(key.getId());
            NavigationContext jcrNav = jcrNavService.loadNavigation(siteKey);
            if (jcrNav != null) {
              NavigationContext created = navService.loadNavigation(siteKey);
              if (created == null) {
                NavigationContext nav = new NavigationContext(siteKey, jcrNav.getState());
                navService.saveNavigation(nav);
                created = navService.loadNavigation(siteKey);
              }

              //
              NodeContext<?> root = navService.loadNode(NodeModel.SELF_MODEL, created, Scope.ALL, null);
              NodeContext<?> jcrRoot = jcrNavService.loadNode(NodeModel.SELF_MODEL, jcrNav, Scope.ALL, null);
              migrateNode(root, jcrRoot);
              navService.saveNode(root, null);
              migrateDescription(root, jcrRoot);
            }
          } catch (Exception ex) {
            LOG.error("Error during migrate navigation of site: " + key.toString(), ex);
            failed.add(key);
            count --;
          } finally {
            LOG.info(String.format("|  // END::migrate navigation of site number: %s (%s site) (type: %s) consumed %s(ms)",
                    offset,
                    key.toString(),
                    type.getName(),
                    System.currentTimeMillis() - t1));
          }
        }

        endTx(begunTx);
        RequestLifeCycle.end();
        RequestLifeCycle.begin(PortalContainer.getInstance());
      }
    }
    return count;
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
      NodeContext child = null;
      if ((child = parent.get(jcrChild.getName())) != null) {
        migrateDescription(child, jcrChild);
      }

    }
    Map<Locale, Described.State> descriptions = jcrDescriptionService.getDescriptions(jcrParent.getId());
    descriptionService.setDescriptions(parent.getId(), descriptions);
  }

//  private List<NavigationContext> getNavigations() {
//    if (navigations == null || navigations.isEmpty()) {
//      navigations = new ArrayList<NavigationContext>();
//      navigations.addAll(jcrNavService.loadNavigations(SiteType.PORTAL));
//      navigations.addAll(jcrNavService.loadNavigations(SiteType.GROUP));
//      navigations.addAll(jcrNavService.loadNavigations(SiteType.USER));
//    }
//    return navigations;
//  }

  @Override
  protected void afterMigration() throws Exception {
    if (forkStop) {
      return;
    }
    if (MigrationContext.getNavigationFailed().isEmpty()) {
      MigrationContext.setNavigationDone(true);
    }
  }

  public void doRemove() throws Exception {
    LOG.info("|\\ START::cleanup site navigations ---------------------------------");
    long t = System.currentTimeMillis();
    long total = 0;
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      total += doRemove(SiteType.PORTAL);
      total += doRemove(SiteType.GROUP);
      total += doRemove(SiteType.USER);
    } finally {
      LOG.info(String.format("|// END::Cleanup navigation for (%s) site(s) consumed %s(ms)",
              total,
              System.currentTimeMillis() - t));
      RequestLifeCycle.end();

      // Clean up success
      boolean isDone = !hasJCRNavigation();
      MigrationContext.setNavCleanupDone(isDone);
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
        boolean begunTx = startTx();

        for (PortalKey key : keys) {
          count ++;
          offset ++;

          long t1 = System.currentTimeMillis();
          LOG.info(String.format("|  \\ START::Clean up site number: %s (%s site) (type: %s)", count, key.toString(), type.getName()));

          try {
            SiteKey siteKey = type.key(key.getId());
            NavigationContext nav = jcrNavService.loadNavigation(siteKey);
            if (nav != null) {
              jcrNavService.destroyNavigation(nav);
            }
            pomStorage.save();

          } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("Error during clean up site: " + key.toString(), ex);
            count --;
          } finally {
            LOG.info(String.format("|  // END::Clean up site number: %s (%s site) (type: %s) consumed %s(ms)",
                    offset,
                    key.toString(),
                    type.getName(),
                    System.currentTimeMillis() - t1));
          }
        }

        endTx(begunTx);
        RequestLifeCycle.end();
        RequestLifeCycle.begin(PortalContainer.getInstance());
      }
    }
    return count;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of navigations from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }

  protected boolean hasJCRNavigation() {
    try {
      String query = "select * from mop:navigation where jcr:path like '/production/mop:workspace/%/%/mop:rootnavigation/mop:children/mop:default/mop:children/'";
      ManageableRepository currentRepository = this.repoService.getCurrentRepository();
      Session session = SessionProvider.createSystemProvider().getSession(workspaceName, currentRepository);
      QueryManager queryManager = session.getWorkspace().getQueryManager();

      javax.jcr.query.Query q = queryManager.createQuery(query, javax.jcr.query.Query.SQL);
      if (q instanceof QueryImpl) {
        ((QueryImpl)q).setOffset(0);
        ((QueryImpl)q).setLimit(1);
      }
      javax.jcr.query.QueryResult rs = q.execute();

      NodeIterator iterator = rs.getNodes();
      if (iterator.hasNext()) {
        Node node = iterator.nextNode();
        System.out.println("Path: " + node.getPath());
        System.out.println("Name: " + node.getName());
        return true;
      }

    } catch (RepositoryException ex) {
      LOG.error("Error while retrieve user portal", ex);
    }
    return false;
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
