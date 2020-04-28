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
package org.exoplatform.portal.mop.page;

import java.util.List;

import org.exoplatform.portal.mop.*;

/**
 * <p>
 * The page service manages the page objects in GateIn, it focus on the page entities and does not provide access to the
 * underlying page layout associated with the page.
 * </p>
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public interface PageService {

    /**
     * Find and returns a page, if no such page exist, null is returned.
     *
     * @param key the page key
     * @return the matching page
     * @throws NullPointerException if the key is null
     * @throws PageServiceException anything that would prevent the operation to succeed
     */
    PageContext loadPage(PageKey key) throws NullPointerException, PageServiceException;

    List<PageContext> loadPages(SiteKey siteKey) throws NullPointerException, PageServiceException;

    /**
     * Create, update a page. When the page state is not null, the page will be created or updated depending on whether or not
     * the page already exists.
     *
     *
     * @param page the page
     *
     * @return true if the page is not already existed, otherwise return false.
     *
     * @throws NullPointerException if the key is null
     * @throws PageServiceException anything that would prevent the operation to succeed
     *
     */
    boolean savePage(PageContext page) throws NullPointerException, PageServiceException;

    /**
     * Destroy a page.
     *
     * @param key the page key
     * @return true when the page was destroyed
     * @throws NullPointerException if the page key is null
     * @throws PageServiceException anything that would prevent the operation to succeed
     */
    boolean destroyPage(PageKey key) throws NullPointerException, PageServiceException;

    /**
     * Clone a page.
     *
     * @param src the source key
     * @param dst the destination key
     * @return the cloned page
     * @throws NullPointerException if any key argument is null
     * @throws PageServiceException anything that would prevent the operation to succeed
     */
    PageContext clone(PageKey src, PageKey dst) throws NullPointerException, PageServiceException;

    /**
     * Query the page service to find pages that match the <code>siteType</code>, <code>siteName</code>, <code>pageName</code>
     * and <code>title</code> criterions.
     *
     * @param offset the query offset
     * @param limit the query limit
     * @param siteType the site type
     * @param siteName the site name
     * @param pageName the page name
     * @param pageTitle the page title
     * @return the query result
     * @throws PageServiceException anything that would prevent the operation to succeed
     */
    QueryResult<PageContext> findPages(int offset, int limit, SiteType siteType, String siteName, String pageName,
            String pageTitle) throws PageServiceException;

}
