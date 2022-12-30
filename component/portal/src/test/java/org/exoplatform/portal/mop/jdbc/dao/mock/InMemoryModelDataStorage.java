/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.portal.mop.jdbc.dao.mock;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.jdbc.dao.ContainerDAO;
import org.exoplatform.portal.mop.jdbc.dao.PageDAO;
import org.exoplatform.portal.mop.jdbc.dao.PermissionDAO;
import org.exoplatform.portal.mop.jdbc.dao.SiteDAO;
import org.exoplatform.portal.mop.jdbc.dao.WindowDAO;
import org.exoplatform.portal.mop.jdbc.service.JDBCModelStorageImpl;

public class InMemoryModelDataStorage extends JDBCModelStorageImpl {

  private static Status status;

  public InMemoryModelDataStorage(SiteDAO siteDAO,
                                  PageDAO pageDAO,
                                  WindowDAO windowDAO,
                                  ContainerDAO containerDAO,
                                  PermissionDAO permissionDAO,
                                  SettingService settingService,
                                  ConfigurationManager confManager) {
    super(siteDAO,
          pageDAO,
          windowDAO,
          containerDAO,
          permissionDAO,
          settingService,
          confManager);
  }

  @Override
  public void saveImportStatus(Status status) {
    InMemoryModelDataStorage.status = status; // NOSONAR
  }

  @Override
  public Status getImportStatus() {
    return status;
  }

}
