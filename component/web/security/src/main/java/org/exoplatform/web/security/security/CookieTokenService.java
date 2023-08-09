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

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.web.security.GateInToken;
import org.exoplatform.web.security.GateInTokenStore;
import org.exoplatform.web.security.hash.JCASaltedHashService;
import org.exoplatform.web.security.hash.SaltedHashException;
import org.exoplatform.web.security.hash.SaltedHashService;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

import java.util.Date;
import java.util.regex.Pattern;

public class CookieTokenService extends AbstractTokenService<GateInToken, String> {

    /** . */
    public static final String LIFECYCLE_NAME = "lifecycle-name";
    public static final String HASH_SERVICE_INIT_PARAM = "hash.service";
    public static final String ONBOARD_TOKEN="onboard";
    public static final String EMAIL_VALIDATION_TOKEN="email-validation";
    public static final String FORGOT_PASSWORD_TOKEN="forgot-password";
    public static final String EXTERNAL_REGISTRATION_TOKEN="external-registration";
    public static final String SEPARATOR_CHAR=".";
    
    private GateInTokenStore tokenStore;

    private SaltedHashService saltedHashService;

    private final Log        log                         = ExoLogger.getLogger(CookieTokenService.class);



    public CookieTokenService(InitParams initParams, GateInTokenStore tokenStore)
            throws TokenServiceInitializationException {
        super(initParams);

        this.tokenStore = tokenStore;

        ObjectParameter hashServiceParam = initParams.getObjectParam(HASH_SERVICE_INIT_PARAM);
        if (hashServiceParam == null || hashServiceParam.getObject() == null) {
            /* the default */
            saltedHashService = new JCASaltedHashService();
        } else {
            saltedHashService = (SaltedHashService) hashServiceParam.getObject();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.exoplatform.web.security.security.AbstractTokenService#start()
     */
    @Override
    public void start() {
        this.tokenStore.cleanLegacy();
        super.start();
    }
    
    public String createToken(String username) {
        return createToken(username,"");
    }
    
    public String createToken(String username, String type) {
        if (validityMillis < 0) {
            throw new IllegalArgumentException();
        }
        if (username == null) {
            throw new NullPointerException();
        }

        String cookieTokenString = null;
        while (cookieTokenString == null) {
            String selector = nextTokenId(); //9 bytes selector
            String validator = nextRandom(); //9 bytes validator

            String hashedRandomString = hashToken(validator+SEPARATOR_CHAR+type);
            long expirationTimeMillis = System.currentTimeMillis() + validityMillis;
            cookieTokenString=selector+SEPARATOR_CHAR+validator;
            try {
                this.tokenStore.saveToken(new GateInTokenStore.TokenData(selector, hashedRandomString,
                                                                         username, new Date(expirationTimeMillis), type));
            } catch (TokenExistsException e) {
                cookieTokenString = null;
            }
        }
        return cookieTokenString;
    }

    @Override
    protected String nextTokenId() {
        return nextRandom();
    }

    @Override
    public GateInToken getToken(String cookieTokenString, String tokenType) {
        try {
            CookieToken token = new CookieToken(cookieTokenString);
            //cookieTokenString = selector#validator

            GateInTokenStore.TokenData encryptedToken = tokenStore.getToken(token.getId());
            //encryptedToken =
            // expirationTime
            // selector
            // hash (hash(validator#type)
            // tokenType
            // userId

            if (encryptedToken != null && cookieTokenString.contains(SEPARATOR_CHAR)) {
                try {
                    String splittedToken[] = cookieTokenString.split(Pattern.quote(SEPARATOR_CHAR));
                    String selector = splittedToken[0];
                    String validator = splittedToken[1];

                    String tokenRandomString=validator+SEPARATOR_CHAR+tokenType;
                    if (saltedHashService.validate(tokenRandomString, encryptedToken.hash)) {
                        return new GateInToken(encryptedToken.expirationTime.getTime(), encryptedToken.username);
                    }
                } catch (SaltedHashException e) {
                    log.warn("Could not validate cookie token against its salted hash.", e);
                }
            }
        } catch (TokenParseException e) {
            log.warn("Could not parse cookie token:"+ e.getMessage());
        }
        return null;
    }
    
    @Override
    public GateInToken getToken(String id) {
        return getToken(id,"");
    }
    
    @Override
    public GateInToken deleteToken(String cookieTokenString) {
        return deleteToken(cookieTokenString,"");
    }
    @Override
    public GateInToken deleteToken(String cookieTokenString, String tokenType) {
        try {
            GateInToken result = this.getToken(cookieTokenString,tokenType);
            if (result != null) {
                CookieToken token = new CookieToken(cookieTokenString);
                tokenStore.deleteToken(token.getId());
                return result;
            }
        } catch (TokenParseException e) {
            log.warn("Could not parse cookie token:"+ e.getMessage());
        }
        return null;
    }

    /**
     * The UI should offer a way to delete all existing tokens of the current user.
     *
     * @param user
     */
    public void deleteTokensOfUser(final String user) {
        this.tokenStore.deleteTokenOfUser(user);
    }
    
    /**
     * The UI should offer a way to delete all existing tokens of the current user.
     *
     * @param username username Target username
     * @param tokenType tokenType of the username
     */
    public void deleteTokensByUsernameAndType(final String username, final String tokenType) {
        this.tokenStore.deleteTokensByUsernameAndType(username, tokenType);
    }
        
    /**
     * Removes all stored tokens
     */
    public void deleteAll() {
        this.tokenStore.deleteAll();
    }

    @Override
    public void cleanExpiredTokens() {
        this.tokenStore.cleanExpired();
    }

    @Override
    public long size() {
        return this.tokenStore.size();
    }

    @Override
    protected String decodeKey(String stringKey) {
        return stringKey;
    }

    private String hashToken(String tokenId) {
        if (saltedHashService != null) {
            try {
                return saltedHashService.getSaltedHash(tokenId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            /* no hash if saltedHashService is null */
            return tokenId;
        }
    }


}
