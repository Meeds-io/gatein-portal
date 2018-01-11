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
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;
import org.exoplatform.services.organization.ExtendedCloneable;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.cache.MembershipCacheKey;
import org.exoplatform.services.organization.cache.OrganizationCacheHandler;
import org.exoplatform.services.organization.idm.ExtGroup;
import org.exoplatform.services.organization.idm.GroupDAOImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMOrganizationServiceImpl;
import org.exoplatform.services.organization.idm.PicketLinkIDMService;
import org.exoplatform.services.organization.impl.GroupImpl;

public class CacheableGroupHandlerImpl extends GroupDAOImpl {

  private final ExoCache<String, Object>             groupCache;

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
  public CacheableGroupHandlerImpl(OrganizationCacheHandler organizationCacheHandler,
                                   PicketLinkIDMOrganizationServiceImpl orgService,
                                   PicketLinkIDMService service,
                                   boolean useCacheList) {
    super(orgService, service);
    this.groupCache = organizationCacheHandler.getGroupCache();
    this.membershipCache = organizationCacheHandler.getMembershipCache();
    this.useCacheList = useCacheList;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addChild(Group parent, Group child, boolean broadcast) throws Exception {
    super.addChild(parent, child, broadcast);
    if (useCacheList && parent != null) {
      groupCache.remove(computeChildrenKey(parent));
    }
    cacheGroup(child);
  }

  /**
   * {@inheritDoc}
   */
  public Group findGroupById(String groupId) throws Exception {
    Group group = null;
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      group = (Group) groupCache.get(groupId);
    }
    if (group == null) {
      group = super.findGroupById(groupId);
      if (group != null) {
        cacheGroup(group);
      }
    }
    return group == null ? null : (Group) ((ExtendedCloneable) group).clone();
  }

  /**
   * {@inheritDoc}
   */
  public Collection<Group> findGroupByMembership(String userName, String membershipType) throws Exception {
    Collection<Group> groups = super.findGroupByMembership(userName, membershipType);

    for (Group group : groups) {
      cacheGroup(group);
    }

    return groups;
  }

