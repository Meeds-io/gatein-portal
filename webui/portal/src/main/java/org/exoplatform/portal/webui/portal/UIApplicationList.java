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

package org.exoplatform.portal.webui.portal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.exoplatform.application.registry.Application;
import org.exoplatform.application.registry.ApplicationCategory;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS Author : Pham Thanh Tung thanhtungty@gmail.com Jun 11, 2009
 */
@ComponentConfig(template = "system:/groovy/portal/webui/application/UIApplicationList.gtmpl", events = { @EventConfig(listeners = UIApplicationList.SelectCategoryActionListener.class) })
public class UIApplicationList extends UIContainer {
    private List<ApplicationCategory> categories;

    private ApplicationCategory selectedCategory;

    public UIApplicationList() {
    }

    public Application getApplication(String id) {
        for (ApplicationCategory category : getCategories()) {
            List<Application> items = category.getApplications();
            for (Application item : items) {
                if (item.getId().equals(id))
                    return item;
            }
        }
        return null;
    }

    public ApplicationCategory getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String categoryName) {
        for (ApplicationCategory category : getCategories()) {
            if (category.getName().equals(categoryName)) {
                selectedCategory = category;
            }
        }
    }

    public List<Application> getApplications() {
        if (selectedCategory == null)
            return null;

        String remoteUser = Util.getPortalRequestContext().getRemoteUser();
        if (remoteUser == null || remoteUser.equals("")) {
            return null;
        }

        UserACL userACL = getApplicationComponent(UserACL.class);

        List<Application> allApps = selectedCategory.getApplications();
        List<Application> apps = new ArrayList<Application>();

        for (Application app : allApps) {
            List<String> accessPermission = app.getAccessPermissions();
            if (accessPermission == null || accessPermission.size() == 0) {
                continue;
            }

            for (String p : accessPermission) {
                if (userACL.hasPermission(p)) {
                    apps.add(app);
                    break;
                }
            }
        }

        return apps;
    }

    public List<ApplicationCategory> getCategories() {
        try {
            // TODO: Handle concurrent requests associated with current session
            if (categories == null) {
                initAllCategories();
            }
            return categories;
        } catch (Exception ex) {
            return null;
        }
    }

    private void initAllCategories() throws Exception {
        String remoteUser = Util.getPortalRequestContext().getRemoteUser();
        if (remoteUser == null || remoteUser.equals("")) {
            return;
        }

        ApplicationRegistryService service = getApplicationComponent(ApplicationRegistryService.class);
        UserACL userACL = getApplicationComponent(UserACL.class);

        final Comparator<Application> appComparator = new Comparator<Application>() {
            public int compare(Application p_1, Application p_2) {
                return p_1.getDisplayName().compareToIgnoreCase(p_2.getDisplayName());
            }
        };
        final Comparator<ApplicationCategory> cateComparator = new Comparator<ApplicationCategory>() {
            public int compare(ApplicationCategory p_1, ApplicationCategory p_2) {
                return p_1.getDisplayName(true).compareToIgnoreCase(p_2.getDisplayName(true));
            }
        };

        List<ApplicationCategory> allCategories = service.getApplicationCategories(remoteUser);
        categories = new ArrayList<ApplicationCategory>();

        for (ApplicationCategory category : allCategories) {
            List<Application> apps = category.getApplications();
            List<String> accessPermission = category.getAccessPermissions();
            if (accessPermission == null) {
                continue;
            }

            for (String p : accessPermission) {
                if (userACL.hasPermission(p)) {
                    if (apps.size() > 0) {
                        Collections.sort(apps, appComparator);
                    }
                    categories.add(category);
                    break;
                }
            }
        }

        if (categories.size() > 0) {
            Collections.sort(categories, cateComparator);
            selectedCategory = categories.get(0);
        }
    }

    public static class SelectCategoryActionListener extends EventListener<UIApplicationList> {
        public void execute(Event<UIApplicationList> event) throws Exception {
            String category = event.getRequestContext().getRequestParameter(OBJECTID);
            UIApplicationList uiApplicationList = event.getSource();
            uiApplicationList.setSelectedCategory(category);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApplicationList);
        }

    }
}
