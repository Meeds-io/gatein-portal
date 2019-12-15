package org.exoplatform.web.security.jpa;

import org.exoplatform.commons.api.persistence.GenericDAO;

import java.util.List;

public interface TokenDAO extends GenericDAO<TokenEntity, Long> {
    TokenEntity findByTokenId(String tokenId);
    List<TokenEntity> findByUsername(String username);
    void cleanExpired();
}
