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

package org.exoplatform.util;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.HashMap;


/**
 * Created by The eXo Platform SARL Author : Tuan Nguyen tuan08@users.sourceforge.net Jun 16, 2006
 */
public class ReflectionUtil {

    public static Object[] EMPTY_ARGS = {};

    private static HashMap<String, SoftReference<Method>> getMethodCache_ = new HashMap<String, SoftReference<Method>>();

    private static HashMap<String, SoftReference<Method>> setMethodCache_ = new HashMap<String, SoftReference<Method>>();

    public static Method getGetBindingMethod(Object bean, String bindingField) throws Exception {
        String key = bindingField + "@" + bean.getClass();
        Method method = null;
        SoftReference<Method> sref = getMethodCache_.get(key);
        if (sref != null)
            method = sref.get();
        if (method == null) {
            Exception exp = null;
            try {
                method = getBindingMethod(bean, bindingField, new Class[] {}, "get");
            } catch (Exception e) {
                exp = e;
            }
            if (method == null) {
                try {
                    method = getBindingMethod(bean, bindingField, new Class[] {}, "is");
                } catch (Exception exp2) {
                }
            }
            if (method == null && exp != null)
                throw exp;
            getMethodCache_.put(key, new SoftReference<Method>(method));
        }
        return method;
    }

    public static Method getSetBindingMethod(Object bean, String bindingField, Class[] args) throws Exception {
        String key = bindingField + "@" + bean.getClass();
        Method method = null;
        SoftReference<Method> sref = setMethodCache_.get(key);
        if (sref != null)
            method = sref.get();
        if (method == null) {
            method = getBindingMethod(bean, bindingField, args, "set");
            setMethodCache_.put(key, new SoftReference<Method>(method));
        }
        return method;
    }

    private static Method getBindingMethod(Object bean, String bindingField, Class[] classes, String prefix) throws Exception {
        StringBuilder b = new StringBuilder();
        b.append(prefix);
        b.append(Character.toUpperCase(bindingField.charAt(0)));
        b.append(bindingField.substring(1));
        return bean.getClass().getMethod(b.toString(), classes);
    }

}
