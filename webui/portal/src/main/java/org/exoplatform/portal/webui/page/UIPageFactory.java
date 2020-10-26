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

package org.exoplatform.portal.webui.page;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * An UIPage abstract factory
 *
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public abstract class UIPageFactory {
    public static String DEFAULT_FACTORY_ID = "Default";

    protected static Map<String, UIPageFactory> pageFactory = new HashMap<String, UIPageFactory>();

    static {
        ServiceLoader<UIPageFactory> loader = ServiceLoader.load(UIPageFactory.class);
        for (UIPageFactory factory : loader) {
            pageFactory.put(factory.getType(), factory);
        }
    }

    public static UIPageFactory getInstance(String type) {
        UIPageFactory factory;

        if (type == null) {
            factory = pageFactory.get(DEFAULT_FACTORY_ID);
        } else {
            factory = pageFactory.get(type);
        }

        if (factory == null) {
            throw new UnsupportedOperationException("The " + type + " page factory is not supported or not loaded");
        }
        return factory;
    }

    public abstract UIPage createUIPage(WebuiRequestContext context) throws Exception;

    public abstract String getType();
}
