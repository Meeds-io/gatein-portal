package org.exoplatform.portal.jdbc.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.portal.jdbc.entity.WindowEntity;

public class WindowDAOImpl extends AbstractDAO<WindowEntity> implements WindowDAO {

  @Override
  public List<WindowEntity> findByIds(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<WindowEntity> query = getEntityManager().createNamedQuery("WindowEntity.findByIds", WindowEntity.class);
    query.setParameter("ids", ids);
    return query.getResultList();
  }

  @Override
  public void deleteById(String id) {
    WindowEntity window = find(id);
    if (window != null) {
      delete(window);
    }
  }

}
