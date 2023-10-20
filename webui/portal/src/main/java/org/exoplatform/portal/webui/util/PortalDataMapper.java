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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.SiteBody;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.webui.application.PortletState;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.container.UIComponentFactory;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UIPageBody;
import org.exoplatform.portal.webui.page.UISiteBody;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.gatein.common.net.media.MediaType;
import org.gatein.pc.api.Portlet;
import org.gatein.pc.api.info.ModeInfo;
import org.gatein.pc.api.info.PortletInfo;

/**
 * Created by The eXo Platform SAS May 4, 2007 TODO: Rename this to PortalDataModelMapper
 */
public class PortalDataMapper {

    protected static final Log log = ExoLogger.getLogger("portal:PortalDataMapper");

    @SuppressWarnings("unchecked")
    public static ModelObject buildModelObject(UIComponent uiComponent) {
        ModelObject model = null;
        try {
            model = uiComponent.buildModelObject();
        } catch (UnsupportedOperationException ex) {
            if (uiComponent instanceof UIPortal) {
                model = toPortal((UIPortal) uiComponent);
            } else if (uiComponent instanceof UIPageBody) {
                model = new PageBody(((UIPageBody) uiComponent).getStorageId());
            } else if (uiComponent instanceof UIPage) {
                model = toPageModel((UIPage) uiComponent);
            } else if (uiComponent instanceof UIPortlet) {
                model = toPortletModel((UIPortlet<Object, ?>) uiComponent);
            } else if (uiComponent instanceof UIContainer) {
                model = toContainer((UIContainer) uiComponent);
            }
        }
        return model;
    }

    public static void toContainer(Container model, UIContainer uiContainer) {
        model.setId(uiContainer.getId());
        model.setName(uiContainer.getName());
        model.setTitle(uiContainer.getTitle());
        model.setIcon(uiContainer.getIcon());
        model.setDescription(uiContainer.getDescription());
        model.setHeight(uiContainer.getHeight());
        model.setWidth(uiContainer.getWidth());
        model.setCssClass(uiContainer.getCssClass());
        model.setProfiles(uiContainer.getProfiles());
        model.setTemplate(uiContainer.getTemplate());
        model.setFactoryId(uiContainer.getFactoryId());
        model.setAccessPermissions(uiContainer.getAccessPermissions());
        model.setMoveAppsPermissions(uiContainer.getMoveAppsPermissions());
        model.setMoveContainersPermissions(uiContainer.getMoveContainersPermissions());

        List<UIComponent> uiChildren = uiContainer.getChildren();
        if (uiChildren == null)
            return;
        ArrayList<ModelObject> children = new ArrayList<ModelObject>();
        for (UIComponent child : uiChildren) {
            ModelObject component = buildModelObject(child);
            if (component != null)
                children.add(component);
        }
        model.setChildren(children);
    }

    private static <S> Application<S> toPortletModel(UIPortlet<S, ?> uiPortlet) {
        Application<S> model;
        PortletState<S> state = uiPortlet.getState();
        ApplicationType<S> type = state.getApplicationType();
        if (type == ApplicationType.PORTLET) {
            model = (Application<S>) Application.createPortletApplication(uiPortlet.getStorageId());
        } else {
            throw new AssertionError();
        }

        //
        model.setStorageName(uiPortlet.getStorageName());
        model.setState(state.getApplicationState());
        model.setTitle(uiPortlet.getTitle());
        model.setWidth(uiPortlet.getWidth());
        model.setHeight(uiPortlet.getHeight());
        model.setDescription(uiPortlet.getDescription());
        model.setShowInfoBar(uiPortlet.getShowInfoBar());
        model.setShowApplicationState(uiPortlet.getShowWindowState());
        model.setShowApplicationMode(uiPortlet.getShowPortletMode());
        model.setDescription(uiPortlet.getDescription());
        model.setIcon(uiPortlet.getIcon());
        model.setProperties(uiPortlet.getProperties());
        model.setTheme(uiPortlet.getTheme());
        model.setAccessPermissions(uiPortlet.getAccessPermissions());
        model.setModifiable(uiPortlet.isModifiable());
        return model;
    }

    private static Container toContainer(UIContainer uiContainer) {
        Container model = new Container(uiContainer.getStorageId());
        toContainer(model, uiContainer);
        return model;
    }

    public static Page toPageModel(UIPage uiPage) {
        Page model = new Page(uiPage.getStorageId());
        toContainer(model, uiPage);
        model.setOwnerId(uiPage.getSiteKey().getName());
        model.setOwnerType(uiPage.getSiteKey().getTypeName());
        model.setIcon(uiPage.getIcon());
        model.setPageId(uiPage.getPageId());
        model.setTitle(uiPage.getTitle());
        model.setAccessPermissions(uiPage.getAccessPermissions());
        model.setEditPermission(uiPage.getEditPermission());
        model.setFactoryId(uiPage.getFactoryId());
        model.setShowMaxWindow(uiPage.isShowMaxWindow());
        model.setHideSharedLayout(uiPage.isHideSharedLayout());
        model.setModifiable(uiPage.isModifiable());
        return model;
    }

