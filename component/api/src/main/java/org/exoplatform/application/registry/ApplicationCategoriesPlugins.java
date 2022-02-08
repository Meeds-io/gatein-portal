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

package org.exoplatform.application.registry;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;

/**
 * Created by The eXo Platform SARL Author : Le Bien Thuy lebienthuy@gmail.com 9
 * Oct 2007
 */

public class ApplicationCategoriesPlugins extends BaseComponentPlugin {
  private ConfigurationManager       cmanager_;

  private ApplicationRegistryService pdcService_;

  private List<ApplicationCategory>  categories;

  private boolean                    merge;

  private boolean                    system;

  public ApplicationCategoriesPlugins(ApplicationRegistryService pdcService, ConfigurationManager cmanager, InitParams params)
      throws Exception {
    categories = params.getObjectParamValues(ApplicationCategory.class);
    if (params.containsKey("merge")) {
      merge = StringUtils.equalsIgnoreCase("true", params.getValueParam("merge").getValue());
    }
    if (params.containsKey("system")) {
      system = StringUtils.equalsIgnoreCase("true", params.getValueParam("system").getValue());
    }
    cmanager_ = cmanager;
    pdcService_ = pdcService;
  }

  public boolean isMerge() {
    return merge;
  }

  public void setMerge(boolean merge) {
    this.merge = merge;
  }

  public void run() throws Exception {
    run(false);
  }

  public List<ApplicationCategory> getCategories() {
    return categories;
  }

  public void run(boolean firstStartup) throws Exception {
    if (categories == null || (!firstStartup && !merge && !system))
      return;
    for (ApplicationCategory category : categories) {
      ApplicationCategory storedCategory = pdcService_.getApplicationCategory(category.getName());
      List<Application> apps = category.getApplications();
      // Recreate category when starting server if deleted by UI for categories
      // of type 'merge = true'
      if (storedCategory == null) {
        pdcService_.save(category);
      }

      // Avoid to reimport applications when deleted by UI in case of 'merge =
      // true'
      if (firstStartup || storedCategory == null) {
        for (Application app : apps) {
          pdcService_.save(category, app);
        }
      } else if (system) {
        for (Application app : apps) {
          Application storedApplication = pdcService_.getApplication(category.getName() + "/" + app.getApplicationName());
          // Avoid to reimport application when modified by UI in case of
          // 'system = true', it will be imported only when not existing
          if (storedApplication == null) {
            pdcService_.save(category, app);
          }
        }
      }
    }
  }
}
