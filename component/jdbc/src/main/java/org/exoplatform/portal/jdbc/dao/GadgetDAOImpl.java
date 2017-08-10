package org.exoplatform.portal.jdbc.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.GadgetEntity;

public class GadgetDAOImpl extends GenericDAOJPAImpl<GadgetEntity, Long> implements GadgetDAO {

  @Override
  public GadgetEntity find(String name) {
    TypedQuery<GadgetEntity> query = getEntityManager().createNamedQuery("GadgetEntity.find", GadgetEntity.class);
    query.setParameter("name", name);
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }
}
