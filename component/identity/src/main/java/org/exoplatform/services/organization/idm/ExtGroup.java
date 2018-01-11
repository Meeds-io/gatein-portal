/*
 * JBoss, a division of Red Hat
 * Copyright 2010, Red Hat Middleware, LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.exoplatform.services.organization.idm;

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.impl.GroupImpl;

/*
 * @author <a href="mailto:boleslaw.dawidowicz at redhat.com">Boleslaw Dawidowicz</a>
 */
public class ExtGroup extends GroupImpl implements Comparable {
    private static final long serialVersionUID = -7379104016885124958L;

    public ExtGroup() {

    }

    public ExtGroup(String name) {
        super(name);
    }

    public String toString() {
        return "Group[" + getId() + "|" + getGroupName() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExtGroup)) {
            return false;
        }

        ExtGroup extGroup = (ExtGroup) o;

        if (getDescription() != null ? !getDescription().equals(extGroup.getDescription()) : extGroup.getDescription() != null) {
            return false;
        }
        if (getGroupName() != null ? !getGroupName().equals(extGroup.getGroupName()) : extGroup.getGroupName() != null) {
            return false;
        }
        if (getId() != null ? !getId().equals(extGroup.getId()) : extGroup.getId() != null) {
            return false;
        }
        if (getLabel() != null ? !getLabel().equals(extGroup.getLabel()) : extGroup.getLabel() != null) {
            return false;
        }
        if (getParentId() != null ? !getParentId().equals(extGroup.getParentId()) : extGroup.getParentId() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getParentId() != null ? getParentId().hashCode() : 0);
        result = 31 * result + (getGroupName() != null ? getGroupName().hashCode() : 0);
        result = 31 * result + (getLabel() != null ? getLabel().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        return result;
    }

    public int compareTo(Object o) {
        if (!(o instanceof Group)) {
            return 0;
        }

        Group group = (Group) o;

        return getGroupName().compareTo(group.getGroupName());

    }
}
