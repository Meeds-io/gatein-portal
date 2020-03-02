/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.organization.webui.component;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * Created by The eXo Platform SAS Author : Huu-Dung Kieu kieuhdung@gmail.com 22 dec. 08
 */
public class GroupManagement {

    /** . */
    private static final Logger log = LoggerFactory.getLogger(GroupManagement.class);

    public static OrganizationService getOrganizationService() {
        return ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationService.class);
    }

    public static UserACL getUserACL() {
        return ExoContainerContext.getService(UserACL.class);
    }

    public static boolean isMembershipOfGroup(String username, String membership, String groupId) throws Exception {
        if (username == null)
            username = getCurrentUsername();
        OrganizationService orgService = getOrganizationService();
        return orgService.getMembershipHandler().findMembershipByUserGroupAndType(username, groupId, membership) != null;
    }

    public static boolean isManagerOfGroup(String username, String groupId) throws Exception {
        return isMembershipOfGroup(username, getUserACL().getAdminMSType(), groupId);
    }

    public static boolean isMemberOfGroup(String username, String groupId) throws Exception {
        if (username == null)
            username = getCurrentUsername();
        OrganizationService orgService = getOrganizationService();
        Collection membership = orgService.getMembershipHandler().findMembershipsByUserAndGroup(username, groupId);
        return membership != null && !membership.isEmpty();
    }

    @Deprecated
    public static boolean isRelatedOfGroup(String username, String groupId) throws Exception {
        if (username == null)
            username = getCurrentUsername();
        OrganizationService orgService = getOrganizationService();
        Collection<Group> groups = orgService.getGroupHandler().findGroupsOfUser(username);
        return groups.stream().anyMatch(g -> g != null && StringUtils.startsWith(g.getId(), groupId));
    }

    public static Collection getRelatedGroups(String username, Collection groups) throws Exception {
        if (username == null)
            username = getCurrentUsername();
        OrganizationService orgService = getOrganizationService();
        Collection userGroups = orgService.getGroupHandler().findGroupsOfUser(username);
        return (Collection) groups.stream().filter(group -> isRelatedGroup((Group) group, userGroups)).collect(Collectors.toList());
    }

    private static boolean isRelatedGroup(Group group, Collection groups) {
        String groupId = group.getId();
        return groups.stream().anyMatch(g -> ((Group) g).getId().startsWith(groupId));
    }

    public static boolean isAdministrator(String username) throws Exception {
        if (username == null)
            username = getCurrentUsername();
        // if getRemoteUser() returns null, then there isn't a logged in user, which means they are not an admin
        return username != null && ( username.equals(getUserACL().getSuperUser()) || isMemberOfGroup(username, getUserACL().getAdminGroups()));
    }


    public static boolean isSuperUserOfGroup(String username, String groupId) {
        try {
            return (GroupManagement.isManagerOfGroup(username, groupId) || (GroupManagement.isAdministrator(username)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
    /**
     * Returns the username of the current user
     * @return String username
     */
    public static String getCurrentUsername() {
        org.exoplatform.services.security.Identity currentIdentity = ConversationState.getCurrent() == null ?
                null : ConversationState.getCurrent().getIdentity();
        if(currentIdentity == null) {
            return null;
        }

        return currentIdentity.getUserId();
    }
}
