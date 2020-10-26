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

package org.exoplatform.web.application;

import java.util.Locale;


/**
 * Created by The eXo Platform SAS Mar 29, 2007
 */
public abstract class URLBuilder<T> {

    /** . */
    protected Locale locale = null;

    /** . */
    protected boolean removeLocale = false;

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setRemoveLocale(boolean removeLocale) {
        this.removeLocale = removeLocale;
    }

    public boolean getRemoveLocale() {
        return removeLocale;
    }

    public final String createURL(String action) {
        throw new UnsupportedOperationException("don't use me");
    }

    public final String createURL(String action, Parameter[] params) {
        throw new UnsupportedOperationException("don't use me");
    }

    public final String createURL(String action, String objectId) {
        throw new UnsupportedOperationException("don't use me");
    }

    public final String createURL(String action, String objectId, Parameter[] params) {
        throw new UnsupportedOperationException("don't use me");
    }

    public final String createURL(T targetComponent, String action, String targetBeanId) {
        return createURL(targetComponent, action, null, targetBeanId, (Parameter[]) null, false);
    }

    public final String createAjaxURL(T targetComponent, String action, String targetBeanId) {
        return createAjaxURL(targetComponent, action, null, targetBeanId, (Parameter[]) null, false);
    }

    public final String createAjaxURL(T targetComponent, String action, String confirm, String targetBeanId) {
        return createAjaxURL(targetComponent, action, confirm, targetBeanId, (Parameter[]) null, false);
    }

    public abstract String createAjaxURL(T targetComponent, String action, String confirm, String targetBeanId,
            Parameter[] params, boolean csrfCheck);

    public abstract String createURL(T targetComponent, String action, String confirm, String targetBeanId, Parameter[] params,
            boolean csrfCheck);
}
