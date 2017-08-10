package org.exoplatform.portal.jdbc.dao;

import java.util.UUID;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.ApplicationEntity;

public class ApplicationDAOImpl extends GenericDAOJPAImpl<ApplicationEntity, String> implements ApplicationDAO {
  @Override
  public ApplicationEntity create(ApplicationEntity entity) {
    entity.setId(UUID.randomUUID().toString());
    return super.create(entity);
  }

  @Override
  public ApplicationEntity find(String categoryName, String name) {
    TypedQuery<ApplicationEntity> query = getEntityManager().createNamedQuery("ApplicationEntity.find", ApplicationEntity.class);
    query.setParameter("catName", categoryName);
    query.setParameter("name", name);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }
}
