package org.exoplatform.portal.jdbc.migration;

import java.util.Locale;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.description.DescriptionServiceImpl;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.listener.ListenerService;

public class NavigationMigrationService extends AbstractMigrationService {
  public static final String     EVENT_LISTENER_KEY = "PORTAL_NAVIGATIONS_MIGRATION";

  private NavigationService      navService;

  private DescriptionService     descriptionService;

  private DescriptionServiceImpl jcrDescriptionService;

  private NavigationServiceImpl  jcrNavService;

  public NavigationMigrationService(InitParams initParams,
                                    POMDataStorage pomStorage,
                                    ModelDataStorage modelDataStorage,
                                    NavigationService navService,
                                    DescriptionService descriptionService,
                                    POMSessionManager manager,
                                    ListenerService listenerService,
                                    RepositoryService repoService,
                                    SettingService settingService) {
    super(initParams, pomStorage, modelDataStorage, listenerService, repoService, settingService);
    this.navService = navService;

    SimpleDataCache cache = new SimpleDataCache();
    this.jcrNavService = new NavigationServiceImpl(manager, cache);

    this.descriptionService = descriptionService;
    this.jcrDescriptionService = new DescriptionServiceImpl(manager);
  }

  @Override
  public void doMigrate(PortalKey siteToMigrateKey) {
    SiteKey siteKey = new SiteKey(SiteType.valueOf(siteToMigrateKey.getType().toUpperCase()), siteToMigrateKey.getId());
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
  }

  @Override
  public void doRemove(PortalKey siteToRemoveKey) throws Exception {
    SiteKey siteKey = new SiteKey(siteToRemoveKey.getType(), siteToRemoveKey.getId());
    NavigationContext nav = jcrNavService.loadNavigation(siteKey);
    if (nav != null) {
      jcrNavService.destroyNavigation(nav);
    }
    pomStorage.save();
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
    Map<Locale, org.exoplatform.portal.mop.State> descriptions = jcrDescriptionService.getDescriptions(jcrParent.getId());
    descriptionService.setDescriptions(parent.getId(), descriptions);
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
