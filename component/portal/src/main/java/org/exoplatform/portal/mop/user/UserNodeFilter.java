/*
 * Copyright (C) 2010 eXo Platform SAS.
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

package org.exoplatform.portal.mop.user;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.NodeFilter;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class UserNodeFilter implements NodeFilter {

  /** . */
  private final UserPortalImpl       userPortal;

  /** . */
  private final UserNodeFilterConfig config;

  public UserNodeFilter(UserPortalImpl userPortal, UserNodeFilterConfig config) {
    if (userPortal == null) {
      throw new NullPointerException();
    }
    if (config == null) {
      throw new NullPointerException();
    }

    //
    this.userPortal = userPortal;
    this.config = config;
  }

  private boolean canRead(NodeState state) {
    PageKey pageRef = state.getPageRef();
    if (pageRef != null) {
      PageContext page = userPortal.service.getPageService().loadPage(pageRef);
      if (page != null) {
        UserACL userACL = userPortal.service.getUserACL();
        return userACL.hasAccessPermission(page, userACL.getUserIdentity(userPortal.getUserName()));
      }
    }
    return true;
  }

  private boolean canWrite(NodeState state) {
    PageKey pageRef = state.getPageRef();
    if (pageRef != null) {
      PageContext page = userPortal.service.getPageService().loadPage(pageRef);
      if (page != null) {
        UserACL userACL = userPortal.service.getUserACL();
        return userACL.hasEditPermission(page, userACL.getUserIdentity(userPortal.getUserName()));
      }
    }
    return false;
  }

  public boolean accept(int depth, String id, String name, NodeState state) {
    Visibility visibility = state.getVisibility();

    // Correct null -> displayed
    if (visibility == null) {
      visibility = Visibility.DISPLAYED;
    }

    // If a visibility is specified then we use it
    if (config.visibility != null && !config.visibility.contains(visibility)) {
      return false;
    }

    // Filter by path
    if (depth > 0 && config.path != null && (depth - 1 >= config.path.length || !config.path[depth - 1].equals(name))) {
      return false;
    }

    // Perform authorization check
    if (config.authorizationMode == UserNodeFilterConfig.AUTH_NO_CHECK) {
      // Do nothing here
    } else {
      if (visibility == Visibility.SYSTEM) {
        if (config.authorizationMode == UserNodeFilterConfig.AUTH_READ_WRITE) {
          if (!canWrite(state)) {
            return false;
          }
        } else {
          if (!canRead(state)) {
            return false;
          }
        }
      } else if (config.authorizationMode == UserNodeFilterConfig.AUTH_READ_WRITE) {
        if (!canRead(state)) {
          return false;
        }
      } else {
        if (!canRead(state)) {
          return false;
        }
      }
    }

    // Now make the custom checks
    if (visibility == Visibility.TEMPORAL && config.temporalCheck) {
      long now = System.currentTimeMillis();
      if (state.getStartPublicationTime() != -1 && now < state.getStartPublicationTime()) {
        return false;
      }
      if (state.getEndPublicationTime() != -1 && now > state.getEndPublicationTime()) {
        return false;
      }
    }

    //
    return true;
  }
}
