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

package org.gatein.security.oauth.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.api.services.oauth2.model.Userinfo;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.impl.UserImpl;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.openid.OpenIdAccessTokenContext;
import org.gatein.security.oauth.spi.OAuthProviderType;
import org.gatein.security.oauth.exception.OAuthException;
import org.gatein.security.oauth.exception.OAuthExceptionCode;
import org.gatein.security.oauth.spi.OAuthPrincipal;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.google.GoogleAccessTokenContext;
import org.gatein.security.oauth.social.FacebookPrincipal;
import org.gatein.security.oauth.twitter.TwitterAccessTokenContext;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Various util methods
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthUtils {

    // Private constructor for utils class
    private OAuthUtils() {}

    // Converting objects

    public static OAuthPrincipal<FacebookAccessTokenContext> convertFacebookPrincipalToOAuthPrincipal(FacebookPrincipal facebookPrincipal, String avatar,
                                            OAuthProviderType<FacebookAccessTokenContext> facebookProviderType, FacebookAccessTokenContext fbAccessTokenContext) {
        return new OAuthPrincipal<FacebookAccessTokenContext>(facebookPrincipal.getUsername(), facebookPrincipal.getFirstName(), facebookPrincipal.getLastName(),
                facebookPrincipal.getAttribute("name"), facebookPrincipal.getEmail(), avatar, fbAccessTokenContext, facebookProviderType);
    }

    public static OAuthPrincipal<TwitterAccessTokenContext> convertTwitterUserToOAuthPrincipal(twitter4j.User twitterUser, TwitterAccessTokenContext accessToken,
                                                             OAuthProviderType<TwitterAccessTokenContext> twitterProviderType) {
        String fullName = twitterUser.getName();
        String firstName;
        String lastName;
        String avatar = twitterUser.getBiggerProfileImageURL();

        int spaceIndex = fullName.lastIndexOf(' ');

        if (spaceIndex != -1) {
            firstName = fullName.substring(0, spaceIndex);
            lastName = fullName.substring(spaceIndex + 1);
        } else {
            firstName = fullName;
            lastName = null;
        }

        return new OAuthPrincipal<TwitterAccessTokenContext>(twitterUser.getScreenName(), firstName, lastName, fullName, null, avatar, accessToken,
                twitterProviderType);
    }

    public static OAuthPrincipal<GoogleAccessTokenContext> convertGoogleInfoToOAuthPrincipal(Userinfo userInfo, GoogleAccessTokenContext accessToken,
                                                               OAuthProviderType<GoogleAccessTokenContext> googleProviderType) {
        // Assume that username is first part of email
        String email = userInfo.getEmail();
        String username = email != null ? email.substring(0, email.indexOf('@')) : userInfo.getGivenName();
        return new OAuthPrincipal<GoogleAccessTokenContext>(username, userInfo.getGivenName(), userInfo.getFamilyName(), userInfo.getName(), userInfo.getEmail(),
                userInfo.getPicture(), accessToken, googleProviderType);
    }
    
    public static OAuthPrincipal<OpenIdAccessTokenContext> convertOpenIdInfoToOAuthPrincipal(JSONObject userInfo,
                                                                                             OpenIdAccessTokenContext accessToken,
                                                                                             OAuthProviderType<OpenIdAccessTokenContext> openIdProviderType) {
        try {
            // Assume that username is first part of email
            String email = userInfo.getString(OAuthConstants.EMAIL_ATTRIBUTE);
            String username = email != null ? email.substring(0, email.indexOf('@')) : userInfo.getString("given_name");
            return new OAuthPrincipal<OpenIdAccessTokenContext>(username,
                                                                userInfo.getString(OAuthConstants.GIVEN_NAME_ATTRIBUTE),
                                                                userInfo.getString(OAuthConstants.FAMILY_NAME_ATTRIBUTE),
                                                                userInfo.getString(OAuthConstants.NAME_ATTRIBUTE),
                                                                userInfo.getString(OAuthConstants.EMAIL_ATTRIBUTE),
                                                                userInfo.has(OAuthConstants.PICTURE_ATTRIBUTE) ?
                                                                userInfo.getString(OAuthConstants.PICTURE_ATTRIBUTE) : null,
                                                                accessToken,
                                                                openIdProviderType);
        } catch (JSONException jsonException) {
            throw new OAuthException(OAuthExceptionCode.ACCESS_TOKEN_ERROR,
                                     "Error during user info reading: response format is ko");
        }
    }
    
    
    
    public static User convertOAuthPrincipalToGateInUser(OAuthPrincipal principal) {
        User gateinUser = new UserImpl(OAuthUtils.refineUserName(principal.getUserName()));
        gateinUser.setFirstName(principal.getFirstName());
        gateinUser.setLastName(principal.getLastName());
        gateinUser.setEmail(principal.getEmail());
        gateinUser.setDisplayName(principal.getDisplayName());
        return gateinUser;
    }

    public static String getURLToRedirectAfterLinkAccount(HttpServletRequest request, HttpSession session) {
        String urlToRedirect = (String)session.getAttribute(OAuthConstants.ATTRIBUTE_URL_TO_REDIRECT_AFTER_LINK_SOCIAL_ACCOUNT);
        if (urlToRedirect == null) {
            urlToRedirect = request.getContextPath();
        } else {
            session.removeAttribute(OAuthConstants.ATTRIBUTE_URL_TO_REDIRECT_AFTER_LINK_SOCIAL_ACCOUNT);
        }

        return urlToRedirect;
    }

    // HTTP related utils

    /**
     * Given a {@link java.util.Map} of params, construct a query string
     *
     * @param params parameters for query
     * @return query string
     */
    public static String createQueryString(Map<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            if (first) {
                first = false;
            } else {
                queryString.append("&");
            }
            queryString.append(paramName).append("=");
            String encodedParamValue;
            try {
                if (paramValue == null)
                    throw new RuntimeException("paramValue is null for paramName=" + paramName);
                encodedParamValue = URLEncoder.encode(paramValue, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new OAuthException(OAuthExceptionCode.UNKNOWN_ERROR, e);
            }
            queryString.append(encodedParamValue);
        }
        return queryString.toString();
    }

    public static String encodeParam(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new OAuthException(OAuthExceptionCode.UNKNOWN_ERROR, uee);
        }
    }

    /**
     * Whole HTTP response as String from given URLConnection
     *
     * @param connection
     * @return whole HTTP response as String
     */
    public static HttpResponseContext readUrlContent(URLConnection connection) throws IOException {
        StringBuilder result = new StringBuilder();

        HttpURLConnection httpURLConnection = (HttpURLConnection)connection;
        int statusCode = httpURLConnection.getResponseCode();

        Reader reader = null;
        try {
            try {
                reader = new InputStreamReader(connection.getInputStream());
            } catch (IOException ioe) {
                reader = new InputStreamReader(httpURLConnection.getErrorStream());
            }

            char[] buffer = new char[50];
            int nrOfChars;
            while ((nrOfChars = reader.read(buffer)) != -1) {
                result.append(buffer, 0, nrOfChars);
            }

            String response = result.toString();
            return new HttpResponseContext(statusCode, response);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * Decode given String to map. For example for input: {@code accessToken=123456&expires=20071458} it returns map with two keys
     * "accessToken" and "expires" and their corresponding values
     *
     * @param encodedData
     * @return map with output data
     */
    public static Map<String, String> formUrlDecode(String encodedData) {
        Map<String, String> params = new HashMap<String, String>();
        String[] elements = encodedData.split("&");
        for (String element : elements) {
            String[] pair = element.split("=");
            if (pair.length == 2) {
                String paramName = pair[0];
                String paramValue;
                try {
                    paramValue = URLDecoder.decode(pair[1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                params.put(paramName, paramValue);
            } else {
                throw new RuntimeException("Unexpected name-value pair in response: " + element);
            }
        }
        return params;
    }

    public static String refineUserName(String username) {
        final String ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012456789._";
        final char replaced = '_';

        if (username == null || username.isEmpty()) {
            return "";
        }

        final char[] chars = username.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (ALLOWED_CHARACTERS.indexOf(chars[i]) == -1) {
                chars[i] = replaced;
            }
        }
        return new String(chars);
    }
}
