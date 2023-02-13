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

import java.util.List;

import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Status;

/**
 * Created by The eXo Platform SAS Apr 19, 2007 This interface is used to load
 * the PortalConfig, Page config and Navigation config from the database
 */
public interface ModelDataStorage {

  void create(PortalConfig config);

  void create(PortalData config);

  void save(PortalConfig config);

  void save(PortalData config);

  void remove(PortalConfig config);

  void remove(PortalData config);

  void remove(SiteKey siteKey);

  PortalData getPortalConfig(SiteKey siteKey);

  PortalData getPortalConfig(PortalKey key);

  PortalConfig getPortalConfig(String ownerType, String portalName);

  /**
   * Retrieves the list of site names of a designated type
   * 
   * @param  siteType {@link SiteType}
   * @param  offset   offset of the query
   * @param  limit    limit to fetch
   * @return          {@link List} of site names
   */
  List<String> getSiteNames(SiteType siteType, int offset, int limit);

  Status getImportStatus();

  void saveImportStatus(Status status);

  Container getSharedLayout(String siteName);

}
