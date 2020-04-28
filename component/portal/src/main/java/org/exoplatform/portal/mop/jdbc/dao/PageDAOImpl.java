/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.portal.mop.jdbc.dao;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.*;
import javax.persistence.criteria.*;

import org.gatein.api.common.Pagination;
import org.gatein.api.page.PageQuery;
import org.gatein.api.site.SiteType;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.EntityManagerHolder;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.jdbc.entity.*;
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
  public ListAccess<PageEntity> findByQuery(PageQuery query) {
    TypedQuery<PageEntity> q = buildQuery(query);
    final List<PageEntity> results = q.getResultList();

    return new ListAccess<PageEntity>() {

      @Override
      public int getSize() throws Exception {
        return results.size();
      }

      @Override
      public PageEntity[] load(int offset, int limit) throws Exception {
        if (limit < 0) {
          limit = getSize();
        }
        return results.subList(offset, offset + limit).toArray(new PageEntity[limit]);
      }
    };
  }

  public TypedQuery<PageEntity> buildQuery(PageQuery query) {
    EntityManager em = EntityManagerHolder.get();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<PageEntity> criteria = cb.createQuery(PageEntity.class);
    Root<PageEntity> pageEntity = criteria.from(PageEntity.class);
    Join<PageEntity, SiteEntity> join = pageEntity.join("owner");

    //
    CriteriaQuery<PageEntity> select = criteria.select(pageEntity);
    select.distinct(true);

    List<Predicate> predicates = new LinkedList<>();

    if (query.getSiteType() != null) {
      predicates.add(cb.equal(join.get("siteType"), convertSiteType(query.getSiteType())));
    }
    if (query.getSiteName() != null) {
      predicates.add(cb.equal(join.get("name"), query.getSiteName()));
    }

    if (query.getDisplayName() != null) {
      predicates.add(cb.like(cb.lower(pageEntity.get("displayName")),
                             "%" + query.getDisplayName().toLowerCase() + "%"));
    }

    select.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));

    //
    TypedQuery<PageEntity> typedQuery = em.createQuery(select);
    Pagination pagination = query.getPagination();
    if (pagination != null && pagination.getLimit() > 0) {
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
