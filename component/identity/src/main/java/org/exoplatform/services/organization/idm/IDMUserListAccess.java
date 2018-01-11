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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.gatein.common.logging.LogLevel;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.picketlink.idm.api.Attribute;
import org.picketlink.idm.api.SortOrder;
import org.picketlink.idm.api.query.UserQuery;
import org.picketlink.idm.api.query.UserQueryBuilder;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.impl.UserImpl;

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

    private boolean isDBOnly;

    public IDMUserListAccess(UserQueryBuilder userQueryBuilder, int pageSize, boolean countAll, boolean isDBOnly, UserStatus userStatus) {
        this.userQueryBuilder = userQueryBuilder;
        this.pageSize = pageSize;
        this.countAll = countAll;
        this.userStatus = userStatus;
        this.isDBOnly = isDBOnly;
    }

    public User[] load(int index, int length) throws Exception {
        if (log.isTraceEnabled()) {
            Tools.logMethodIn(log, LogLevel.TRACE, "load", new Object[] { "index", index, "length", length });
        }

        if(length == 0) {
            return new User[0];
        }

        //As test suppose, we should throw exception when try to load more element than size
        int totalSize = this.getSize();
        if(index + length > totalSize) {
            throw new IllegalArgumentException("Try to get more than number users can retrieve");
        }

        Collection<org.picketlink.idm.api.User> users = null;

        if (fullResults == null) {
            getOrganizationService().flush();

            userQueryBuilder.page(index, length);
            UserQuery query = userQueryBuilder.sort(SortOrder.ASCENDING).createQuery();
            users = getIDMService().getIdentitySession().list(query);

            if ((this.userStatus == UserStatus.ENABLED || this.userStatus == UserStatus.DISABLED) && !isDBOnly) {
                users= filterUserByStatus(users, this.userStatus, index, length);
            }
        } else {
            if ((this.userStatus == UserStatus.ENABLED || this.userStatus == UserStatus.DISABLED) && !isDBOnly) {
                //Need to check all returned users is enabled
                users = filterUserByStatus(fullResults, this.userStatus, index, length);
            } else{
                users = fullResults.subList(index, index + length);
            }
        }

        User[] exoUsers = new User[length];

        int i = 0;

        for (org.picketlink.idm.api.User user : users) {
            User gtnUser = new UserImpl(user.getId());
            ((UserDAOImpl) getOrganizationService().getUserHandler()).populateUser(gtnUser, getIDMService()
                    .getIdentitySession());
            exoUsers[i++] = gtnUser;
            lastExisting = gtnUser;
        }

        if (length > users.size()) {
            int additionalLength = length - users.size();
            int additionalIndex = index + length;
            if((additionalIndex + additionalLength) > totalSize) {
              additionalLength = totalSize - additionalIndex;
            }
            if(additionalLength > 0) {
              User[] additionalUsers = load(additionalIndex, additionalLength);
              if(additionalUsers != null) {
                for (User user : additionalUsers) {
                  if(user != null && StringUtils.isNotBlank(user.getUserName())) {
                    exoUsers[i++] = user;
                  }
                }
              }
            }
            while (i < length) {
              exoUsers[i++] = lastExisting;
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

              if ((this.userStatus == UserStatus.ENABLED || this.userStatus == UserStatus.DISABLED) && !isDBOnly) {
                result = filterUserByStatus(fullResults, this.userStatus, 0, fullResults.size()).size();
              } else {
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

    private Set<org.picketlink.idm.api.User> filterUserByStatus(Collection<org.picketlink.idm.api.User> fullResults, UserStatus userStatus, int index, int length) throws Exception {
        Set<org.picketlink.idm.api.User> result = new LinkedHashSet<org.picketlink.idm.api.User>();
        if (fullResults != null && fullResults.size() > 0) {
            int offset = 0;
            Iterator<org.picketlink.idm.api.User> iterator = fullResults.iterator();
            while (iterator.hasNext() && result.size() < length) {
              org.picketlink.idm.api.User user = iterator.next();
                Attribute attr = getIDMService().getIdentitySession().getAttributesManager().getAttribute(user.getKey(), UserDAOImpl.USER_ENABLED);
                if ((userStatus == UserStatus.ENABLED && attr == null) || (userStatus == UserStatus.DISABLED && attr != null && attr.getValue().toString().equals("false"))) {
                    // Check if we have to get a subset of real fullResults or from results returned by a query
                    if(this.fullResults == fullResults) {
                      if (offset >= index) {
                          result.add(user);
                      }
                      offset++;
                    } else {
                      result.add(user);
                    }
                }
            }
        }
        return result;
    }
}
