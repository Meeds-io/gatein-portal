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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

public class SiteDAOImpl extends AbstractDAO<SiteEntity> implements SiteDAO {
  private PageDAO pageDAO;
  
  private NavigationDAO navDAO;
  
  public SiteDAOImpl(PageDAO pageDAO, NavigationDAO navDAO) {
    super();
    this.pageDAO = pageDAO;
    this.navDAO = navDAO;
  }

  @Override
  @ExoTransactional
  public void delete(SiteEntity entity) {
    navDAO.deleteByOwner(entity.getSiteType(), entity.getName());
    pageDAO.deleteByOwner(entity.getId());
    super.delete(entity);
  }

  @Override
  @ExoTransactional
  public void deleteAll(List<SiteEntity> entities) {
    for (SiteEntity entity : entities) {
      delete(entity);
    }
  }

  @Override
  @ExoTransactional
  public void deleteAll() {
    List<SiteEntity> entities = findAll();

    for (SiteEntity entity : entities) {
      delete(entity);
    }
  }

  @Override
  public SiteEntity findByKey(SiteKey siteKey) {
    TypedQuery<SiteEntity> query = getEntityManager().createNamedQuery("SiteEntity.findByKey", SiteEntity.class);

    query.setParameter("siteType", siteKey.getType());
    query.setParameter("name", siteKey.getName());
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public List<SiteEntity> findByType(SiteType siteType) {
    TypedQuery<SiteEntity> query = getEntityManager().createNamedQuery("SiteEntity.findByType", SiteEntity.class);
    query.setParameter("siteType", siteType);

    return query.getResultList();
  }

  @Override
  public List<SiteKey> findSiteKey(SiteType siteType) {
    List<SiteKey> keys = new ArrayList<>();

    TypedQuery<String> query = getEntityManager().createNamedQuery("SiteEntity.findSiteKey", String.class);
    query.setParameter("siteType", siteType);

    for (String name : query.getResultList()) {
      keys.add(new SiteKey(siteType, name));
    }
    return keys;
  }

}
