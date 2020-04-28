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

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;


/**
 * Map to wrap application scope attributes.
 * <br>
 * <br>
 * <br>
 * Date: Mar 11, 2004 11:21:25 PM
 *
 * @author Clinton Begin
 */
public class ApplicationMap extends BaseHttpMap {
    private ServletContext context;

    public ApplicationMap(HttpServletRequest request) {
        context = request.getSession().getServletContext();
    }

    protected Enumeration getNames() {
        return context.getAttributeNames();
    }

    protected Object getValue(Object key) {
        return context.getAttribute(String.valueOf(key));
    }

    protected void putValue(Object key, Object value) {
        context.setAttribute(String.valueOf(key), value);
    }

    protected void removeValue(Object key) {
        context.removeAttribute(String.valueOf(key));
    }
}
