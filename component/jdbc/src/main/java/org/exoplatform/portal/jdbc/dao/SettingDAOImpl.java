package org.exoplatform.portal.jdbc.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.SettingEntity;

public class SettingDAOImpl extends GenericDAOJPAImpl<SettingEntity, Long> implements SettingDAO {

  @Override
  public SettingEntity findByName(String name) {
    TypedQuery<SettingEntity> query = getEntityManager().createNamedQuery("SettingEntity.findByName", SettingEntity.class);
    query.setParameter("name", name);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }  
}
