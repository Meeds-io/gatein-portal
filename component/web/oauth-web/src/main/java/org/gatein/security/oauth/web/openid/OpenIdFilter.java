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

package org.gatein.security.oauth.web.openid;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.services.oauth2.model.Userinfo;
import jdk.nashorn.api.scripting.JSObject;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.google.GoogleProcessor;
import org.gatein.security.oauth.openid.OpenIdAccessTokenContext;
import org.gatein.security.oauth.openid.OpenIdProcessor;
import org.gatein.security.oauth.spi.InteractionState;
import org.gatein.security.oauth.spi.OAuthPrincipal;
import org.gatein.security.oauth.spi.OAuthProviderType;
import org.gatein.security.oauth.utils.OAuthUtils;
import org.gatein.security.oauth.web.OAuthProviderFilter;
import org.json.JSONObject;

/**
 * Filter for integration with authentication handhsake via OpenId
 */
public class OpenIdFilter extends OAuthProviderFilter<OpenIdAccessTokenContext> {

    @Override
    protected OAuthProviderType<OpenIdAccessTokenContext> getOAuthProvider() {
        return this.getOauthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_OPEN_ID, OpenIdAccessTokenContext.class);
    }

    @Override
    protected void initInteraction(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().removeAttribute(OAuthConstants.ATTRIBUTE_AUTH_STATE);
        request.getSession().removeAttribute(OAuthConstants.ATTRIBUTE_VERIFICATION_STATE);
    }

    @Override
    protected OAuthPrincipal<OpenIdAccessTokenContext> getOAuthPrincipal(HttpServletRequest request, HttpServletResponse response,
                                                                         InteractionState<OpenIdAccessTokenContext> interactionState) {
        OpenIdAccessTokenContext accessTokenContext = interactionState.getAccessTokenContext();
        JSONObject userInfo = ((OpenIdProcessor)getOauthProviderProcessor()).obtainUserInfo(accessTokenContext);
        
        if (log.isTraceEnabled()) {
            log.trace("Obtained tokenResponse from OpenId authentication: " + accessTokenContext);
        }

        return OAuthUtils.convertOpenIdInfoToOAuthPrincipal(userInfo, accessTokenContext, getOAuthProvider());
    }

}
