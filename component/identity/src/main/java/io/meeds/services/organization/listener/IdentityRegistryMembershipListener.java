/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.services.organization.listener;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;

public class IdentityRegistryMembershipListener extends MembershipEventListener {

  private IdentityRegistry identityRegistry;

  public IdentityRegistryMembershipListener(IdentityRegistry identityRegistry) {
    this.identityRegistry = identityRegistry;
  }

  @Override
  public void postDelete(Membership m) throws Exception {
    Identity identity = identityRegistry.getIdentity(m.getUserName());
    if (identity != null) {
      String groupId = m.getGroupId();
      String membershipType = m.getMembershipType();
      identity.getMemberships()
              .removeIf(membership -> StringUtils.equals(membership.getGroup(), groupId)
                                      || StringUtils.equals(membership.getMembershipType(), membershipType));
    }
  }

  @Override
  public void postSave(Membership membership, boolean isNew) throws Exception {
    Identity identity = identityRegistry.getIdentity(membership.getUserName());
    MembershipEntry membershipEntry = new MembershipEntry(membership.getGroupId(), membership.getMembershipType());
    if (identity != null
        && identity.getMemberships().stream().noneMatch(m -> m.equals(membershipEntry))) {
      identity.getMemberships().add(membershipEntry);
    }
  }

}
