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

package org.exoplatform.commons.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.Set;

import org.gatein.common.io.IOTools;

/**
 * A class that contains utility method that make the caller not worry much about the unexpectable expected such as argument
 * nullity or the control flow due to exceptions.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class Safe {

    private Safe() {
    }

    /**
     * Return true if both objects are null or both are non null and the equals method of one object returns true when it is
     * invoked with the other object as argument.
     *
     * @param o1 the first object
     * @param o2 the second object
     * @return true if string are safely equal
     */
    public static boolean equals(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            return o2 != null && o1.equals(o2);
        }
    }

    /**
     * Close a closable object. The provided object may be null or thrown an IOException or a runtime exception during the
     * invocation of the close method without changing the control flow of the method caller. If the closeable was succesfully
     * closed the method returns true.
     *
     * @param closeable the closeable
     * @return true if the object was closed
     */
    public static boolean close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                return true;
            } catch (IOException ignore) {
            } catch (RuntimeException ignore) {
            }
        }
        return false;
    }

    public static byte[] getBytes(InputStream is) {
        byte[] bytes;

        if (is == null) {
            return null;
        }

        try {
            bytes = IOTools.getBytes(is);
            return bytes;
        } catch (IOException ignore) {
            // todo: should log
            return null;
        } finally {
            IOTools.safeClose(is);
        }
    }

    /**
     * Wrap the set so that it is unmodifiable when it is not null, otherwise returns null.
     *
     * TODO it would be nice to avoid to rewrap unmodifiable set (not sure it is non proprietary possible)
     * @param set the set to wrap
     * @param <E> the set generic element type
     * @return the unmodifiable set
     */
    public static <E> Set<E> unmodifiableSet(Set<E> set) {
        if (set == null) {
            return null;
        } else {
            return Collections.unmodifiableSet(set);
        }
    }

    // THIS CODE IS TEMPORARY

    /** . */
    private static final Field listAccessField;

    static {
        try {
            listAccessField = LazyList.class.getDeclaredField("listAccess");
            listAccessField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    public static <E> ListAccess<E> unwrap(PageList<E> pageList) {
        LazyPageList<E> lazyPageList = (LazyPageList<E>) pageList;

        //
        try {
            // Get LazyList first
            LazyList<E> lazyList = (LazyList<E>) lazyPageList.getAll();

            // Now get list access
            return (ListAccess<E>) listAccessField.get(lazyList);
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    public static long parseLong(String s) {
        try {
            return s == null ? 0 : Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
