package org.exoplatform.portal.mop.jdbc.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.application.registry.dao.ApplicationDAO;
import org.exoplatform.application.registry.entity.ApplicationEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class ApplicationDAOImpl extends GenericDAOJPAImpl<ApplicationEntity, Long> implements ApplicationDAO {

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
