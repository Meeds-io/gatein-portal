/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.gatein.portal.controller.resource;

import java.io.Serializable;
import java.util.Locale;

import org.exoplatform.commons.utils.Safe;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
class ScriptKey implements Serializable {

    /** . */
    final ResourceId id;

    /** . */
    final boolean minified;

    /** . */
    final Locale locale;

    /** . */
    final int hashCode;

    ScriptKey(ResourceId id, boolean minified, Locale locale) {
        this.id = id;
        this.minified = minified;
        this.locale = locale;
        this.hashCode = id.hashCode() ^ (locale != null ? locale.hashCode() : 0) ^ (minified ? ~1 : 0);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ScriptKey) {
            ScriptKey that = (ScriptKey) obj;
            return id.equals(that.id) && (minified == that.minified) && Safe.equals(locale, that.locale);
        }
        return false;
    }

    @Override
    public String toString() {
        return "ScriptKey[id=" + id + ",minified=" + minified + ",locale=" + locale + "]";
    }
}
