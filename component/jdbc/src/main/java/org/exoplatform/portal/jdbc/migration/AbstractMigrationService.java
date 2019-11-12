package org.exoplatform.portal.jdbc.migration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.persistence.EntityManager;

import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public abstract class AbstractMigrationService<T> {
  protected Log                           LOG;

  protected final static String           LIMIT_THRESHOLD_KEY = "LIMIT_THRESHOLD";

  protected POMDataStorage     pomStorage;

  protected final ListenerService listenerService;

  protected final EntityManagerService    entityManagerService;

  protected boolean                       forkStop            = false;

  protected int                           LIMIT_THRESHOLD     = 10;

  public AbstractMigrationService(InitParams initParams,
                                  POMDataStorage pomDataStorage,
                                  ListenerService listenerService,
                                  EntityManagerService entityManagerService) {
    this.pomStorage = pomDataStorage;
    this.listenerService = listenerService;
    this.entityManagerService = entityManagerService;
    LOG = ExoLogger.getLogger(this.getClass().getName());
  }

  public void addMigrationListener(Listener<T, String> listener) {
    this.listenerService.addListener(getListenerKey(), listener);
  }

  protected void broadcastListener(T t, String newId) {
    try {
      this.listenerService.broadcast(new Event(getListenerKey(), t, newId));
    } catch (Exception e) {
      LOG.error("Failed to broadcast event", e);
    }
  }

  public void start() {
    forkStop = false;
    try {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      beforeMigration();
      //
      doMigration();
      //
      afterMigration();
    } catch (Exception e) {
      LOG.error("Failed to run migration data from JCR to Mysql.", e);
    } finally {
      RequestLifeCycle.end();
    }
  }

  public void stop() {
    forkStop = true;
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

  /**
   * Starts the transaction if it isn't existing
   * 
   * @return
   */
  protected boolean startTx() {
    EntityManager em = entityManagerService.getEntityManager();
    if (!em.getTransaction().isActive()) {
      em.getTransaction().begin();
      LOG.debug("started new transaction");
      return true;
    }
    return false;
  }

  /**
   * Stops the transaction
   * 
   * @param requestClose
   */
  public void endTx(boolean requestClose) {
    EntityManager em = entityManagerService.getEntityManager();
    try {
      if (requestClose && em.getTransaction().isActive()) {
        em.getTransaction().commit();
        LOG.debug("commited transaction");
      }
    } catch (RuntimeException e) {
      LOG.error("Failed to commit to DB::" + e.getMessage(), e);
      em.getTransaction().rollback();
    }
  }

  protected String getProperty(Node node, String propName) throws Exception {
    try {
      return node.getProperty(propName).getString();
    } catch (Exception ex) {
      return null;
    }
  }

  protected String[] getProperties(Node node, String propName) throws Exception {
    List<String> values = new LinkedList<>();
    try {
      for (Value val : node.getProperty(propName).getValues()) {
        values.add(val.getString());
      }
      return values.toArray(new String[values.size()]);
    } catch (Exception ex) {
      return null;
    }
  }

  protected ContainerData migrateContainer(ContainerData containerData) throws Exception {
    //
    List<ComponentData> children = this.migrateComponents(containerData.getChildren());
    ContainerData layout = new ContainerData(null,
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
    return layout;
  }

  protected <S> ApplicationData<S> migrateApplication(ApplicationData<S> app) throws Exception {
    S s = pomStorage.load(app.getState(), app.getType());
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
            app.getAccessPermissions()
    );
  }

  protected BodyData migrateBodyData(BodyData body) {
    return new BodyData(null, body.getType());
  }

  protected PageData migratePageData(PageData page) throws Exception {
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


  protected List<ComponentData> migrateComponents(List<ComponentData> list) throws Exception {
    List<ComponentData> result = new ArrayList<>();

    for (ComponentData comp : list) {
      if (comp instanceof ContainerData) {
        result.add(migrateContainer((ContainerData)comp));
      } else if (comp instanceof PageData) {
        result.add(this.migratePageData((PageData)comp));
      } else if (comp instanceof BodyData) {
        result.add(migrateBodyData((BodyData)comp));
      } else if (comp instanceof ApplicationData) {
        result.add(migrateApplication((ApplicationData)comp));
      }
    }

    return result;
  }

  protected abstract void beforeMigration() throws Exception;

  public abstract void doMigration() throws Exception;

  protected abstract void afterMigration() throws Exception;

  public abstract void doRemove() throws Exception;

  protected abstract String getListenerKey();
}
