package org.apache.struts.beanaction.httpmap;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;


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
public class ParameterMap extends BaseHttpMap {
    private HttpServletRequest request;

    public ParameterMap(HttpServletRequest request) {
        this.request = request;
    }

    protected Enumeration getNames() {
        return request.getParameterNames();
    }

    protected Object getValue(Object key) {
        return request.getParameter(String.valueOf(key));
    }

    protected Object[] getValues(Object key) {
        return request.getParameterValues(String.valueOf(key));
    }

    protected void putValue(Object key, Object value) {
        throw new UnsupportedOperationException("Cannot put value to ParameterMap.");
    }

    protected void removeValue(Object key) {
        throw new UnsupportedOperationException("Cannot remove value from ParameterMap.");
    }
}
