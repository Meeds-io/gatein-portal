/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.portal.config;

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
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.jdbc.service.PageServiceImpl;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.ModelChange;
import org.exoplatform.portal.pom.data.ModelData;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PageKey;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author  <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class DataStorageImpl implements DataStorage {

  private static Log             LOG           = ExoLogger.getExoLogger(PageServiceImpl.class);

  private static final int       DEFAULT_LIMIT = 50;

  private ModelDataStorage       delegate;

  private ListenerService        listenerService;

  private Map<String, Container> sharedLayouts = new HashMap<>();

  public DataStorageImpl(ModelDataStorage delegate, ListenerService listenerService) {
    this.delegate = delegate;
    this.listenerService = listenerService;
  }

  public void create(PortalConfig config) {
    delegate.create(config.build());
    broadcastEvent(config, PORTAL_CONFIG_CREATED);
  }

  public void save(PortalConfig config) {
    delegate.save(config.build());
    broadcastEvent(config, PORTAL_CONFIG_UPDATED);
  }

  public void remove(PortalConfig config) {
    delegate.remove(config.build());
    broadcastEvent(config, PORTAL_CONFIG_REMOVED);
  }

  @Override
  public List<String> getSiteNames(SiteType siteType, int offset, int limit) {
    return delegate.getSiteNames(siteType, offset, limit);
  }

  public List<ModelChange> save(Page page) {
    List<ModelChange> changes = delegate.save(page.build());
    broadcastEvent(page, PAGE_UPDATED);
    return changes;
  }

  public <S> S load(ApplicationState<S> state, ApplicationType<S> type) {
    return delegate.load(state, type);
  }

  public <S> ApplicationState<S> save(ApplicationState<S> state, S preferences) {
    return delegate.save(state, preferences);
  }

  public Container getSharedLayout(String siteName) {
    String cacheKey = siteName;
    if (StringUtils.isBlank(cacheKey)) {
      cacheKey = "DEFAULT-SITE";
    }
    if (PropertyManager.isDevelopping() || !sharedLayouts.containsKey(cacheKey)) {
      Container sharedLayout = delegate.getSharedLayout(siteName);
      if (sharedLayout == null && StringUtils.isNotBlank(siteName)) {
        // Return default shared layout if dedicated shared layout wasn't found
        return getSharedLayout(null);
      } else {
        sharedLayouts.put(cacheKey, sharedLayout);
      }
    }
    return sharedLayouts.get(cacheKey);
  }

  public PortalConfig getPortalConfig(String portalName) {
    return getPortalConfig(PortalConfig.PORTAL_TYPE, portalName);
  }

  public Page getPage(String pageId) {
    PageKey key = PageKey.create(pageId);
    PageData data = delegate.getPage(key);
    return data != null ? new Page(data) : null;
  }

  private abstract class Bilto<O extends ModelObject, D extends ModelData> {

    final Query<O>      q;

    final Class<D>      dataType;

    final Comparator<O> cp;

    Bilto(Query<O> q, Class<D> dataType) {
      this.q = q;
      this.dataType = dataType;
      this.cp = null;
    }

    Bilto(Query<O> q, Class<D> dataType, Comparator<O> cp) {
      this.q = q;
      this.dataType = dataType;
      this.cp = cp;
    }

    protected abstract O create(D d);

    ListAccess<O> execute() {
      Query<D> delegateQ = new Query<D>(q, dataType);
      LazyPageList<D> r = delegate.find(delegateQ, null);
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

  @Deprecated(forRemoval = true)
  public List<String> getAllPortalNames() {
    return delegate.getSiteNames(SiteType.PORTAL, 0, DEFAULT_LIMIT);
  }

  @Deprecated(forRemoval = true)
  public List<String> getAllGroupNames() {
    return delegate.getSiteNames(SiteType.GROUP, 0, DEFAULT_LIMIT);
  }

  public <T> LazyPageList<T> find(Query<T> q) {
    return find(q, null);
  }

  public <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator) {
    return new LazyPageList<T>(find2(q, sortComparator), 10);
  }

  public <T> ListAccess<T> find2(Query<T> q) {
    return find2(q, null);
  }

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

  public <S> String getId(ApplicationState<S> state) {
    return delegate.getId(state);
  }

  public PortalConfig getPortalConfig(String ownerType, String portalName) {
    PortalKey key = new PortalKey(ownerType, portalName);
    PortalData data = delegate.getPortalConfig(key);
    return data != null ? new PortalConfig(data) : null;
  }

  public <S> Application<S> getApplicationModel(String applicationStorageId) {
    ApplicationData<S> applicationData = delegate.getApplicationData(applicationStorageId);
    return new Application<>(applicationData);
  }

  @Override
  public Status getImportStatus() {
    return delegate.getImportStatus();
  }

  @Override
  public void saveImportStatus(Status status) {
    delegate.saveImportStatus(status);
  }

  private void broadcastEvent(Object data, String eventName) {
    try {
      listenerService.broadcast(eventName, this, data);
    } catch (Exception e) {
      LOG.warn("Error while broadcasting event {} on config {}. Operation will continue processing.", eventName, data, e);
    }
  }

}
