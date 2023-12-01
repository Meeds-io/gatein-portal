/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.portal.security.listener;

import java.util.Collection;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;

import io.meeds.common.ContainerTransactional;
import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.service.SecuritySettingService;

/**
 * This listener is triggered at the whole first user login to add its default
 * memberships switch configuration made on {@link SecuritySettingService}
 */
public class DefaultUserMembershipListener extends Listener<ConversationRegistry, ConversationState> {

  private static final Log       LOG                  = ExoLogger.getLogger(DefaultUserMembershipListener.class);

  private static final String    INTERNAL_USERS_GROUP = "/platform/users";

  private static final String    USER_PROFILE         = "UserProfile";

  private static final String    MEMBER_MT            = "member";

  private ExoContainer           container;

  private SecuritySettingService securitySettingService;

  private OrganizationService    organizationService;

  public DefaultUserMembershipListener(ExoContainer container) {
    this.container = container;
  }

  @Override
  @ContainerTransactional
  public void onEvent(Event<ConversationRegistry, ConversationState> event) throws Exception {
    if (organizationService == null) {
      organizationService = this.container.getComponentInstanceOfType(OrganizationService.class);
    }
    if (securitySettingService == null) {
      securitySettingService = this.container.getComponentInstanceOfType(SecuritySettingService.class);
    }
    ConversationState state = event.getData();
    if (state == null
        || state.getIdentity() == null
        || state.getIdentity().getUserId() == null
        || StringUtils.equals(state.getIdentity().getUserId(), IdentityConstants.ANONIM)
        || ArrayUtils.isEmpty(securitySettingService.getRegistrationGroupIds())) {
      return;
    }
    try {
      User user = getUser(state);
      if (user != null
          && isFirstTimeLogin(user)
          && allowToAddToDefaultGroups(user)) {
        LOG.info("First time login for user {}, adding it into default groups", user.getUserName());
        addUserToDefaultGroups(user);
      }
    } catch (Exception e) {
      LOG.warn("Error while updating default groups of user {}",
               state.getIdentity().getUserId(),
               e);
    }
  }

  private boolean isFirstTimeLogin(User user) {
    return user.getLastLoginTime() == null || user.getLastLoginTime().equals(user.getCreatedDate());
  }

  private boolean allowToAddToDefaultGroups(User user) throws Exception {
    return securitySettingService.getRegistrationType() == UserRegistrationType.OPEN
           || isInternalUser(user);
  }

  private boolean isInternalUser(User user) throws Exception {
    Collection<Membership> memberships = organizationService.getMembershipHandler()
                                                            .findMembershipsByUserAndGroup(user.getUserName(),
                                                                                           INTERNAL_USERS_GROUP);
    return CollectionUtils.isNotEmpty(memberships);
  }

  private void addUserToDefaultGroups(User user) throws Exception {
    MembershipType memberMembershipType = organizationService.getMembershipTypeHandler().findMembershipType(MEMBER_MT);
    for (String groupId : securitySettingService.getRegistrationGroupIds()) {
      Group group = organizationService.getGroupHandler().findGroupById(groupId);
      if (group == null) {
        LOG.warn("Group with id {} wasn't found, the newly logged in user will not be added into it", groupId);
      } else {
        Membership membership = organizationService.getMembershipHandler()
                                                   .findMembershipByUserGroupAndType(user.getUserName(),
                                                                                     groupId,
                                                                                     MEMBER_MT);
        if (membership == null) {
          organizationService.getMembershipHandler().linkMembership(user, group, memberMembershipType, true);
        }
      }
    }
  }

  private User getUser(ConversationState state) throws Exception {
    User user = (User) state.getAttribute(USER_PROFILE);
    if (user == null) {
      user = organizationService.getUserHandler().findUserByName(state.getIdentity().getUserId());
      state.setAttribute(USER_PROFILE, user);
    }
    return user;
  }

}
