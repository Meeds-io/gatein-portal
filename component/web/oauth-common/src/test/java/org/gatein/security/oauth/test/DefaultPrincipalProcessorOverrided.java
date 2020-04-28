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
package org.gatein.security.oauth.test;

import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.impl.UserImpl;
import org.gatein.security.oauth.principal.DefaultPrincipalProcessor;
import org.gatein.security.oauth.spi.OAuthPrincipal;

public class DefaultPrincipalProcessorOverrided extends DefaultPrincipalProcessor {
    @Override
    public User convertToGateInUser(OAuthPrincipal principal) {
        return new UserImpl("User_overrided");
    }
}
