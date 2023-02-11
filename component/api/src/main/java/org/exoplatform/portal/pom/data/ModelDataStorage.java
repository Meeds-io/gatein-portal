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
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Status;

/**
 * Created by The eXo Platform SAS Apr 19, 2007 This interface is used to load
 * the PortalConfig, Page config and Navigation config from the database
 */
public interface ModelDataStorage {

  void create(PortalData config);

  void save(PortalData config);

  void remove(PortalData config);

  PortalData getPortalConfig(PortalKey key);

  /**
   * Retrieves the list of site names of a designated type
   * 
   * @param  siteType {@link SiteType}
   * @param  offset offset of the query
   * @param  limit limit to fetch
   * @return {@link List} of site names
   */
  List<String> getSiteNames(SiteType siteType, int offset, int limit);

  /**
   * Saves a page. If a page with the same id already exists then a merge
   * operation will occur, otherwise it throws {@link IllegalStateException}
   * From PLF 5.3.x (RDBMS implementation) we drop support return the change
   * list as it's not used any where. So the method always return the empty
   * list.
   *
   * @param  page      the page to save
   * @return           the list of model changes that occured during the save
   *                   operation
   */
  List<ModelChange> save(PageData page);

  PageData getPage(PageKey key);

  <S> String getId(ApplicationState<S> state);

  <S> S load(ApplicationState<S> state, ApplicationType<S> type);

  <S> ApplicationState<S> save(ApplicationState<S> state, S preferences);

  <S> ApplicationData<S> getApplicationData(String applicationStorageId);

  Status getImportStatus();

  void saveImportStatus(Status status);

  Container getSharedLayout(String siteName);

  <T> LazyPageList<T> find(Query<T> q);

  <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator);

}
