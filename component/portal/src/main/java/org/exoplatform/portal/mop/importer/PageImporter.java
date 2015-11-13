/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.exoplatform.portal.mop.importer;

import java.util.HashMap;
import java.util.List;

import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.management.operations.page.PageUtils;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.page.PageServiceWrapper;
import org.exoplatform.portal.mop.page.PageState;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public class PageImporter {
    private final SiteKey siteKey;
    /** . */
    private final List<Page> list;

    /** . */
    private final DataStorage service;

    /** . */
    private final PageService pageService;

    /** . */
    private final ImportMode mode;

    public PageImporter(ImportMode importMode, SiteKey siteKey, List<Page> list, DataStorage dataStorage_, PageService pageService) {
        this.siteKey = siteKey;
        this.mode = importMode;
        this.list = list;
        this.service = dataStorage_;
        this.pageService = pageService;
    }

    public void perform() throws Exception {
        HashMap<String, Page> hashPageList = new HashMap<String, Page>();
        for (Page page : list) {
            hashPageList.put(page.getPageId(), page);
        }

        //We temporary don't support to delete user site's pages, it's risk because user site page, navigation is controlled by 
        //UserSiteLifecycle which is difference with portal site and group site
        if (mode == ImportMode.OVERWRITE && !siteKey.getType().equals(SiteType.USER)) {
            List<PageContext> allPages = ((PageServiceWrapper)pageService).loadPages(siteKey);
            for (PageContext currentPage : allPages) {
                String currentPageId = currentPage.getKey().format();
                if (!hashPageList.containsKey(currentPageId)) {
                    pageService.destroyPage(currentPage.getKey());
                }
            }
        }
        
        for (Page src : list) {
            PageContext existingPage = pageService.loadPage(src.getPageKey());
            Page dst;
            
            //
            switch (mode) {
                case CONSERVE:
                    dst = null;
                    break;
                case INSERT:
                    if (existingPage == null) {
                        dst = src;
                    } else {
                        dst = null;
                    }
                    break;
                case MERGE:
                case OVERWRITE:
                    dst = src;
                    break;
                default:
                    throw new AssertionError();
            }
            
            if (dst != null) {
                PageState dstState = PageUtils.toPageState(dst);
                
                pageService.savePage(new PageContext(src.getPageKey(), dstState));
                service.save(dst);
            }            
        }
    }
}
