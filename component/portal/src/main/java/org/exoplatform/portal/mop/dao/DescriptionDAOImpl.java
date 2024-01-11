package org.exoplatform.portal.mop.dao;

import java.util.Map;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.DescriptionEntity;
import org.exoplatform.portal.jdbc.entity.DescriptionState;

public class DescriptionDAOImpl extends GenericDAOJPAImpl<DescriptionEntity, Long> implements DescriptionDAO {
  
  @Override
  @ExoTransactional
  public int deleteByRefId(String refId) {
    DescriptionEntity desc = getByRefId(refId);
    if (desc != null) {
      this.delete(desc);
      return 1; 
    } else {
      return 0;      
    }
  }

  @Override
  @ExoTransactional
  public DescriptionEntity saveDescriptions(String refId, Map<String, DescriptionState> states) {
    if (refId == null) {
      throw new IllegalArgumentException("refId , states must not be null");
    }

    DescriptionEntity entity = getOrCreate(refId);
    entity.setLocalized(states);
    if (entity.getId() == null) {
      this.create(entity);
    } else {      
      this.update(entity);
    }

    return entity;
  }

  @Override
  @ExoTransactional
  public DescriptionEntity saveDescription(String refId, DescriptionState state) {
    if (refId == null) {
      throw new IllegalArgumentException("refId , states must not be null");
    }
    
    DescriptionEntity entity = getOrCreate(refId);
    entity.setState(state);
    if (entity.getId() == null) {
      this.create(entity);
    } else {
      this.update(entity);
    }
    return entity;
  }

  @Override
  public DescriptionEntity getByRefId(String refId) {
    if (refId == null) {
      return null;
    }
    
    TypedQuery<DescriptionEntity> query = getEntityManager().createNamedQuery("DescriptionEntity.getByRefId", DescriptionEntity.class);
    query.setParameter("refId", refId);
    
    try {
      return query.getSingleResult();      
    } catch (NoResultException ex) {
      return null;
    }
  }

  private DescriptionEntity getOrCreate(String refId) {
    DescriptionEntity entity = getByRefId(refId);
    if (entity == null) {
      entity = new DescriptionEntity();
      entity.setReferenceId(refId);
    }
    return entity;
  } 
}
