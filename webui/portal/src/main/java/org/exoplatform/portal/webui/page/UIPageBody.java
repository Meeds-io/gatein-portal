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

package org.exoplatform.portal.webui.page;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * May 19, 2006
 */
@ComponentConfig(template = "system:/groovy/portal/webui/page/UIPageBody.gtmpl")
public class UIPageBody extends UIComponentDecorator {

    private String storageId;

    private String pageName;

    /** . */
    private final Logger log = LoggerFactory.getLogger(UIPageBody.class);

    public UIPageBody(PageBody model) {
        setId("UIPageBody");
    }

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public UIPageBody() {
        setId("UIPageBody");
    }

    public void init(PageBody model) {
        setId("UIPageBody");
    }

    public String getPageName() {
      return pageName;
    }

    public void setPageBody(UserNode pageNode, UIPortal uiPortal) throws Exception {
        WebuiRequestContext context = Util.getPortalRequestContext();
        uiPortal.setMaximizedUIComponent(null);

        UIPage uiPage;
        uiPage = getUIPage(pageNode, uiPortal, context);
        if (uiPage == null) {
            setUIComponent(null);
            return;
        }

        setUIComponent(uiPage);
        pageName = uiPage.getName();
        if (uiPage.isShowMaxWindow()) {
            uiPortal.setMaximizedUIComponent(uiPage);
        } else {
            UIComponent maximizedComponent = uiPortal.getMaximizedUIComponent();
            if (maximizedComponent != null && maximizedComponent instanceof UIPage) {
                uiPortal.setMaximizedUIComponent(null);
            }
        }
    }

    /**
     * Return cached UIPage or a newly built UIPage
     *
     * @param pageReference
     * @param page
     * @param uiPortal
     * @return
     */
    private UIPage getUIPage(UserNode pageNode, UIPortal uiPortal, WebuiRequestContext context) throws Exception {
        PageContext pageContext = null;
        String pageReference = null;
        ExoContainer appContainer = context.getApplication().getApplicationServiceContainer();
        UserPortalConfigService userPortalConfigService = (UserPortalConfigService) appContainer
                .getComponentInstanceOfType(UserPortalConfigService.class);

        if (pageNode != null && pageNode.getPageRef() != null) {
            pageReference = pageNode.getPageRef().format();
            pageContext = userPortalConfigService.getPage(pageNode.getPageRef());
        }

        // The page has been deleted
        if (pageContext == null) {
            // Clear the UIPage from cache in UIPortal
            uiPortal.clearUIPage(pageReference);
            return null;
        }

        UIPage uiPage = uiPortal.getUIPage(pageReference);
        if (uiPage != null) {
            return uiPage;
        }

        try {
            UIPageFactory clazz = UIPageFactory.getInstance(pageContext.getState().getFactoryId());
            uiPage = clazz.createUIPage(context);

            Page page = userPortalConfigService.getDataStorage().getPage(pageReference);
            pageContext.update(page);
            PortalDataMapper.toUIPage(uiPage, page);
            uiPortal.setUIPage(pageReference, uiPage);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Could not handle page '" + pageContext.getKey().format() + "'.", e);
            }
            throw e;
        }

        return uiPage;
    }
}
