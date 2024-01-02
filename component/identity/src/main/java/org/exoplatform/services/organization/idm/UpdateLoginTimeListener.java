/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.organization.idm;

import java.util.Calendar;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.security.ConversationRegistry;
import org.exoplatform.services.security.ConversationState;

import io.meeds.common.ContainerTransactional;

@Asynchronous
public class UpdateLoginTimeListener extends Listener<ConversationRegistry, ConversationState> {

  private static final Log LOG = ExoLogger.getLogger(UpdateLoginTimeListener.class);

  public static final String  USER_PROFILE = "UserProfile";

  private PortalContainer     container;

  private OrganizationService organizationService;

  public UpdateLoginTimeListener(PortalContainer container) {
    this.container = container;
  }

  @Override
  @ContainerTransactional
  public void onEvent(Event<ConversationRegistry, ConversationState> event) {
    if (organizationService == null) {
      organizationService = this.container.getComponentInstanceOfType(OrganizationService.class);
    }
    UserHandler userHandler = organizationService.getUserHandler();
    ConversationState state = event.getData();
    String userId = state.getIdentity().getUserId();
    try {
      User user = (User) state.getAttribute(USER_PROFILE);
      if (user == null) {
        user = userHandler.findUserByName(userId);
        state.setAttribute(USER_PROFILE, user);
      }
      user.setLastLoginTime(Calendar.getInstance().getTime());
      userHandler.saveUser(user, false);
    } catch (Exception e) {
      LOG.error("Error while updating the last login time for user {}", userId, e);
    }
  }
}
