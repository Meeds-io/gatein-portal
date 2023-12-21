/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.services.organization.idm;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.common.transaction.JTAUserTransactionLifecycleService;
import org.picketlink.idm.api.IdentitySession;

/**
 * Abstract superclass for other DAO classes
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AbstractDAOImpl {
    protected final PicketLinkIDMService service_;

    protected final PicketLinkIDMOrganizationServiceImpl orgService;

    protected final Log                                log = ExoLogger.getLogger(getClass());

    public AbstractDAOImpl(PicketLinkIDMOrganizationServiceImpl orgService, PicketLinkIDMService idmService) {
        service_ = idmService;
        this.orgService = orgService;
    }

    public void handleException(String messageToLog, Exception e) {
        try {
          // Mark JTA transaction to rollback-only if JTA setup is enabled
          if (orgService.getConfiguration().isUseJTA()) {
              try {
                  JTAUserTransactionLifecycleService transactionLfService = (JTAUserTransactionLifecycleService) ExoContainerContext
                          .getCurrentContainer().getComponentInstanceOfType(JTAUserTransactionLifecycleService.class);
                  UserTransaction tx = transactionLfService.getUserTransaction();
                  if (tx.getStatus() == Status.STATUS_ACTIVE) {
                      tx.setRollbackOnly();
                  }
              } catch (Exception tre) {
                  log.warn("Unable to set Transaction status to be rollback only", tre);
              }
          } else {
              orgService.recoverFromIDMError();
          }
          // Always throw the original exception to make sure that top layer services
          // are triggered about the error
          throw new IllegalStateException(messageToLog, e);
        } catch (IllegalStateException e1) {
          throw e1;
        } catch (Exception e1) {
          log.warn("Second Exception when rolling back original error {}/{}", messageToLog, e.getMessage(), e1);
          throw new IllegalStateException(messageToLog, e);
        }
    }

    protected IdentitySession getIdentitySession() throws Exception {
        return service_.getIdentitySession();
    }
}
