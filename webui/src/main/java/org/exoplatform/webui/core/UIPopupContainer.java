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

package org.exoplatform.webui.core;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/**
 * Created by The eXo Platform SARL Author : pham tuan phamtuanchip@yahoo.de Oct 03, 2006 9:43:23 AM
 */
@ComponentConfig(lifecycle = Lifecycle.class)
public class UIPopupContainer extends UIContainer {
    public UIPopupContainer() throws Exception {
        addChild(createUIComponent(UIPopupWindow.class, null, null).setRendered(false));
    }

    public void processRender(WebuiRequestContext context) throws Exception {
        context.getWriter().append("<span class=\"").append(getId()).append("\" id=\"").append(getId()).append("\">");
        renderChildren(context);
        context.getWriter().append("</span>");
    }

    public <T extends UIComponent> T activate(Class<T> type, int width) throws Exception {
        return activate(type, null, width, 0);
    }

    public <T extends UIComponent> T activate(Class<T> type, String configId, int width, int height) throws Exception {
        T comp = createUIComponent(type, configId, null);
        activate(comp, width, height);
        return comp;
    }

    public void activate(UIComponent uiComponent, int width, int height) throws Exception {
        activate(uiComponent, width, height, true);
    }

    public void activate(UIComponent uiComponent, int width, int height, boolean isResizeable) throws Exception {
        UIPopupWindow popup = getChild(UIPopupWindow.class);
        popup.setUIComponent(uiComponent);
        ((UIPopupComponent) uiComponent).activate();
        popup.setWindowSize(width, height);
        popup.setRendered(true);
        popup.setShow(true);
        popup.setResizable(isResizeable);
    }

    public void deActivate() throws Exception {
        UIPopupWindow popup = getChild(UIPopupWindow.class);
        if (popup.getUIComponent() != null)
            ((UIPopupComponent) popup.getUIComponent()).deActivate();
        popup.setUIComponent(null);
        popup.setRendered(false);
    }

    public void cancelPopupAction() throws Exception {
        deActivate();
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        context.addUIComponentToUpdateByAjax(this);
    }
}
