package org.picketlink.idm.impl.store.hibernate;

import org.hibernate.SessionFactory;
import org.picketlink.idm.common.exception.IdentityException;
import org.picketlink.idm.spi.store.IdentityStoreSession;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper around HibernateEntityManager This class replaces the original
 * PicketLink HibernateIdentityStoreSessionImpl class to fix transaction status
 * management.
 */
public class ExoHibernateIdentityStoreSessionImpl implements IdentityStoreSession {
  private static Logger              log               = Logger.getLogger(HibernateIdentityStoreImpl.class.getName());

  private final SessionFactory       sessionFactory;

  private final boolean              lazyStartOfHibernateTransaction;

  // Tracking status of Hibernate transaction. Value "true" means that Hibernate
  // transaction has been started for this client
  private final ThreadLocal<Boolean> hibernateTxStatus = new ThreadLocal<>();

  public ExoHibernateIdentityStoreSessionImpl(SessionFactory sessionFactory, boolean lazyStartOfHibernateTransaction) {
    this.sessionFactory = sessionFactory;
    this.lazyStartOfHibernateTransaction = lazyStartOfHibernateTransaction;
  }

  public Object getSessionContext() {
    return sessionFactory.getCurrentSession();
  }

  public void close() throws IdentityException {
    sessionFactory.getCurrentSession().close();
  }

  public void save() throws IdentityException {
    // In non-lazy setup we always need flush
    Boolean flushNeeded = !lazyStartOfHibernateTransaction;

    // Now check if Hibernate transaction was started. We need flush in that case
    if (!flushNeeded) {
      Boolean txStatus = hibernateTxStatus.get();
      flushNeeded = txStatus != null && txStatus;
    }

    if (flushNeeded) {
      sessionFactory.getCurrentSession().flush();
    }
  }

  public void clear() throws IdentityException {
    // In non-lazy setup we always need clear
    Boolean clearNeeded = !lazyStartOfHibernateTransaction;

    // Now check if Hibernate transaction was started. We need clear in that case
    if (!clearNeeded) {
      Boolean txStatus = hibernateTxStatus.get();
      clearNeeded = txStatus != null && txStatus;
    }

    if (clearNeeded) {
      sessionFactory.getCurrentSession().clear();
    }
  }

  public boolean isOpen() {
    return sessionFactory.getCurrentSession().isOpen();
  }

  public boolean isTransactionSupported() {
    return true;
  }

  public void startTransaction() {
    // Init ThreadLocal but don't start real Hibernate transaction if option
    // lazyStartOfHibernateTransaction is enabled
    if (lazyStartOfHibernateTransaction) {
      if (hibernateTxStatus.get() == null) {
        hibernateTxStatus.set(Boolean.FALSE);
      }
    } else {
      startHibernateTransaction();
    }
  }

  public void commitTransaction() {
    if (lazyStartOfHibernateTransaction) {
      Boolean hbTxStatus = hibernateTxStatus.get();

      // Commit hibernate transaction only if it has really been started
      if (hbTxStatus != null && hbTxStatus) {
        try {
          commitHibernateTransaction();
        } finally {
          // reset PicketLink Hibernate transaction status to be consistent with the real
          // Hibernate transaction status (see EXOGTN-2384)
          hibernateTxStatus.set(null);
        }
      }
    } else {
      commitHibernateTransaction();
    }
  }

  public void rollbackTransaction() {
    if (lazyStartOfHibernateTransaction) {
      Boolean hbTxStatus = hibernateTxStatus.get();
      // Rollback hibernate transaction only if it has really been started
      if (hbTxStatus != null && hbTxStatus) {
        try {
          rollbackHibernateTransaction();
        } finally {
          // reset PicketLink Hibernate transaction status to be consistent with the real
          // Hibernate transaction status (see EXOGTN-2384)
          hibernateTxStatus.set(null);
        }
      }
    } else {
      rollbackHibernateTransaction();
    }
  }

  public boolean isTransactionActive() {
    if (lazyStartOfHibernateTransaction) {
      return hibernateTxStatus.get() != null && hibernateTxStatus.get();
    } else {
      return isHibernateTransactionActive();
    }
  }

  void startHibernateTransactionIfNotStartedYet() {
    Boolean txStatus = hibernateTxStatus.get();

    // Now we really need to start Hibernate transaction at this point if not
    // already started
    if (txStatus == null || !txStatus) {
      startHibernateTransaction();
      hibernateTxStatus.set(Boolean.TRUE);
    }
  }

  private void startHibernateTransaction() {
    if (log.isLoggable(Level.FINER)) {
      log.log(Level.FINER, "Going to start Hibernate transaction");
    }

    sessionFactory.getCurrentSession().getTransaction().begin();
  }

  private void commitHibernateTransaction() {
    if (log.isLoggable(Level.FINER)) {
      log.log(Level.FINER, "Going to commit Hibernate transaction");
    }

    sessionFactory.getCurrentSession().getTransaction().commit();
  }

  private void rollbackHibernateTransaction() {
    if (log.isLoggable(Level.FINER)) {
      log.log(Level.FINER, "Going to rollback Hibernate transaction");
    }

    sessionFactory.getCurrentSession().getTransaction().rollback();
  }

  private boolean isHibernateTransactionActive() {
    return sessionFactory.getCurrentSession().getTransaction().isActive();
  }

  Boolean getHibernateTxStatus() {
    return hibernateTxStatus.get();
  }
}
