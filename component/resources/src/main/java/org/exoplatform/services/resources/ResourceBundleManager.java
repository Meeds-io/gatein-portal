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

package org.exoplatform.services.resources;

import java.util.ResourceBundle;


/**
 * Encapsulate how the portal obtain resources bundles.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class ResourceBundleManager {

    /** . */
    private final LocaleConfigService localConfigService;

    public ResourceBundleManager(LocaleConfigService localConfigService) {
        this.localConfigService = localConfigService;
    }

    public ResourceBundle getNavigationResourceBundle(String language, String ownerType, String ownerId) {
        LocaleConfig localeConfig = localConfigService.getLocaleConfig(language);
        if (localeConfig != null) {
            if (ownerType.equals("group")) {
                // Remove the trailing '/' for a group
                ownerId = ownerId.substring(1);
            }
            return localeConfig.getNavigationResourceBundle(ownerType, ownerId);
        }
        return null;
    }
}
