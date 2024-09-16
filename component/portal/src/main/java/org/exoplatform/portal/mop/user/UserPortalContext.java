/*
 * Copyright (C) 2010 eXo Platform SAS.
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

package org.exoplatform.portal.mop.user;

import java.util.Locale;
import java.util.ResourceBundle;


/**
 * The context of a user within its portal.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 * @deprecated Not needed anymore in Meeds context, should be removed after
 */
@Deprecated(forRemoval = true, since = "7.0")
public interface UserPortalContext {

    /**
     * Provide an opportunity to use a resource bundle for a specified navigation. It no such bundle can be found then null can
     * be returned.
     *
     * @param navigation the navigation that will be localized
     * @return the resource bundle to use
     */
    ResourceBundle getBundle(UserNavigation navigation);

    /**
     * Returns the user locale.
     *
     * @return the user locale
     */
    Locale getUserLocale();
}
