package org.exoplatform.services.organization.idm;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

import org.picketlink.idm.api.Attribute;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;

public class EntityMapperUtils {

  private static final Log       LOG                  = ExoLogger.getLogger(EntityMapperUtils.class);

  public static final String     USER_PASSWORD        = "password";

  public static final String     USER_FIRST_NAME      = "firstName";

  public static final String     USER_LAST_NAME       = "lastName";

  public static final String     USER_DISPLAY_NAME    = "displayName";

  public static final String     USER_EMAIL           = "email";

  public static final String     USER_CREATED_DATE    = "createdDate";

  public static final String     USER_LAST_LOGIN_TIME = "lastLoginTime";

  public static final String     USER_ORGANIZATION_ID = "organizationId";

  public static final String     USER_ENABLED         = "enabled";

  public static final String     ORIGINATING_STORE    = "store";

  public static final DateFormat dateFormat           = DateFormat.getInstance();

  public static void populateUser(User user, Map<String, Attribute> attrs) {
    populateUser(user, attrs, false);
  }

  @SuppressWarnings("deprecation")
  public static boolean populateUser(User user, Map<String, Attribute> attrs, boolean checkIfChanged) {
    if (user == null) {
      throw new IllegalArgumentException("User entity is mandatory");
    }
    if (attrs == null) {
      throw new IllegalStateException("Attributes map is mandatory");
    }
    boolean changed = false;
    if (attrs.containsKey(USER_CREATED_DATE)) {
      Date origCreatedDate = user.getCreatedDate();
      Date createdDate = null;
      try {
        long date = Long.parseLong(attrs.get(USER_CREATED_DATE).getValue().toString());
        createdDate = new Date(date);
        user.setCreatedDate(createdDate);
      } catch (NumberFormatException e) {
        // For backward compatibility with GateIn 3.0 and EPP 5 Beta
        try {
          createdDate = dateFormat.parse(attrs.get(USER_CREATED_DATE).getValue().toString());
          user.setCreatedDate(createdDate);
        } catch (ParseException e2) {
          LOG.error("Cannot parse the creation date for: " + user.getUserName());
        }
      }
      changed |= checkIfChanged && !Objects.equals(createdDate, origCreatedDate);
    }
    if (attrs.containsKey(USER_EMAIL)) {
      String email = attrs.get(USER_EMAIL).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(email, user.getEmail());
      user.setEmail(email);
    }
    if (attrs.containsKey(USER_FIRST_NAME)) {
      String firstName = attrs.get(USER_FIRST_NAME).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(firstName, user.getFirstName());
      user.setFirstName(firstName);
    }
    if (attrs.containsKey(USER_LAST_LOGIN_TIME)) {
      Date originalLastLoginDate = user.getLastLoginTime();
      Date lastLoginDate = null;
      try {
        Long lastLoginMillis = null;
        Attribute lastLoginAttr = attrs.get(USER_LAST_LOGIN_TIME);
        if (lastLoginAttr != null) {
          Object lastLoginValue = lastLoginAttr.getValue();
          if (lastLoginValue != null) {
            lastLoginMillis = Long.parseLong(lastLoginValue.toString());
          }
        }
        if (lastLoginMillis != null) {
          lastLoginDate = new Date(lastLoginMillis);
          user.setLastLoginTime(lastLoginDate);
        }
      } catch (NumberFormatException e) {
        // For backward compatibility with GateIn 3.0 and EPP 5 Beta
        try {
          lastLoginDate = dateFormat.parse(attrs.get(USER_LAST_LOGIN_TIME).getValue().toString());
          user.setLastLoginTime(lastLoginDate);
        } catch (ParseException e2) {
          LOG.error("Cannot parse the last login date for: " + user.getUserName());
        }
      }
      changed |= checkIfChanged && !Objects.equals(originalLastLoginDate, lastLoginDate);
    }
    if (attrs.containsKey(USER_LAST_NAME)) {
      String lastName = attrs.get(USER_LAST_NAME).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(lastName, user.getLastName());
      user.setLastName(lastName);
    }
    if (attrs.containsKey(USER_DISPLAY_NAME)) {
      // TODO: GTNPORTAL-2358 Change once displayName will be available as
      // part of Organization API
      String fullName = attrs.get(USER_DISPLAY_NAME).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(fullName, user.getDisplayName());
      user.setDisplayName(fullName);
    }
    if (attrs.containsKey(USER_ORGANIZATION_ID)) {
      String organizationId = attrs.get(USER_ORGANIZATION_ID).getValue().toString();
      changed |= checkIfChanged && !Objects.equals(organizationId, user.getOrganizationId());
      user.setOrganizationId(organizationId);
    }
    if (attrs.containsKey(USER_ENABLED)) {
      // used when populating User from AD ; it returns numbers : 512 = enbaled, 514 = disabled
      String status = attrs.get(USER_ENABLED).getValue().toString();

      // used when populating User from the platform ; it return true or false
      // if it is from AD it always returns false since it is a number .
      Boolean enabled = Boolean.parseBoolean(attrs.get(USER_ENABLED).getValue().toString()) ;

      if (status.equals("512")  )
      {
        enabled = true;
      }
      changed |= checkIfChanged && !Objects.equals(enabled, user.isEnabled());
      ((UserImpl) user).setEnabled(enabled);
    }
    if (user instanceof UserImpl && attrs.containsKey(ORIGINATING_STORE)) {
      UserImpl userImpl = (UserImpl) user;
      userImpl.setOriginatingStore(attrs.get(ORIGINATING_STORE).getValue().toString());
    }
    return changed;
  }

}
