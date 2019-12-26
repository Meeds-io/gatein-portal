package org.exoplatform.portal.jdbc.dao;

import java.util.UUID;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.application.registry.dao.CategoryDAO;
import org.exoplatform.application.registry.entity.CategoryEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class CategoryDAOImpl extends GenericDAOJPAImpl<CategoryEntity, Long> implements CategoryDAO {

  @Override
  public CategoryEntity findByName(String name) {
    TypedQuery<CategoryEntity> query = getEntityManager().createNamedQuery("CategoryEntity.findByName", CategoryEntity.class);
    query.setParameter("name", name);
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }
}
