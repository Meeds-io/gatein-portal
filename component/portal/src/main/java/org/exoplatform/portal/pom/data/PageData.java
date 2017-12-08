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


/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class PageData extends ContainerData {

    private static final long serialVersionUID = 2741613958131096156L;

    /** . */
    private final PageKey key;

    /** . */
    private final String editPermission;

    /** . */
    private final boolean showMaxWindow;

    public PageData(String storageId, String id, String name, String icon, String template, String factoryId, String title,
            String description, String width, String height, List<String> accessPermissions, List<ComponentData> children,
            String ownerType, String ownerId, String editPermission, boolean showMaxWindow,
            List<String> moveAppsPermissions, List<String> moveContainersPermissions) {
        super(storageId, id, name, icon, template, factoryId, title, description, width, height, accessPermissions,
                moveContainersPermissions, moveContainersPermissions, children);

        //
        this.key = new PageKey(ownerType, ownerId, name);
        this.editPermission = editPermission;
        this.showMaxWindow = showMaxWindow;
    }

    public PageKey getKey() {
        return key;
    }

    public String getOwnerType() {
        return key.getType();
    }

    public String getOwnerId() {
        return key.getId();
    }

    public String getEditPermission() {
        return editPermission;
    }

    public boolean isShowMaxWindow() {
        return showMaxWindow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageData)) return false;
        if (!super.equals(o)) return false;

        PageData pageData = (PageData) o;

        return key != null ? key.equals(pageData.key) : pageData.key == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
