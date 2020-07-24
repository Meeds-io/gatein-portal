/*
 * JBoss, a division of Red Hat
 * Copyright 2013, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
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

package org.gatein.security.oauth.openid;

import java.io.Serializable;

import com.github.scribejava.core.model.OAuth2AccessToken;
import org.gatein.security.oauth.spi.AccessTokenContext;

/**
 * Encapsulate informations about OpenId access token
 *
 */
public class OpenIdAccessTokenContext extends AccessTokenContext implements Serializable {

    private static final long serialVersionUID = -7038197192745766989L;
    
    public final OAuth2AccessToken accessToken;

    public OpenIdAccessTokenContext(OAuth2AccessToken tokenData, String... scopes) {
        super(scopes);
        if (tokenData == null) {
            throw new IllegalArgumentException("tokenData can't be null");
        }
        this.accessToken = tokenData;
    }

    public OpenIdAccessTokenContext(OAuth2AccessToken tokenData, String scopeAsString) {
        super(scopeAsString);
        if (tokenData == null) {
            throw new IllegalArgumentException("tokenData can't be null");
        }
        this.accessToken = tokenData;
    }
    
    @Override
    public String getAccessToken() {
        return accessToken.getAccessToken();
    }
    
    public OAuth2AccessToken getTokenData() {
        return accessToken;
    }

}
