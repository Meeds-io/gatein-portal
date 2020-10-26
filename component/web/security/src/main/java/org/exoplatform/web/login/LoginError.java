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

package org.exoplatform.web.login;

public class LoginError {
    public static final String ERROR_PARAM = "_error";

    public static final int DISABLED_USER_ERROR = 1;

    private int code;

    private String data;

    public LoginError(int code, String data) {
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public String getData() {
        return data;
    }

    public static LoginError parse(String error) {
        if (error != null && !error.isEmpty()) {
            String[] tmp = error.split(":");
            if (tmp.length == 2) {
                return new LoginError(Integer.parseInt(tmp[0]), tmp[1]);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(String.valueOf(code));
        builder.append(":").append(data);
        return builder.toString();
    }
}