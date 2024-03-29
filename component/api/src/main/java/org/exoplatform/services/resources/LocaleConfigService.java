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

package org.exoplatform.services.resources;

import java.util.Collection;


/**
 * @author Benjamin Mestrallet benjamin.mestrallet@exoplatform.com This Service is used to manage the locales that the
 *         applications can handle
 */
public interface LocaleConfigService {

    /**
     * @return Return the default LocaleConfig
     */
    LocaleConfig getDefaultLocaleConfig();

    /**
     * @param lang a locale language
     * @return The LocalConfig
     */
    LocaleConfig getLocaleConfig(String lang);

    /**
     * @return All the LocalConfig that manage by the service
     */
    Collection<LocaleConfig> getLocalConfigs();

    /**
     * Saves new default locale
     * 
     * @param locale {@link LocaleConfig#getLocaleName()} to set as default
     */
    default void saveDefaultLocaleConfig(String locale) {
      throw new UnsupportedOperationException();
    }

}
