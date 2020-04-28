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

package org.exoplatform.webui.core;

import java.util.Iterator;
import java.util.List;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(template = "system:/groovy/webui/core/UIVirtualList.gtmpl",
        events = { @EventConfig(listeners = UIVirtualList.LoadNextActionListener.class, csrfCheck = false) })
@Serialized
public class UIVirtualList extends UIComponentDecorator {
    private int height;

    private boolean autoAdjustHeight;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isAutoAdjustHeight() {
        return autoAdjustHeight;
    }

    public void setAutoAdjustHeight(boolean auto) {
        this.autoAdjustHeight = auto;
    }

    @Override
    public String event(String name) throws Exception {
      UIComponent parent = this.getParent();
      return parent.event(name);
    }

    @Override
    public String event(String name, String beanId) throws Exception {
        UIComponent parent = this.getParent();
        return parent.event(name, beanId);
    }

    @Override
    public String event(String name, String beanId, Parameter[] params) throws Exception {
      UIComponent parent = this.getParent();
      return parent.event(name, beanId, params);
    }

    public void dataBind(Iterator<List<?>> source) throws Exception {
        UIRepeater repeater = this.getRepeater();
        repeater.setSource(source);
    }

    public UIRepeater getRepeater() {
        try {
            return (UIRepeater) this.uicomponent_;
        } catch (Exception e) {
            throw new NullPointerException("Repeater doesn't attached");
        }
    }

    public static class LoadNextActionListener extends EventListener<UIVirtualList> {
        public void execute(Event<UIVirtualList> event) throws Exception {
            UIVirtualList virtualList = event.getSource();
            UIRepeater repeater = virtualList.getRepeater();
            WebuiRequestContext rContext = event.getRequestContext();

            repeater.feedNext();
            RequireJS require = rContext.getJavascriptManager().require("SHARED/uiVirtualList", "uiVirtualList");
            if (repeater.hasNext()) {
                require.addScripts("uiVirtualList.updateList('" + virtualList.getId() + "', true);");
            } else {
                require.addScripts("uiVirtualList.updateList('" + virtualList.getId() + "', false);");
            }

            rContext.addUIComponentToUpdateByAjax((UIComponent) repeater);
        }
    }
}
