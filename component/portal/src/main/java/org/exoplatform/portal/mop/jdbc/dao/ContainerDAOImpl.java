package org.exoplatform.portal.mop.jdbc.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.portal.mop.jdbc.entity.ContainerEntity;

public class ContainerDAOImpl extends AbstractDAO<ContainerEntity> implements ContainerDAO {

  @Override
  public List<ContainerEntity> findByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<ContainerEntity> query = getEntityManager().createNamedQuery("ContainerEntity.findByIds", ContainerEntity.class);
    query.setParameter("ids", ids);
    return query.getResultList();
  }

  @Override
  public void deleteById(Long id) {
    ContainerEntity containerEntity = find(id);
    if (containerEntity != null) {
      delete(containerEntity);
    }
  }

}
