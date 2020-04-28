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

package org.gatein.portal.controller.resource;

import java.util.Date;

import org.exoplatform.commons.utils.PropertyManager;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
class ScriptResult {

    private ScriptResult() {
    }

    static class Resolved extends ScriptResult {

        /** . */
        final byte[] bytes;

        final long lastModified;

        Resolved(byte[] bytes) {
            this.bytes = bytes;
            // string of date retrieve from Http header doesn't have miliseconds
            // we need to remove miliseconds
            lastModified = (new Date().getTime() / 1000) * 1000;
        }

        boolean isModified(long ifModifiedSince) {
            if (PropertyManager.isDevelopping()) {
                return true;
            } else {
                return lastModified > ifModifiedSince;
            }
        }
    }

    static class Error extends ScriptResult {

        /** . */
        final String message;

        Error(String message) {
            this.message = message;
        }
    }

    static ScriptResult NOT_FOUND = new ScriptResult();

}
