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

package org.exoplatform.portal.pom.data;

import org.gatein.mop.api.Key;
import org.gatein.mop.api.ValueType;

/**
 * A class to hold the various attributes mapped between the model and the mop
 * layer.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class MappedAttributes {

  private MappedAttributes() {
  }

  /** . */
  public static final Key<String>  ID                  = Key.create("id", ValueType.STRING);

  /** . */
  public static final Key<String>  NAME                = Key.create("name", ValueType.STRING);

  /** . */
  public static final Key<Boolean> SHOW_MAX_WINDOW     = Key.create("show-max-window", ValueType.BOOLEAN);

  public static final Key<Boolean> HIDE_SHARED_LAYOUT  = Key.create("hide-shared-layout", ValueType.BOOLEAN);

  /** . */
  public static final Key<String>  FACTORY_ID          = Key.create("factory-id", ValueType.STRING);

  /** . */
  public static final Key<String>  CSS_CLASS           = Key.create("css-class", ValueType.STRING);

  public static final Key<String>  BORDER_COLOR        = Key.create("border-color", ValueType.STRING);

  public static final Key<String>  TEXT_COLOR          = Key.create("text-color", ValueType.STRING);

  public static final Key<String>  TEXT_HEADER_COLOR   = Key.create("text-header-color", ValueType.STRING);

  public static final Key<String>  TEXT_SUBTITLE_COLOR = Key.create("text-subtitle-color", ValueType.STRING);

  public static final Key<String>  BACKGROUND_COLOR    = Key.create("background-color", ValueType.STRING);

  public static final Key<String>  BACKGROUND_IMAGE    = Key.create("background-image", ValueType.STRING);

  public static final Key<String>  BACKGROUND_EFFECT   = Key.create("background-effect", ValueType.STRING);

  public static final Key<String>  BACKGROUND_POSITION = Key.create("background-position", ValueType.STRING);

  public static final Key<String>  BACKGROUND_REPEAT   = Key.create("background-repeat", ValueType.STRING);

  public static final Key<String>  BACKGROUND_SIZE     = Key.create("background-size", ValueType.STRING);

  public static final Key<String>  BORDER_SIZE         = Key.create("border-size", ValueType.STRING);

  public static final Key<String>  BOX_SHADOW          = Key.create("box-shadow", ValueType.STRING);

  /** . */
  public static final Key<String>  PROFILES            = Key.create("profiles", ValueType.STRING);

  /** . */
  public static final Key<Integer> PRIORITY            = Key.create("priority", ValueType.INTEGER);

  /** . */
  public static final Key<String>  ICON                = Key.create("icon", ValueType.STRING);

  /** . */
  public static final Key<String>  URI                 = Key.create("uri", ValueType.STRING);

  /** . */
  public static final Key<String>  TEMPLATE            = Key.create("template", ValueType.STRING);

  /** . */
  public static final Key<String>  LOCALE              = Key.create("locale", ValueType.STRING);

  /** . */
  public static final Key<String>  SKIN                = Key.create("skin", ValueType.STRING);

  /** . */
  public static final Key<String>  WIDTH               = Key.create("width", ValueType.STRING);

  /** . */
  public static final Key<String>  HEIGHT              = Key.create("height", ValueType.STRING);

  /** . */
  public static final Key<String>  TYPE                = Key.create("type", ValueType.STRING);

  /** . */
  public static final Key<Boolean> SHOW_INFO_BAR       = Key.create("showinfobar", ValueType.BOOLEAN);

  /** . */
  public static final Key<Boolean> SHOW_MODE           = Key.create("showmode", ValueType.BOOLEAN);

  /** . */
  public static final Key<Boolean> SHOW_WINDOW_STATE   = Key.create("showwindowstate", ValueType.BOOLEAN);

  /** . */
  public static final Key<String>  THEME               = Key.create("theme", ValueType.STRING);
}
