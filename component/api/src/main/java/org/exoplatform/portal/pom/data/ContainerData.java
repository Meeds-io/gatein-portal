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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(force = true)
public class ContainerData extends ComponentData {

    private static final long serialVersionUID = 7328080023001711540L;

    /** . */
    private final String id;

    /** . */
    private final String name;

    /** . */
    private final String icon;

    /** . */
    private final String template;

    /** . */
    private final String factoryId;

    /** . */
    private final String title;

    /** . */
    private final String description;

    /** . */
    private final String width;

    /** . */
    private final String height;

    /** . */
    private final String cssClass;

    /** . */
    private final String borderColor;

    /** . */
    private final String profiles;

    /** . */
    private final List<String> accessPermissions;

    private final List<String> moveAppsPermissions;

    private final List<String> moveContainersPermissions;

    /** . */
    private final List<ComponentData> children;

    public ContainerData(String storageId,
                         String id,
                         String name,
                         String icon,
                         String template,
                         String factoryId,
                         String title,
                         String description,
                         String width,
                         String height,
                         String cssClass,
                         String borderColor,
                         String profiles,
                         List<String> accessPermissions,
                         List<String> moveAppsPermissions,
                         List<String> moveContainersPermissions,
                         List<ComponentData> children) {
        super(storageId, null);

        //
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.template = template;
        this.factoryId = factoryId;
        this.title = title;
        this.description = description;
        this.width = width;
        this.height = height;
        this.cssClass = cssClass;
        this.borderColor = borderColor;
        this.profiles = profiles;
        this.accessPermissions = accessPermissions;
        this.moveAppsPermissions = moveAppsPermissions;
        this.moveContainersPermissions = moveContainersPermissions;
        this.children = children;
    }

}
