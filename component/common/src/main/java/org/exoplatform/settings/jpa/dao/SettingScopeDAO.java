package org.exoplatform.settings.jpa.dao;

import java.util.List;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.settings.jpa.entity.ScopeEntity;

public class SettingScopeDAO extends GenericDAOJPAImpl<ScopeEntity, Long> implements org.exoplatform.settings.jpa.SettingScopeDAO {
  private static final Log LOG = ExoLogger.getLogger(SettingScopeDAO.class);

  @ExoTransactional
  @Override
  public ScopeEntity getScopeByTypeAndName(String scopeType, String scopeName) {
    TypedQuery<ScopeEntity> query;
    if (StringUtils.isBlank(scopeName)) {
      query = getEntityManager().createNamedQuery("SettingsScopeEntity.getScopeWithNullName", ScopeEntity.class)
                                .setParameter("scopeType", scopeType);
    } else {
      query = getEntityManager().createNamedQuery("SettingsScopeEntity.getScope", ScopeEntity.class)
                                .setParameter("scopeName", scopeName)
                                .setParameter("scopeType", scopeType);
    }
    try {
      List<ScopeEntity> scopes = query.getResultList();
      if (scopes == null || scopes.isEmpty()) {
        return null;
      } else {
        if (scopes.size() > 1) {
          LOG.warn("More than one scope element was found for tyme '{}' and name ''", scopeType, scopeName);
        }
        return scopes.get(0);
      }
    } catch (NoResultException e) {
      return null;
    }
  }
}
