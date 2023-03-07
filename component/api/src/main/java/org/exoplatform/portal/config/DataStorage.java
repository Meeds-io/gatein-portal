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
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.data.ModelChange;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

/**
 * Created by The eXo Platform SAS Apr 19, 2007 This interface is used to load
 * the PortalConfig, Page config and Navigation config from the database
 * 
 * @deprecated use {@link LayoutService} since this is service layout and this
 *             interface name is ambiguous with storage layer
 */
@Deprecated(forRemoval = true, since = "6.5")
public interface DataStorage {

  int    DEFAULT_LIMIT         = 50;

  String PAGE_CREATED          = "org.exoplatform.portal.config.DataStorage.pageCreated".intern();

  String PAGE_REMOVED          = "org.exoplatform.portal.config.DataStorage.pageRemoved".intern();

  String PAGE_UPDATED          = "org.exoplatform.portal.config.DataStorage.pageUpdated".intern();

  String PORTAL_CONFIG_CREATED = "org.exoplatform.portal.config.DataStorage.portalConfigCreated".intern();

  String PORTAL_CONFIG_REMOVED = "org.exoplatform.portal.config.DataStorage.portalConfigRemoved".intern();

  String PORTAL_CONFIG_UPDATED = "org.exoplatform.portal.config.DataStorage.portalConfigUpdated".intern();

  /**
   * Create a PortalConfig in database <br>
   * Then broadcast PORTAL_CONFIG_CREATED event
   *
   * @param config {@link PortalConfig}
   */
  void create(PortalConfig config);

  /**
   * This method should update the PortalConfig object <br>
   * Then broadcast PORTAL_CONFIG_UPDATED event
   *
   * @param config {@link PortalConfig}
   */
  void save(PortalConfig config);

  /**
   * This method should load the PortalConfig object from db according to the
   * portalName
   *
   * @param  portalName
   * @return            {@link PortalConfig}
   */
  PortalConfig getPortalConfig(String portalName);

  /**
   * This method should load the PortalConfig object from db according to the
   * portalName and ownerType
   *
   * @param  portalName
   * @param  ownerType
   * @return            {@link PortalConfig}
   */
  PortalConfig getPortalConfig(String ownerType, String portalName);

  /**
   * Retrieves {@link PortalConfig} of designated {@link SiteKey}
   * 
   * @param siteKey {@link SiteKey}
   * @return null if not found, else {@link PortalConfig}
   */
  PortalConfig getPortalConfig(SiteKey siteKey);

  /**
   * Remove the PortalConfig from the database <br>
   * Then broadcast PORTAL_CONFIG_REMOVED event
   *
   * @param config {@link PortalConfig}
   */
  void remove(PortalConfig config);

  /**
   * This method should load the Page object from the database according to the
   * pageId
   *
   * @param  pageId - String represent id of page, it must be valid pageId (3
   *                  parts saparate by :: )
   * @return        {@link Page}
   */
  Page getPage(String pageId);

  /**
   * Retrieves Page designated by its key
   * 
   * @param pageKey {@link PageKey}
   * @return {@link Page}
   */
  Page getPage(PageKey pageKey);

  void remove(Page page);

  void remove(PageKey pageKey);

  /**
   * This method should create or update the given page object <br>
   * Then broasdcast PAGE_CREATED event
   *
   * @param page
   */
  void create(Page page);

  /**
   * Saves a page. If a page with the same id already exists then a merge
   * operation will occur, otherwise it throws {@link IllegalStateException}
   * <br>
   * The operation returns a list of the change object that describes the
   * changes that occured during the save operation. <br>
   * Then broadcast PAGE_UPDATED event
   *
   * @param  page the page to save
   * @return      the list of model changes that occurred during the save
   */
  List<ModelChange> save(Page page);

  /**
   * Retrieved {@link Application} model switch its storage identifier
   * 
   * @param  <S>                  can be of type {@link Portlet} only, see
   *                                {@link ApplicationType}
   * @param  applicationStorageId
   * @return                      {@link Application}
   */
  <S> Application<S> getApplicationModel(String applicationStorageId);

  /**
   * Return contentId according to each state (transient, persistent, clone)
   * 
   * @param  <S>   can be of type {@link Portlet} only, see
   *                 {@link ApplicationType}
   * @param  state
   * @return       {@link ApplicationState} of type {@link Portlet}
   */
  <S> String getId(ApplicationState<S> state);

  /**
   * Return content state. If can't find, return null
   * 
   * @param  <S>   can be of type {@link Portlet} only, see
   *                 {@link ApplicationType}
   * @param  state - ApplicationState object
   * @param  type  - ApplicationType object
   * @return       {@link Portlet}
   */
  <S> S load(ApplicationState<S> state, ApplicationType<S> type);

  /**
   * Save content state <br>
   * 
   * @param  <S>         can be of type {@link Portlet} only, see
   *                       {@link ApplicationType}
   * @param  state       - ApplicationState object. It must be
   *                       CloneApplicationState or PersistentApplicationState
   *                       object
   * @param  preferences - object to be saved
   * @return {@link ApplicationState} typed with {@link Portlet}
   */
  <S> ApplicationState<S> save(ApplicationState<S> state, S preferences);

  /**
   * Return LazyPageList of object (sorted) which type and other info determined
   * in Query object
   * @param <T> could be of type {@link Page} or {@link PortalConfig}
   *
   * @param q - Query object
   * @param sortComparator {@link Comparator} used for sorting results
   * @return {@link LazyPageList}
   */
  <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator);

  /**
   * Return ListAccess, we can retrieved array of object (sorted) in database
   * through this.
   * @param <T> could be of type {@link Page} or {@link PortalConfig}
   *
   * @param q              - Query object
   * @param sortComparator - Comparator object, used to sort the result list
   * @return {@link LazyPageList}
   */
  <T> ListAccess<T> find2(Query<T> q, Comparator<T> sortComparator);

  /**
   * Return shared layout containing common layout of all sites (user, group and
   * sites). This will retrieve the layout from
   * /conf/portal/portal/sharedlayout-{siteName}.xml else if not found, retrieve
   * it from /conf/portal/portal/sharedlayout.xml
   * 
   * @param  siteName
   * @return          {@link Container}
   */
  Container getSharedLayout(String siteName);

  /**
   * @return All portal site names
   * @deprecated since 6.4.0, use {@link #getSiteNames(SiteType, int, int)} instead
   */
  @Deprecated(forRemoval = true, since = "6.5")
  default List<String> getAllPortalNames() {
    return getSiteNames(SiteType.PORTAL, 0, DEFAULT_LIMIT);
  }

  /**
   * @return All group site names
   * @deprecated since 6.4.0, use {@link #getSiteNames(SiteType, int, int)} instead
   */
  @Deprecated(forRemoval = true, since = "6.5")
  default List<String> getAllGroupNames() {
    return getSiteNames(SiteType.GROUP, 0, DEFAULT_LIMIT);
  }

  /**
   * Retrieves all site type names with pagination.
   * 
   * @param siteType {@link SiteType}
   * @param offset offset of the query
   * @param limit limit of the list to fetch
   * @return {@link List} of corresponding site names, else empty list if not found
   */
  List<String> getSiteNames(SiteType siteType, int offset, int limit);

  default Status getImportStatus() {
    throw new UnsupportedOperationException();
  }

  default void saveImportStatus(Status status) {
    throw new UnsupportedOperationException();
  }

}
