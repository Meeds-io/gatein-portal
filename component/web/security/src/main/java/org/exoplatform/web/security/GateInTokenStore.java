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
package org.exoplatform.web.security;

import org.exoplatform.web.security.security.TokenExistsException;
import org.gatein.wci.security.Credentials;

import java.util.Date;

public interface GateInTokenStore {
    void cleanLegacy();

    void saveToken(TokenData data) throws TokenExistsException;

    TokenData getToken(String tokenId);

    void deleteToken(String tokenId);

    void deleteTokenOfUser(String user);

    void deleteAll();

    void cleanExpired();

    long size();

    class TokenData {
        /** . */
        public final String tokenId;

        /** . */
        public final String hash;

        /** . */
        public final Credentials payload;

        /** . */
        public final Date expirationTime;

        public TokenData(String tokenId, String hash, Credentials payload, Date expirationTime) {
            this.tokenId = tokenId;
            this.hash = hash;
            this.expirationTime = expirationTime;
            this.payload = payload;
        }
    }
}
