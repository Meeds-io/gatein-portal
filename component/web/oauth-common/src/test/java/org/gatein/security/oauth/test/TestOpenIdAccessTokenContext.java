package org.gatein.security.oauth.test;

import com.github.scribejava.core.model.OAuth2AccessToken;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.openid.OpenIdAccessTokenContext;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestOpenIdAccessTokenContext {

  @Test
  public void testOpenIdAccessTokenContextCreation() {
    OAuth2AccessToken token = new OAuth2AccessToken("token", "raw");
    OpenIdAccessTokenContext tokenContext = new OpenIdAccessTokenContext(token,"openid");

    assertEquals(0,tokenContext.getCustomClaims().size());

    Map<String, String> customClaims = new HashMap<>();
    customClaims.put("customClaims", "custom");
    tokenContext.addCustomClaims(customClaims);

    assertEquals(1,tokenContext.getCustomClaims().size());
    assertEquals("custom",tokenContext.getCustomClaims().get("customClaims"));


  }
}
