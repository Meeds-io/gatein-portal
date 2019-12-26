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

package org.exoplatform.portal.pom.data;

import java.util.Comparator;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.mop.importer.Imported.Status;

/**
 * Created by The eXo Platform SAS Apr 19, 2007
 *
 * This interface is used to load the PortalConfig, Page config and Navigation config from the database
 */
public interface ModelDataStorage {

    String DEFAULT_SHAREDLAYOUT_PATH = "war:/conf/portal/portal/sharedlayout.xml";

    void create(PortalData config) throws Exception;

    void save(PortalData config) throws Exception;

    PortalData getPortalConfig(PortalKey key) throws Exception;

    void remove(PortalData config) throws Exception;

    PageData getPage(PageKey key) throws Exception;

    /**
     * Saves a page. If a page with the same id already exists then a merge operation will occur, otherwise it throws
     * {@link IllegalStateException}
     *
     * From PLF 5.3.x (RDBMS implementation) we drop support return the change list as it's not used any where.
     * So the method always return the empty list.
     *
     * @param page the page to save
     * @return the list of model changes that occured during the save operation
     * @throws Exception any exception
     */
    List<ModelChange> save(PageData page) throws Exception;

    <S> String getId(ApplicationState<S> state) throws Exception;

    <S> S load(ApplicationState<S> state, ApplicationType<S> type) throws Exception;

    <S> ApplicationState<S> save(ApplicationState<S> state, S preferences) throws Exception;

    <T> LazyPageList<T> find(Query<T> q) throws Exception;

    <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator) throws Exception;

    Container getSharedLayout(String siteName) throws Exception;

    void save() throws Exception;

    /**
     * This method is deprecated as it is for standalone mode but we drop support this feature in PLF 5.3.x (portal RDBMS)
     * So this method will be dropped too. It will always return NULL
     * @param workspaceObjectId
     * @return
     * @throws Exception
     */
    @Deprecated
    String[] getSiteInfo(String workspaceObjectId) throws Exception;

    <S> ApplicationData<S> getApplicationData(String applicationStorageId);

    /****************************************************************
     * Proxy methods of public API to access/modify MOP mixins,
     *
     * temporarily put here
     ***************************************************************/
    @Deprecated
    <A> A adapt(ModelData modelData, Class<A> type);
    @Deprecated
    <A> A adapt(ModelData modelData, Class<A> type, boolean create);

    Status getImportStatus();

    void saveImportStatus(Status status);

}
