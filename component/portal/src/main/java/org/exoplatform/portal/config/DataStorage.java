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

import java.util.Comparator;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.importer.Imported.Status;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.pom.config.tasks.PreferencesTask;
import org.exoplatform.portal.pom.data.ModelChange;

/**
 * Created by The eXo Platform SAS Apr 19, 2007
 *
 * This interface is used to load the PortalConfig, Page config and Navigation config from the database
 */
public interface DataStorage {
    String PAGE_CREATED = "org.exoplatform.portal.config.DataStorage.pageCreated".intern();

    String PAGE_REMOVED = "org.exoplatform.portal.config.DataStorage.pageRemoved".intern();

    String PAGE_UPDATED = "org.exoplatform.portal.config.DataStorage.pageUpdated".intern();

    String PORTAL_CONFIG_CREATED = "org.exoplatform.portal.config.DataStorage.portalConfigCreated".intern();

    String PORTAL_CONFIG_REMOVED = "org.exoplatform.portal.config.DataStorage.portalConfigRemoved".intern();

    String PORTAL_CONFIG_UPDATED = "org.exoplatform.portal.config.DataStorage.portalConfigUpdated".intern();

    /**
     * Create a PortalConfig in database <br>
     * Then broadcast PORTAL_CONFIG_CREATED event
     *
     * @param config
     */
    void create(PortalConfig config) throws Exception;

    /**
     * This method should update the PortalConfig object <br>
     * Then broadcast PORTAL_CONFIG_UPDATED event
     *
     * @param config
     */
    void save(PortalConfig config) throws Exception;

    /**
     * This method should load the PortalConfig object from db according to the portalName
     *
     * @param portalName
     */
    PortalConfig getPortalConfig(String portalName) throws Exception;

    /**
     * This method should load the PortalConfig object from db according to the portalName and ownerType
     *
     * @param portalName
     * @param ownerType
     */
    PortalConfig getPortalConfig(String ownerType, String portalName) throws Exception;

    /**
     * Remove the PortalConfig from the database <br>
     * Then broadcast PORTAL_CONFIG_REMOVED event
     *
     * @param config
     * @throws Exception
     */
    void remove(PortalConfig config) throws Exception;

    /**
     * This method should load the Page object from the database according to the pageId
     *
     * @param pageId - String represent id of page, it must be valid pageId (3 parts saparate by :: )
     */
    Page getPage(String pageId) throws Exception;

    /**
     * @deprecated replaced by
     *             {@link PageService#clone(org.exoplatform.portal.mop.page.PageKey, org.exoplatform.portal.mop.page.PageKey)}
     *
     */
    @Deprecated
    Page clonePage(String pageId, String clonedOwnerType, String clonedOwnerId, String clonedName);

    /**
     * @deprecated replaced by {@link PageService#destroyPage(org.exoplatform.portal.mop.page.PageKey)}
     */
    @Deprecated
    void remove(Page page);

    /**
     * This method should create or udate the given page object <br>
     * Then broasdcast PAGE_CREATED event
     *
     * @deprecated This is replaced by {@link PageService#savePage(org.exoplatform.portal.mop.page.PageContext)}
     *
     * @param page
     */
    @Deprecated
    void create(Page page);

    /**
     * Saves a page. If a page with the same id already exists then a merge operation will occur, otherwise it throws
     * {@link IllegalStateException} <br>
     *
     * The operation returns a list of the change object that describes the changes that occured during the save operation. <br>
     *
     * Then broadcast PAGE_UPDATED event
     *
     * @param page the page to save
     * @return the list of model changes that occured during the save operation
     * @throws Exception any exception
     */
    List<ModelChange> save(Page page) throws Exception;

    /**
     * Return contentId according to each state (transient, persitent, clone)
     *
     * @param state
     */
    <S> String getId(ApplicationState<S> state) throws Exception;

    /**
     * Return content state. If can't find, return null
     *
     * @see PreferencesTask
     * @param state - ApplicationState object
     * @param type - ApplicationType object
     */
    <S> S load(ApplicationState<S> state, ApplicationType<S> type) throws Exception;

    /**
     * Save content state <br>
     *
     * @param state - ApplicationState object. It must be CloneApplicationState or PersistentApplicationState object
     * @param preferences - object to be saved
     */
    <S> ApplicationState<S> save(ApplicationState<S> state, S preferences) throws Exception;

    /**
     * Return LazyPageList of object (unsorted) which type and other info determined in Query object
     *
     * @param q - Query object
     */
    <T> LazyPageList<T> find(Query<T> q) throws Exception;

    /**
     * Return LazyPageList of object (sorted) which type and other info determined in Query object
     *
     * @param q - Query object
     */
    <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator) throws Exception;

    /**
     * Return ListAccess, we can retrieved array of object (unsorted) in database through this.
     *
     * @param q - Query object
     */
    <T> ListAccess<T> find2(Query<T> q) throws Exception;

    /**
     * Return ListAccess, we can retrieved array of object (sorted) in database through this.
     *
     * @param q - Query object
     * @param sortComparator - Comparator object, used to sort the result list
     */
    <T> ListAccess<T> find2(Query<T> q, Comparator<T> sortComparator) throws Exception;

    /**
     * Return Container object - info that be used to build this Container is retrieved from
     * /conf/portal/portal/sharedlayout.xml
     */
    Container getSharedLayout() throws Exception;

    void save() throws Exception;

    /**
     * Returns the list of all portal names.
     *
     * @return the portal names
     * @throws Exception any exception
     */
    List<String> getAllPortalNames() throws Exception;

    List<String> getAllGroupNames() throws Exception;

    /**
     * This method is deprecated ad it's for the standalone feature but we will drop this feature in PLF 5.3.x (portal RDBMS)
     * so this method will always return NULL from PLF 5.3.x
     *
     * Returns a String array that contains two elements. The first one is the site type and the second one is site name. <br>
     *
     * @param applicationStorageId
     * @return
     * @throws Exception
     */
    @Deprecated
    String[] getSiteInfo(String applicationStorageId) throws Exception;

    <S> Application<S> getApplicationModel(String applicationStorageId) throws Exception;

    /*************************************************************
     * Public API to access/modify MOP mixin, temporarily put here
     **************************************************************/

    @Deprecated
    <A> A adapt(ModelObject modelObject, Class<A> type);

    @Deprecated
    <A> A adapt(ModelObject modelObject, Class<A> type, boolean create);

    Status getImportStatus();

    void saveImportStatus(Status status);

}
