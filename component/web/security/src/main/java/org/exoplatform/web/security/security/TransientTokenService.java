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

    public String createToken(String username) {
        if (validityMillis < 0) {
            throw new IllegalArgumentException();
        }
        if (username == null) {
            throw new NullPointerException();
        }
        String tokenId = nextTokenId();
        long expirationTimeMillis = System.currentTimeMillis() + validityMillis;
        tokens.put(tokenId, new GateInToken(expirationTimeMillis, username));
        return tokenId;
    }

    @Override
    public GateInToken getToken(String id, String type) {
        return tokens.get(id);
    }
    
    @Override
    public GateInToken getToken(String id) {
        return getToken(id,"");
    }

    @Override
    protected String decodeKey(String stringKey) {
        return stringKey;
    }
    
    @Override
    public GateInToken deleteToken(String id,String tokenType) {
        GateInToken token = tokens.get(id);
        tokens.remove(id);
        return token;
    }
    @Override
    public GateInToken deleteToken(String id) {
        return deleteToken(id,"");
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
