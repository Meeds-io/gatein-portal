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
import java.util.LinkedList;
import java.util.List;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Dashboard;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.DashboardData;
import org.exoplatform.portal.pom.data.ModelChange;
import org.exoplatform.portal.pom.data.ModelData;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PageKey;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.ListenerService;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class DataStorageImpl implements DataStorage {
    /** . */
    private ModelDataStorage delegate;

    private ListenerService listenerServ_;

    private Container sharedLayout = null;

    protected ExoCache<PortalKey, PortalData> portalConfigCache;

    protected FutureExoCache<PortalKey, PortalData, Object> futurePortalConfigCache;

    public DataStorageImpl(CacheService cacheService, ModelDataStorage delegate, ListenerService listenerServ) {
        this.delegate = delegate;
        this.listenerServ_ = listenerServ;

        this.portalConfigCache = cacheService.getCacheInstance("portal.PortalConfig");
        this.futurePortalConfigCache = new FutureExoCache<>(new Loader<PortalKey, PortalData, Object>() {
          @Override
          public PortalData retrieve(Object context, PortalKey key) throws Exception {
            PortalData portalConfig = delegate.getPortalConfig(key);
            return portalConfig == null ? PortalData.NULL_OBJECT : portalConfig;
          }
        }, portalConfigCache);
    }

    /**
     * @deprecated replaced by
     *             {@link PageService#clone(org.exoplatform.portal.mop.page.PageKey, org.exoplatform.portal.mop.page.PageKey)}
     *
     */
    public Page clonePage(String pageId, String clonedOwnerType, String clonedOwnerId, String clonedName) {
        throw new UnsupportedOperationException();
    }

    public void create(PortalConfig config) throws Exception {
        // Clear cache entry since this method can be called even to update an existing
        // Portal Config
        PortalKey key = new PortalKey(config.getType(), config.getName());
        delegate.create(config.build());
        portalConfigCache.remove(key);
        listenerServ_.broadcast(PORTAL_CONFIG_CREATED, this, config);
    }

    public void save(PortalConfig config) throws Exception {
        PortalKey key = new PortalKey(config.getType(), config.getName());
        delegate.save(config.build());
        portalConfigCache.remove(key);
        listenerServ_.broadcast(PORTAL_CONFIG_UPDATED, this, config);
    }

    public void remove(PortalConfig config) throws Exception {
        PortalKey key = new PortalKey(config.getType(), config.getName());
        portalConfigCache.remove(key);
        delegate.remove(config.build());
        listenerServ_.broadcast(PORTAL_CONFIG_REMOVED, this, config);
    }

    public void create(Page page) {
        throw new UnsupportedOperationException();
    }

    public List<ModelChange> save(Page page) throws Exception {
        List<ModelChange> changes = delegate.save(page.build());
        listenerServ_.broadcast(PAGE_UPDATED, this, page);
        return changes;
    }

    /**
     * @deprecated replaced by {@link PageService#destroyPage(org.exoplatform.portal.mop.page.PageKey)}
     *
     */
    public void remove(Page page) {
        throw new UnsupportedOperationException();
    }

    public <S> S load(ApplicationState<S> state, ApplicationType<S> type) throws Exception {
        return delegate.load(state, type);
    }

    public <S> ApplicationState<S> save(ApplicationState<S> state, S preferences) throws Exception {
        return delegate.save(state, preferences);
    }

    public Container getSharedLayout() throws Exception {
        if (PropertyManager.isDevelopping() || sharedLayout == null) {
          sharedLayout = delegate.getSharedLayout();
        }
        return sharedLayout;
    }

    public PortalConfig getPortalConfig(String portalName) throws Exception {
        return getPortalConfig(PortalConfig.PORTAL_TYPE, portalName);
    }

    public Page getPage(String pageId) throws Exception {
        PageKey key = PageKey.create(pageId);
        PageData data = delegate.getPage(key);
        return data != null ? new Page(data) : null;
    }

    private abstract class Bilto<O extends ModelObject, D extends ModelData> {

        final Query<O> q;

        final Class<D> dataType;

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

        ListAccess<O> execute() throws Exception {
            Query<D> delegateQ = new Query<D>(q, dataType);
            LazyPageList<D> r = delegate.find(delegateQ, null);
            List<D> tmp = r.getAll();
            tmp = sort(tmp, this.cp);
            final List<D> list = tmp;
            return new ListAccess<O>() {
                public int getSize() throws Exception {
                    return list.size();
                }

                public O[] load(int index, int length) throws Exception {
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
                List<D> tmpList = new ArrayList<D>();
                for (int i = 0; i < list.size(); i++) {
                    tmpList.add(list.get(i));
                }
                Collections.sort(tmpList, new Comparator<D>() {
                    public int compare(D d1, D d2) {
                        O o1 = create(d1);
                        O o2 = create(d2);
                        return comparator.compare(o1, o2);
                    }

                });
                return tmpList;
            } else {
                return list;
            }
        }
    }

    public List<String> getAllPortalNames() throws Exception {

        Query<PortalKey> q = new Query<PortalKey>("portal", null, PortalKey.class);
        List<PortalKey> keys = delegate.find(q).getAll();
        LinkedList<String> list = new LinkedList<String>();
        for (PortalKey key : keys) {
            list.add(key.getId());
        }
        return list;
    }

    public List<String> getAllGroupNames() throws Exception {

        Query<PortalKey> q = new Query<PortalKey>("group", null, PortalKey.class);
        List<PortalKey> keys = delegate.find(q).getAll();
        LinkedList<String> list = new LinkedList<String>();
        for (PortalKey key : keys) {
            list.add(key.getId());
        }
        return list;
    }

    public <T> ListAccess<T> find2(Query<T> q) throws Exception {
        return find2(q, null);
    }

    public <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator) throws Exception {
        return new LazyPageList<T>(find2(q, sortComparator), 10);
    }

    public <T> ListAccess<T> find2(Query<T> q, Comparator<T> sortComparator) throws Exception {
        Class<T> type = q.getClassType();
        if (type == Page.class) {
            Bilto<Page, PageData> bilto = new Bilto<Page, PageData>((Query<Page>) q, PageData.class,
                    (Comparator<Page>) sortComparator) {
                @Override
                protected Page create(PageData pageData) {
                    return new Page(pageData);
                }
            };
            return (ListAccess<T>) bilto.execute();
        } else if (type == PortalConfig.class) {
            Bilto<PortalConfig, PortalData> bilto = new Bilto<PortalConfig, PortalData>((Query<PortalConfig>) q,
                    PortalData.class, (Comparator<PortalConfig>) sortComparator) {
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

    public <T> LazyPageList<T> find(Query<T> q) throws Exception {
        return find(q, null);
    }

    public <S> String getId(ApplicationState<S> state) throws Exception {
        return delegate.getId(state);
    }

    public void save() throws Exception {
        delegate.save();
    }

    public PortalConfig getPortalConfig(String ownerType, String portalName) throws Exception {
      PortalKey key = new PortalKey(ownerType, portalName);
      PortalData data = futurePortalConfigCache.get(null, key);
      return data != null && data.getKey() != null ? new PortalConfig(data) : null;
    }

    public Dashboard loadDashboard(String dashboardId) throws Exception {
        DashboardData data = delegate.loadDashboard(dashboardId);
        return data != null ? new Dashboard(data) : null;
    }

    public void saveDashboard(Dashboard dashboard) throws Exception {
        delegate.saveDashboard(dashboard.build());
    }

    public <A> A adapt(ModelObject modelObject, Class<A> type) {
        return delegate.adapt(modelObject.build(), type);
    }

    public <A> A adapt(ModelObject modelObject, Class<A> type, boolean create) {
        return delegate.adapt(modelObject.build(), type, create);
    }

    public String[] getSiteInfo(String applicationStorageId) throws Exception {
        // TODO Auto-generated method stub
        return delegate.getSiteInfo(applicationStorageId);
    }

    public <S> Application<S> getApplicationModel(String applicationStorageId) throws Exception {
        // TODO Auto-generated method stub
        try {
            ApplicationData<S> applicationData = delegate.getApplicationData(applicationStorageId);
            return new Application<S>(applicationData);
        } catch (NoSuchDataException ex) {
            // TODO: Throw something else
            throw ex;
        }
    }
}
