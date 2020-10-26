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

import org.exoplatform.component.test.*;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageService;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TestSearch extends AbstractConfigTest {

  /** . */
  private DataStorage storage;

  /** . */
  private PageService pageService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    storage = getContainer().getComponentInstanceOfType(DataStorage.class);
    pageService = getContainer().getComponentInstanceOfType(PageService.class);
  }

  private void assertPageFound(int offset,
                               int limit,
                               SiteType siteType,
                               String siteName,
                               String pageName,
                               String title,
                               String expectedPage) {
    QueryResult<PageContext> res = pageService.findPages(offset, limit, siteType, siteName, pageName, title);
    assertEquals(1, res.getSize());
    assertEquals(expectedPage, res.iterator().next().getKey().format());
  }

  private void assertPageNotFound(int offset, int limit, SiteType siteType, String siteName, String pageName, String title) {
    QueryResult<PageContext> res = pageService.findPages(offset, limit, siteType, siteName, pageName, title);
    assertEquals(0, res.getSize());
  }

  public void testSearchPage() {
    Page page = new Page();
    page.setPageId("portal::test::searchedpage");
    pageService.savePage(new PageContext(page.getPageKey(), null));

    PageContext pageContext = pageService.loadPage(page.getPageKey());
    pageContext.setState(pageContext.getState().builder().displayName("Juuu Ziii").build());
    pageService.savePage(pageContext);

    //
    assertPageFound(0, 10, null, null, null, "Juuu Ziii", "portal::test::searchedpage");
    assertPageFound(0, 10, null, null, null, "Juuu", "portal::test::searchedpage");
    assertPageFound(0, 10, null, null, null, "Ziii", "portal::test::searchedpage");
    assertPageFound(0, 10, null, null, null, "juuu ziii", "portal::test::searchedpage");
    assertPageFound(0, 10, null, null, null, "juuu", "portal::test::searchedpage");
    assertPageFound(0, 10, null, null, null, "ziii", "portal::test::searchedpage");
    assertPageFound(0, 10, null, null, null, "juu", "portal::test::searchedpage");
    assertPageFound(0, 10, null, null, null, "zii", "portal::test::searchedpage");
    assertPageFound(0, 10, null, null, null, "ju", "portal::test::searchedpage");
    assertPageFound(0, 10, null, null, null, "zi", "portal::test::searchedpage");

    assertPageNotFound(0, 10, null, null, null, "foo");
    assertPageNotFound(0, 10, null, null, null, "foo bar");
  }

  public void testSearchPageByOwnerID() {
    QueryResult<PageContext> res = pageService.findPages(0, 10, null, "foo", null, null);
    assertEquals(0, res.getSize());

    res = pageService.findPages(0, 10, null, "test", null, null);
    int pageNum = res.getSize();
    assertTrue(pageNum > 0);

    // Test trim ownerID
    res = pageService.findPages(0, 10, null, "   test   ", null, null);
    assertEquals(pageNum, res.getSize());

    // This should returns all pages
    res = pageService.findPages(0, 10, null, null, null, null);
    assertTrue(res.getSize() > 0);
  }
}
