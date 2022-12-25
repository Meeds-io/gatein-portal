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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.idm.EntityMapperUtils;
import org.exoplatform.services.organization.idm.UserImpl;

public class InMemoryUserHandler implements UserHandler {

  private static final Log        LOG                              = ExoLogger.getLogger(InMemoryUserHandler.class);

  private static final String     ERROR_BROADCASTING_EVENT_MESSAGE = "Error broadcasting event : {}";

  public static final String      USER_PASSWORD                    = EntityMapperUtils.USER_PASSWORD;

  public static final String      USER_PASSWORD_SALT               = "passwordSalt";

  public static final String      USER_FIRST_NAME                  = EntityMapperUtils.USER_FIRST_NAME;

  public static final String      USER_LAST_NAME                   = EntityMapperUtils.USER_LAST_NAME;

  public static final String      USER_DISPLAY_NAME                = EntityMapperUtils.USER_DISPLAY_NAME;

  public static final String      USER_EMAIL                       = EntityMapperUtils.USER_EMAIL;

  public static final String      USER_CREATED_DATE                = EntityMapperUtils.USER_CREATED_DATE;

  public static final String      USER_LAST_LOGIN_TIME             = EntityMapperUtils.USER_LAST_LOGIN_TIME;

  public static final String      USER_ORGANIZATION_ID             = EntityMapperUtils.USER_ORGANIZATION_ID;

  public static final String      USER_ENABLED                     = EntityMapperUtils.USER_ENABLED;

  public static final DateFormat  dateFormat                       = DateFormat.getInstance();

  private OrganizationService     organizationService;

  private List<UserEventListener> userListeners                    = new ArrayList<>();

  private Map<String, User>       usersById                        = new HashMap<>();

