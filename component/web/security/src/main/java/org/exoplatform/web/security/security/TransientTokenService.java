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

package org.exoplatform.web.security.security;

import java.util.concurrent.ConcurrentHashMap;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.web.security.GateInToken;
import org.gatein.wci.security.Credentials;

/**
 * Created by The eXo Platform SAS Author : liem.nguyen ncliam@gmail.com Jun 5, 2009
 */
public class TransientTokenService extends PlainTokenService<GateInToken, String> {

    protected final ConcurrentHashMap<String, GateInToken> tokens = new ConcurrentHashMap<String, GateInToken>();

    public TransientTokenService(InitParams initParams) throws TokenServiceInitializationException {
        super(initParams);
    }

    public String createToken(Credentials credentials) {
        if (validityMillis < 0) {
            throw new IllegalArgumentException();
        }
        if (credentials == null) {
            throw new NullPointerException();
        }
        String tokenId = nextTokenId();
        long expirationTimeMillis = System.currentTimeMillis() + validityMillis;
        tokens.put(tokenId, new GateInToken(expirationTimeMillis, credentials));
        return tokenId;
    }

    @Override
    public GateInToken getToken(String id) {
        return tokens.get(id);
    }

    @Override
    protected String decodeKey(String stringKey) {
        return stringKey;
    }

    @Override
    public GateInToken deleteToken(String id) {
        GateInToken token = tokens.get(id);
        tokens.remove(id);
        return token;
    }

    @Override
    public String[] getAllTokens() {
        return tokens.keySet().toArray(new String[] {});
    }

    @Override
    public long size() {
        return tokens.size();
    }
}
