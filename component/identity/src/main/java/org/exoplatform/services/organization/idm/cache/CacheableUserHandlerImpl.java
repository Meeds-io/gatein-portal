/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.organization.idm.cache;

import java.util.List;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.organization.ExtendedCloneable;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.cache.MembershipCacheKey;
import org.exoplatform.services.organization.cache.OrganizationCacheHandler;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.UserDAOImpl;

public class CacheableUserHandlerImpl extends UserDAOImpl {

  private final ExoCache<String, User>               userCache;

  private final FutureExoCache<String, User, UserStatus> futureUserCache;

  private final ExoCache<String, UserProfile>        userProfileCache;

  private final ExoCache<MembershipCacheKey, Object> membershipCache;

  /**
   * Used to avoid this problem 
   * 1/ Delete from cache
   * 2/ super.delete
   * 2.1 trigger preDelete listeners: the listener.findEntity => cache is populated again
   * 2.2 delete from Store
   * 2.3 trigger postDelete listeners => Error: when
   *    listener a listener calls findUserById, the entity is returned from cache
   */
  private final ThreadLocal<Boolean>                 disableCacheInThread = new ThreadLocal<>();

  @SuppressWarnings("unchecked")
  public CacheableUserHandlerImpl(OrganizationCacheHandler organizationCacheHandler,
                                  PicketLinkIDMOrganizationServiceImpl orgService,
                                  PicketLinkIDMService idmService) {
    super(orgService, idmService);
    this.userCache = organizationCacheHandler.getUserCache();
    futureUserCache = new FutureExoCache<>(new Loader<String, User, UserStatus>() {
      @Override
      public User retrieve(UserStatus context, String key) throws Exception {
        disableCacheInThread.set(true);
        try {
          return findUserByName(key, context);
        } finally {
          disableCacheInThread.set(false);
        }
      }
    }, userCache);
    this.userProfileCache = organizationCacheHandler.getUserProfileCache();
    this.membershipCache = organizationCacheHandler.getMembershipCache();
  }

  /**
   * {@inheritDoc}
   */
  public User removeUser(String userName, boolean broadcast) throws Exception {
    User user = null;
    disableCacheInThread.set(true);
    try {
      user = super.removeUser(userName, broadcast);

      userCache.remove(userName);
      userProfileCache.remove(userName);

      if (user != null) {
        membershipCache.remove(new MembershipCacheKey(userName, null, null));
        List<?> objects = membershipCache.getCachedObjects();
        for (Object obj : objects) {
          if (obj instanceof Membership) {
            Membership membership = (Membership) obj;
            if (membership.getUserName().equals(userName)) {
              membershipCache.remove(new MembershipCacheKey(membership));
            }
          }
        }
      }
    } finally {
      disableCacheInThread.set(false);
    }
    return user;
  }

  /**
   * {@inheritDoc}
   */
  public void saveUser(User user, boolean broadcast) throws Exception {
    disableCacheInThread.set(true);
    try {
      super.saveUser(user, broadcast);
      userCache.remove(user.getUserName());
    } finally {
      disableCacheInThread.set(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void createUser(User user, boolean broadcast) throws Exception {
    disableCacheInThread.set(true);
    try {
      super.createUser(user, broadcast);
      userCache.remove(user.getUserName());
    } finally {
      disableCacheInThread.set(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  public User setEnabled(String userName, boolean enabled, boolean broadcast) throws Exception {
    User result = super.setEnabled(userName, enabled, broadcast);
    userCache.remove(userName);
    return result;
  }

  /**
   * {@inheritDoc}
   */
  public User findUserByName(String userName, UserStatus status) throws Exception {
    User user = null;
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      user = futureUserCache.get(status, userName);
    } else {
      user = super.findUserByName(userName, status);
    }
    return user == null ? null : (status.matches(user.isEnabled()) ? (User) ((ExtendedCloneable) user).clone() : null);
  }

  public void clearCache() {
    userCache.clearCache();
  }

}
