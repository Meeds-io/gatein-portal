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
package org.exoplatform.component.test;

import java.util.Map;

import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * A base class for tests that provides base function test.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class AbstractGateInTest extends BaseGateInTest {

    /** . */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected AbstractGateInTest() {
    }

    protected AbstractGateInTest(String name) {
        super(name);
    }

    protected void beforeRunBare() {
        //
    }

    /**
     * After the run base, it should not throw anything as it is executed in a finally clause.
     */
    protected void afterRunBare() {
        //
    }

    @Override
    public final void runBare() throws Throwable {
        // Patch a bug with maven that does not pass properly the system property
        // with an empty value
        if ("org.hsqldb.jdbcDriver".equals(System.getProperty("gatein.test.datasource.driver"))) {
            System.setProperty("gatein.test.datasource.password", "");
        }

        // We use system.out.println on purpose to bypass the logging layer
        // and have this information going to the user
        System.out.println("Running unit test:" + getName());

        //
        log.info("Running unit test:" + getName());
        for (Map.Entry<?, ?> entry : System.getProperties().entrySet()) {
            if (entry.getKey() instanceof String) {
                String key = (String) entry.getKey();
                log.debug(key + "=" + entry.getValue());
            }
        }

        //
        beforeRunBare();

        //
        try {
            super.runBare();
        } catch (Throwable throwable) {
            log.error("Unit test " + getName() + " did not complete", throwable);

            // We use system.out.println on purpose to bypass the logging layer
            // and have this information going to the user
            System.out.println("Unit test " + getName() + " did not complete:");
            throwable.printStackTrace(System.out);

            //
            throw throwable;
        } finally {
            afterRunBare();
        }
    }
}
