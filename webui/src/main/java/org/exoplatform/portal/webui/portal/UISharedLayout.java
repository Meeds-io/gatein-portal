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

package org.exoplatform.portal.webui.portal;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.page.UISiteBody;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;

@ComponentConfig(

)
public class UISharedLayout extends UIContainer {

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    PortalRequestContext requestContext = RequestContext.getCurrentInstance();
    if (isShowSharedLayout(requestContext)) {
      processContainerRender(context);
    } else {
      processSiteBodyRender(context);
    }
  }

  public boolean isShowSharedLayout(PortalRequestContext requestContext) {
    boolean showSharedLayout = !requestContext.isHideSharedLayout() && (Util.getUIPage() == null || !Util.getUIPage().isHideSharedLayout());
    if (requestContext.getUserPortalConfig() != null && requestContext.getUserPortalConfig().getPortalConfig() != null) {
      showSharedLayout = showSharedLayout && (requestContext.getSiteType() != SiteType.PORTAL
                                              || requestContext.getUserPortalConfig().getPortalConfig().isDisplayed());
    }
    return showSharedLayout;
  }

  protected void processSiteBodyRender(WebuiRequestContext context) throws Exception {
    UISiteBody uiSiteBody = findFirstComponentOfType(UISiteBody.class);
    uiSiteBody.processRender(context);
  }

  protected void processContainerRender(WebuiRequestContext context) throws Exception {
    super.processRender(context);
  }

}
