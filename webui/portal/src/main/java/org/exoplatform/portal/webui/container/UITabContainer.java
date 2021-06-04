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

package org.exoplatform.portal.webui.container;

import java.util.Collections;
import java.util.List;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.container.UIContainerActionListener.EditContainerActionListener;
import org.exoplatform.portal.webui.portal.UIPortalComponentActionListener.DeleteComponentActionListener;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * May 19, 2006
 */
@ComponentConfig(template = "system:/groovy/portal/webui/container/UITabContainer.gtmpl", events = {
        @EventConfig(listeners = EditContainerActionListener.class),
        @EventConfig(listeners = DeleteComponentActionListener.class, confirm = "UIContainer.deleteContainer"),
        @EventConfig(listeners = UITabContainer.SelectTabActionListener.class),
        @EventConfig(listeners = UITabContainer.MoveLeftActionListener.class),
        @EventConfig(listeners = UITabContainer.MoveRightActionListener.class),
        @EventConfig(listeners = UITabContainer.AddTabActionListener.class) })
public class UITabContainer extends UIContainer {

    public static final String TAB_CONTAINER = "TabContainer";
    public static final String FIRST_TAB = "FirstTab";
    public static final String LAST_TAB = "LastTab";

    public UITabContainer() {
        super();
    }

    private void moveTab(UITabContainer container, String childId, boolean isToLeft) {
        UIComponent selectedChild = container.getChildById(childId);
        List<UIComponent> children = container.getChildren();
        int selectedIndex = children.indexOf(selectedChild);
        if (isToLeft) {
            if (selectedIndex > 0) {
                Collections.swap(children, selectedIndex, selectedIndex - 1);
            }
        } else {
            if (selectedIndex < children.size() - 1) {
                Collections.swap(children, selectedIndex, selectedIndex + 1);
            }
        }
    }

    public String getTabState(UIComponent uiChild, UITabContainer uiContainer) {
        String tabState = "";
        List<UIComponent> children = uiContainer.getChildren();
        int tabIndex = children.indexOf(uiChild);
        if (tabIndex == 0) {
            tabState = FIRST_TAB;
        } else if (tabIndex == children.size() - 1) {
            tabState = LAST_TAB;
        }
        return tabState;
    }

    public static class SelectTabActionListener extends EventListener<UITabContainer> {
        public void execute(Event<UITabContainer> event) throws Exception {
            String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
            UITabContainer container = event.getSource();
            UIComponent goal = container.findComponentById(objectId);
            if (goal == null) {
                return;
            }
            UITabContainer parent = goal.getParent();
            List<UIComponent> children = parent.getChildren();
            for (UIComponent child : children) {
                if (child.getId().equals(objectId)) {
                    child.setRendered(true);
                    continue;
                }
                child.setRendered(false);
            }
        }
    }

    public abstract static class MoveActionListener extends EventListener<UITabContainer> {
        public void doMove(Event<UITabContainer> event, boolean isToLeft) {
            PortalRequestContext context = (PortalRequestContext) event.getRequestContext();
            UITabContainer container = event.getSource();
            String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
            if (container == null || objectId == null) {
                return;
            }
            container.moveTab(container, objectId, isToLeft);
            context.addUIComponentToUpdateByAjax(container);
            context.ignoreAJAXUpdateOnPortlets(true);
            context.getJavascriptManager().require("SHARED/portalComposer", "portalComposer")
                    .addScripts("portalComposer.toggleSaveButton();");
        }
    }

    public static class MoveLeftActionListener extends MoveActionListener {
        @Override
        public void execute(Event<UITabContainer> event) throws Exception {
            doMove(event, true);
        }
    }

    public static class MoveRightActionListener extends MoveActionListener {
        @Override
        public void execute(Event<UITabContainer> event) throws Exception {
            doMove(event, false);
        }
    }

    public static class AddTabActionListener extends EventListener<UITabContainer> {
        @Override
        public void execute(Event<UITabContainer> event) throws Exception {
            UITabContainer container = event.getSource();
            PortalRequestContext pcontext = (PortalRequestContext) event.getRequestContext();
            if (container == null) {
                return;
            }

            List<UIComponent> children = container.getChildren();
            for (UIComponent child : children) {
                if (child.isRendered() && child instanceof UIContainer) {
                    UIContainer newTabContainer = container.addChild(UIContainer.class, null, null);
                    newTabContainer.setTemplate(child.getTemplate());
                    child.setRendered(false);
                    newTabContainer.setRendered(true);
                    newTabContainer.setId(String.valueOf(Math.abs(newTabContainer.hashCode())));
                    newTabContainer.setMoveAppsPermissions(container.getMoveAppsPermissions());
                    newTabContainer.setMoveContainersPermissions(container.getMoveContainersPermissions());
                    pcontext.addUIComponentToUpdateByAjax(container);
                    pcontext.ignoreAJAXUpdateOnPortlets(true);
                    pcontext.getJavascriptManager().require("SHARED/portalComposer", "portalComposer")
                            .addScripts("portalComposer.toggleSaveButton();");
                    return;
                }
            }
        }
    }
}
