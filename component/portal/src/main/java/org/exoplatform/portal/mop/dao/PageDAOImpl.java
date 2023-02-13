package org.exoplatform.portal.mop.dao;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.gatein.api.common.Pagination;
import org.gatein.api.page.PageQuery;
import org.gatein.api.site.SiteType;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.EntityManagerHolder;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.page.PageKey;

public class PageDAOImpl extends AbstractDAO<PageEntity> implements PageDAO {

  @Override
  public PageEntity findByKey(PageKey pageKey) {
    TypedQuery<PageEntity> query = getEntityManager().createNamedQuery("PageEntity.findByKey", PageEntity.class);

    SiteKey siteKey = pageKey.getSite();
    query.setParameter("ownerType", siteKey.getType());
    query.setParameter("ownerId", siteKey.getName());
    query.setParameter("name", pageKey.getName());
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  @ExoTransactional
  public void deleteByOwner(long id) {
    Query query = getEntityManager().createNamedQuery("PageEntity.deleteByOwner");

    query.setParameter("ownerId", id);
    query.executeUpdate();
  }

  @Override
  public ListAccess<PageKey> findByQuery(PageQuery query) {
    TypedQuery<PageKey> q = buildQuery(query);
    final List<PageKey> results = q.getResultList();

    return new ListAccess<PageKey>() {

      @Override
      public int getSize() throws Exception {
        return results.size();
      }

      @Override
      public PageKey[] load(int offset, int limit) throws Exception {
        if (limit < 0) {
          limit = getSize();
        }
        return results.subList(offset, offset + limit).toArray(new PageKey[limit]);
      }
    };
  }

  public TypedQuery<PageKey> buildQuery(PageQuery query) {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<PageKey> criteria = cb.createQuery(PageKey.class);
    Root<PageEntity> pageEntity = criteria.from(PageEntity.class);
    Join<PageEntity, SiteEntity> join = pageEntity.join("owner");

    CriteriaQuery<PageKey> select = criteria.multiselect(join.get("siteType"), join.get("name"), pageEntity.get("name"));
    select.distinct(true);

    List<Predicate> predicates = new LinkedList<>();

    if (query.getSiteType() != null && query.getSiteName() != null) {
      if (query.getSiteType() != null) {
        predicates.add(cb.equal(join.get("siteType"), convertSiteType(query.getSiteType())));
      }
      if (query.getSiteName() != null) {
        predicates.add(cb.equal(join.get("name"), query.getSiteName()));
      }
    }

    if (query.getDisplayName() != null) {
      predicates.add(cb.like(cb.lower(pageEntity.get("displayName")),
                             "%" + query.getDisplayName().toLowerCase() + "%"));
    }

    select.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

    //
    TypedQuery<PageKey> typedQuery = em.createQuery(select);
    Pagination pagination = query.getPagination();
    if (pagination != null && pagination.getLimit() > 0) {
      select.orderBy(cb.desc(join.get("siteType")), cb.asc(join.get("name")), cb.asc(pageEntity.get("name")));
      typedQuery.setFirstResult(pagination.getOffset());
      typedQuery.setMaxResults(pagination.getLimit());
    }
    //
    return typedQuery;
  }

  private org.exoplatform.portal.mop.SiteType convertSiteType(SiteType siteType) {
    switch (siteType) {
    case SITE:
      return org.exoplatform.portal.mop.SiteType.PORTAL;
    case SPACE:
      return org.exoplatform.portal.mop.SiteType.GROUP;
    default:
      return org.exoplatform.portal.mop.SiteType.USER;
    }
  }
}
