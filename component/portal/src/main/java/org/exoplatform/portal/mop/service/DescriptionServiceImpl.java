/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.portal.mop.service;

import org.apache.commons.collections.MapUtils;
import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleManager;

import java.util.*;

public class DescriptionServiceImpl implements DescriptionService {

  private DescriptionStorage descriptionStorage;

  private NavigationService navigationService;

  private LocaleConfigService localeConfigService;

  private ResourceBundleManager resourceBundleManager;


  public DescriptionServiceImpl(DescriptionStorage descriptionStorage, NavigationService navigationService,
                                LocaleConfigService localeConfigService, ResourceBundleManager resourceBundleManager) {
    this.descriptionStorage = descriptionStorage;
    this.navigationService = navigationService;
    this.localeConfigService = localeConfigService;
    this.resourceBundleManager = resourceBundleManager;
  }

  @Override
  public Map<Locale, State> getDescriptions(String nodeId) {
    Map<Locale, State> nodeLabels = descriptionStorage.getDescriptions(nodeId);
    NodeData nodeData = navigationService.getNodeById(Long.valueOf(nodeId));
    if (MapUtils.isEmpty(nodeLabels)) {
      Map<Locale, State> nodeLocalizedLabels = new HashMap<>();
      localeConfigService.getLocalConfigs().forEach(localeConfig -> {
        Locale locale = localeConfig.getLocale();
        String label = nodeData.getState().getLabel();
        if (ExpressionUtil.isResourceBindingExpression(label)) {
          SiteKey siteKey = nodeData.getSiteKey();
          ResourceBundle nodeLabelResourceBundle = resourceBundleManager.getNavigationResourceBundle(getLocaleName(locale),
                  siteKey.getTypeName(),
                  siteKey.getName());
          if (nodeLabelResourceBundle != null) {
            label = ExpressionUtil.getExpressionValue(nodeLabelResourceBundle, label);
          }
        }
        nodeLocalizedLabels.put(locale, new State(label, null));
      });
      return nodeLocalizedLabels;
    } else {
      return nodeLabels;
    }
  }

  @Override
  public void setDescriptions(String id, Map<Locale, State> descriptions) {
    descriptionStorage.setDescriptions(id, descriptions);
  }

  private String getLocaleName(Locale locale) {
    return locale.toLanguageTag().replace("-", "_"); // Use same name as
    // localeConfigService
  }
}
