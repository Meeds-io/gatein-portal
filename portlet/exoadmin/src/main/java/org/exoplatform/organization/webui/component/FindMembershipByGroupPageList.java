/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.organization.webui.component;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PageListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.OrganizationService;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class FindMembershipByGroupPageList extends PageListAccess<Membership, String> {

    public FindMembershipByGroupPageList(String groupId, int pageSize) {
        super(groupId, pageSize);
    }

    @Override
    protected ListAccess<Membership> create(String state) throws Exception {
        ExoContainer container = PortalContainer.getInstance();
        OrganizationService service = (OrganizationService) container.getComponentInstance(OrganizationService.class);
        MembershipHandler handler = service.getMembershipHandler();
        Group group = service.getGroupHandler().findGroupById(state);
        return handler.findAllMembershipsByGroup(group);
    }
}
