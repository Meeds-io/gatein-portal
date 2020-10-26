/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.portal.config;

import org.exoplatform.container.PortalContainer;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public abstract class AbstractSiteDataImportTest extends AbstractDataImportTest {

  @Override
  protected final String getConfig1() {
    return "site1";
  }

  @Override
  protected final String getConfig2() {
    return "site2";
  }

  @Override
  protected final void afterSecondBoot(PortalContainer container) throws Exception {
    afterFirstBoot(container);
  }

  @Override
  protected void afterSecondBootWithWantReimport(PortalContainer container) throws Exception {
    afterSecondBootWithOverride(container);
  }

  @Override
  protected final void afterSecondBootWithNoMixin(PortalContainer container) throws Exception {
    afterSecondBoot(container);
  }
}
