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

package org.exoplatform.portal.config.model;

import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.BodyData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.ModelData;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class ModelObject {

    /** Storage id. */
    String storageId;

    /** The storage name that is unique among a container context. */
    String storageName;

    /**
     * Create a new object.
     *
     * @param storageId if the storage id is null
     */
    public ModelObject(String storageId) {
        this.storageId = storageId;
    }

    protected ModelObject() {
        this.storageId = null;
    }

    public String getStorageId() {
        return storageId;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public void resetStorage() {
      this.storageId = null;
      this.storageName = null;
    }

    public abstract ModelData build();

    public static ModelObject build(ModelData data) {
        if (data instanceof ContainerData) {
            return new Container((ContainerData) data);
        } else if (data instanceof PageData) {
            return new Page((PageData) data);
        } else if (data instanceof BodyData) {
            BodyData bodyData = (BodyData) data;
            switch (bodyData.getType()) {
                case PAGE:
                    return new PageBody(data.getStorageId());
                case SITE:
                    return new SiteBody(data.getStorageId());
                default:
                    throw new AssertionError();
            }
        } else if (data instanceof ApplicationData) {
            ApplicationData applicationData = (ApplicationData) data;
            ApplicationType type = applicationData.getType();
            if (ApplicationType.PORTLET == type) {
                return Application.createPortletApplication((ApplicationData<Portlet>) applicationData);
            }
        }
        return null;
    }
}
