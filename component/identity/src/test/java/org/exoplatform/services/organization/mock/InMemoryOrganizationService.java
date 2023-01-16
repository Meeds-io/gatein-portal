/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * 
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

import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.services.organization.BaseOrganizationService;

public class InMemoryOrganizationService extends BaseOrganizationService implements Startable, ComponentRequestLifecycle {

  public InMemoryOrganizationService() {
    groupDAO_ = new InMemoryGroupHandler(this);
    userDAO_ = new InMemoryUserHandler(this);
    membershipTypeDAO_ = new InMemoryMembershipTypeHandler(this);
    membershipDAO_ = new InMemoryMembershipHandler();
    userProfileDAO_ = new InMemoryUserProfileHandler();
  }

  public void flush() {
    // Nothing to do
  }

  public void clearCaches() {
    // Nothing to do
  }

  public final org.picketlink.idm.api.Group getJBIDMGroup(String _groupId) {// NOSONAR
    throw new UnsupportedOperationException();
  }

}
