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

package org.exoplatform.portal.resource.config.tasks;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;

import org.exoplatform.portal.resource.SkinDependentManager;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.resource.config.xml.SkinConfigParser;

import lombok.Getter;
import lombok.Setter;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * Created by eXoPlatform SAS
 *
 * Author: Minh Hoang TO - hoang281283@gmail.com
 *
 * Sep 16, 2009
 */
public class PortletSkinTask extends AbstractSkinModule implements SkinConfigTask {

    @Getter
    @Setter
    private String applicationName;

    @Getter
    @Setter
    private String portletName;

    @Getter
    @Setter
    private List<String> additionalModules;

    public PortletSkinTask() {
        super(null);
        this.overwrite = true;
    }

    private void bindingApplicationName(Element element) {
        NodeList nodes = element.getElementsByTagName(SkinConfigParser.APPLICATION_NAME_TAG);
        if (nodes == null || nodes.getLength() < 1) {
            return;
        }
        this.applicationName = nodes.item(0).getFirstChild().getNodeValue();
    }

    private void bindingPortletName(Element element) {
        NodeList nodes = element.getElementsByTagName(SkinConfigParser.PORTLET_NAME_TAG);
        if (nodes == null || nodes.getLength() < 1) {
            return;
        }
        this.portletName = nodes.item(0).getFirstChild().getNodeValue();
    }

    protected void bindingAdditionalModules(Element element) {
      NodeList nodes = element.getElementsByTagName(SkinConfigParser.ADDITIONAL_MODULE);
      if (nodes == null || nodes.getLength() == 0) {
        return;
      }
      int length = nodes.getLength();
      List<String> filteredPortalModuleNames = new ArrayList<>();
      for (int i = 0; i < length; i++) {
        filteredPortalModuleNames.add(nodes.item(i).getFirstChild().getNodeValue());
      }
      this.additionalModules = filteredPortalModuleNames;
    }

    public void execute(SkinService skinService, ServletContext scontext) {
        if (portletName == null) {
            return;
        }
        if(skinName == null) {
            skinName = skinService.getDefaultSkin();
        }
        if (applicationName == null) {
            applicationName = scontext.getContextPath();
        }
        String moduleName = applicationName + "/" + portletName;
        String fullCSSPath = cssPath == null ? null : "/" + applicationName + cssPath;
        int priority;
        try {
            priority = Integer.valueOf(cssPriority);
        } catch (Exception e) {
            priority = Integer.MAX_VALUE;
        }
        skinService.addSkin(moduleName, skinName, fullCSSPath, priority, overwrite, additionalModules);
        updateSkinDependentManager("/" + applicationName, moduleName, skinName);
    }

    private void updateSkinDependentManager(String webApp, String moduleName, String skinName) {
        SkinDependentManager.addPortletSkin(webApp, moduleName, skinName);
        SkinDependentManager.addSkinDeployedInApp(webApp, skinName);
    }

    public void binding(Element elemt) {
        bindingApplicationName(elemt);
        bindingPortletName(elemt);
        bindingCSSPath(elemt);
        bindingSkinName(elemt);
        bindingOverwrite(elemt);
        bindingCSSPriority(elemt);
        bindingAdditionalModules(elemt);
    }

}
