package org.exoplatform.portal.jdbc.migration;

import java.util.*;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.*;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.description.DescriptionServiceImpl;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.listener.ListenerService;

public class NavigationMigrationService extends AbstractMigrationService<NavigationContext> {
  public static final String     EVENT_LISTENER_KEY = "PORTAL_NAVIGATIONS_MIGRATION";

  private NavigationService      navService;

  private DescriptionService     descriptionService;

  private DescriptionServiceImpl jcrDescriptionService;

  private NavigationServiceImpl  jcrNavService;

  public NavigationMigrationService(InitParams initParams,
                                    POMDataStorage pomStorage,
                                    NavigationService navService,
                                    DescriptionService descriptionService,
                                    POMSessionManager manager,
                                    ListenerService listenerService,
                                    RepositoryService repoService,
                                    SettingService settingService,
                                    EntityManagerService entityManagerService) {
    super(initParams, pomStorage, listenerService, repoService, settingService, entityManagerService);
    this.navService = navService;

    SimpleDataCache cache = new SimpleDataCache();
    this.jcrNavService = new NavigationServiceImpl(manager, cache);

    this.descriptionService = descriptionService;
    this.jcrDescriptionService = new DescriptionServiceImpl(manager);
  }

  @Override
  protected void beforeMigration() {
    MigrationContext.setNavDone(false);
  }

  public void doMigration() {

    long t = System.currentTimeMillis();

    long total = 0;
    long count = 0;
    Set<PortalKey> sitesFailed = new HashSet<>();

    log.info("|\\ START::migrate site navigations");

    log.info("|  \\ START::migrate navigation of site type " + SiteType.PORTAL.getName());
    count = doMigrate(SiteType.PORTAL, sitesFailed);
    log.info("|  // END::migrate navigation of site type " + SiteType.PORTAL.getName() + ", migrated for " + count + " site(s)");
    total += count;

    log.info("|  \\ START::migrate navigation of site type " + SiteType.GROUP.getName());
    count = doMigrate(SiteType.GROUP, sitesFailed);
    log.info("|  // END::migrate navigation of site type " + SiteType.GROUP.getName() + ", migrated for " + count + " site(s)");
    total += count;

    log.info("|  \\ START::migrate navigation of site type " + SiteType.USER.getName());
    count = doMigrate(SiteType.USER, sitesFailed);
    log.info("|  // END::migrate navigation of site type " + SiteType.USER.getName() + ", migrated for " + count + " site(s)");
    total += count;

    log.info("|// END::migrated navigation for " + total + " site(s) in " + (System.currentTimeMillis() - t) + "ms");

    MigrationContext.setNavigationFailed(sitesFailed);
    restartTransaction();
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
          if (isNavigationMigrated(key) || !isSiteMigrated(key)) {
            continue;
          }

          long t1 = System.currentTimeMillis();
          log.info("|  \\ START::Migrate navigation for site number: {} ({} site) (type: {})",
                   offset,
                   key.toString(),
                   type.getName());

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
            setNavigationMigrated(key);
          } catch (Exception ex) {
            log.error("Error during migrate navigation of site: " + key.toString(), ex);
            failed.add(key);
            count--;
          } finally {
            restartTransaction();
            log.info("|  // END::migrate navigation of site number: {} ({} site) (type: {}) consumed {}(ms)",
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

  private void migrateNode(NodeContext<?> parent, NodeContext<?> jcrParent) {
    for (int i = 0; i < jcrParent.getNodeCount(); i++) {
      NodeContext<?> jcrChild = jcrParent.get(i);
      NodeContext<?> child = parent.get(jcrChild.getName());
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
      NodeContext<?> jcrChild = jcrParent.get(i);
      NodeContext<?> child = null;
      if ((child = parent.get(jcrChild.getName())) != null) {
        migrateDescription(child, jcrChild);
      }

    }
    Map<Locale, Described.State> descriptions = jcrDescriptionService.getDescriptions(jcrParent.getId());
    descriptionService.setDescriptions(parent.getId(), descriptions);
  }

  @Override
  protected void afterMigration() {
    if (forceStop) {
      return;
    }
    if (MigrationContext.getNavigationFailed().isEmpty()) {
      MigrationContext.setNavDone(MigrationContext.isSiteDone() && MigrationContext.isPageDone());
    }
  }

  public void doRemove() {
    log.info("|\\ START::cleanup site navigations ---------------------------------");
    long t = System.currentTimeMillis();
    long total = 0;
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      total += doRemove(SiteType.PORTAL);
      total += doRemove(SiteType.GROUP);
      total += doRemove(SiteType.USER);
    } finally {
      log.info("|// END::Cleanup navigation for ({}) site(s) consumed {}(ms)",
               total,
               System.currentTimeMillis() - t);
      RequestLifeCycle.end();

      // Clean up success
      boolean isDone = !hasJCRNavigation();
      MigrationContext.setNavCleanupDone(isDone);
      if (isDone) {
        settingService.remove(CONTEXT, new org.exoplatform.commons.api.settings.data.Scope("Navigation", null));
      }
      restartTransaction();
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
          offset++;

          long t1 = System.currentTimeMillis();
          log.info("|  \\ START::Clean up site number: {} ({} site) (type: {})",
                   count,
                   key.toString(),
                   type.getName());
          try {
            SiteKey siteKey = type.key(key.getId());
            NavigationContext nav = jcrNavService.loadNavigation(siteKey);
            if (nav != null) {
              jcrNavService.destroyNavigation(nav);
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

  protected boolean hasJCRNavigation() {
    try {
      String query =
                   "select * from mop:navigation where jcr:path like '/production/mop:workspace/%/%/mop:rootnavigation/mop:children/mop:default/mop:children/'";
      ManageableRepository currentRepository = this.repoService.getCurrentRepository();
      Session session = SessionProvider.createSystemProvider().getSession(workspaceName, currentRepository);
      QueryManager queryManager = session.getWorkspace().getQueryManager();

      javax.jcr.query.Query q = queryManager.createQuery(query, javax.jcr.query.Query.SQL);
      if (q instanceof QueryImpl) {
        ((QueryImpl) q).setOffset(0);
        ((QueryImpl) q).setLimit(1);
      }
      javax.jcr.query.QueryResult rs = q.execute();
      return rs.getNodes().hasNext();
    } catch (RepositoryException ex) {
      log.error("Error while retrieve user portal", ex);
    }
    return false;
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