    private static PortalConfig toPortal(UIPortal uiPortal) {
        PortalConfig model = new PortalConfig(uiPortal.getSiteType().getName(), uiPortal.getName(), uiPortal.getStorageId());
        model.setAccessPermissions(uiPortal.getAccessPermissions());
        model.setEditPermission(uiPortal.getEditPermission());
        model.setLabel(uiPortal.getLabel());
        model.setDescription(uiPortal.getDescription());
        model.setLocale(uiPortal.getLocale());
        model.setSkin(uiPortal.getSkin());
        model.setModifiable(uiPortal.isModifiable());
        model.setProperties(uiPortal.getProperties());
        model.setPortalRedirects(uiPortal.getPortalRedirects());
        model.setDefaultLayout(uiPortal.isUseDynamicLayout());

        model.setPortalLayout(new Container());

        List<UIComponent> children = uiPortal.getChildren();
        if (children == null || children.isEmpty()) {
          // Use default portal layout when the UI doesn't define one
          // The default portal layout will add just the PageBody component
          // which is mandatory to display page content inside PortalLayout
          model.useDefaultPortalLayout();
          return model;
        }
        ArrayList<ModelObject> newChildren = new ArrayList<>();
        for (UIComponent child : children) {
            ModelObject component = buildModelObject(child);
            if (component != null)
                newChildren.add(component);
        }
        model.getPortalLayout().setChildren(newChildren);
        model.getPortalLayout().setMoveAppsPermissions(uiPortal.getMoveAppsPermissions());
        model.getPortalLayout().setMoveContainersPermissions(uiPortal.getMoveContainersPermissions());
        return model;
    }

    /**
     * Fill the UI component with both information from the persistent model and some coming from the portlet.xml defined by the
     * JSR 286 specification
     */
    private static <S> void toUIPortlet(UIPortlet<S, ?> uiPortlet, Application<S> model) {

        //
        PortletState<S> portletState = new PortletState<S>(model.getState(), model.getType());

        /*
         * Fill UI component object with info from the XML file that persist portlet information
         */
        uiPortlet.setWidth(model.getWidth());
        uiPortlet.setHeight(model.getHeight());
        uiPortlet.setState(portletState);
        uiPortlet.setTitle(model.getTitle());
        uiPortlet.setIcon(model.getIcon());
        uiPortlet.setDescription(model.getDescription());
        uiPortlet.setShowInfoBar(model.getShowInfoBar());
        uiPortlet.setShowWindowState(model.getShowApplicationState());
        uiPortlet.setShowPortletMode(model.getShowApplicationMode());
        uiPortlet.setProperties(model.getProperties());
        uiPortlet.setTheme(model.getTheme());
        if (model.getAccessPermissions() != null)
            uiPortlet.setAccessPermissions(model.getAccessPermissions());
        uiPortlet.setModifiable(model.isModifiable());

        Portlet portlet = uiPortlet.getProducedOfferedPortlet();
        if (portlet == null || portlet.getInfo() == null)
            return;

        PortletInfo portletInfo = portlet.getInfo();

        /*
         * Define which portlet modes the portlet supports and hence should be shown in the portlet info bar
         */
        Set<ModeInfo> modes = portletInfo.getCapabilities().getModes(MediaType.create("text/html"));
        List<String> supportModes = new ArrayList<String>();
        for (ModeInfo modeInfo : modes) {
            String modeName = modeInfo.getModeName().toLowerCase();
            if ("config".equals(modeInfo.getModeName())) {
                supportModes.add(modeName);
            } else {
                supportModes.add(modeName);
            }
        }

        if (supportModes.size() > 1)
            supportModes.remove("view");
        uiPortlet.setSupportModes(supportModes);
    }

    public static void toUIContainer(UIContainer uiContainer, Container model) throws Exception {
        uiContainer.setStorageId(model.getStorageId());
        uiContainer.setId(model.getId());
        uiContainer.setWidth(model.getWidth());
        uiContainer.setHeight(model.getHeight());
        uiContainer.setProfiles(model.getProfiles());
        uiContainer.setCssClass(model.getCssClass());
        uiContainer.setTitle(model.getTitle());
        uiContainer.setIcon(model.getIcon());
        uiContainer.setDescription(model.getDescription());
        uiContainer.setFactoryId(model.getFactoryId());
        uiContainer.setName(model.getName());
        uiContainer.setTemplate(model.getTemplate());
        if (model.getAccessPermissions() != null) {
            uiContainer.setAccessPermissions(model.getAccessPermissions());
        }
        uiContainer.setMoveAppsPermissions(model.getMoveAppsPermissions());
        uiContainer.setMoveContainersPermissions(model.getMoveContainersPermissions());

        List<ModelObject> children = model.getChildren();
        if (children == null)
            return;
        for (Object child : children) {
            buildUIContainer(uiContainer, child);
        }
    }

