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
