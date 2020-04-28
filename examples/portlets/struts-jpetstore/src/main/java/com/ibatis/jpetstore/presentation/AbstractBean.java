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
package com.ibatis.jpetstore.presentation;

import org.apache.struts.beanaction.ActionContext;
import org.apache.struts.beanaction.BaseBean;

public abstract class AbstractBean extends BaseBean {

    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";
    public static final String SIGNON = "signon";
    public static final String SHIPPING = "shipping";
    public static final String CONFIRM = "confirm";

    protected void setMessage(String value) {
        ActionContext.getActionContext().getRequestMap().put("message", value);
    }

}