    public static void toUIPage(UIPage uiPage, Page model) throws Exception {
        toUIContainer(uiPage, model);
        uiPage.setSiteKey(new SiteKey(model.getOwnerType(), model.getOwnerId()));
        uiPage.setIcon(model.getIcon());
        uiPage.setAccessPermissions(model.getAccessPermissions());
        uiPage.setEditPermission(model.getEditPermission());
        uiPage.setFactoryId(model.getFactoryId());
        uiPage.setPageId(model.getPageId());
        uiPage.setTitle(model.getTitle());
        uiPage.setShowMaxWindow(model.isShowMaxWindow());
        uiPage.setHideSharedLayout(model.isHideSharedLayout());
        uiPage.setModifiable(model.isModifiable());

        List<UIPortlet> portlets = new ArrayList<UIPortlet>();
        uiPage.findComponentOfType(portlets, UIPortlet.class);
        for (UIPortlet portlet : portlets) {
            portlet.setPortletInPortal(false);
        }
    }

    public static void toUIPortal(UIPortal uiPortal, PortalConfig model) throws Exception {
        uiPortal.setSiteKey(new SiteKey(model.getType(), model.getName()));
        uiPortal.setStorageId(model.getStorageId());
        uiPortal.setName(model.getName());
        uiPortal.setId("UIPortal");
        // uiPortal.setFactoryId(model.getFactoryId());
        uiPortal.setModifiable(model.isModifiable());

        uiPortal.setLabel(model.getLabel());
        uiPortal.setDescription(model.getDescription());
        uiPortal.setLocale(model.getLocale());
        uiPortal.setSkin(model.getSkin());
        uiPortal.setAccessPermissions(model.getAccessPermissions());
        uiPortal.setEditPermission(model.getEditPermission());
        uiPortal.setProperties(model.getProperties());
        uiPortal.setRedirects(model.getPortalRedirects());
        uiPortal.setUseDynamicLayout(model.isDefaultLayout());
        LayoutService layoutService = uiPortal.getApplicationComponent(LayoutService.class);
        PortalConfig metaSite = layoutService.getPortalConfig("dw");

        Container layout = model.isDisplayed() ? metaSite.getPortalLayout() : model.getPortalLayout();
        
        uiPortal.setMoveAppsPermissions(layout.getMoveAppsPermissions());
        uiPortal.setMoveContainersPermissions(layout.getMoveContainersPermissions());
        List<ModelObject> children = layout .getChildren();
        if (children != null) {
            for (Object child : children) {
                buildUIContainer(uiPortal, child);
            }
        }
    }

    private static void buildUIContainer(UIContainer uiContainer, Object model) throws Exception {
        UIComponent uiComponent = null;
        WebuiRequestContext context = Util.getPortalRequestContext();

        if (model instanceof SiteBody) {
            UISiteBody uiSiteBody = uiContainer.createUIComponent(context, UISiteBody.class, null, null);
            uiSiteBody.setStorageId(((SiteBody) model).getStorageId());
            uiComponent = uiSiteBody;
        } else if (model instanceof PageBody) {
            UIPageBody uiPageBody = uiContainer.createUIComponent(context, UIPageBody.class, null, null);
            uiPageBody.setStorageId(((PageBody) model).getStorageId());
            uiComponent = uiPageBody;
        } else if (model instanceof Application) {
            Application application = (Application) model;

            UIPortlet uiPortlet = uiContainer.createUIComponent(context, UIPortlet.class, null, null);
            uiPortlet.setStorageId(application.getStorageId());
            if (application.getStorageName() != null) {
                uiPortlet.setStorageName(application.getStorageName());
            }
            toUIPortlet(uiPortlet, application);
            uiComponent = uiPortlet;
        } else if (model instanceof Container) {
            Container container = (Container) model;

            UIComponentFactory<? extends UIContainer> factory = UIComponentFactory.getInstance(UIContainer.class);
            UIContainer uiTempContainer = factory.createUIComponent(container.getFactoryId(), context);

            if (uiTempContainer == null) {
                log.warn("Can't find container factory for: {}. Default container is used", container.getFactoryId());
                uiTempContainer = uiContainer.createUIComponent(context, UIContainer.class, null, null);
            }

            toUIContainer(uiTempContainer, (Container) model);
            uiComponent = uiTempContainer;
        }
        uiContainer.addChild(uiComponent);
    }

}
