/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.services.organization.idm;

import javax.security.auth.login.LoginException;

import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.security.jaas.AbstractLoginModule;

/**
 * Login module can be used to add authenticated user to some group after successful login.<br>
 * For example, user can be add as "member" to group "/platform/users" after his login. Group name and Membership type are
 * configurable and if they are not provided by configuration, then value "member" is used as default value for membership type
 * and "/platform/users" for group.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 * @author <a href="mailto:boleslaw.dawidowicz@redhat.com">Boleslaw Dawidowicz</a>
 */
public class CustomMembershipLoginModule extends AbstractLoginModule {
    /** Logger. */
    private static final Log log = ExoLogger.getLogger(CustomMembershipLoginModule.class);

    private static final String OPTION_MEMBERSHIP_TYPE = "membershipType";

    private static final String OPTION_GROUP_ID = "groupId";

    // values obtained from configuration options
    private String membershipType;

    private String groupId;

    // MembershipEntry with values provided from configuration. We will use it to check if user is not already presented in our
    // group.
    private MembershipEntry requestedMembershipEntry;

    /**
     * Read values from configuration. Default values ("member" and "/platform/users") are used if options are missing in
     * configuration.
     */
    protected void afterInitialize() {
        membershipType = options.get(OPTION_MEMBERSHIP_TYPE) != null ? (String) options.get(OPTION_MEMBERSHIP_TYPE) : "member";
        groupId = options.get(OPTION_GROUP_ID) != null ? (String) options.get(OPTION_GROUP_ID) : "/platform/users";
        // membershipType is * so we are not checking exact value of membershipType in method login
        requestedMembershipEntry = new MembershipEntry(groupId);
    }

    /**
     * @see javax.security.auth.spi.LoginModule#login()
     */
    @SuppressWarnings("unchecked")
    public boolean login() throws LoginException {
        if (log.isDebugEnabled()) {
            log.debug("login invoked!");
        }
        try {
            // Get identity set by SharedStateLoginModule in case of successful authentication
            Identity identity = null;
            if (sharedState.containsKey("exo.security.identity")) {
                identity = (Identity) sharedState.get("exo.security.identity");
            }

            // Return if identity is not present (this means that user authentication failed in SharedStateLoginModule)
            if (identity == null) {
                log.warn("Identity not found in shared state under exo.security.identity. This login module will be ignored");
                return false;
            }

            // Check if user is already added to our group with given membershipType. If yes, we don't need to do something.
            if (identity.getMemberships().contains(requestedMembershipEntry)) {
                if (log.isTraceEnabled()) {
                    log.trace("Requested membership entry " + requestedMembershipEntry + " already presented for user "
                            + identity.getUserId());
                }
                return true;
            }

            // Now add our user to requested group
            log.info("User " + identity.getUserId() + " will be added to group " + groupId + " as " + membershipType + ".");
            addUserToPlatformUsers(identity.getUserId());

            // Recreate identity
            Authenticator authenticator = (Authenticator) getContainer().getComponentInstanceOfType(Authenticator.class);
            identity = authenticator.createIdentity(identity.getUserId());
            sharedState.put("exo.security.identity", identity);
            return true;
        } catch (Exception e) {
            LoginException le = new LoginException();
            le.initCause(e);
            throw le;
        }
    }

    /**
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    public boolean commit() throws LoginException {
        return true;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#abort()
     */
    public boolean abort() throws LoginException {
        return true;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#logout()
     */
    public boolean logout() throws LoginException {
        return true;
    }

    @Override
    protected Log getLogger() {
        return log;
    }

    /**
     * Add given user to our group with given membershipType.
     *
     * @param userId
     */
    private void addUserToPlatformUsers(String userId) throws Exception {
        OrganizationService orgService = (OrganizationService) getContainer().getComponentInstanceOfType(
                OrganizationService.class);
        try {
            begin(orgService);
            User user = orgService.getUserHandler().findUserByName(userId);
            MembershipType memberType = orgService.getMembershipTypeHandler().findMembershipType(membershipType);
            Group platformUsersGroup = orgService.getGroupHandler().findGroupById(groupId);
            orgService.getMembershipHandler().linkMembership(user, platformUsersGroup, memberType, true);
        } catch (Exception e) {
            log.error("Failed to add user " + userId + " to group " + groupId + ".", e);
            // don't rethrow login exception in case of failure.
            // throw e;
        } finally {
            end(orgService);
        }
    }

    private void begin(OrganizationService orgService) {
        if (orgService instanceof ComponentRequestLifecycle) {
            RequestLifeCycle.begin((ComponentRequestLifecycle) orgService);
        }
    }

    private void end(OrganizationService orgService) {
        if (orgService instanceof ComponentRequestLifecycle) {
            RequestLifeCycle.end();
        }
    }
}