  public InMemoryUserHandler(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  public final List<UserEventListener> getUserEventListeners() {
    return Collections.unmodifiableList(userListeners);
  }

  @Override
  public void addUserEventListener(UserEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    userListeners.add(listener);
  }

  @Override
  public void removeUserEventListener(UserEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    userListeners.remove(listener);
  }

  @Override
  public User createUserInstance() {
    return new UserImpl();
  }

  @Override
  public User createUserInstance(String username) {
    return new UserImpl(username);
  }

  @Override
  public void createUser(User user, boolean broadcast) {
    if (usersById.containsKey(user.getUserName())) {
      return;
    }
    saveUser(user, broadcast);
  }

  @Override
  public void saveUser(User user, boolean broadcast) {
    boolean isNew = usersById.containsKey(user.getUserName());
    if (broadcast) {
      preSave(user, isNew);
    }
    usersById.put(user.getUserName(), user);
    if (broadcast) {
      postSave(user, isNew);
    }
  }

  @Override
  public User setEnabled(String userName, boolean enabled, boolean broadcast) {
    if (!usersById.containsKey(userName)) {
      return null;
    }
    User user = usersById.get(userName);
    if (user.isEnabled() == enabled) {
      return user;
    }
    if (broadcast) {
      preSetEnabled(user);
    }

    ((UserImpl) user).setEnabled(enabled);

    if (broadcast) {
      postSetEnabled(user);
    }
    return user;
  }

  @Override
  public User removeUser(String userName, boolean broadcast) {
    User user = usersById.get(userName);
    if (broadcast) {
      preDelete(user);
    }

    // Remove all memberships and profile first
    getMembershipHandler().removeMembershipByUser(userName, false);
    getUserProfileHandler().removeUserProfile(userName, false);

    usersById.remove(userName);

    if (broadcast) {
      postDelete(user);
    }
    return user;
  }

  @Override
  public User findUserByName(String userName) {
    return findUserByName(userName, UserStatus.ENABLED);
  }

  @Override
  public User findUserByName(String userName, UserStatus userStatus) {
    if (!usersById.containsKey(userName)) {
      return null;
    }
    User user = usersById.get(userName);
    return filterUserStatus(user, userStatus);
  }

  @Override
  public LazyPageList<User> getUserPageList(int pageSize) {
    return new LazyPageList<>(new InMemoryListAccess<>(new ArrayList<>(usersById.values())),
                              pageSize);
  }

  public ListAccess<User> findAllUsers() {
    return findAllUsers(UserStatus.ENABLED);
  }

  @Override
  public ListAccess<User> findAllUsers(UserStatus userStatus) {
    List<User> users = usersById.values()
                                .stream()
                                .map(user -> filterUserStatus(user, userStatus))
                                .filter(Objects::nonNull)
                                .toList();
    return new InMemoryListAccess<>(users);
  }

  @Override
  public boolean authenticate(String username, String password) {
    return usersById.containsKey(username) && StringUtils.equals(usersById.get(username).getPassword(), password);
  }

  @Override
  public LazyPageList<User> findUsers(Query q) {
    return new LazyPageList<>(findUsersByQuery(q), 20);
  }

  @Override
  public InMemoryListAccess<User> findUsersByQuery(Query q) {
    return findUsersByQuery(q, UserStatus.ENABLED);
  }

  @Override
  public InMemoryListAccess<User> findUsersByQuery(Query q, UserStatus userStatus) {
    List<User> users = usersById.values()
                                .stream()
                                .map(user -> filterUserStatus(user, userStatus))
                                .filter(user -> contains(user.getEmail(), q.getEmail())
                                    && contains(user.getFirstName(), q.getFirstName())
                                    && contains(user.getLastName(), q.getLastName())
                                    && contains(user.getUserName(), q.getUserName()))
                                .filter(Objects::nonNull)
                                .toList();
    return new InMemoryListAccess<>(users);
  }

  public LazyPageList<User> findUsersByGroup(String groupId) {
    InMemoryListAccess<User> users = findUsersByGroupId(groupId);
    return new LazyPageList<>(users, users.getSize());
  }

  public InMemoryListAccess<User> findUsersByGroupId(String groupId) {
    return findUsersByGroupId(groupId, UserStatus.ENABLED);
  }

  @Override
  public InMemoryListAccess<User> findUsersByGroupId(String groupId, UserStatus userStatus) {
    List<User> users = getMembershipHandler().findMembershipsByGroupId(groupId)
                                             .stream()
                                             .map(membership -> usersById.get(membership.getUserName()))
                                             .map(user -> filterUserStatus(user, userStatus))
                                             .filter(Objects::nonNull)
                                             .toList();
    return new InMemoryListAccess<>(users);
  }

  @Override
  public InMemoryListAccess<User> findUsersByQuery(Query query, List<String> groupIds, UserStatus userStatus) {
    List<User> users = usersById.values()
                                .stream()
                                .map(user -> filterUserStatus(user, userStatus))
                                .filter(user -> CollectionUtils.isEmpty(groupIds)
                                    || groupIds.stream()
                                               .anyMatch(groupId -> !getMembershipHandler().findMembershipsByUserAndGroup(user.getUserName(),
                                                                                                                          groupId)
                                                                                           .isEmpty()))
                                .filter(Objects::nonNull)
                                .toList();
    return new InMemoryListAccess<>(users);
  }

  @Override
  public boolean isUpdateLastLoginTime() {
    return true;
  }

  private User filterUserStatus(User user, UserStatus userStatus) {
    if (user == null || userStatus == null) {
      return user;
    }
    switch (userStatus) {
    case ANY: {
      return user;
    }
    case ENABLED: {
      return user.isEnabled() ? user : null;
    }
    case DISABLED: {
      return user.isEnabled() ? null : user;
    }
    default:
      return null;
    }
  }

  private void preSave(User user, boolean isNew) {
    for (UserEventListener listener : userListeners) {
      try {
        listener.preSave(user, isNew);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
    }
  }

  private void postSave(User user, boolean isNew) {
    for (UserEventListener listener : userListeners) {
      try {
        listener.postSave(user, isNew);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
    }
  }

  private void preDelete(User user) {
    for (UserEventListener listener : userListeners) {
      try {
        listener.preDelete(user);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
    }
  }

  private void postDelete(User user) {
    for (UserEventListener listener : userListeners) {
      try {
        listener.postDelete(user);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
    }
  }

  private void preSetEnabled(User user) {
    for (UserEventListener listener : userListeners)
      try {
        listener.preSetEnabled(user);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
  }

  private void postSetEnabled(User user) {
    for (UserEventListener listener : userListeners)
      try {
        listener.postSetEnabled(user);
      } catch (Exception e) {
        LOG.warn(ERROR_BROADCASTING_EVENT_MESSAGE, listener.getClass(), e);
      }
  }

  private InMemoryUserProfileHandler getUserProfileHandler() {
    return (InMemoryUserProfileHandler) organizationService.getUserProfileHandler();
  }

  private InMemoryMembershipHandler getMembershipHandler() {
    return (InMemoryMembershipHandler) organizationService.getMembershipHandler();
  }

  private boolean contains(String value, String queryString) {
    return StringUtils.isBlank(queryString) || StringUtils.contains(value, queryString);
  }

}
