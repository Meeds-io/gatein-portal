/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.organization.idm;

import java.util.List;

import javax.transaction.Status;

import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.common.transaction.JTAUserTransactionLifecycleService;
import org.picketlink.idm.api.Transaction;
import org.picketlink.idm.spi.configuration.metadata.IdentityConfigurationMetaData;
import org.picketlink.idm.spi.configuration.metadata.IdentityStoreConfigurationMetaData;
import org.picocontainer.Startable;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.organization.BaseOrganizationService;
import org.exoplatform.services.organization.cache.OrganizationCacheHandler;
import org.exoplatform.services.organization.idm.cache.CacheableGroupHandlerImpl;
import org.exoplatform.services.organization.idm.cache.CacheableMembershipHandlerImpl;
import org.exoplatform.services.organization.idm.cache.CacheableMembershipTypeHandlerImpl;
import org.exoplatform.services.organization.idm.cache.CacheableUserHandlerImpl;
import org.exoplatform.services.organization.idm.cache.CacheableUserProfileHandlerImpl;

/*
 * @author <a href="mailto:boleslaw.dawidowicz at redhat.com">Boleslaw Dawidowicz</a>
 */
public class PicketLinkIDMOrganizationServiceImpl extends BaseOrganizationService implements Startable,
        ComponentRequestLifecycle {

    // We may have several portal containers thus we need one PicketLinkIDMService per portal container
    // private static PicketLinkIDMService jbidmService_;
    private PicketLinkIDMServiceImpl idmService_;

    public static final String CONFIGURATION_OPTION = "configuration";

    private Config configuration = new Config();

    private JTAUserTransactionLifecycleService jtaTransactionLifecycleService;

    private  OrganizationCacheHandler organizationCacheHandler;

    private static final Logger log = LoggerFactory.getLogger(PicketLinkIDMOrganizationServiceImpl.class);
    private static final boolean traceLoggingEnabled = log.isTraceEnabled();

    // Indicates whether any call to startRequest and endRequest is accepted
    private volatile boolean acceptComponentRequestCall;

    public PicketLinkIDMOrganizationServiceImpl(InitParams params, PicketLinkIDMService idmService,
                                                  JTAUserTransactionLifecycleService jtaTransactionLifecycleService, OrganizationCacheHandler organizationCacheHandler) throws Exception {
      this.idmService_ = (PicketLinkIDMServiceImpl) idmService;
      this.jtaTransactionLifecycleService = jtaTransactionLifecycleService;
      this.organizationCacheHandler = organizationCacheHandler;

      if (params != null) {
        // Options
        ObjectParameter configurationParam = params.getObjectParam(CONFIGURATION_OPTION);
  
        if (configurationParam != null) {
          this.configuration = (Config) configurationParam.getObject();
          initConfiguration(params);
        }
      }

      if(organizationCacheHandler != null && (this.configuration == null || this.configuration.isUseCache())) {
        groupDAO_ = new CacheableGroupHandlerImpl(organizationCacheHandler, this, idmService, this.configuration.isCountPaginatedUsers());
        userDAO_ = new CacheableUserHandlerImpl(organizationCacheHandler, this, idmService);
        userProfileDAO_ = new CacheableUserProfileHandlerImpl(organizationCacheHandler, this, idmService);
        membershipDAO_ = new CacheableMembershipHandlerImpl(organizationCacheHandler, this, idmService, true);
        membershipTypeDAO_ = new CacheableMembershipTypeHandlerImpl(organizationCacheHandler, this, idmService);
      } else {
        groupDAO_ = new GroupDAOImpl(this, idmService);
        userDAO_ = new UserDAOImpl(this, idmService);
        userProfileDAO_ = new UserProfileDAOImpl(this, idmService);
        membershipDAO_ = new MembershipDAOImpl(this, idmService);
        membershipTypeDAO_ = new MembershipTypeDAOImpl(this, idmService);
      }

    }

    public PicketLinkIDMOrganizationServiceImpl(InitParams params, PicketLinkIDMService idmService,
            JTAUserTransactionLifecycleService jtaTransactionLifecycleService) throws Exception {
      this(params, idmService, jtaTransactionLifecycleService, null);
    }

    public final org.picketlink.idm.api.Group getJBIDMGroup(String groupId) throws Exception {
        String[] ids = groupId.split("/");
        String name = ids[ids.length - 1];
        String parentId = null;
        if (groupId.contains("/")) {
            parentId = groupId.substring(0, groupId.lastIndexOf("/"));
        }

        String plGroupName = configuration.getPLIDMGroupName(name);

        return idmService_.getIdentitySession().getPersistenceManager()
                .findGroup(plGroupName, getConfiguration().getGroupType(parentId));
    }

    @Override
    public void start() {
        try {
            if (configuration.isUseJTA()) {
                jtaTransactionLifecycleService.registerListener(new IDMTransactionSyncListener(idmService_));
            }
            acceptComponentRequestCall = true;

            RequestLifeCycle.begin(this);

            super.start();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            RequestLifeCycle.end();
        }

    }

    @Override
    public void stop() {
        // toto
    }

    /*
   */
    /**
     * Used to allow nested requests (as done by the authenticator during unit tests) and avoid to commit two times the same
     * transaction.
     */
    /*
     *
     * private ThreadLocal<AtomicInteger> currentRequestCount = new ThreadLocal<AtomicInteger>() {
     *
     * @Override protected AtomicInteger initialValue() { return new AtomicInteger(); } };
     */

    public void startRequest(ExoContainer container) {
        if (!acceptComponentRequestCall)
            return;
        try {
            if (configuration.isUseJTA()) {
                if (traceLoggingEnabled) {
                    log.trace("Starting UserTransaction in method startRequest");
                }

                try {
                    if(jtaTransactionLifecycleService.getUserTransaction().getStatus() != Status.STATUS_NO_TRANSACTION &&
                            jtaTransactionLifecycleService.getUserTransaction().getStatus() != Status.STATUS_ACTIVE){
                        //Commit or Rollback JTA transaction according to it's current status
                        jtaTransactionLifecycleService.finishJTATransaction();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                jtaTransactionLifecycleService.beginJTATransaction();
            } else {

                if (!idmService_.getIdentitySession().getTransaction().isActive()) {
                    idmService_.getIdentitySession().beginTransaction();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void flush() {
        if (configuration.isUseJTA()) {
            if (traceLoggingEnabled) {
                log.trace("Flushing UserTransaction in method flush");
            }
            // Complete restart of JTA transaction don't have good performance. So we will only sync identitySession (same
            // as for non-jta environment)
            // finishJTATransaction();
            // beginJTATransaction();
            try {
                if (jtaTransactionLifecycleService.getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                    idmService_.getIdentitySession().save();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            try {
                if (idmService_.getIdentitySession().getTransaction().isActive()) {
                    idmService_.getIdentitySession().save();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                recoverFromIDMError(e);
            }
        }
    }

    public void endRequest(ExoContainer container) {
        if (!acceptComponentRequestCall)
            return;
        if (configuration.isUseJTA()) {
            if (traceLoggingEnabled) {
                log.trace("Finishing UserTransaction in method endRequest");
            }
            try {
                jtaTransactionLifecycleService.finishJTATransaction();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            try {
                Transaction transaction = idmService_.getIdentitySession().getTransaction();
                if (transaction.isActive()) {
                    transaction.commit();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                recoverFromIDMError(e);
            }
        }
    }

    public boolean isStarted(ExoContainer container) {
        try {
            if (configuration.isUseJTA()) {
                return jtaTransactionLifecycleService.getUserTransaction() == null ? false :
                        jtaTransactionLifecycleService.getUserTransaction().getStatus()== Status.STATUS_ACTIVE;
            } else {
                return (idmService_.getIdentitySession() == null || idmService_.getIdentitySession().getTransaction() == null) ? false :
                        idmService_.getIdentitySession().getTransaction().isActive();
            }
        } catch (Exception e) {
            log.error("Error while checking on Transaction status : ", e);
        }
        return false;
    }

    // Should be used only for non-JTA environment
    public void recoverFromIDMError(Exception e) {
        try {
            // We need to restart Hibernate transaction if it's available. First rollback old one and then start new one
            Transaction idmTransaction = idmService_.getIdentitySession().getTransaction();
            if (idmTransaction != null && idmTransaction.isActive()) {
                try {
                  log.info("IDM Transaction rollback");
                  idmTransaction.rollback();
                  log.info("IDM Transaction has been rolled-backed");
                } catch (Exception e1) {
                  log.warn("Error during IDM Transaction rollback.", e1);
                }
                try {
                  log.info("IDM Transaction restart");
                  idmTransaction.start();
                  log.info("IDM Transaction restarted");
                } catch (Exception e1) {
                  log.warn("Error during IDM Transaction restart, a new transaction will be started", e1);
                  idmService_.getIdentitySession().beginTransaction();
                  log.info("New IDM Transaction started");
                }
            } else {
              log.info("New IDM Transaction start");
              idmService_.getIdentitySession().beginTransaction();
              log.info("New IDM Transaction started");
            }
        } catch (Exception e1) {
            log.warn("Error during recovery of old error", e1);
        }
    }

    public Config getConfiguration() {
        return configuration;
    }

    public void clearCaches(){
        if(organizationCacheHandler != null && (this.configuration == null || this.configuration.isUseCache())) {
            if(groupDAO_ != null && groupDAO_ instanceof CacheableGroupHandlerImpl){
                ((CacheableGroupHandlerImpl) groupDAO_).clearCache();
            }
            if(userDAO_ != null && userDAO_ instanceof CacheableGroupHandlerImpl){
                ((CacheableGroupHandlerImpl) userDAO_).clearCache();
            }
            if(userProfileDAO_ != null && userProfileDAO_ instanceof CacheableGroupHandlerImpl){
                ((CacheableGroupHandlerImpl) userProfileDAO_).clearCache();
            }
            if(membershipDAO_ != null && membershipDAO_ instanceof CacheableGroupHandlerImpl){
                ((CacheableGroupHandlerImpl) membershipDAO_).clearCache();
            }
            if(membershipTypeDAO_ != null && membershipTypeDAO_ instanceof CacheableGroupHandlerImpl){
                ((CacheableGroupHandlerImpl) membershipTypeDAO_).clearCache();
            }
        }
    }

    public void setConfiguration(Config configuration) {
        this.configuration = configuration;
    }

    private void initConfiguration(InitParams params) {
      IdentityConfigurationMetaData configMD = ((PicketLinkIDMServiceImpl) this.idmService_).getConfigMD();
      List<IdentityStoreConfigurationMetaData> identityStores = null;
      if (configMD != null) {
        identityStores = configMD.getIdentityStores();
      }
      /* If you have DB only setup */
      if (identityStores != null && identityStores.size() > 1) {
        this.configuration.setCountPaginatedUsers(false);
        this.configuration.setSkipPaginationInMembershipQuery(true);
      } else {
        /* If you have DB+LDAP setup */
        this.configuration.setCountPaginatedUsers(true);
        this.configuration.setSkipPaginationInMembershipQuery(false);
      }
    }
}
