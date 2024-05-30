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

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIComponentDecorator;

/**
 * May 19, 2006
 */
@ComponentConfig(template = "system:/groovy/portal/webui/page/UISiteBody.gtmpl")
public class UISiteBody extends UIComponentDecorator {

    /** The storage id. */
    private String storageId;

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    @Override
    public void processRender(WebuiRequestContext context) throws Exception {
      PortalRequestContext requestContext = RequestContext.getCurrentInstance();
      if (isShowSiteBody(requestContext)) {
        processContainerRender(context);
      } else {
        processPageBodyRender(context);
      }
    }

    public String getSiteClass() {
      String portalOwner = ((PortalRequestContext) RequestContext.getCurrentInstance()).getPortalOwner();
      if (StringUtils.isBlank(portalOwner)) {
        return "";
      } else {
        return portalOwner.toUpperCase() + "Site";
      }
    }

    @Override
    public UIComponent getUIComponent() {
      return PortalRequestContext.getCurrentInstance().getUiPortal();
    }

    public UIPortal getUIPortal() {
      return (UIPortal) getUIComponent();
    }

    @Override
    protected void setChildComponent(UIComponent uicomponent) {
      PortalRequestContext.getCurrentInstance().setUiPortal((UIPortal) uicomponent);
    }

    protected boolean isShowSiteBody(PortalRequestContext requestContext) {
      return !requestContext.isShowMaxWindow() && (Util.getUIPage() == null || !Util.getUIPage().isShowMaxWindow());
    }

    protected void processPageBodyRender(WebuiRequestContext context) throws Exception {
      UIPageBody uiPageBody = findFirstComponentOfType(UIPageBody.class);
      uiPageBody.processRender(context);
    }

    protected void processContainerRender(WebuiRequestContext context) throws Exception {
      super.processRender(context);
    }

}
