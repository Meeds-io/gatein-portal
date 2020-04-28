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
package org.apache.struts.beanaction;

import java.lang.reflect.Method;


public class ActionInvoker {
    private Method method;
    private BaseBean bean;

    public ActionInvoker(BaseBean bean, Method method) {
        this.method = method;
        this.bean = bean;
    }

    public String invoke() {
        try {
            return (String) method.invoke(bean, (Object[]) null);
        } catch (Exception e) {
            throw new BeanActionException("Error invoking Action.  Cause: " + e, e);
        }
    }
}
