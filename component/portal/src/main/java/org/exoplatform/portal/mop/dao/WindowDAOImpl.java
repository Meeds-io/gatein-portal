package org.exoplatform.portal.mop.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.gatein.api.common.Pagination;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.portal.jdbc.entity.WindowEntity;

public class WindowDAOImpl extends AbstractDAO<WindowEntity> implements WindowDAO {

  @Override
  @ExoTransactional
  public WindowEntity find(Long id) {
    return super.find(id);
  }

  @Override
  @ExoTransactional
  public List<Long> findIdsByContentIds(List<String> contentIds, Pagination pagination) {
    if (contentIds == null || contentIds.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<Long> query = getEntityManager().createNamedQuery("WindowEntity.findByContentIds", Long.class);
    query.setParameter("contentIds", contentIds);
    if (pagination != null && pagination.getLimit() > 0) {
      query.setFirstResult(pagination.getOffset());
      query.setMaxResults(pagination.getLimit());
    }
    query.setParameter("contentIds", contentIds);
    return query.getResultList();
  }

  @Override
  @ExoTransactional
  public int updateContentId(String oldContentId, String newContentId) {
    Query query = getEntityManager().createNamedQuery("WindowEntity.updateContentId");
    query.setParameter("oldContentId", oldContentId);
    query.setParameter("newContentId", newContentId);
    return query.executeUpdate();
  }

  @Override
  @ExoTransactional
  public int deleteByContentId(String contentId) {
    Query query = getEntityManager().createNamedQuery("WindowEntity.deleteByContentId");
    query.setParameter("contentId", contentId);
    return query.executeUpdate();
  }

  @Override
  @ExoTransactional
  public List<WindowEntity> findByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<WindowEntity> query = getEntityManager().createNamedQuery("WindowEntity.findByIds", WindowEntity.class);
    query.setParameter("ids", ids);
    return query.getResultList();
  }
  
  @Override
  @ExoTransactional
  public void deleteById(Long id) {
    WindowEntity window = find(id);
    if (window != null) {
      delete(window);
    }
  }

}
