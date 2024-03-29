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

import org.exoplatform.portal.pom.config.Utils;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author  <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalKey extends OwnerKey {

  private static final long serialVersionUID = -6089922297660724879L;

  public PortalKey(String type, String id) {
    super(type, id);
  }

  public static PortalKey create(String compositeId) {
    if (compositeId == null) {
      throw new NullPointerException();
    }
    String[] components = Utils.split("::", compositeId);
    if (components.length != 2) {
      throw new IllegalArgumentException("Wrong navigation id key format " + compositeId);
    }
    return new PortalKey(components[0], components[1]);
  }

  @Override
  public String toString() {
    return String.format("%s::%s", getType(), getId());
  }
}
