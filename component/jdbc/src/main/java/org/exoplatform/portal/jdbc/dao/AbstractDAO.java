package org.exoplatform.portal.jdbc.dao;

import java.util.UUID;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;

public class AbstractDAO<T extends ComponentEntity> extends GenericDAOJPAImpl<T, String> {
  @Override
  public T create(T entity) {
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());      
    }
    return super.create(entity);
  }
}
