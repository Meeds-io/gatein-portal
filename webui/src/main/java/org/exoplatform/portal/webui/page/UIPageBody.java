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

package org.exoplatform.portal.webui.page;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;

/**
 * May 19, 2006
 */
@ComponentConfig(template = "system:/groovy/portal/webui/page/UIPageBody.gtmpl")
public class UIPageBody extends UIComponentDecorator {

    private String storageId;

    /** . */
    private final Log log = ExoLogger.getLogger(UIPageBody.class);

    public UIPageBody() {
        setId("UIPageBody");
    }

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public String getPageName() {
      UIPage uiPage = getUIPage();
      return uiPage == null ? null : uiPage.getName();
    }

    public void setPageBody(UserNode pageNode, UIPortal uiPortal) throws Exception {
      PortalRequestContext context = Util.getPortalRequestContext();
      uiPortal.setMaximizedUIComponent(null);

      UIPage uiPage = getUIPage();
      if (uiPage == null) {
        uiPage = context.getUIPage(pageNode, uiPortal);
        if (uiPage == null) {
          setUIComponent(null);
          return;
        }
        setUIComponent(uiPage);
      }

      if (uiPage.isShowMaxWindow()) {
        context.setShowMaxWindow(true);
      }
      if (uiPage.isHideSharedLayout()) {
        context.setHideSharedLayout(true);
      }
      if (context.isShowMaxWindow()) {
        uiPortal.setMaximizedUIComponent(uiPage);
      } else {
        UIComponent maximizedComponent = uiPortal.getMaximizedUIComponent();
        if (maximizedComponent instanceof UIPage) {
          uiPortal.setMaximizedUIComponent(null);
        }
      }
    }

    @Override
    public UIComponent getUIComponent() {
      return PortalRequestContext.getCurrentInstance().getUiPage();
    }

    @Override
    protected void setChildComponent(UIComponent uicomponent) {
      PortalRequestContext.getCurrentInstance().setUiPage((UIPage) uicomponent);
    }

    public UIPage getUIPage() {
      return (UIPage) getUIComponent();
    }

    private PortalRequestContext getRequestContext() {
      return PortalRequestContext.getCurrentInstance();
    }
}
