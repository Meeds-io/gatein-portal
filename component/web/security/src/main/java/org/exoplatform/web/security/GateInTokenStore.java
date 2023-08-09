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
    
    void deleteTokensByUsernameAndType(String username, String tokenType);

    class TokenData {
        /** . */
        public final String tokenId;

        /** . */
        public final String hash;

        /** . */
        public final String username;

        /** . */
        public final Date expirationTime;
        
        public final String tokenType;

        public TokenData(String tokenId, String hash, String username, Date expirationTime, String tokenType) {
            this.tokenId = tokenId;
            this.hash = hash;
            this.expirationTime = expirationTime;
            this.username = username;
            this.tokenType = tokenType;
        }
    }
}
