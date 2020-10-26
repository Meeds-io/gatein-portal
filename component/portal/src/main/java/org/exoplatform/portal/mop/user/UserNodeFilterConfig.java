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

package org.exoplatform.portal.mop.user;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.mop.Visibility;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class UserNodeFilterConfig {

    /** . */
    public static final int AUTH_NO_CHECK = 0;

    /** . */
    public static final int AUTH_READ = 1;

    /** . */
    public static final int AUTH_READ_WRITE = 2;

    /** . */
    final Set<Visibility> visibility;

    /** . */
    final int authorizationMode;

    /** . */
    final boolean temporalCheck;

    /** An optional path restricting what should be seen, this is not exposed outside this API (!!!). */
    String[] path;

    public UserNodeFilterConfig(Builder builder) {
        if (builder == null) {
            throw new NullPointerException();
        }

        //
        this.visibility = Safe.unmodifiableSet(builder.withVisibility);
        this.authorizationMode = builder.withAuthorizationMode;
        this.temporalCheck = builder.withTemporalCheck;
        this.path = builder.path;
    }

    public Set<Visibility> getVisibility() {
        return visibility;
    }

    public int getAuthorizationMode() {
        return authorizationMode;
    }

    public boolean getTemporalCheck() {
        return temporalCheck;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(UserNodeFilterConfig predicate) {
        return new Builder(predicate);
    }

    public static class Builder {

        /** . */
        private Set<Visibility> withVisibility;

        /** . */
        private int withAuthorizationMode;

        /** . */
        private boolean withTemporalCheck;

        /** . */
        private String[] path;

        private Builder() {
            this.withVisibility = null;
            this.withAuthorizationMode = AUTH_NO_CHECK;
            this.withTemporalCheck = false;
            this.path = null;
        }

        private Builder(UserNodeFilterConfig predicate) {
            this.withVisibility = predicate.visibility;
            this.withAuthorizationMode = predicate.authorizationMode;
            this.withTemporalCheck = predicate.temporalCheck;
            this.path = predicate.path;
        }

        public Builder withVisibility(Visibility first, Visibility... rest) {
            withVisibility = EnumSet.of(first, rest);
            return this;
        }

        public Builder withVisibility(Visibility first) {
            withVisibility = EnumSet.of(first);
            return this;
        }

        public Builder withVisibility(Visibility[] all) {
            if (all.length == 0) {
                withVisibility = Collections.emptySet();
            } else if (all.length == 1) {
                withVisibility = EnumSet.of(all[0]);
            } else {
                Visibility[] rest = new Visibility[all.length - 1];
                System.arraycopy(all, 1, rest, 0, rest.length);
                withVisibility = EnumSet.of(all[0], rest);
            }
            return this;
        }

        public Builder withoutVisibility() {
            withVisibility = null;
            return this;
        }

        public Builder withTemporalCheck() {
            this.withTemporalCheck = true;
            return this;
        }

        public Builder withoutTemporalCheck() {
            this.withTemporalCheck = false;
            return this;
        }

        public Builder withAuthMode(int withAuthorizationMode) {
            if (withAuthorizationMode < 0 || withAuthorizationMode > 2) {
                throw new IllegalArgumentException("Wrong authorization mode value");
            }
            this.withAuthorizationMode = withAuthorizationMode;
            return this;
        }

        public Builder withReadWriteCheck() {
            this.withAuthorizationMode = AUTH_READ_WRITE;
            return this;
        }

        public Builder withReadCheck() {
            this.withAuthorizationMode = AUTH_READ;
            return this;
        }

        public Builder withNoCheck() {
            this.withAuthorizationMode = AUTH_NO_CHECK;
            return this;
        }

        public UserNodeFilterConfig build() {
            return new UserNodeFilterConfig(this);
        }
    }
}
