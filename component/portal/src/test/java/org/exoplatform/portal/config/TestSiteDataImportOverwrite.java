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
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.page.*;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public class TestSiteDataImportOverwrite extends AbstractSiteDataImportTest {

  @Override
  protected ImportMode getMode() {
    return ImportMode.OVERWRITE;
  }

  @Override
  protected void afterOneBootWithExtention(PortalContainer container) throws Exception {
    begin();
    // Test portal
    DataStorage dataStorage = container.getComponentInstanceOfType(DataStorage.class);
    PageService pageService = container.getComponentInstanceOfType(PageService.class);
    PortalConfig portal = dataStorage.getPortalConfig("classic");
    Container layout = portal.getPortalLayout();
    assertEquals(1, layout.getChildren().size());
    Application<Portlet> layoutPortlet = (Application<Portlet>) layout.getChildren().get(0);
    assertEquals("site2/layout", dataStorage.getId(layoutPortlet.getState()));
    PageContext page = pageService.loadPage(PageKey.parse("portal::classic::home"));
    assertNull(page);
    page = pageService.loadPage(PageKey.parse("portal::classic::page1"));
    assertNotNull(page);
    assertEquals("site 2", page.getState().getDisplayName());
    page = pageService.loadPage(PageKey.parse("portal::classic::page2"));
    assertNotNull(page);
    assertEquals("site 2", page.getState().getDisplayName());
    // Test group
    portal = dataStorage.getPortalConfig(SiteType.GROUP.getName(), "/platform/administrators");
    layout = portal.getPortalLayout();
    assertEquals(1, layout.getChildren().size());
    layoutPortlet = (Application<Portlet>) layout.getChildren().get(0);
    assertEquals("site1/layout", dataStorage.getId(layoutPortlet.getState()));
    page = pageService.loadPage(PageKey.parse("group::/platform/administrators::page1"));
    assertNotNull(page);
    assertEquals("site 2", page.getState().getDisplayName());

    end();
  }

  @Override
  protected void afterSecondBootWithOverride(PortalContainer container) throws Exception {
    begin();

    DataStorage dataStorage = container.getComponentInstanceOfType(DataStorage.class);
    PageService pageService = container.getComponentInstanceOfType(PageService.class);
    PortalConfig portal = dataStorage.getPortalConfig("classic");
    Container layout = portal.getPortalLayout();
    assertEquals(1, layout.getChildren().size());
    Application<Portlet> layoutPortlet = (Application<Portlet>) layout.getChildren().get(0);
    assertEquals("site2/layout", dataStorage.getId(layoutPortlet.getState()));

    //
    PageContext home = pageService.loadPage(PageKey.parse("portal::classic::home"));
    assertNull(home);

    PageContext page1 = pageService.loadPage(PageKey.parse("portal::classic::page1"));
    assertNotNull(page1);
    assertEquals("site 2", page1.getState().getDisplayName());

    PageContext page2 = pageService.loadPage(PageKey.parse("portal::classic::page2"));
    assertNotNull(page2);
    assertEquals("site 2", page2.getState().getDisplayName());

    end();
  }
}
