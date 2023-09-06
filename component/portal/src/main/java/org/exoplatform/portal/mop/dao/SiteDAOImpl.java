package org.exoplatform.portal.mop.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

public class SiteDAOImpl extends AbstractDAO<SiteEntity> implements SiteDAO {

  private static final String        SITE_NAME                = "name";

  private static final String        SITE_TYPE                = "siteType";

  private static final String        DISPLAYED                = "displayed";

  private static final String        SPACE_SITE_TYPE_PREFIX   = "/spaces/";

  private static final String        QUERY_FILTER_FIND_PREFIX = "SiteEntity.findSites";

  private final Map<String, Boolean> filterNamedQueries       = new HashMap<>();

  private PageDAO                    pageDAO;

  private NavigationDAO              navDAO;

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
    query.setParameter(SITE_NAME, siteKey.getName());
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
  public List<SiteEntity> findSites(SiteFilter siteFilter) {
    TypedQuery<SiteEntity> query = buildQueryFromFilter(siteFilter);
    if (siteFilter.getOffset() > 0) {
      query.setFirstResult(siteFilter.getOffset());
    }
    if (siteFilter.getLimit() > 0) {
      query.setMaxResults(siteFilter.getLimit());
    }
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

  private <T> TypedQuery<T> buildQueryFromFilter(SiteFilter filter) {
    List<String> suffixes = new ArrayList<>();
    List<String> predicates = new ArrayList<>();
    buildPredicates(filter, suffixes, predicates);

    TypedQuery<T> query;
    String queryName = getQueryFilterName(suffixes);
    if (filterNamedQueries.containsKey(queryName)) {
      query = (TypedQuery<T>) getEntityManager().createNamedQuery(queryName, SiteEntity.class);
    } else {
      String queryContent = getQueryFilterContent(predicates);
      query = (TypedQuery<T>) getEntityManager().createQuery(queryContent, SiteEntity.class);
      getEntityManager().getEntityManagerFactory().addNamedQuery(queryName, query);
      filterNamedQueries.put(queryName, true);
    }

    addQueryFilterParameters(filter, query);
    return query;
  }

  private void buildPredicates(SiteFilter filter, List<String> suffixes, List<String> predicates) {
    if (filter.getSiteType() != null) {
      suffixes.add("Type");
      predicates.add("s.siteType = :siteType");
    }
    if (!filter.isAllSites()) {
      suffixes.add("Displayed");
      predicates.add("s.displayed = :displayed");
    }
    if (StringUtils.isNotBlank(filter.getExcludedSiteName())) {
      suffixes.add("Excluding" + filter.getExcludedSiteName());
      predicates.add("s.name != :name");
    }
  }

  private String getQueryFilterName(List<String> suffixes) {
    return suffixes.isEmpty() ? QUERY_FILTER_FIND_PREFIX : QUERY_FILTER_FIND_PREFIX + "By" + StringUtils.join(suffixes, "And");
  }

  private <T> void addQueryFilterParameters(SiteFilter filter, TypedQuery<T> query) {
    if (filter.getSiteType() != null) {
      query.setParameter(SITE_TYPE, filter.getSiteType());
    }
    if (!filter.isAllSites()) {
      query.setParameter(DISPLAYED, filter.isDisplayed());
    }
    if (StringUtils.isNotBlank(filter.getExcludedSiteName())) {
      query.setParameter(SITE_NAME, filter.getExcludedSiteName());
    }
  }

  private String getQueryFilterContent(List<String> predicates) {
    String querySelect = "SELECT s FROM GateInSite s ";

    String queryContent;
    if (predicates.isEmpty()) {
      queryContent = querySelect;
    } else {
      queryContent = querySelect + " WHERE " + StringUtils.join(predicates, " AND ");
    }
    queryContent +=  " ORDER BY s.displayOrder ASC, s.label ASC";
    return queryContent;
  }
}
