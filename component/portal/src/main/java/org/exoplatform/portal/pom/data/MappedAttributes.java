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

package org.exoplatform.portal.pom.data;

import org.gatein.mop.api.Key;
import org.gatein.mop.api.ValueType;

/**
 * A class to hold the various attributes mapped between the model and the mop layer.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class MappedAttributes {

    private MappedAttributes() {
    }

    /** . */
    public static final Key<String> ID = Key.create("id", ValueType.STRING);

    /** . */
    public static final Key<String> NAME = Key.create("name", ValueType.STRING);

    /** . */
    public static final Key<Boolean> SHOW_MAX_WINDOW = Key.create("show-max-window", ValueType.BOOLEAN);

    /** . */
    public static final Key<String> FACTORY_ID = Key.create("factory-id", ValueType.STRING);

    /** . */
    public static final Key<String> CSS_CLASS = Key.create("css-class", ValueType.STRING);

    /** . */
    public static final Key<String> PROFILES = Key.create("profiles", ValueType.STRING);

    /** . */
    public static final Key<Integer> PRIORITY = Key.create("priority", ValueType.INTEGER);

    /** . */
    public static final Key<String> ICON = Key.create("icon", ValueType.STRING);

    /** . */
    public static final Key<String> URI = Key.create("uri", ValueType.STRING);

    /** . */
    public static final Key<String> TEMPLATE = Key.create("template", ValueType.STRING);

    /** . */
    public static final Key<String> LOCALE = Key.create("locale", ValueType.STRING);

    /** . */
    public static final Key<String> SKIN = Key.create("skin", ValueType.STRING);

    /** . */
    public static final Key<String> WIDTH = Key.create("width", ValueType.STRING);

    /** . */
    public static final Key<String> HEIGHT = Key.create("height", ValueType.STRING);

    /** . */
    public static final Key<String> TYPE = Key.create("type", ValueType.STRING);

    /** . */
    public static final Key<Boolean> SHOW_INFO_BAR = Key.create("showinfobar", ValueType.BOOLEAN);

    /** . */
    public static final Key<Boolean> SHOW_MODE = Key.create("showmode", ValueType.BOOLEAN);

    /** . */
    public static final Key<Boolean> SHOW_WINDOW_STATE = Key.create("showwindowstate", ValueType.BOOLEAN);

    /** . */
    public static final Key<String> THEME = Key.create("theme", ValueType.STRING);
}
