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

package org.exoplatform.commons.serialization.model;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class TypeModel<O> {

    /** . */
    private final Class<O> javaType;

    /** . */
    private final TypeModel<? super O> superType;

    TypeModel(Class<O> javaType, TypeModel<? super O> superType) {
        this.javaType = javaType;
        this.superType = superType;
    }

    public String getName() {
        return javaType.getName();
    }

    public Class<O> getJavaType() {
        return javaType;
    }

    public TypeModel<? super O> getSuperType() {
        return superType;
    }

    @Override
    public String toString() {
        return "TypeModel[name=" + javaType.getName() + "]";
    }
}