  /**
   * {@inheritDoc}
   */
  public Collection<Group> resolveGroupByMembership(String userName, String membershipType) throws Exception {
    Collection<Group> groups = super.resolveGroupByMembership(userName, membershipType);

    for (Group group : groups) {
      cacheGroup(group);
    }

    return groups;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public Collection<Group> findGroups(Group parent) throws Exception {
    Collection<Group> groups = null;
    if (useCacheList && (disableCacheInThread.get() == null || !disableCacheInThread.get())) {
      String childrenCacheKey = computeChildrenKey(parent);
      groups = (Collection<Group>) groupCache.get(childrenCacheKey);
    }
    if (groups == null) {
      groups = super.findGroups(parent);
      for (Group group : groups) {
        cacheGroup(group);
      }
    }
    return groups;
  }

  /**
   * {@inheritDoc}
   */
  public Collection<Group> findGroupsOfUser(String user) throws Exception {
    Collection<Group> groups = super.findGroupsOfUser(user);
    for (Group group : groups) {
      cacheGroup(group);
    }

    return groups;
  }

  /**
   * {@inheritDoc}
   */
  public Collection<Group> getAllGroups() throws Exception {
    Collection<Group> groups = super.getAllGroups();
    for (Group group : groups) {
      cacheGroup(group);
    }
    return groups;
  }

  /**
   * {@inheritDoc}
   */
  public Group removeGroup(Group group, boolean broadcast) throws Exception {
    Group gr = null;
    disableCacheInThread.set(true);
    try {
      gr = super.removeGroup(group, broadcast);

      String groupId = getGroupId(group);

      // Delete related cache entries
      groupCache.select(new ClearGroupCacheByGroupIdSelector(groupId, group.getParentId(), useCacheList));
      membershipCache.select(new ClearMembershipCacheByGroupIdSelector(groupId));
    } finally {
      disableCacheInThread.set(false);
    }
    return gr;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getGroupId(org.picketlink.idm.api.Group jbidGroup,
                              List<org.picketlink.idm.api.Group> processed) throws Exception {
    String cacheKey = String.valueOf(jbidGroup.hashCode());

    String groupId = null;
    if (disableCacheInThread.get() == null || !disableCacheInThread.get()) {
      groupId = (String) groupCache.get(cacheKey);
    }
    if (groupId == null) {
      groupId = super.getGroupId(jbidGroup, processed);
      if (groupId != null) {
        groupCache.put(cacheKey, groupId);
      }
    }
    return groupId;
  }

  /**
   * {@inheritDoc}
   */
  public void saveGroup(Group group, boolean broadcast) throws Exception {
    super.saveGroup(group, broadcast);
    cacheGroup(group);
  }

  public void clearCache() {
    groupCache.clearCache();
  }

  private void cacheGroup(Group group) {
    String groupId = group.getId();
    if (StringUtils.isBlank(groupId)) {
      groupId = getGroupId(group);
      if (group instanceof ExtGroup) {
        ((ExtGroup) group).setId(groupId);
      } else if (group instanceof GroupImpl) {
        ((GroupImpl) group).setId(groupId);
      }
    }
    groupCache.put(groupId, (Group) ((ExtendedCloneable) group).clone());
  }

  private static final String computeChildrenKey(Group parent) {
    return computeChildrenKey(getGroupId(parent));
  }

  private static final String computeChildrenKey(String parentId) {
    return "children_" + parentId;
  }

  private static final String getGroupId(Group group) {
    if (group == null) {
      return null;
    }
    if (StringUtils.isNotBlank(group.getId())) {
      return group.getId();
    }
    return (StringUtils.isBlank(group.getParentId()) ? "" : group.getParentId()) + "/" + group.getGroupName();
  }

  public static final class ClearGroupCacheByGroupIdSelector implements CachedObjectSelector<String, Object> {
    private String groupId;

    private String childrenKey;

    private String parentCachedChildrenKey;

    public ClearGroupCacheByGroupIdSelector(String groupId, String parentId, boolean clearCachedChildrenList) {
      this.groupId = groupId;
      if (clearCachedChildrenList) {
        this.childrenKey = computeChildrenKey(groupId);
        if (StringUtils.isNotBlank(parentId)) {
          this.parentCachedChildrenKey = computeChildrenKey(parentId);
        }
      }
    }

    @Override
    public void onSelect(ExoCache<? extends String, ? extends Object> cache,
                         String key,
                         ObjectCacheInfo<? extends Object> ocinfo) throws Exception {
      cache.remove(key);
    }

    @Override
    public boolean select(String key, ObjectCacheInfo<? extends Object> ocinfo) {
      if (key.equals(groupId) || key.startsWith(groupId + "/")
          || (StringUtils.isNotBlank(childrenKey) && (key.equals(childrenKey) || key.startsWith(childrenKey + "/")))
          || (StringUtils.isNotBlank(parentCachedChildrenKey) && (key.equals(parentCachedChildrenKey)))) {
        return true;
      }
      return false;
    }
  }

  public static final class ClearMembershipCacheByGroupIdSelector implements CachedObjectSelector<MembershipCacheKey, Object> {
    private String groupId;

    public ClearMembershipCacheByGroupIdSelector(String groupId) {
      this.groupId = groupId;
    }

    @Override
    public void onSelect(ExoCache<? extends MembershipCacheKey, ? extends Object> cache,
                         MembershipCacheKey key,
                         ObjectCacheInfo<? extends Object> ocinfo) throws Exception {
      cache.remove(key);
    }

    @Override
    public boolean select(MembershipCacheKey key, ObjectCacheInfo<? extends Object> ocinfo) {
      Object obj = ocinfo.get();
      if (obj instanceof Membership) {
        Membership cachedMembership = (Membership) ocinfo.get();
        if (cachedMembership.getGroupId().equals(groupId)) {
          return true;
        }
      } else if (obj instanceof Collection) {
        // Delete all cached user's memberships when deleting a group
        return true;
      }
      return false;
    }
  }
}
