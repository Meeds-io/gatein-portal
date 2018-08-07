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

package org.exoplatform.portal.webui.util;

import java.util.List;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UIPageFactory;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.workspace.UIEditInlineWorkspace;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIPortalToolPanel;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;

/**
 * Jun 5, 2006
 */
public class Util {
    public static PortalRequestContext getPortalRequestContext() {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        if (!(context instanceof PortalRequestContext)) {
            context = (WebuiRequestContext) context.getParentAppRequestContext();
        }
        return (PortalRequestContext) context;
    }

    public static UIPortalApplication getUIPortalApplication() {
        return (UIPortalApplication) getPortalRequestContext().getUIApplication();
    }

    public static UIPortal getUIPortal() {
        // return getUIPortalApplication().<UIWorkingWorkspace> getChildById(UIPortalApplication.UI_WORKING_WS_ID)
        // .findFirstComponentOfType(UIPortal.class);
        return getUIPortalApplication().getCurrentSite();
    }

    public static UIPortalToolPanel getUIPortalToolPanel() {
        return getUIPortalApplication().findFirstComponentOfType(UIPortalToolPanel.class);
    }

    /**
     * View component on UIWorkspaceWorking $uicomp : current component on UIWorkspaceWorking $clazz : Class of component should
     * show on UIWorkspaceWorking
     */
    public static <T extends UIComponent> T showComponentOnWorking(UIComponent uicomp, Class<T> clazz) throws Exception {
        UIPortalApplication uiPortalApp = uicomp.getAncestorOfType(UIPortalApplication.class);
        UIWorkingWorkspace uiWorkingWS = uiPortalApp.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
        UIPortalToolPanel uiToolPanel = uiWorkingWS.findFirstComponentOfType(UIPortalToolPanel.class).setRendered(true);
        T uiWork = uiToolPanel.createUIComponent(clazz, null, null);
        uiToolPanel.setUIComponent(uiWork);
        // uiWorkingWS.setRenderedChild(UIPortalToolPanel.class) ;
        return uiWork;
    }

    @SuppressWarnings("unchecked")
    public static <T extends UIComponent> T findUIComponent(UIComponent uiComponent, Class<T> clazz, Class ignoreClazz) {
        if (clazz.isInstance(uiComponent))
            return (T) uiComponent;
        if (!(uiComponent instanceof UIContainer))
            return null;
        List<UIComponent> children = ((UIContainer) uiComponent).getChildren();
        for (UIComponent child : children) {
            if (clazz.isInstance(child))
                return (T) child;
            else if (!ignoreClazz.isInstance(child)) {
                UIComponent value = findUIComponent(child, clazz, ignoreClazz);
                if (value != null)
                    return (T) value;
            }
        }
        return null;
    }

    public static void findUIComponents(UIComponent uiComponent, List<UIComponent> list, Class clazz, Class ignoreClazz) {
        if (clazz.isInstance(uiComponent))
            list.add(uiComponent);
        if (!(uiComponent instanceof UIContainer))
            return;
        List<UIComponent> children = ((UIContainer) uiComponent).getChildren();
        for (UIComponent child : children) {
            if (clazz.isInstance(child)) {
                list.add(child);
            } else if (!ignoreClazz.isInstance(child)) {
                findUIComponents(child, list, clazz, ignoreClazz);
            }
        }
    }

    /**
     * @deprecated use {@link #toUIPage(String, UIComponent)} instead
     *
     * @param node
     * @param uiParent
     * @return
     * @throws Exception
     */
    @Deprecated
    public static UIPage toUIPage(PageNode node, UIComponent uiParent) throws Exception {
        return toUIPage(node.getPageReference(), uiParent);
    }

    public static UIPage toUIPage(String pageRef, UIComponent uiParent) throws Exception {
        UserPortalConfigService configService = uiParent.getApplicationComponent(UserPortalConfigService.class);
        PageContext pageContext = configService.getPage(PageKey.parse(pageRef));
        Page page = configService.getDataStorage().getPage(pageRef);
        pageContext.update(page);
        return toUIPage(page, uiParent);
    }

    public static UIPage toUIPage(Page page, UIComponent uiParent) throws Exception {
        UIPage uiPage = Util.getUIPortal().findFirstComponentOfType(UIPage.class);
        if (uiPage != null && uiPage.getId().equals(page.getId()))
            return uiPage;
        WebuiRequestContext context = Util.getPortalRequestContext();

        UIPageFactory clazz = UIPageFactory.getInstance(page.getFactoryId());
        uiPage = clazz.createUIPage(context);

        PortalDataMapper.toUIPage(uiPage, page);
        return uiPage;
    }

    public static void showComponentEditInBlockMode() {
        updatePortalMode();
        UIPortalApplication portalApp = getUIPortalApplication();
        UIEditInlineWorkspace uiEditWS = portalApp.findFirstComponentOfType(UIEditInlineWorkspace.class);

        UIComponent uiComponent = uiEditWS.getUIComponent();
        if (uiComponent instanceof UIPortal) {
            UIPortal uiPortal = (UIPortal) uiComponent;
            uiPortal.setMaximizedUIComponent(null);
        } else {
            UIPortalToolPanel uiPortalToolPanel = getUIPortalToolPanel();
            UIPage uiPage = uiPortalToolPanel.findFirstComponentOfType(UIPage.class);
            if (uiPage != null) {
                Util.getPortalRequestContext()
                        .getJavascriptManager().require("SHARED/portal", "portal")
                        .addScripts("eXo.portal.UIPortal.showComponentEditInBlockMode();");
            }
        }
    }

    public static void showComponentEditInViewMode() {
        updatePortalMode();
        UIPortalApplication portalApp = getUIPortalApplication();
        UIEditInlineWorkspace uiEditWS = portalApp.findFirstComponentOfType(UIEditInlineWorkspace.class);

        UIComponent uiComponent = uiEditWS.getUIComponent();
        if (uiComponent instanceof UIPortal) {
            UIPortal uiPortal = (UIPortal) uiComponent;
            uiPortal.setMaximizedUIComponent(null);
        }

        PortalRequestContext context = Util.getPortalRequestContext();
        context.getJavascriptManager().require("SHARED/portal", "portal")
                .addScripts("portal.UIPortal.showComponentEditInViewMode();");
    }

    public static UIWorkingWorkspace updateUIApplication(Event<? extends UIComponent> event) {
        PortalRequestContext pcontext = (PortalRequestContext) event.getRequestContext();
        UIPortalApplication uiPortalApp = event.getSource().getAncestorOfType(UIPortalApplication.class);

        UIWorkingWorkspace uiWorkingWS = uiPortalApp.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
        pcontext.addUIComponentToUpdateByAjax(uiWorkingWS);
        pcontext.ignoreAJAXUpdateOnPortlets(true);
        return uiWorkingWS;
    }

    public static void updatePortalMode() {
        PortalRequestContext context = Util.getPortalRequestContext();
        UIPortalApplication uiPortalApp = (UIPortalApplication)context.getUIApplication();
        boolean isShowMaxWindow = false;
        UIPage page = uiPortalApp.findFirstComponentOfType(UIPage.class);
        if (page != null && page.isShowMaxWindow()) {
            isShowMaxWindow = true;
        }
        context .getJavascriptManager()
                .require("SHARED/portal", "portal")
                .addScripts("portal.UIPortal.updatePortalMode(" + uiPortalApp.getModeState() + ", '" + uiPortalApp.getEditLevel().toString() + "', " + Boolean.toString(UIPage.isFullPreview()) + ", " + Boolean.toString(isShowMaxWindow)+ ");");
    }

}
