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

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalData extends ModelData {

  public static final PortalData    NULL_OBJECT      = new PortalData();

  private static final long         serialVersionUID = 5245652127699244955L;

  /** . */
  private final PortalKey           key;

  /** . */
  private final String              locale;

  /** . */
  private final List<String>        accessPermissions;

  /** . */
  private final String              editPermission;

  /** . */
  private final Map<String, String> properties;

  /** . */
  private final String              skin;

  /** . */
  private final ContainerData       portalLayout;

  private final boolean             defaultLayout;

  private final String              label;

  private final String              description;

  private final boolean             displayed;

  private final int                 displayOrder;

  private long                      bannerFileId;

  private PortalData() {
    super(null, null);
    this.key = null;
    this.locale = null;
    this.label = null;
    this.description = null;
    this.accessPermissions = null;
    this.editPermission = null;
    this.properties = null;
    this.skin = null;
    this.portalLayout = null;
    this.defaultLayout = false;
    this.displayed = true;
    this.displayOrder = 0;
  }

  public PortalData(String storageId, // NOSONAR
                    String name,
                    String type,
                    String locale,
                    String label,
                    String description,
                    List<String> accessPermissions,
                    String editPermission,
                    Map<String, String> properties,
                    String skin,
                    ContainerData portalLayout,
                    boolean displayed,
                    int displayOrder,
                    long bannerFileId) {
    this(storageId,
         name,
         type,
         locale,
         label,
         description,
         accessPermissions,
         editPermission,
         properties,
         skin,
         portalLayout,
         false,
         displayed,
         displayOrder,
         bannerFileId);
  }

  public PortalData(String storageId, // NOSONAR
                    String name,
                    String type,
                    String locale,
                    String label,
                    String description,
                    List<String> accessPermissions,
                    String editPermission,
                    Map<String, String> properties,
                    String skin,
                    ContainerData portalLayout,
                    boolean defaultLayout,
                    boolean displayed,
                    int displayOrder,
                    long bannerFileId) {
    super(storageId, null);

    //
    this.key = new PortalKey(type, name);
    this.locale = locale;
    this.label = label;
    this.description = description;
    this.accessPermissions = accessPermissions;
    this.editPermission = editPermission;
    this.properties = properties;
    this.skin = skin;
    this.portalLayout = portalLayout;
    this.defaultLayout = defaultLayout;
    this.displayed = displayed;
    this.displayOrder = displayOrder;
    this.bannerFileId = bannerFileId;
  }

  public String getName() {
    return key.getId();
  }

  public String getType() {
    return key.getType();
  }

  public boolean isNull() {
    return key == null;
  }
}
