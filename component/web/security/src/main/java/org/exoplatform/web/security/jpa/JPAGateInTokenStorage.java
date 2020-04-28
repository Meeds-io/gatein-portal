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
package org.exoplatform.web.security.jpa;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.web.security.GateInTokenStore;
import org.exoplatform.web.security.security.TokenExistsException;
import org.gatein.wci.security.Credentials;

import java.util.List;

public class JPAGateInTokenStorage implements GateInTokenStore {

    private TokenDAO tokenDAO;

    public JPAGateInTokenStorage(InitParams initParams, TokenDAO tokenDAO) {
        this.tokenDAO = tokenDAO;
    }

    @Override
    public void cleanLegacy() {
    }

    @Override
    @ExoTransactional
    public void saveToken(TokenData data) throws TokenExistsException {
        TokenEntity existing = this.tokenDAO.findByTokenId(data.tokenId);
        if (existing != null) {
            throw new TokenExistsException();
        }
        TokenEntity entity = new TokenEntity();
        entity.setTokenId(data.tokenId);
        entity.setTokenHash(data.hash);
        entity.setUsername(data.payload.getUsername());
        entity.setPassword(data.payload.getPassword());
        entity.setExpirationTime(data.expirationTime);

        this.tokenDAO.create(entity);
    }

    @Override
    @ExoTransactional
    public TokenData getToken(String tokenId) {
        TokenEntity entity = this.tokenDAO.findByTokenId(tokenId);
        if (entity != null) {
            return new TokenData(entity.getTokenId(), entity.getTokenHash(),
                    new Credentials(entity.getUsername(), entity.getPassword()), entity.getExpirationTime());
        }
        return null;
    }

    @Override
    @ExoTransactional
    public void deleteToken(String tokenId) {
        TokenEntity entity = this.tokenDAO.findByTokenId(tokenId);
        if (entity != null) {
            this.tokenDAO.delete(entity);
        }
    }

    @Override
    @ExoTransactional
    public void deleteTokenOfUser(String user) {
        List<TokenEntity> entities = this.tokenDAO.findByUsername(user);
        if (entities != null && !entities.isEmpty()) {
            this.tokenDAO.deleteAll(entities);
        }
    }

    @Override
    @ExoTransactional
    public void deleteAll() {
        this.tokenDAO.deleteAll();
    }

    @Override
    @ExoTransactional
    public void cleanExpired() {
        this.tokenDAO.cleanExpired();
    }

    @Override
    @ExoTransactional
    public long size() {
        return this.tokenDAO.count();
    }
}
