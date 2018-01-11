/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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

import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.organization.ExtendedCloneable;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.cache.MembershipCacheKey;
import org.exoplatform.services.organization.cache.OrganizationCacheHandler;
import org.exoplatform.services.organization.idm.MembershipDAOImpl;
import org.exoplatform.services.organization.idm.MembershipImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;

public class CacheableMembershipHandlerImpl extends MembershipDAOImpl {

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

  private boolean                                    useCacheList;

  @SuppressWarnings("unchecked")
  public CacheableMembershipHandlerImpl(OrganizationCacheHandler organizationCacheHandler,
                                        PicketLinkIDMOrganizationServiceImpl orgService,
                                        PicketLinkIDMService service,
                                        boolean useCacheList) {
    super(orgService, service);
    this.membershipCache = organizationCacheHandler.getMembershipCache();
    this.useCacheList = useCacheList;
  }

  /**
   * {@inheritDoc}
   */
  public Membership findMembership(String id) throws Exception {
    Membership membership = null;
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      membership = (Membership) membershipCache.get(new MembershipCacheKey(new MembershipImpl(id)));
    }
    if (membership == null) {
      membership = super.findMembership(id);

      if (membership != null) {
        cacheMembership(membership);
      }
    }
    return membership == null ? null : (Membership) ((ExtendedCloneable) membership).clone();
  }

  /**
   * {@inheritDoc}
   */
  public Membership findMembershipByUserGroupAndType(String userName, String groupId, String type) throws Exception {
    Membership membership = null;
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      membership = (Membership) membershipCache.get(new MembershipCacheKey(userName, groupId, type));
    }

    if (membership == null) {
      membership = super.findMembershipByUserGroupAndType(userName, groupId, type);

      if (membership != null) {
        cacheMembership(membership);
      }
    }

    return membership == null ? null : (Membership) ((ExtendedCloneable) membership).clone();
  }

  /**
   * {@inheritDoc}
   */
  public Collection<Membership> findMembershipsByGroup(Group group) throws Exception {
    @SuppressWarnings("unchecked")
    Collection<Membership> memberships = super.findMembershipsByGroup(group);
    for (Membership membership : memberships) {
      cacheMembership(membership);
    }

    return memberships;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public Collection<Membership> findMembershipsByUser(String userName) throws Exception {
    Collection<Membership> memberships = null;
    MembershipCacheKey cacheKey = new MembershipCacheKey(userName, null, null);
    if (useCacheList && (disableCacheInThread.get() == null || !disableCacheInThread.get())) {
      memberships = (Collection<Membership>) membershipCache.get(cacheKey);
    }
    if (memberships == null) {
      memberships = super.findMembershipsByUser(userName);
      if (useCacheList) {
        membershipCache.put(cacheKey, memberships);
      }
      for (Membership membership : memberships) {
        cacheMembership(membership);
      }
    }
    return memberships;
  }

  /**
   * {@inheritDoc}
   */
  public Collection<Membership> findMembershipsByUserAndGroup(String userName, String groupId) throws Exception {
    @SuppressWarnings("unchecked")
    Collection<Membership> memberships = super.findMembershipsByUserAndGroup(userName, groupId);
    for (Membership membership : memberships) {
      cacheMembership(membership);
    }
    return memberships;
  }

  /**
   * {@inheritDoc}
   */
  public Membership removeMembership(String id, boolean broadcast) throws Exception {
    Membership membership = null;
    disableCacheInThread.set(true);
    try {
      membership = super.removeMembership(id, broadcast);
      if (membership != null) {
        membershipCache.remove(new MembershipCacheKey(membership));
        if (useCacheList) {
          membershipCache.remove(new MembershipCacheKey(membership.getUserName(), null, null));
        }
      }
    } finally {
      disableCacheInThread.set(false);
    }
    return membership;
  }

  @Override
  public void saveMembership(Membership m, boolean broadcast) throws Exception {
    super.saveMembership(m, broadcast);
    if (useCacheList) {
      membershipCache.remove(new MembershipCacheKey(m.getUserName(), null, null));
    }
  }

  @Override
  public void createMembership(Membership m, boolean broadcast) throws Exception {
    super.createMembership(m, broadcast);
    if (useCacheList) {
      membershipCache.remove(new MembershipCacheKey(m.getUserName(), null, null));
    }
  }

  @Override
  public void linkMembership(User user, Group g, MembershipType mt, boolean broadcast) throws Exception {
    super.linkMembership(user, g, mt, broadcast);
    if (useCacheList) {
      membershipCache.remove(new MembershipCacheKey(user.getUserName(), null, null));
    }
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public Collection<Membership> removeMembershipByUser(String username, boolean broadcast) throws Exception {
    Collection<Membership> memberships = null;
    disableCacheInThread.set(true);
    try {
      memberships = super.removeMembershipByUser(username, broadcast);
      for (Membership membership : memberships) {
        membershipCache.remove(new MembershipCacheKey(membership));
        if (useCacheList) {
          membershipCache.remove(new MembershipCacheKey(membership.getUserName(), null, null));
        }
      }
    } finally {
      disableCacheInThread.set(false);
    }
    return memberships;
  }

  public void clearCache() {
    membershipCache.clearCache();
  }

  private void cacheMembership(Membership membership) {
    membership = (Membership) ((ExtendedCloneable) membership).clone();
    membershipCache.put(new MembershipCacheKey(membership), membership);
  }
}
