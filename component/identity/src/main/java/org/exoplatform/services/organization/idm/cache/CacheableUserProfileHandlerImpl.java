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

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.cache.OrganizationCacheHandler;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.idm.UserProfileDAOImpl;
import org.exoplatform.services.organization.impl.UserProfileImpl;

@SuppressWarnings("deprecation")
public class CacheableUserProfileHandlerImpl extends UserProfileDAOImpl {
  private UserProfile                         NULL_OBJECT          = createUserProfileInstance();

  /**
   * Used to avoid this problem 
   * 1/ Delete from cache
   * 2/ super.delete
   * 2.1 trigger preDelete listeners: the listener.findEntity => cache is populated again
   * 2.2 delete from Store
   * 2.3 trigger postDelete listeners => Error: when
   *    listener a listener calls findUserById, the entity is returned from cache
   */
  private final ThreadLocal<Boolean>          disableCacheInThread = new ThreadLocal<>();

  private final ExoCache<String, UserProfile> userProfileCache;

  @SuppressWarnings("unchecked")
  public CacheableUserProfileHandlerImpl(OrganizationCacheHandler organizationCacheHandler,
                                         PicketLinkIDMOrganizationServiceImpl orgService,
                                         PicketLinkIDMService service) {
    super(orgService, service);
    this.userProfileCache = organizationCacheHandler.getUserProfileCache();
  }

  /**
   * {@inheritDoc}
   */
  public UserProfile findUserProfileByName(String userName) throws Exception {
    UserProfile userProfile = null;
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      userProfile = (UserProfile) userProfileCache.get(userName);
      if (userProfile == null) {
        userProfile = NULL_OBJECT;
        userProfileCache.put(userName, userProfile);
      }
    }

    if (userProfile == null) {
      userProfile = super.findUserProfileByName(userName);
      if (userProfile == null) {
        userProfile = NULL_OBJECT;
        userProfileCache.put(userName, userProfile);
      } else {
        userProfile.setUserName(userName);
        cacheUserProfile(userProfile);
      }
    }

    return userProfile == NULL_OBJECT ? null : ((UserProfileImpl) userProfile).clone();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserProfile getProfile(String userName) {
    UserProfile userProfile = null;
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      userProfile = (UserProfile) userProfileCache.get(userName);
      userProfile = (userProfile == null || userProfile == NULL_OBJECT
          || StringUtils.isBlank(userProfile.getUserName())
          || userProfile.getUserInfoMap() == null || userProfile.getUserInfoMap().isEmpty()) ? null : userProfile;
      if (userProfile != null)
        return userProfile;
    }

    userProfile = super.getProfile(userName);
    if (userProfile == null) {
      userProfile = NULL_OBJECT;
    } else {
      userProfile.setUserName(userName);
    }
    cacheUserProfile(userProfile);

    return userProfile == NULL_OBJECT ? null : userProfile;
  }

  /**
   * {@inheritDoc}
   */
  public Collection<UserProfile> findUserProfiles() throws Exception {
    @SuppressWarnings("unchecked")
    Collection<UserProfile> userProfiles = super.findUserProfiles();
    for (UserProfile userProfile : userProfiles)
      cacheUserProfile(userProfile);

    return userProfiles;
  }

  /**
   * {@inheritDoc}
   */
  public UserProfile removeUserProfile(String userName, boolean broadcast) throws Exception {
    disableCacheInThread.set(true);
    UserProfile userProfile = null;
    try {
      userProfile = super.removeUserProfile(userName, broadcast);
      userProfileCache.remove(userName);
      return userProfile;
    } finally {
      disableCacheInThread.set(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void saveUserProfile(UserProfile profile, boolean broadcast) throws Exception {
    super.saveUserProfile(profile, broadcast);
    cacheUserProfile(profile);
  }

  public void clearCache() {
    userProfileCache.clearCache();
  }

  private void cacheUserProfile(UserProfile userProfile) {
    if (StringUtils.isNotBlank(userProfile.getUserName())) {
      userProfileCache.put(userProfile.getUserName(), ((UserProfileImpl) userProfile).clone());
    }
  }

}
