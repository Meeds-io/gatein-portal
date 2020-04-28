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
package org.apache.struts.beanaction.httpmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * <br>
 * <br>
 * Date: Mar 11, 2004 10:39:51 PM
 *
 * @author Clinton Begin
 */
public abstract class BaseHttpMap implements Map {
    public int size() {
        return keySet().size();
    }

    public boolean isEmpty() {
        return keySet().size() == 0;
    }

    public boolean containsKey(Object key) {
        return keySet().contains(key);
    }

    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    public Object get(Object key) {
        return getValue(key);
    }

    public Object put(Object key, Object value) {
        Object old = getValue(key);
        putValue(key, value);
        return old;
    }

    public Object remove(Object key) {
        Object old = getValue(key);
        removeValue(key);
        return old;
    }

    public void putAll(Map map) {
        Iterator i = map.keySet().iterator();
        while (i.hasNext()) {
            Object key = i.next();
            putValue(key, map.get(key));
        }
    }

    public void clear() {
        Iterator i = keySet().iterator();
        while (i.hasNext()) {
            removeValue(i.next());
        }
    }

    public Set keySet() {
        Set keySet = new HashSet();
        Enumeration names = getNames();
        while (names.hasMoreElements()) {
            keySet.add(names.nextElement());
        }
        return keySet;
    }

    public Collection values() {
        List list = new ArrayList();
        Enumeration names = getNames();
        while (names.hasMoreElements()) {
            list.add(getValue(names.nextElement()));
        }
        return list;
    }

    public Set entrySet() {
        return new HashSet();
    }

    protected abstract Enumeration getNames();

    protected abstract Object getValue(Object key);

    protected abstract void putValue(Object key, Object value);

    protected abstract void removeValue(Object key);
}
