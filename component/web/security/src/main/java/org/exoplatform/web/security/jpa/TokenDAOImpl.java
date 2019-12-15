package org.exoplatform.web.security.jpa;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.Date;
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
        TypedQuery<TokenEntity> query = getEntityManager().createNamedQuery("GateInToken.findExpired", TokenEntity.class);
        query.setParameter("expireTime", System.currentTimeMillis());
        List<TokenEntity> entities = query.getResultList();
        if (entities != null && !entities.isEmpty()) {
            this.deleteAll(entities);
        }
    }
}
