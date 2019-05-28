package org.exoplatform.portal.config;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.security.Identity;

/**
 * Plugin user by UserACL to check if the given user has permissions to see the
 * given group
 */
public abstract class GroupVisibilityPlugin extends BaseComponentPlugin {
  public abstract boolean hasPermission(Identity userIdentity, Group group);
}
