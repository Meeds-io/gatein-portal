package org.exoplatform.portal.mop.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

public class SiteDAOImpl extends AbstractDAO<SiteEntity> implements SiteDAO {

  private static final String NAME                   = "name";

  private static final String SITE_TYPE              = "siteType";

  private static final String SPACE_SITE_TYPE_PREFIX = "/spaces/";

  private PageDAO             pageDAO;

  private NavigationDAO       navDAO;

  public SiteDAOImpl(PageDAO pageDAO, NavigationDAO navDAO) {
    super();
    this.pageDAO = pageDAO;
    this.navDAO = navDAO;
  }

  @Override
  @ExoTransactional
  public void deleteAll(List<SiteEntity> entities) {
    for (SiteEntity entity : entities) {
      navDAO.deleteByOwner(entity.getSiteType(), entity.getName());
      pageDAO.deleteByOwner(entity.getId());
      delete(entity);
    }
  }

  @Override
  @ExoTransactional
  public void deleteAll() {
    List<SiteEntity> entities = findAll();

    for (SiteEntity entity : entities) {
      navDAO.deleteByOwner(entity.getSiteType(), entity.getName());
      pageDAO.deleteByOwner(entity.getId());
      delete(entity);
    }
  }

  @Override
  public SiteEntity findByKey(SiteKey siteKey) {
    TypedQuery<SiteEntity> query = getEntityManager().createNamedQuery("SiteEntity.findByKey", SiteEntity.class);

    query.setParameter(SITE_TYPE, siteKey.getType());
    query.setParameter(NAME, siteKey.getName());
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  @Override
  public List<SiteEntity> findByType(SiteType siteType) {
    TypedQuery<SiteEntity> query = getEntityManager().createNamedQuery("SiteEntity.findByType", SiteEntity.class);
    query.setParameter(SITE_TYPE, siteType);

    return query.getResultList();
  }

  @Override
  public List<SiteKey> findSiteKey(SiteType siteType) {
    List<SiteKey> keys = new ArrayList<>();

    TypedQuery<String> query = getEntityManager().createNamedQuery("SiteEntity.findSiteKey", String.class);
    query.setParameter(SITE_TYPE, siteType);

    for (String name : query.getResultList()) {
      keys.add(new SiteKey(siteType, name));
    }
    return keys;
  }

  @Override
  public List<String> findGroupSites(int offset, int limit) {
    TypedQuery<String> query = getEntityManager().createNamedQuery("SiteEntity.findGroupSites", String.class);
    query.setParameter(SITE_TYPE, SiteType.GROUP);
    query.setParameter("excludeName", SPACE_SITE_TYPE_PREFIX);
    if (offset > 0) {
      query.setFirstResult(offset);
    }
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  @Override
  public List<String> findSpaceSites(int offset, int limit) {
    TypedQuery<String> query = getEntityManager().createNamedQuery("SiteEntity.findSpaceSites", String.class);
    query.setParameter(SITE_TYPE, SiteType.GROUP);
    query.setParameter("includeName", SPACE_SITE_TYPE_PREFIX + "%");
    if (offset > 0) {
      query.setFirstResult(offset);
    }
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  @Override
  public List<String> findPortalSites(int offset, int limit) {
    return getSiteNames(SiteType.PORTAL, offset, limit);
  }

  @Override
  public List<String> findUserSites(int offset, int limit) {
    return getSiteNames(SiteType.USER, offset, limit);
  }

  @Override
  public List<SiteEntity> findAPortalSitesOrderedByDisplayOrder() {
    TypedQuery<SiteEntity> query = getEntityManager().createNamedQuery("SiteEntity.findPortalSitesOrderedByDisplayOrder", SiteEntity.class);
    query.setParameter(SITE_TYPE, SiteType.PORTAL);
    List<SiteEntity> resultList = query.getResultList();
    return resultList == null ? Collections.emptyList() : resultList;
  }

  private List<String> getSiteNames(SiteType siteType, int offset, int limit) {
    TypedQuery<String> query = getEntityManager().createNamedQuery("SiteEntity.findPortalSites", String.class);
    query.setParameter(SITE_TYPE, siteType);
    if (offset > 0) {
      query.setFirstResult(offset);
    }
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

}
