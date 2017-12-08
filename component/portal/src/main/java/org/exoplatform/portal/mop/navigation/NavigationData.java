/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.exoplatform.portal.mop.navigation;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.gatein.mop.api.workspace.Navigation;

/**
 * An immutable navigation data class.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class NavigationData implements Serializable {

    /** Useful. */
    static final NavigationData EMPTY = new NavigationData();

    /** . */
    final SiteKey key;

    /** . */
    final NavigationState state;

    /** . */
    final String rootId;

    private NavigationData() {
        this.key = null;
        this.state = null;
        this.rootId = null;
    }

    NavigationData(SiteKey key, Navigation node) {
        String rootId = node.getObjectId();
        NavigationState state = new NavigationState(node.getAttributes().getValue(MappedAttributes.PRIORITY, 1));

        //
        this.key = key;
        this.state = state;
        this.rootId = rootId;
    }

    NavigationData(SiteKey key, NavigationState state, String rootId) {
        this.key = key;
        this.state = state;
        this.rootId = rootId;
    }

    protected Object readResolve() {
        if (key == null && state == null && rootId == null) {
            return EMPTY;
        } else {
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavigationData)) return false;

        NavigationData that = (NavigationData) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        return StringUtils.equals(rootId,that.rootId);

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (rootId != null ? rootId.hashCode() : 0);
        return result;
    }
}
