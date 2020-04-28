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

package org.exoplatform.web.portal;

import java.util.*;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserPortalContext;

/**
 * A simple implementation for unit tests.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class SimpleUserPortalContext implements UserPortalContext {

    /** . */
    private final Map<SiteKey, ResourceBundle> bundles;

    /** . */
    private final Locale locale;

    public SimpleUserPortalContext(Locale locale) {
        this.locale = locale;
        this.bundles = new HashMap<SiteKey, ResourceBundle>();
    }

    void add(SiteKey key, ResourceBundle bundle) {
        bundles.put(key, bundle);
    }

    public ResourceBundle getBundle(UserNavigation navigation) {
        return bundles.get(navigation.getKey());
    }

    public Locale getUserLocale() {
        return locale;
    }
}