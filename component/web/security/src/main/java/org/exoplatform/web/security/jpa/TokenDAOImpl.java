package org.exoplatform.web.security.jpa;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class TokenDAOImpl extends GenericDAOJPAImpl<TokenEntity, Long> implements TokenDAO {

    @Override
    public TokenEntity findByTokenId(String tokenId) {
        TypedQuery<TokenEntity> query = getEntityManager().createNamedQuery("GateInToken.findByTokenId", TokenEntity.class);
        query.setParameter("tokenId", tokenId);
        try {
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public List<TokenEntity> findByUsername(String username) {
        TypedQuery<TokenEntity> query = getEntityManager().createNamedQuery("GateInToken.findByUser", TokenEntity.class);
        query.setParameter("username", username);
        return query.getResultList();
    }

    @Override
    @ExoTransactional
    public void cleanExpired() {
        Query query = getEntityManager().createNamedQuery("GateInToken.deleteExpiredTokens");
        query.setParameter("expireTime", System.currentTimeMillis());
        query.executeUpdate();
    }

    @Override
    @ExoTransactional
    public void deleteTokensByUsernameAndType(String username, String tokenType) {
        Query query = getEntityManager().createNamedQuery("GateInToken.deleteTokensByUserAndType");
        query.setParameter("username", username);
        query.setParameter("tokenType", tokenType);
        query.executeUpdate();
    }
}
