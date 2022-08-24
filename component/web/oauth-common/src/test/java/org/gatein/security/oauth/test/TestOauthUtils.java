package org.gatein.security.oauth.test;

import com.github.scribejava.core.model.OAuth2AccessToken;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.openid.OpenIdAccessTokenContext;
import org.gatein.security.oauth.spi.OAuthPrincipal;
import org.gatein.security.oauth.utils.OAuthUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestOauthUtils {

  @Test
  public void testConvertOpenIdInfoToOAuthPrincipalWithUserInfo() throws JSONException {

    JSONObject userInfo = new JSONObject();
    userInfo.put(OAuthConstants.EMAIL_ATTRIBUTE,"usertest@acme.com");
    userInfo.put(OAuthConstants.GIVEN_NAME_ATTRIBUTE,"User");
    userInfo.put(OAuthConstants.FAMILY_NAME_ATTRIBUTE,"Test");
    userInfo.put(OAuthConstants.NAME_ATTRIBUTE,"User Test");
    userInfo.put(OAuthConstants.PICTURE_ATTRIBUTE,"https://www.test.com/avatar");

    OAuth2AccessToken token = new OAuth2AccessToken("token","raw");
    OpenIdAccessTokenContext openIdAccessTokenContext = new OpenIdAccessTokenContext(token, "openid");


    OAuthPrincipal principal = OAuthUtils.convertOpenIdInfoToOAuthPrincipal(userInfo,openIdAccessTokenContext,null);

    assertNull(principal.getName());
    assertEquals("usertest@acme.com",principal.getEmail());
    assertEquals("User",principal.getFirstName());
    assertEquals("Test",principal.getLastName());
    assertEquals("User Test",principal.getDisplayName());
    assertEquals("https://www.test.com/avatar",principal.getAvatar());

  }

  @Test
  public void testConvertOpenIdInfoToOAuthPrincipalWithCustomClaims() throws JSONException {

    JSONObject userInfo = new JSONObject();

    OAuth2AccessToken token = new OAuth2AccessToken("token","raw");
    OpenIdAccessTokenContext openIdAccessTokenContext = new OpenIdAccessTokenContext(token, "openid");
    Map<String, String> customClaims = new HashMap<>();
    customClaims.put(OAuthConstants.EMAIL_ATTRIBUTE,"usertest@acme.com");
    customClaims.put(OAuthConstants.GIVEN_NAME_ATTRIBUTE,"User");
    customClaims.put(OAuthConstants.FAMILY_NAME_ATTRIBUTE,"Test");
    customClaims.put(OAuthConstants.NAME_ATTRIBUTE,"User Test");
    openIdAccessTokenContext.addCustomClaims(customClaims);


    OAuthPrincipal principal = OAuthUtils.convertOpenIdInfoToOAuthPrincipal(userInfo,openIdAccessTokenContext,null);

    assertNull(principal.getName());
    assertEquals("usertest@acme.com",principal.getEmail());
    assertEquals("User",principal.getFirstName());
    assertEquals("Test",principal.getLastName());
    assertEquals("User Test",principal.getDisplayName());
    assertNull(principal.getAvatar());

  }
}
