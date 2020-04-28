/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.web.security.security;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.web.security.GateInToken;
import org.exoplatform.web.security.GateInTokenStore;
import org.exoplatform.web.security.codec.AbstractCodec;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.hash.JCASaltedHashService;
import org.exoplatform.web.security.hash.SaltedHashException;
import org.exoplatform.web.security.hash.SaltedHashService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.wci.security.Credentials;

import java.util.Date;


/**
 * <p>
 * Created by The eXo Platform SAS Author : liem.nguyen ncliam@gmail.com Jun 5, 2009
 * </p>
 *
 * On 2013-01-02 the followig was added by ppalaga@redhat.com:
 * <ul>
 * <li>Passwords encrypted symmetrically before they are stored. The functionaliy was taken from <a
 * href="https://github.com/exoplatform/exogtn/commit/5ef8b0fa2d639f4d834444468426dfb2c8485ae9"
 * >https://github.com/exoplatform/exogtn/commit/5ef8b0fa2d639f4d834444468426dfb2c8485ae9</a> with minor modifications. See
 * {@link #codec}</li>
 * <li>The tokens are not stored in plain text, but intead only their salted hash is stored. See {@link #saltedHashService}. To
 * enable this, the following was done:
 * <ul>
 * <li>The structure of the underlying store was changed from
 *
 * <pre>
 * autologin
 * |- plain-token1 user="user1" password="***" expiration="..."
 * |- plain-token2 user="user2" password="***" expiration="..."
 * `- ...
 * </pre>
 *
 * to
 *
 * <pre>
 * autologin
 * |- user1
 * |  |- plain-token1 user="user1" password="***" expiration="..."
 * |  |- plain-token2 user="user1" password="***" expiration="..."
 * |  `- ...
 * |- user2
 * |  |- plain-token3 user="user2" password="***" expiration="..."
 * |  |- plain-token4 user="user2" password="***" expiration="..."
 * |  `- ...
 * `- ...
 * </pre>
 *
 * </li>
 * <li>The value of the token was changed from {@code "rememberme" + randomString} to {@code userName + '.' + randomString}</li>
 * </ul>
 * </li>
 * </ul>

 * <p>
 * It should be considered in the future if the password field can be removed altogether.
 * </p>
 *
 */
public class CookieTokenService extends AbstractTokenService<GateInToken, String> {

    /** . */
    public static final String LIFECYCLE_NAME = "lifecycle-name";
    public static final String HASH_SERVICE_INIT_PARAM = "hash.service";

    private GateInTokenStore tokenStore;

    /**
     * {@link AbstractCodec} used to symmetrically encrypt passwords before storing them.
     */
    private AbstractCodec codec;

    private SaltedHashService saltedHashService;

    private final Logger log = LoggerFactory.getLogger(CookieTokenService.class);

    public CookieTokenService(InitParams initParams, GateInTokenStore tokenStore, CodecInitializer codecInitializer)
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
        this.codec = codecInitializer.getCodec();
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

    public String createToken(final Credentials credentials) {
        if (validityMillis < 0) {
            throw new IllegalArgumentException();
        }
        if (credentials == null) {
            throw new NullPointerException();
        }

        String cookieTokenString = null;
        while (cookieTokenString == null) {
            String randomString = nextTokenId();
            String id = nextRandom();
            cookieTokenString = new CookieToken(id, randomString).toString();

            String hashedRandomString = hashToken(randomString);
            long expirationTimeMillis = System.currentTimeMillis() + validityMillis;

            /* the symmetric encryption happens here */
            String encryptedPassword = codec.encode(credentials.getPassword());
            Credentials encodedCredentials = new Credentials(credentials.getUsername(), encryptedPassword);

            try {
                this.tokenStore.saveToken(new GateInTokenStore.TokenData(id, hashedRandomString, encodedCredentials, new Date(expirationTimeMillis)));
//                tokenContainer.saveToken(context.getSession(), id, hashedRandomString, encodedCredentials, new Date(expirationTimeMillis));
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
    public GateInToken getToken(String cookieTokenString) {
        try {
            CookieToken token = new CookieToken(cookieTokenString);

            GateInTokenStore.TokenData encryptedToken = tokenStore.getToken(token.getId());
            if (encryptedToken != null) {
                try {
                    if (saltedHashService.validate(token.getRandomString(), encryptedToken.hash)) {
                        Credentials encryptedCredentials = encryptedToken.payload;
                        Credentials decryptedCredentials = new Credentials(encryptedCredentials.getUsername(),
                                    codec.decode(encryptedCredentials.getPassword()));

                        return new GateInToken(encryptedToken.expirationTime.getTime(), decryptedCredentials);
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
    public GateInToken deleteToken(String cookieTokenString) {
        try {
            GateInToken result = this.getToken(cookieTokenString);
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
