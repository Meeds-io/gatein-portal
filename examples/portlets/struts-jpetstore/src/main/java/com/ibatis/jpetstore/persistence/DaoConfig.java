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
package com.ibatis.jpetstore.persistence;

import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import com.ibatis.common.jdbc.ScriptRunner;
import com.ibatis.common.resources.Resources;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.dao.client.DaoManagerBuilder;

public class DaoConfig {

    private static final String resource = "com/ibatis/jpetstore/persistence/dao.xml";
    private static final DaoManager daoManager;

    static {
        try {
            daoManager = newDaoManager(null);
            Properties props = Resources.getResourceAsProperties("properties/database.properties");
            String url = props.getProperty("url");
            String driver = props.getProperty("driver");
            String username = props.getProperty("username");
            String password = props.getProperty("password");
            if (url.equals("jdbc:hsqldb:mem:jpetstore")) {
                Class.forName(driver).newInstance();
                Connection conn = DriverManager.getConnection(url, username, password);
                try {
                    ScriptRunner runner = new ScriptRunner(conn, false, false);
                    runner.setErrorLogWriter(null);
                    runner.setLogWriter(null);
                    runner.runScript(Resources.getResourceAsReader("ddl/hsql/jpetstore-hsqldb-schema.sql"));
                    runner.runScript(Resources.getResourceAsReader("ddl/hsql/jpetstore-hsqldb-dataload.sql"));
                } finally {
                    conn.close();
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("Description.  Cause: " + e, e);
        }

    }

    public static DaoManager getDaoManager() {
        return daoManager;
    }

    public static DaoManager newDaoManager(Properties props) {
        try {
            Reader reader = Resources.getResourceAsReader(resource);
            return DaoManagerBuilder.buildDaoManager(reader, props);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize DaoConfig.  Cause: " + e, e);
        }
    }

}
