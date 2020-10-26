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

package org.exoplatform.portal.mop.navigation;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class HierarchyException extends RuntimeException {

  private static final long    serialVersionUID = -1801123785396739016L;

  /** . */
  private final HierarchyError error;

  public HierarchyException(HierarchyError error) {
    super(error.toString());
    this.error = error;
  }

  public HierarchyException(HierarchyError error, String message) {
    super(message);

    //
    this.error = error;
  }

  public HierarchyException(HierarchyError error, String message, Throwable cause) {
    super(message, cause);

    //
    this.error = error;
  }

  public HierarchyException(HierarchyError error, Throwable cause) {
    super(cause);

    //
    this.error = error;
  }

  public HierarchyError getError() {
    return error;
  }
}
