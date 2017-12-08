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

import java.io.Serializable;
import java.util.Objects;


/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class ModelData implements Serializable {

    /** Storage id. */
    private final String storageId;

    /** The storage name that is unique among a container context. */
    private final String storageName;

    protected ModelData(String storageId, String storageName) {
        this.storageId = storageId;
        this.storageName = storageName;
    }

    public String getStorageId() {
        return storageId;
    }

    public String getStorageName() {
        return storageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelData)) return false;
        ModelData modelData = (ModelData) o;
        return Objects.equals(storageId, modelData.storageId) &&
                Objects.equals(storageName, modelData.storageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageId, storageName);
    }
}
