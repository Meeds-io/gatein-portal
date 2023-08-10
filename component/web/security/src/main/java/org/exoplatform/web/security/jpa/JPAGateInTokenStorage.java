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
        entity.setUsername(data.username);
        entity.setExpirationTime(data.expirationTime);
        entity.setTokenType(data.tokenType);

        this.tokenDAO.create(entity);
    }

    @Override
    @ExoTransactional
    public TokenData getToken(String tokenId) {
        TokenEntity entity = this.tokenDAO.findByTokenId(tokenId);
        if (entity != null) {
            return new TokenData(entity.getTokenId(), entity.getTokenHash(),
                    entity.getUsername(), entity.getExpirationTime(), entity.getTokenType());
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

    @Override
    @ExoTransactional
    public void deleteTokensByUsernameAndType(String username, String tokenType) {
      
      this.tokenDAO.deleteTokensByUsernameAndType(username, tokenType);
      
    }
}
