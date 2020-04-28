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

package org.exoplatform.portal.application;

import java.util.Locale;
import java.util.ResourceBundle;

import org.exoplatform.services.portletcontainer.bundle.ResourceBundleDelegate;
import org.exoplatform.services.resources.ResourceBundleService;

/**
 * @author Benjamin Mestrallet benjamin.mestrallet@exoplatform.com
 */
public class ResourceBundleDelegateImpl implements ResourceBundleDelegate {

    private ResourceBundleService resourceBundleService;

    public ResourceBundleDelegateImpl(ResourceBundleService resourceBundleService) {
        this.resourceBundleService = resourceBundleService;
    }

    public ResourceBundle lookupBundle(String portletBundleName, Locale locale) {
        String[] portalBundles = resourceBundleService.getSharedResourceBundleNames();
        String[] bundles = new String[portalBundles.length + 1];
        for (int i = 0; i < portalBundles.length; i++) {
            bundles[i] = portalBundles[i];
        }
        bundles[portalBundles.length] = portletBundleName;
        return resourceBundleService.getResourceBundle(bundles, locale);
    }

}
