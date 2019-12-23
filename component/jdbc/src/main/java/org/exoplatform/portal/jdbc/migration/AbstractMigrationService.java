package org.exoplatform.portal.jdbc.migration;

import java.util.*;

import javax.jcr.*;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang3.StringUtils;
import org.chromattic.ext.format.BaseEncodingObjectFormatter;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.SiteKey;
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

public abstract class AbstractMigrationService<T> {

  protected static final String                      CONTEXT_KEY            = "PORTAL_MIGRATION_ENTITIES";

  protected static final Context                     CONTEXT                = Context.GLOBAL.id("PORTAL_MIGRATION_ENTITIES");

  protected static final String                      LIMIT_THRESHOLD_KEY    = "LIMIT_THRESHOLD";

  protected static final String                      DEFAULT_WORKSPACE_NAME = "portal-system";

  protected static final BaseEncodingObjectFormatter formatter              = new BaseEncodingObjectFormatter();

  protected static boolean                           forceStop              = false;

  protected final Log                                log;

  protected int                                      limitThreshold         = 10;

  protected final POMDataStorage                     pomStorage;

  protected final ListenerService                    listenerService;

  protected final EntityManagerService               entityManagerService;

  protected final RepositoryService                  repoService;

  protected final SettingService                     settingService;

  protected String                                   workspaceName;

  public AbstractMigrationService(InitParams initParams,
                                  POMDataStorage pomDataStorage,
                                  ListenerService listenerService,
                                  RepositoryService repoService,
                                  SettingService settingService,
                                  EntityManagerService entityManagerService) {
    this.pomStorage = pomDataStorage;
    this.listenerService = listenerService;
    this.entityManagerService = entityManagerService;
    this.settingService = settingService;
    this.repoService = repoService;

    this.log = ExoLogger.getLogger(this.getClass().getName());

    this.workspaceName = getString(initParams, "workspace", DEFAULT_WORKSPACE_NAME);
    this.limitThreshold = getInteger(initParams, LIMIT_THRESHOLD_KEY, 1);
  }

  public void addMigrationListener(Listener<T, String> listener) {
    this.listenerService.addListener(getListenerKey(), listener);
  }

  protected void broadcastListener(T t, String newId) {
    try {
      this.listenerService.broadcast(new Event<>(getListenerKey(), t, newId));
    } catch (Exception e) {
      log.error("Failed to broadcast event", e);
    }
  }

  public void start() {
    RequestLifeCycle.begin(PortalContainer.getInstance());
    try {
      beforeMigration();
      doMigration();
      afterMigration();
    } finally {
      RequestLifeCycle.end();
      restartTransaction();
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

  protected Set<PortalKey> findSites(SiteType type, long offset, long limit) {
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

    return result;
  }

  public void setSiteMigrated(SiteKey key) {
    settingService.set(CONTEXT, Scope.PORTAL.id(key.getTypeName()), key.getName(), SettingValue.create(true));
  }
  
  public void setSiteMigrated(PortalKey key) {
    settingService.set(CONTEXT, Scope.PORTAL.id(key.getType()), key.getId(), SettingValue.create(true));
  }

  public boolean isSiteMigrated(PortalKey key) {
    SettingValue<?> settingValue =
                                 settingService.get(CONTEXT, Scope.PORTAL.id(key.getType()), key.getId());
    return settingValue != null && Boolean.parseBoolean(settingValue.getValue().toString());
  }

  public boolean isSiteMigrated(SiteKey key) {
    SettingValue<?> settingValue =
                                 settingService.get(CONTEXT, Scope.PORTAL.id(key.getTypeName()), key.getName());
    return settingValue != null && Boolean.parseBoolean(settingValue.getValue().toString());
  }

  public void setPageMigrated(org.exoplatform.portal.mop.page.PageKey key) {
    settingService.set(CONTEXT,
                       Scope.PAGE.id(key.getSite().getTypeName() + "::" + key.getSite().getName()),
                       key.getName(),
                       SettingValue.create(true));
  }

  public boolean isPageMigrated(org.exoplatform.portal.mop.page.PageKey key) {
    SettingValue<?> settingValue = settingService.get(CONTEXT,
                                                      Scope.PAGE.id(key.getSite().getTypeName() + "::" + key.getSite().getName()),
                                                      key.getName());
    return settingValue != null && Boolean.parseBoolean(settingValue.getValue().toString());
  }

  public void setNavigationMigrated(PortalKey key) {
    settingService.set(CONTEXT,
                       new Scope("Navigation", key.getType()),
                       key.getId(),
                       SettingValue.create(true));
  }

  public boolean isNavigationMigrated(PortalKey key) {
    SettingValue<?> settingValue = settingService.get(CONTEXT,
                                                      new Scope("Navigation", key.getType()),
                                                      key.getId());
    return settingValue != null && Boolean.parseBoolean(settingValue.getValue().toString());
  }

  protected void restartTransaction() {
    if (forceStop) {
      return;
    }
    int i = 0;
    // Close transactions until no encapsulated transaction
    boolean success = true;
    do {
      try {
        RequestLifeCycle.end();
        i++;
      } catch (IllegalStateException e) {
        success = false;
      }
    } while (success);

    // Restart transactions with the same number of encapsulations
    for (int j = 0; j < i; j++) {
      RequestLifeCycle.begin(PortalContainer.getInstance());
    }
  }

  protected abstract void beforeMigration();

  public abstract void doMigration();

  protected abstract void afterMigration();

  public abstract void doRemove();

  protected abstract String getListenerKey();
}
