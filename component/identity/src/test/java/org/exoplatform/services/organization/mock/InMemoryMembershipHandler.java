/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.services.organization.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.idm.MembershipImpl;

public class InMemoryMembershipHandler implements MembershipHandler {

  private static final Log              LOG                              = ExoLogger.getLogger(InMemoryMembershipHandler.class);

  private static final String           ERROR_BROADCASTING_EVENT_MESSAGE = "Error broadcasting event : {}";

  private List<MembershipEventListener> membershiptListeners             = new ArrayList<>();

  private Map<String, Membership>       membershipsById                  = new HashMap<>();

  private Map<String, List<Membership>> userMemberships                  = new HashMap<>();

  private Map<String, List<Membership>> groupMemberships                 = new HashMap<>();

  private Map<String, List<Membership>> membershipTypeMemberships        = new HashMap<>();

  @Override
  public void addMembershipEventListener(MembershipEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    membershiptListeners.add(listener);
  }

  @Override
  public void removeMembershipEventListener(MembershipEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    membershiptListeners.remove(listener);
  }

  @Override
  public final Membership createMembershipInstance() {
    return new MembershipImpl();
  }

  @Override
  public void createMembership(Membership membership, boolean broadcast) {
    saveMembership(membership, broadcast);
  }

  @Override
  public void linkMembership(User user, Group group, MembershipType membershipType, boolean broadcast) {
    createMembership(new MembershipImpl(membershipType.getName() + ":" + user.getUserName() + ":" + group.getId()), broadcast);
  }

  @Override
  public Membership removeMembership(String membershipId, boolean broadcast) {
    if (!membershipsById.containsKey(membershipId)) {
      return null;
    }
    return removeMembership(membershipsById.get(membershipId), broadcast);
  }

  @Override
  public List<Membership> removeMembershipByUser(String userName, boolean broadcast) {
    List<Membership> memberships = userMemberships.compute(userName,
                                                           (key,
                                                            existingMemberships) -> existingMemberships == null ? Collections.emptyList()
                                                                                                                : new ArrayList<>(existingMemberships));
    memberships.forEach(membership -> removeMembership(membership.getId(), broadcast));
    return memberships;
  }

  public List<Membership> removeMembershipByGroup(String groupId, boolean broadcast) {
    List<Membership> memberships = groupMemberships.compute(groupId,
                                                            (key,
                                                             existingMemberships) -> existingMemberships == null ? Collections.emptyList()
                                                                                                                 : new ArrayList<>(existingMemberships));
    memberships.forEach(membership -> removeMembership(membership.getId(), broadcast));
    return memberships;
  }

  public List<Membership> removeMembershipByMembershipType(String membershipType, boolean broadcast) {
    List<Membership> memberships = membershipTypeMemberships.compute(membershipType,
                                                                     (key,
                                                                      existingMemberships) -> existingMemberships == null ? Collections.emptyList()
                                                                                                                          : new ArrayList<>(existingMemberships));
    memberships.forEach(membership -> removeMembership(membership.getId(), broadcast));
    return memberships;
  }

  @Override
  public Membership findMembership(String id) {
    return membershipsById.get(id);
  }

  @Override
  public Membership findMembershipByUserGroupAndType(String userName, String groupId, String type) {
    return membershipsById.values()
                          .stream()
                          .filter(membership -> StringUtils.equals(userName, membership.getUserName())
                              && StringUtils.equals(groupId, membership.getGroupId())
                              && StringUtils.equals(type, membership.getMembershipType()))
                          .findAny()
                          .orElse(null);
  }

  @Override
  public List<Membership> findMembershipsByUserAndGroup(String userName, String groupId) {
    return membershipsById.values()
                          .stream()
                          .filter(membership -> StringUtils.equals(userName, membership.getUserName())
                              && StringUtils.equals(groupId, membership.getGroupId()))
                          .toList();
  }

  @Override
  public List<Membership> findMembershipsByUser(String userName) {
    return userMemberships.computeIfAbsent(userName, key -> Collections.emptyList());
  }

  @Override
  public ListAccess<Membership> findAllMembershipsByUser(User user) {
    return new InMemoryListAccess<>(findMembershipsByUser(user.getUserName()));
  }

  @Override
  public List<Membership> findMembershipsByGroup(Group group) {
    return groupMemberships.computeIfAbsent(group.getId(), key -> Collections.emptyList());
  }

  public List<Membership> findMembershipsByGroupId(String groupId) {
    return groupMemberships.computeIfAbsent(groupId, key -> Collections.emptyList());
  }

  @Override
  public ListAccess<Membership> findAllMembershipsByGroup(Group group) {
    return new InMemoryListAccess<>(findMembershipsByGroup(group));
  }

  private void saveMembership(Membership membership, boolean broadcast) {
    if (membershipsById.containsKey(membership.getId())) {
      return;
    }
    if (broadcast) {
      preSave(membership);
    }
    membershipsById.put(membership.getId(), membership);
    userMemberships.computeIfAbsent(membership.getUserName(), key -> new ArrayList<Membership>()).add(membership);
    groupMemberships.computeIfAbsent(membership.getGroupId(), key -> new ArrayList<Membership>()).add(membership);
    membershipTypeMemberships.computeIfAbsent(membership.getMembershipType(), key -> new ArrayList<Membership>()).add(membership);
    if (broadcast) {
      postSave(membership);
    }
  }

  private Membership removeMembership(Membership membership, boolean broadcast) {
    if (broadcast) {
      preDelete(membership);
    }
    userMemberships.computeIfPresent(membership.getUserName(), (key, memberships) -> {
      memberships.removeIf(existingMembership -> StringUtils.equals(existingMembership.getId(), membership.getId()));
      return memberships;
    });
    groupMemberships.computeIfPresent(membership.getGroupId(), (key, memberships) -> {
      memberships.removeIf(existingMembership -> StringUtils.equals(existingMembership.getId(), membership.getId()));
      return memberships;
    });
    membershipTypeMemberships.computeIfPresent(membership.getMembershipType(), (key, memberships) -> {
      memberships.removeIf(existingMembership -> StringUtils.equals(existingMembership.getId(), membership.getId()));
      return memberships;
    });
    membershipsById.remove(membership.getId());
    if (broadcast) {
      postDelete(membership);
    }
    return membership;
  }

  private void preSave(Membership membership) {
    for (MembershipEventListener listener : membershiptListeners) {
      try {
        listener.preSave(membership, true);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
    }
  }

  private void postSave(Membership membership) {
    for (MembershipEventListener listener : membershiptListeners) {
      try {
        listener.postSave(membership, true);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
    }
  }

  private void preDelete(Membership membership) {
    for (MembershipEventListener listener : membershiptListeners) {
      try {
        listener.preDelete(membership);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
    }
  }

  private void postDelete(Membership membership) {
    for (MembershipEventListener listener : membershiptListeners) {
      try {
        listener.postDelete(membership);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
    }
  }

}
