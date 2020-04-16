/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.gatein.security.oauth.web.linkedin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.apache.commons.lang.StringUtils;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.exception.OAuthException;
import org.gatein.security.oauth.exception.OAuthExceptionCode;
import org.gatein.security.oauth.linkedin.LinkedinAccessTokenContext;
import org.gatein.security.oauth.linkedin.LinkedinProcessor;
import org.gatein.security.oauth.spi.InteractionState;
import org.gatein.security.oauth.spi.OAuthPrincipal;
import org.gatein.security.oauth.spi.OAuthProviderType;
import org.gatein.security.oauth.web.OAuthProviderFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class LinkedInFilter extends OAuthProviderFilter<LinkedinAccessTokenContext> {
    private static String URL_CURRENT_PROFILE_USER = "https://api.linkedin.com/v2/me:(id,first-name,last-name,email-address,public-profile-url,picture-url,picture-urls::(original))?format=json";

    @Override
    protected OAuthProviderType<LinkedinAccessTokenContext> getOAuthProvider() {
        return getOauthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_LINKEDIN, LinkedinAccessTokenContext.class);
    }

    @Override
    protected void initInteraction(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().removeAttribute(OAuthConstants.ATTRIBUTE_LINKEDIN_REQUEST_TOKEN);
    }

    @Override
    protected OAuthPrincipal<LinkedinAccessTokenContext> getOAuthPrincipal(HttpServletRequest request, HttpServletResponse response, InteractionState<LinkedinAccessTokenContext> interactionState) throws IOException, ExecutionException, InterruptedException {
        LinkedinAccessTokenContext accessTokenContext = interactionState.getAccessTokenContext();

        OAuthRequest oauthRequest = new OAuthRequest(Verb.GET, URL_CURRENT_PROFILE_USER);
        accessTokenContext.oauth20Service.signRequest(accessTokenContext.accessToken, oauthRequest);
        Response responses = accessTokenContext.oauth20Service.execute((OAuthRequest) request);
        String body = responses.getBody();

        try {
            JSONObject json = new JSONObject(body);
            String id = json.getString("id");
            String firstName = json.getString("firstName");
            String lastName = json.getString("lastName");
            String displayName = firstName + " " + lastName;
            String email = json.getString("emailAddress");

            String avatar = json.optString("pictureUrl");
            JSONObject profilePictures = json.optJSONObject("pictureUrls");
            if (profilePictures != null) {
                JSONArray arr = profilePictures.optJSONArray("values");
                if (arr != null && arr.length() > 0) {
                    avatar = arr.getString(0);
                }
            }

            OAuthPrincipal<LinkedinAccessTokenContext> principal =
                    new OAuthPrincipal<LinkedinAccessTokenContext>(id, firstName, lastName, displayName, email, avatar, accessTokenContext, getOAuthProvider());

            return principal;

        } catch (JSONException ex) {
            throw new OAuthException(OAuthExceptionCode.LINKEDIN_ERROR, "Error when obtaining user", ex);
        }
    }
}
