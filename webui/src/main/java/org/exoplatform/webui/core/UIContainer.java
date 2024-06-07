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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/** Created by The eXo Platform SAS May 7, 2006 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
@Serialized
public class UIContainer extends UIComponent {

    private List<UIComponent> children;

    public boolean hasChildren() {
        return children != null && children.size() > 0;
    }

    public void setChildren(List<UIComponent> ls) {
        this.children = new CopyOnWriteArrayList<UIComponent>(ls);
    }

    public List<UIComponent> getChildren() {
        if (children == null) {
            children = new CopyOnWriteArrayList<UIComponent>();
        }
        return children;
    }

    public void addChild(UIComponent uicomponent) {
        if (children == null) {
            children = new CopyOnWriteArrayList<UIComponent>();
        }
        uicomponent.setParent(this);
        children.add(uicomponent);
    }

    public UIComponent removeChildById(String id) {
        if (children == null) {
            return null;
        }
        for (UIComponent child : children) {
            if (!id.equals(child.getId())) {
                continue;
            }
            child.setParent(null);
            children.remove(child);
            return child;
        }
        return null;
    }

    public <T extends UIComponent> T removeChild(Class<T> clazz) {
        if (children == null) {
            return null;
        }
        for (UIComponent child : children) {
            if (!clazz.isInstance(child)) {
                continue;
            }
            child.setParent(null);
            children.remove(child);
            return clazz.cast(child);
        }
        return null;
    }

    // todo (julien) : this is not type safe

    @SuppressWarnings("unchecked")
    public <T extends UIComponent> T getChildById(String id) {
        List<UIComponent> subComponents = getChildren();
        if (subComponents == null) {
            return null;
        }
        for (UIComponent child : subComponents) {
            if (id.equals(child.getId())) {
                return (T) child;
            }
        }
        return null;
    }

    // todo (julien) : this is not type safe

    @SuppressWarnings("unchecked")
    public <T extends UIComponent> T getChild(int idx) {
        List<UIComponent> subComponents = getChildren();
        if (subComponents == null) {
            return null;
        }
        if (subComponents.size() <= idx) {
            return null;
        }
        return (T) subComponents.get(idx);
    }

    public <T extends UIComponent> T getChild(Class<T> clazz) {
        List<UIComponent> subComponents = getChildren();
        if (subComponents == null) {
            return null;
        }
        for (UIComponent uichild : subComponents) {
            if (clazz.isInstance(uichild)) {
                return clazz.cast(uichild);
            }
        }
        return null;
    }

    // todo (julien) : this is not type safe

    @SuppressWarnings("unchecked")
    public <T extends UIComponent> T replaceChild(String targetChildId, UIComponent newComponent) throws Exception {
        List<UIComponent> subComponents = getChildren();
        if (subComponents == null) {
            throw new Exception("Cannot  find the child : " + targetChildId);
        }
        for (int i = 0; i < subComponents.size(); i++) {
            UIComponent child = subComponents.get(i);
            if (targetChildId.equals(child.getId())) {
                child.setParent(null);
                newComponent.setParent(this);
                subComponents.set(i, newComponent);
                return (T) child;
            }
        }
        throw new Exception("Cannot  find the child : " + targetChildId);
    }

    public <T extends UIComponent> T replaceChild(String targetChildId, Class<T> type, String configId, String id)
            throws Exception {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        WebuiApplication app = (WebuiApplication) context.getApplication();
        UIComponent comp = app.createUIComponent(type, configId, id, context);
        comp = replaceChild(targetChildId, comp);
        return type.cast(comp);
    }

    public <T extends UIComponent> T addChild(Class<T> type, String configId, String id) throws Exception {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        WebuiApplication app = (WebuiApplication) context.getApplication();
        T comp = app.createUIComponent(type, configId, id, context);
        addChild(comp);
        return comp;
    }

    // todo (julien) : this is not type safe

    @SuppressWarnings("unchecked")
    public <T extends UIComponent> T findComponentById(String id) {
        List<UIComponent> subComponents = getChildren();
        if (getId() != null && getId().equals(id)) {
            return (T) this;
        }
        if (subComponents == null) {
            return null;
        }
        for (UIComponent uichild : subComponents) {
            UIComponent found = uichild.findComponentById(id);
            if (found != null) {
                return (T) found;
            }
        }
        return null;
    }

    /*
     * @SuppressWarnings("unchecked") public <T extends UIComponent> T findRenderComponentById(String id) { if(!isRendered())
     * return null; if(getId().equals(id)) return (T)this ; if(children == null) return null ; for (UIComponent uichild :
     * children) { UIComponent found = uichild.findComponentById(id) ; if(found != null) return (T)found ; } return null ; }
     */

    public <T extends UIComponent> T findFirstComponentOfType(Class<T> type) {
        List<UIComponent> subComponents = getChildren();
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        if (children == null) {
            return null;
        }
        for (UIComponent uichild : subComponents) {
            T found = uichild.findFirstComponentOfType(type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public <T> void findComponentOfType(List<T> list, Class<T> type) {
        List<UIComponent> subComponents = getChildren();
        if (type.isInstance(this) && !list.contains(this)) {
            list.add(type.cast(this));
        }
        if (subComponents == null) {
            return;
        }
        for (UIComponent uichild : subComponents) {
            uichild.findComponentOfType(list, type);
        }
    }

    public void setRenderedChild(String id) {
        List<UIComponent> list = getChildren();
        for (UIComponent child : list) {
            if (child.getId().equals(id)) {
                child.setRendered(true);
            } else {
                child.setRendered(false);
            }
        }
    }

    public <T extends UIComponent> void setRenderedChild(Class<T> type) {
        List<UIComponent> list = getChildren();
        for (UIComponent child : list) {
            if (type.isInstance(child)) {
                child.setRendered(true);
            } else {
                child.setRendered(false);
            }
        }
    }

    public void setRenderedChildrenOfTypes(Class<?>[] types) {
        List<UIComponent> list = getChildren();
        for (UIComponent child : list) {
            child.setRendered(false);
            for (Class<?> type : types) {
                if (type.isInstance(child)) {
                    child.setRendered(true);
                    break;
                }
            }
        }
    }

    public void renderChild(String id) throws Exception {
        renderChild(getChildById(id));
    }

    public <T extends UIComponent> void renderChild(Class<T> clazz) throws Exception {
        renderChild(getChild(clazz));
    }

    public void renderUIComponent(UIComponent uicomponent) throws Exception {
        uicomponent.processRender((WebuiRequestContext) WebuiRequestContext.getCurrentInstance());
    }

    public void renderChild(int index) throws Exception {
        renderChild(getChild(index));
    }

    public void renderChild(UIComponent child) throws Exception {
        if (child.isRendered()) {
            child.processRender((WebuiRequestContext) WebuiRequestContext.getCurrentInstance());
        }
    }

    public void renderChildren() throws Exception {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        renderChildren(context);
    }

    public void renderChildren(WebuiRequestContext context) throws Exception {
        for (UIComponent child : getChildren()) {
            if (child.isRendered()) {
                child.processRender(context);
            }
        }
    }

    public static class SelectTabActionListener extends EventListener<UIContainer> {
        public void execute(Event<UIContainer> event) throws Exception {
            WebuiRequestContext context = event.getRequestContext();
            UIContainer uiContainer = event.getSource();
            String renderTab = context.getRequestParameter(OBJECTID);
            if (renderTab == null) {
                return;
            }
            UIComponent uiComp = uiContainer.findComponentById(renderTab);
            if (uiComp != null) {
                for (UIComponent child : uiContainer.getChildren()) {
                    child.setRendered(false);
                }
                uiComp.setRendered(true);
            }
        }
    }
}
