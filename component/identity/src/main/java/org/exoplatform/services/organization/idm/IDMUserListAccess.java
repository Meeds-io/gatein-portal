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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.impl.UserImpl;
import org.gatein.common.logging.LogLevel;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.api.SortOrder;
import org.picketlink.idm.api.query.UserQuery;
import org.picketlink.idm.api.query.UserQueryBuilder;
import org.picketlink.idm.impl.api.query.UserQueryBuilderImpl;

/*
 * @author <a href="mailto:boleslaw.dawidowicz at redhat.com">Boleslaw Dawidowicz</a>
 */
public class IDMUserListAccess implements ListAccess<User>, Serializable {
    private static Logger log = LoggerFactory.getLogger(IDMUserListAccess.class);

    private final UserQueryBuilder userQueryBuilder;

    private final int pageSize;

    private final boolean countAll;

    private final UserStatus userStatus;

    private List<org.picketlink.idm.api.User> fullResults;

    private int size = -1;

    private User lastExisting;

    public IDMUserListAccess(UserQueryBuilder userQueryBuilder, int pageSize, boolean countAll, UserStatus userStatus) {
        this.userQueryBuilder = userQueryBuilder;
        this.pageSize = pageSize;
        this.countAll = countAll;
        this.userStatus = userStatus;
    }

    public User[] load(int index, int length) throws Exception {
        if (log.isTraceEnabled()) {
            Tools.logMethodIn(log, LogLevel.TRACE, "load", new Object[] { "index", index, "length", length });
        }

        if(length == 0) {
            return new User[0];
        }

        //As test suppose, we should throw exception when try to load more element than size
        if(index + length > this.getSize()) {
            throw new IllegalArgumentException("Try to get more than number users can retrieve");
        }

        List<org.picketlink.idm.api.User> users = null;

        if (fullResults == null) {
            getOrganizationService().flush();

            if (this.userStatus == UserStatus.ENABLED) {
                //In the case of LDAP activated store, pagination will be disabled setPage(false)
                userQueryBuilder.page(index, length);
                UserQuery query = userQueryBuilder.sort(SortOrder.ASCENDING).createQuery();
                List<org.picketlink.idm.api.User> allUsers = getIDMService().getIdentitySession().list(query);
                //Need to check all returned users is enabled
                //In the case of enabled store DB + LDAP PersistenceManager return All users from
                //LDAP and enbled user from DB
                users = filterUserEnabled(allUsers, index, length);
            }else{
                userQueryBuilder.page(index, length);
                UserQuery query = userQueryBuilder.sort(SortOrder.ASCENDING).createQuery();
                users = getIDMService().getIdentitySession().list(query);
            }
        } else {
            if (this.userStatus == UserStatus.ENABLED) {
                //Need to check all returned users is enabled
                users = filterUserEnabled(fullResults, index, length);
            }else{
                users = fullResults.subList(index, index + length);
            }
        }

        User[] exoUsers = new User[length];

        int i = 0;

        for (; i < users.size(); i++) {
            org.picketlink.idm.api.User user = users.get(i);

            User gtnUser = new UserImpl(user.getId());
            ((UserDAOImpl) getOrganizationService().getUserHandler()).populateUser(gtnUser, getIDMService()
                    .getIdentitySession());
            exoUsers[i] = gtnUser;
            lastExisting = gtnUser;
        }

        if (length > users.size()) {

            for (; i < length; i++) {
                exoUsers[i] = lastExisting;
            }
        }

        if (log.isTraceEnabled()) {
            Tools.logMethodOut(log, LogLevel.TRACE, "load", exoUsers);
        }

        return exoUsers;
    }

    public int getSize() throws Exception {
        if (log.isTraceEnabled()) {
            Tools.logMethodIn(log, LogLevel.TRACE, "getSize", null);
        }

        getOrganizationService().flush();

        int result;

        if (size < 0) {

            if (fullResults != null) {
                result = fullResults.size();
            } else if (countAll) {
                /*
                    wait for PersistenceManager.getUserCount(true)
                    see https://community.jboss.org/wiki/DisabledUser
                */
//             result = getIDMService().getIdentitySession().getPersistenceManager().getUserCount(enabledOnly);
                result = getIDMService().getIdentitySession().getPersistenceManager().getUserCount();
            } else {
                userQueryBuilder.page(0, 0);
                UserQuery query = userQueryBuilder.sort(SortOrder.ASCENDING).createQuery();
                fullResults = getIDMService().getIdentitySession().list(query);
                if (this.userStatus == UserStatus.ENABLED) {
                    result = filterUserEnabled(fullResults,0,0).size();
                }else{
                    result = fullResults.size();
                }
            }

            size = result;
        } else {
            result = size;
        }

        if (log.isTraceEnabled()) {
            Tools.logMethodOut(log, LogLevel.TRACE, "getSize", result);
        }

        return result;

    }

    PicketLinkIDMService getIDMService() {
        return ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(PicketLinkIDMService.class);
    }

    PicketLinkIDMOrganizationServiceImpl getOrganizationService() {
        return (PicketLinkIDMOrganizationServiceImpl) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(
                OrganizationService.class);
    }

    private List<org.picketlink.idm.api.User> filterUserEnabled(List<org.picketlink.idm.api.User> fullResults,int index, int length) throws Exception {
        List<org.picketlink.idm.api.User> result = new ArrayList<org.picketlink.idm.api.User>();
        if (fullResults != null && fullResults.size() > 0) {
            int offset = 0;
            for (org.picketlink.idm.api.User user : fullResults) {
                Attribute attr = getIDMService().getIdentitySession().getAttributesManager().getAttribute(user.getKey(), UserDAOImpl.USER_ENABLED);
                if (attr == null || attr.getValue().toString().equals("true")) {
                    if (offset >= index) {
                        result.add(user);
                        if (length != 0 && result.size() == length) {
                            break;
                        }
                    }
                    offset++;
                }
            }
        }
        return result;
    }
}
