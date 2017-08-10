package org.exoplatform.portal.jdbc.migration;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.persistence.EntityManager;

import org.picocontainer.Startable;

import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public abstract class AbstractMigrationService<T> implements Startable {
  protected Log                           LOG;

  protected final static String           LIMIT_THRESHOLD_KEY = "LIMIT_THRESHOLD";

  protected final EventManager<T, String> eventManager;

  protected final EntityManagerService    entityManagerService;

  protected boolean                       forkStop            = false;

  protected int                           LIMIT_THRESHOLD     = 100;

  public AbstractMigrationService(InitParams initParams,
                                  EventManager<T, String> eventManager,
                                  EntityManagerService entityManagerService) {
    this.eventManager = eventManager;
    this.entityManagerService = entityManagerService;
    LOG = ExoLogger.getLogger(this.getClass().getName());
  }

  public void addMigrationListener(Listener<T, String> listener) {
    eventManager.addEventListener(getListenerKey(), listener);
  }

  public void removeMigrationListener(Listener<T, String> listener) {
    eventManager.removeEventListener(getListenerKey(), listener);
  }

  protected void broadcastListener(T t, String newId) {
    List<Listener<T, String>> listeners = eventManager.getEventListeners(getListenerKey());
    for (Listener<T, String> listener : listeners) {
      try {
        Event<T, String> event = new Event<T, String>(getListenerKey(), t, newId);
        listener.onEvent(event);
      } catch (Exception e) {
        LOG.error("Failed to broadcastListener for listener: " + listener.getName(), e);
      }
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

  protected abstract void beforeMigration() throws Exception;

  public abstract void doMigration() throws Exception;

  protected abstract void afterMigration() throws Exception;

  public abstract void doRemove() throws Exception;

  protected abstract String getListenerKey();
}
