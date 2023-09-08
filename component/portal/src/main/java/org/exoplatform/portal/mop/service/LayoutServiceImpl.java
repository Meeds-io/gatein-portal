/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.portal.mop.service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.storage.LayoutStorage;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.mop.storage.SiteStorage;
import org.exoplatform.portal.pom.data.ModelChange;
import org.exoplatform.portal.pom.data.ModelData;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class LayoutServiceImpl implements LayoutService {

  private static final Log       LOG           = ExoLogger.getLogger(LayoutServiceImpl.class);

  private ListenerService        listenerService;

  private SiteStorage            siteStorage;

  private PageStorage            pageStorage;

  private LayoutStorage          layoutStorage;

  private UserACL                userACL;

  private Map<String, Container> sharedLayouts = new HashMap<>();

  public LayoutServiceImpl(ListenerService listenerService,
                           SiteStorage siteStorage,
                           PageStorage pageStorage,
                           LayoutStorage layoutStorage,
                           UserACL userACL) {
    this.listenerService = listenerService;
    this.siteStorage = siteStorage;
    this.pageStorage = pageStorage;
    this.layoutStorage = layoutStorage;
    this.userACL = userACL;
  }

  @Override
  public void create(PortalConfig config) {
    siteStorage.create(config.build());
    broadcastEvent(PORTAL_CONFIG_CREATED, config);
  }

  @Override
  public void save(PortalConfig config) {
    siteStorage.save(config);
    broadcastEvent(PORTAL_CONFIG_UPDATED, config);
  }

  @Override
  public void remove(PortalConfig config) {
    siteStorage.remove(config);
    broadcastEvent(PORTAL_CONFIG_REMOVED, config);
  }

  @Override
  public PortalConfig getPortalConfig(String portalName) {
    return getPortalConfig(PortalConfig.PORTAL_TYPE, portalName);
  }

  @Override
  public PortalConfig getPortalConfig(SiteKey siteKey) {
    PortalData portalData = siteStorage.getPortalConfig(siteKey);
    return portalData == null ? null : new PortalConfig(portalData);
  }

  @Override
  public PortalConfig getPortalConfig(long siteId) {
    PortalData portalData = siteStorage.getPortalConfig(siteId);
    return portalData == null ? null : new PortalConfig(portalData);
  }

  @Override
  public PortalConfig getPortalConfig(String ownerType, String portalName) {
    return siteStorage.getPortalConfig(ownerType, portalName);
  }

  @Override
  public List<String> getSiteNames(SiteType siteType, int offset, int limit) {
    return siteStorage.getSiteNames(siteType, offset, limit);
  }

  @Override
  public void create(Page page) {
    pageStorage.save(page.build());
    broadcastEvent(PAGE_CREATED, page);
  }

  @Override
  public List<ModelChange> save(Page page) {
    pageStorage.save(page.build());
    broadcastEvent(PAGE_UPDATED, page);
    // Not used, kept as is to not break the API definition
    return Collections.<ModelChange> emptyList();
  }

  @Override
  public void save(PageContext pageContext, Page page) {
    pageStorage.savePage(pageContext);
    pageStorage.save(page.build());
    broadcastEvent(PAGE_UPDATED, page);
  }

  @Override
  public void save(PageContext pageContext) {
    pageStorage.savePage(pageContext);
    broadcastEvent(PAGE_UPDATED, pageStorage.getPage(pageContext.getKey()));
  }

  @Override
  public void removePages(SiteKey siteKey) {
    List<PageContext> pages = pageStorage.loadPages(siteKey);
    pages.forEach(context -> remove(context.getKey()));
  }

  @Override
  public void remove(PageKey pageKey) {
    Page page = pageStorage.getPage(pageKey);
    remove(page);
  }

  @Override
  public void remove(Page page) {
    pageStorage.destroyPage(page.getPageKey());
  }

  @Override
  public Page getPage(String pageId) {
    return pageStorage.getPage(pageId);
  }

  @Override
  public Page getPage(PageKey pageKey) {
    return pageStorage.getPage(pageKey);
  }

  @Override
  public PageContext getPageContext(PageKey pageKey) {
    return pageStorage.loadPage(pageKey);
  }

  @Override
  public List<PageContext> findPages(SiteKey siteKey) {
    return pageStorage.loadPages(siteKey);
  }

  @Override
  public QueryResult<PageContext> findPages(int offset,
                                            int limit,
                                            SiteType siteKey,
                                            String siteName,
                                            String pageName,
                                            String pageTitle) {
    return pageStorage.findPages(offset, limit, siteKey, siteName, pageName, pageTitle);
  }

  @Override
  public <S> S load(ApplicationState<S> state, ApplicationType<S> type) {
    return layoutStorage.load(state, type);
  }

  @Override
  public <S> ApplicationState<S> save(ApplicationState<S> state, S preferences) {
    return layoutStorage.save(state, preferences);
  }

  @Override
  public <S> String getId(ApplicationState<S> state) {
    return layoutStorage.getId(state);
  }

  @Override
  public <S> Application<S> getApplicationModel(String applicationStorageId) {
    return layoutStorage.getApplicationModel(applicationStorageId);
  }

  @Override
  public Container getSharedLayout(String siteName) {
    String cacheKey = siteName;
    if (StringUtils.isBlank(cacheKey)) {
      cacheKey = "DEFAULT-SITE";
    }
    if (PropertyManager.isDevelopping() || !sharedLayouts.containsKey(cacheKey)) {
      Container sharedLayout = siteStorage.getSharedLayout(siteName);
      if (sharedLayout == null && StringUtils.isNotBlank(siteName)) {
        // Return default shared layout if dedicated shared layout wasn't found
        return getSharedLayout(null);
      } else {
        sharedLayouts.put(cacheKey, sharedLayout);
      }
    }
    return sharedLayouts.get(cacheKey);
  }

  @Override
  public Status getImportStatus() {
    return siteStorage.getImportStatus();
  }

  @Override
  public void saveImportStatus(Status status) {
    siteStorage.saveImportStatus(status);
  }

  @Override
  public <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator) {
    return new LazyPageList<>(find2(q, sortComparator), 10);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> ListAccess<T> find2(Query<T> q, Comparator<T> sortComparator) {
    Class<T> type = q.getClassType();
    if (type == Page.class) {
      Bilto<Page, PageData> bilto = new Bilto<Page, PageData>((Query<Page>) q,
                                                              PageData.class,
                                                              (Comparator<Page>) sortComparator) {
        @Override
        protected Page create(PageData pageData) {
          return new Page(pageData);
        }
      };
      return (ListAccess<T>) bilto.execute();
    } else if (type == PortalConfig.class) {
      Bilto<PortalConfig, PortalData> bilto = new Bilto<PortalConfig, PortalData>((Query<PortalConfig>) q,
                                                                                  PortalData.class,
                                                                                  (Comparator<PortalConfig>) sortComparator) {
        @Override
        protected PortalConfig create(PortalData portalData) {
          return new PortalConfig(portalData);
        }
      };
      return (ListAccess<T>) bilto.execute();
    } else {
      throw new UnsupportedOperationException("Cannot query type " + type);
    }
  }

  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  public <T> LazyPageList<T> findLazyPageList(Query<T> q) { // NOSONAR
    Class<T> type = q.getClassType();
    if (PageData.class.equals(type)) {
      throw new UnsupportedOperationException("Use PageService.findPages to instead of");
    } else if (PortletPreferences.class.equals(type)) {
      // this task actually return empty portlet preferences
      return new LazyPageList<>(new ListAccess<T>() {
        public T[] load(int index, int length) throws Exception {
          throw new AssertionError();
        }

        public int getSize() throws Exception {
          return 0;
        }
      }, 10);
    } else if (PortalData.class.equals(type)) {
      String ownerType = q.getOwnerType();

      ListAccess<PortalData> la = new ListAccess<PortalData>() {

        private int          cacheOffset;

        private int          cacheLimit;

        private List<String> siteNames;

        public PortalData[] load(int offset, int limit) throws Exception {
          SiteType siteType = ownerType == null ? SiteType.PORTAL : SiteType.valueOf(ownerType.toUpperCase());
          return getSiteNames(offset, limit).stream()
                                            .map(siteName -> siteStorage.getPortalConfig(new SiteKey(siteType.getName(), siteName)))
                                            .toList()
                                            .toArray(new PortalData[siteNames.size()]);
        }

        public int getSize() throws Exception {
          return getSiteNames(0, -1).size();
        }

        private List<String> getSiteNames(int offset, int limit) {
          if (siteNames == null || cacheOffset != offset || cacheLimit != limit) {
            SiteType siteType = ownerType == null ? SiteType.PORTAL : SiteType.valueOf(ownerType.toUpperCase());
            siteNames = siteStorage.getSiteNames(siteType, offset, limit);
            cacheOffset = offset;
            cacheLimit = limit;
          }
          return siteNames;
        }
      };
      return new LazyPageList(la, 10);
    } else if (PortalKey.class.equals(type) && ("portal".equals(q.getOwnerType()) || "group".equals(q.getOwnerType()))) {
      String ownerType = q.getOwnerType();
      ListAccess<PortalKey> la = new ListAccess<PortalKey>() {
        private List<String> siteNames;

        public PortalKey[] load(int offset, int limit) throws Exception {
          return getSiteNames(offset, limit).stream()
                                            .map(siteName -> new PortalKey(ownerType, siteName))
                                            .toList()
                                            .toArray(new PortalKey[siteNames.size()]);
        }

        public int getSize() throws Exception {
          return getSiteNames(0, -1).size();
        }

        private List<String> getSiteNames(int offset, int limit) {
          if (siteNames == null) {
            SiteType siteType = SiteType.PORTAL;
            if (ownerType != null) {
              siteType = SiteType.valueOf(ownerType.toUpperCase());
            }
            siteNames = siteStorage.getSiteNames(siteType, offset, limit);
          }
          return siteNames;
        }
      };
      return new LazyPageList(la, 10);
    } else {
      throw new UnsupportedOperationException("Could not perform search on query " + q);
    }
  }

  @Override
  public List<PortalConfig> getSites(SiteFilter filter) {
    List<PortalData> portalDataList = siteStorage.getSites(filter);
    return portalDataList.isEmpty() ? Collections.emptyList()
                                                            : portalDataList.stream().map(PortalConfig::new).toList();
  }

  private abstract class Bilto<O extends ModelObject, D extends ModelData> {

    final Query<O>      q;

    final Class<D>      dataType;

    final Comparator<O> cp;

    Bilto(Query<O> q, Class<D> dataType, Comparator<O> cp) {
      this.q = q;
      this.dataType = dataType;
      this.cp = cp;
    }

    protected abstract O create(D d);

    ListAccess<O> execute() {
      Query<D> delegateQ = new Query<>(q, dataType);
      LazyPageList<D> r = findLazyPageList(delegateQ);
      List<D> tmp;
      try {
        tmp = r.getAll();
      } catch (Exception e) {
        throw new IllegalStateException("Error retrieving element with query " + q, e);
      }
      tmp = sort(tmp, this.cp);
      final List<D> list = tmp;
      return new ListAccess<O>() {
        public int getSize() {
          return list.size();
        }

        @SuppressWarnings("unchecked")
        public O[] load(int index, int length) {
          O[] pages = (O[]) Array.newInstance(q.getClassType(), length);
          int i = 0;
          for (D data : list.subList(index, index + length)) {
            pages[i++] = create(data);
          }
          return pages;
        }
      };
    }

    private List<D> sort(List<D> list, final Comparator<O> comparator) {
      if (comparator != null) {
        List<D> tmpList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
          tmpList.add(list.get(i));
        }
        Collections.sort(tmpList, (D d1, D d2) -> {
          O o1 = create(d1);
          O o2 = create(d2);
          return comparator.compare(o1, o2);
        });
        return tmpList;
      } else {
        return list;
      }
    }
  }

  protected void broadcastEvent(String eventName, Object data) {
    try {
      listenerService.broadcast(eventName, this, data);
    } catch (Exception e) {
      LOG.warn("Error when broadcasting notification " + eventName + " for " + data, e);
    }
  }
}
