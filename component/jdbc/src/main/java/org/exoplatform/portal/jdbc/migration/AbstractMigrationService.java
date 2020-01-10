package org.exoplatform.portal.jdbc.migration;

import java.util.*;

import javax.jcr.*;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang3.StringUtils;
import org.chromattic.ext.format.BaseEncodingObjectFormatter;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public abstract class AbstractMigrationService {

  protected static final String                      LIMIT_THRESHOLD_KEY    = "LIMIT_THRESHOLD";

  protected static final String                      DEFAULT_WORKSPACE_NAME = "portal-system";

  protected static final BaseEncodingObjectFormatter formatter              = new BaseEncodingObjectFormatter();

  protected final Log                                log;

  protected int                                      limitThreshold         = 10;

  protected final POMDataStorage                     pomStorage;

  protected final ModelDataStorage                   modelStorage;

  protected final ListenerService                    listenerService;

  protected final RepositoryService                  repoService;

  protected final SettingService                     settingService;

  protected String                                   workspaceName;

  public AbstractMigrationService(InitParams initParams,
                                  POMDataStorage pomDataStorage,
                                  ModelDataStorage modelStorage,
                                  ListenerService listenerService,
                                  RepositoryService repoService,
                                  SettingService settingService) {
    this.pomStorage = pomDataStorage;
    this.modelStorage = modelStorage;
    this.listenerService = listenerService;
    this.repoService = repoService;
    this.settingService = settingService;

    this.log = ExoLogger.getLogger(this.getClass().getName());

    this.workspaceName = getString(initParams, "workspace", DEFAULT_WORKSPACE_NAME);
    this.limitThreshold = getInteger(initParams, LIMIT_THRESHOLD_KEY, 1);
  }

  public void addMigrationListener(Listener<Object, String> listener) {
    this.listenerService.addListener(getListenerKey(), listener);
  }

  protected void broadcastListener(Object t, String newId) {
    try {
      this.listenerService.broadcast(new Event<>(getListenerKey(), t, newId));
    } catch (Exception e) {
      log.error("Failed to broadcast event", e);
    }
  }

  protected int getInteger(InitParams params, String key, int defaultValue) {
    try {
      return Integer.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  protected String getString(InitParams params, String key, String defaultValue) {
    try {
      return params.getValueParam(key).getValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  protected String getProperty(Node node, String propName) {
    try {
      return node.getProperty(propName).getString();
    } catch (Exception ex) {
      return null;
    }
  }

  protected String[] getProperties(Node node, String propName) {
    List<String> values = new LinkedList<>();
    try {
      for (Value val : node.getProperty(propName).getValues()) {
        values.add(val.getString());
      }
      return values.toArray(new String[values.size()]);
    } catch (Exception ex) {
      log.warn("Error reading property '{}' of node {}", propName, node, ex);
      return new String[0];
    }
  }

  protected ContainerData migrateContainer(ContainerData containerData) {
    //
    List<ComponentData> children = this.migrateComponents(containerData.getChildren());
    return new ContainerData(null,
                             containerData.getId(),
                             containerData.getName(),
                             containerData.getIcon(),
                             containerData.getTemplate(),
                             containerData.getFactoryId(),
                             containerData.getTitle(),
                             containerData.getDescription(),
                             containerData.getWidth(),
                             containerData.getHeight(),
                             containerData.getAccessPermissions(),
                             containerData.getMoveAppsPermissions(),
                             containerData.getMoveContainersPermissions(),
                             children);
  }

  protected <S> ApplicationData<S> migrateApplication(ApplicationData<S> app) {
    if (StringUtils.isBlank(app.getStorageId())) {
      throw new IllegalStateException("Unexpected empty application storage id");
    }
    ApplicationData<S> applicationData = pomStorage.getApplicationData(app.getStorageId());
    try {
      S s = pomStorage.load(applicationData.getState(), applicationData.getType());
      String contentId = pomStorage.getId(app.getState());
      ApplicationState<S> migrated = new TransientApplicationState<>(contentId, s);

      return new ApplicationData<>(null,
                                   app.getStorageName(),
                                   app.getType(),
                                   migrated,
                                   app.getId(),
                                   app.getTitle(),
                                   app.getIcon(),
                                   app.getDescription(),
                                   app.isShowInfoBar(),
                                   app.isShowApplicationState(),
                                   app.isShowApplicationMode(),
                                   app.getTheme(),
                                   app.getWidth(),
                                   app.getHeight(),
                                   app.getProperties(),
                                   app.getAccessPermissions());
    } catch (Exception e) {
      throw new IllegalStateException("Unable to retrieve data of application with storage id " + app.getStorageId(), e);
    }
  }

  protected BodyData migrateBodyData(BodyData body) {
    return new BodyData(null, body.getType());
  }

  protected PageData migratePageData(PageData page) {
    List<ComponentData> children = this.migrateComponents(page.getChildren());
    return new PageData(null,
                        page.getId(),
                        page.getName(),
                        page.getIcon(),
                        page.getTemplate(),
                        page.getFactoryId(),
                        page.getTitle(),
                        page.getDescription(),
                        page.getWidth(),
                        page.getHeight(),
                        page.getAccessPermissions(),
                        children,
                        page.getOwnerType(),
                        page.getOwnerId(),
                        page.getEditPermission(),
                        page.isShowMaxWindow(),
                        page.getMoveAppsPermissions(),
                        page.getMoveContainersPermissions());
  }

  protected List<ComponentData> migrateComponents(List<ComponentData> list) {
    List<ComponentData> result = new ArrayList<>();

    for (ComponentData comp : list) {
      if (comp instanceof ContainerData) {
        result.add(migrateContainer((ContainerData) comp));
      } else if (comp instanceof PageData) {
        result.add(this.migratePageData((PageData) comp));
      } else if (comp instanceof BodyData) {
        result.add(migrateBodyData((BodyData) comp));
      } else if (comp instanceof ApplicationData) {
        ApplicationData<?> application = migrateApplication((ApplicationData<?>) comp);
        if (application == null) {
          continue;
        }
        result.add(application);
      }
    }

    return result;
  }

  protected List<PortalKey> getSitesToMigrate() {
    List<PortalKey> portalKeys = MigrationContext.getSitesToMigrate();
    if (portalKeys != null) {
      return portalKeys;
    }
    portalKeys = Collections.synchronizedList(new LinkedList<>()); // NOSONAR

    long t1 = System.currentTimeMillis();
    log.info("  \\ START::COMPUTE site keys to migrate");

    findSites(portalKeys, SiteType.PORTAL);
    int portalSitesCount = portalKeys.size();
    log.info("  --- {} PORTAL sites to migrate", portalSitesCount);
    MigrationContext.restartTransaction(); // To prevent session timeout

    findSites(portalKeys, SiteType.GROUP);
    int groupSitesCount = portalKeys.size() - portalSitesCount;
    log.info("  --- {} GROUP sites to migrate", groupSitesCount);
    MigrationContext.restartTransaction(); // To prevent session timeout

    findSites(portalKeys, SiteType.USER);
    int userSitesCount = portalKeys.size() - portalSitesCount - groupSitesCount;
    log.info("  --- {} USER sites to migrate", userSitesCount);
    MigrationContext.restartTransaction(); // To prevent session timeout

    MigrationContext.setSitesToMigrate(portalKeys);
    log.info("  / END::COMPUTE site keys to migrate in {}ms", System.currentTimeMillis() - t1);
    return portalKeys;
  }

  protected void findSites(List<PortalKey> portalKeys, SiteType type) {
    long offset = 0;
    long limit = limitThreshold;

    boolean hasNext = false;
    do {
      if (MigrationContext.isForceStop()) {
        log.info("|  \\ STOPPING migration (server terminated)");
        break;
      }
      hasNext = findSites(portalKeys, type, offset, limit);
      offset += limit;
    } while (hasNext);
  }

  protected boolean findSites(List<PortalKey> portalKeys, SiteType type, long offset, long limit) {
    Set<PortalKey> result = new HashSet<>();
    String mopType = "mop:" + type.getName().toLowerCase() + "site";

    try {
      String query = "select * from " + mopType;
      ManageableRepository currentRepository = this.repoService.getCurrentRepository();
      Session session = SessionProvider.createSystemProvider().getSession(workspaceName, currentRepository);
      QueryManager queryManager = session.getWorkspace().getQueryManager();

      javax.jcr.query.Query q = queryManager.createQuery(query, javax.jcr.query.Query.SQL);
      if (q instanceof QueryImpl) {
        ((QueryImpl) q).setOffset(offset);
        ((QueryImpl) q).setLimit(limit);
      }
      javax.jcr.query.QueryResult rs = q.execute();

      NodeIterator iterator = rs.getNodes();
      while (iterator.hasNext()) {
        Node node = iterator.nextNode();
        String path = node.getPath();
        String siteName = path.substring(path.lastIndexOf(':') + 1);
        siteName = formatter.decodeNodeName(null, siteName);

        result.add(new PortalKey(type.getName().toLowerCase(), siteName));
      }

    } catch (RepositoryException ex) {
      log.error("Error while retrieve user portal", ex);
    }
    if (!result.isEmpty()) {
      portalKeys.addAll(result);
    }
    return !result.isEmpty();
  }

  public abstract void doMigrate(PortalKey siteToMigrateKey) throws Exception; // NOSONAR

  public abstract void doRemove(PortalKey siteToRemoveKey) throws Exception; // NOSONAR

  protected abstract String getListenerKey();
}
