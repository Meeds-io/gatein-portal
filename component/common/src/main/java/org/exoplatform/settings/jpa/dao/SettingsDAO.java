package org.exoplatform.settings.jpa.dao;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.settings.jpa.entity.SettingsEntity;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * Created by exo on 3/8/17.
 */
public class SettingsDAO extends GenericDAOJPAImpl<SettingsEntity, Long> implements org.exoplatform.settings.jpa.SettingsDAO {
  private static final Log LOG = ExoLogger.getLogger(SettingsDAO.class);

  @ExoTransactional
  @Override
  public List<SettingsEntity> getSettingsByContextTypeAndName(String contextType, String contextName) {
    TypedQuery<SettingsEntity> query;
    if (StringUtils.isBlank(contextName)) {
      query = getEntityManager().createNamedQuery("SettingsEntity.getSettingsByContextByTypeAndNullName", SettingsEntity.class)
                                .setParameter("contextType", contextType);
    } else {
      query = getEntityManager().createNamedQuery("SettingsEntity.getSettingsByContextByTypeAndName", SettingsEntity.class)
                                .setParameter("contextType", contextType)
                                .setParameter("contextName", contextName);
    }
    return query.getResultList();
  }

  @ExoTransactional
  @Override
  public List<SettingsEntity> getSettingsByContextAndScope(String contextType,
                                                           String contextName,
                                                           String scopeType,
                                                           String scopeName) {
    TypedQuery<SettingsEntity> query;
    boolean nullScopeName = StringUtils.isBlank(scopeName);
    if (nullScopeName) {
      query = getEntityManager().createNamedQuery("SettingsEntity.getSettingsByContextAndScopeWithNullName", SettingsEntity.class)
                                .setParameter("contextType", contextType)
                                .setParameter("contextName", contextName)
                                .setParameter("scopeType", scopeType);
    } else {
      query = getEntityManager().createNamedQuery("SettingsEntity.getSettingsByContextAndScope", SettingsEntity.class)
                                .setParameter("contextType", contextType)
                                .setParameter("contextName", contextName)
                                .setParameter("scopeType", scopeType)
                                .setParameter("scopeName", scopeName);
    }
    return query.getResultList();
  }

  @ExoTransactional
  @Override
  public SettingsEntity getSettingByContextAndScopeAndKey(String contextType,
                                                          String contextName,
                                                          String scopeType,
                                                          String scopeName,
                                                          String key) {

    TypedQuery<SettingsEntity> query;
    boolean nullScope = StringUtils.isBlank(scopeName);
    if (nullScope) {
      query = getEntityManager().createNamedQuery("SettingsEntity.getSettingByContextAndScopeWithNullNameAndKey", SettingsEntity.class)
                                .setParameter("settingName", key)
                                .setParameter("contextType", contextType)
                                .setParameter("contextName", contextName)
                                .setParameter("scopeType", scopeType);
    } else {
      query = getEntityManager().createNamedQuery("SettingsEntity.getSettingByContextAndScopeAndKey", SettingsEntity.class)
                                .setParameter("settingName", key)
                                .setParameter("contextType", contextType)
                                .setParameter("contextName", contextName)
                                .setParameter("scopeType", scopeType)
                                .setParameter("scopeName", scopeName);
    }
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } catch (NonUniqueResultException e) {
      LOG.warn("Setting with name ={}, contextType = {}, contextName = {}, scopeType = {}, scopeName = {} is not unique",
               key,
               contextType,
               contextName,
               scopeType,
               scopeName);
      return query.getResultList().get(0);
    }
  }

  @ExoTransactional
  @Override
  public long countSettingsByNameAndValueAndScope(String scopeType, String scopeName, String key, String value) {
    TypedQuery<Long> query;
    if (StringUtils.isBlank(scopeName)) {
      query = getEntityManager().createNamedQuery("SettingsEntity.countSettingsByNameAndValueAndScopeWithNullName", Long.class)
                                .setParameter("settingName", key)
                                .setParameter("settinValue", value)
                                .setParameter("scopeType", scopeType);
    } else {
      query = getEntityManager().createNamedQuery("SettingsEntity.countSettingsByNameAndValueAndScope", Long.class)
                                .setParameter("settingName", key)
                                .setParameter("settingValue", value)
                                .setParameter("scopeType", scopeType)
                                .setParameter("scopeName", scopeName);
    }
    return query.getSingleResult().longValue();
  }
}
