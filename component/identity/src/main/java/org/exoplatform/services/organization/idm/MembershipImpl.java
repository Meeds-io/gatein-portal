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

package org.exoplatform.services.organization.idm;

import org.exoplatform.services.organization.Membership;

/*
 * @author <a href="mailto:boleslaw.dawidowicz at redhat.com">Boleslaw Dawidowicz</a>
 */
public class MembershipImpl extends org.exoplatform.services.organization.impl.MembershipImpl implements Comparable {
    private static final long serialVersionUID = -8572523969777642576L;

    public MembershipImpl() {
    }

    public MembershipImpl(String id) {
        String[] fields = id.split(":");

        // Id can be pure "//" in some cases
        if (fields[0] != null) {
            setMembershipType(fields[0]);
        }
        if (fields[1] != null) {
            setUserName(fields[1]);
        }
        if (fields[2] != null) {
            setGroupId(fields[2]);
        }
    }

    public String getId() {
        StringBuffer id = new StringBuffer();

        if (getMembershipType() != null) {
            id.append(getMembershipType());
        }
        id.append(":");
        if (getUserName() != null) {
            id.append(getUserName());
        }
        id.append(":");
        if (getGroupId() != null) {
            id.append(getGroupId());
        }

        return id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MembershipImpl)) {
            return false;
        }

        MembershipImpl that = (MembershipImpl) o;

        if (getGroupId() != null ? !getGroupId().equals(that.getGroupId()) : that.getGroupId() != null) {
            return false;
        }
        if (getMembershipType() != null ? !getMembershipType().equals(that.getMembershipType()) : that.getMembershipType() != null) {
            return false;
        }
        if (getUserName() != null ? !getUserName().equals(that.getUserName()) : that.getUserName() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = getMembershipType() != null ? getMembershipType().hashCode() : 0;
        result = 31 * result + (getUserName() != null ? getUserName().hashCode() : 0);
        result = 31 * result + (getGroupId() != null ? getGroupId().hashCode() : 0);
        return result;
    }

    public int compareTo(Object o) {
        if (!(o instanceof Membership) || getUserName() == null) {
            return 0;
        }

        Membership m = (Membership) o;

        return getUserName().compareTo(m.getUserName());
    }

    @Override
    public String toString() {
        return "MembershipImpl{" + "MembershipType='" + getMembershipType() + '\'' + ", userName='" + getUserName() + '\''
                + ", GroupId ='" + getGroupId() + '\'' + '}';
    }
}
