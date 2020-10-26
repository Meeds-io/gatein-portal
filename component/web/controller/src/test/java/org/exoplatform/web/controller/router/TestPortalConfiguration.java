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

package org.exoplatform.web.controller.router;

import static org.exoplatform.web.controller.metadata.DescriptorBuilder.*;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.web.controller.QualifiedName;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TestPortalConfiguration extends AbstractTestController {

    /** . */
    private Router router;

    @Override
    protected void setUp() throws Exception {
        this.router = router().add(
                route("/private/{gtn:sitetype}/{gtn:sitename}{gtn:path}").with(routeParam("gtn:handler").withValue("site"),
                        routeParam("gtn:handler").withValue("site"), pathParam("gtn:path").matchedBy(".*").preservePath()),
                route("/private/{gtn:sitetype}/{gtn:sitename}{gtn:path}").with(routeParam("gtn:handler").withValue("site"),
                        pathParam("gtn:path").matchedBy(".*").preservePath()),
                route("/groups/{gtn:sitetype}/{gtn:sitename}{gtn:path}").with(routeParam("gtn:handler").withValue("site"),
                        pathParam("gtn:path").matchedBy(".*").preservePath()),
                route("/users/{gtn:sitetype}/{gtn:sitename}{gtn:path}").with(routeParam("gtn:handler").withValue("site"),
                        pathParam("gtn:path").matchedBy(".*").preservePath())).build();
    }

    public void testComponent() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "site");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_SITETYPE, "portal");
        expectedParameters.put(Names.GTN_PATH, "/");

        //
        assertEquals(expectedParameters, router.route("/private/portal/classic/"));
        assertEquals("/private/portal/classic/", router.render(expectedParameters));
    }

    public void testPrivateClassic() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "site");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_SITETYPE, "portal");
        expectedParameters.put(Names.GTN_PATH, "");

        //
        assertEquals(expectedParameters, router.route("/private/portal/classic"));
        assertEquals("/private/portal/classic", router.render(expectedParameters));
    }

    public void testPrivateClassicSlash() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "site");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_SITETYPE, "portal");
        expectedParameters.put(Names.GTN_PATH, "/");

        //
        assertEquals(expectedParameters, router.route("/private/portal/classic/"));
        assertEquals("/private/portal/classic/", router.render(expectedParameters));
    }

    public void testPrivateClassicHome() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "site");
        expectedParameters.put(Names.GTN_SITENAME, "classic");
        expectedParameters.put(Names.GTN_SITETYPE, "portal");
        expectedParameters.put(Names.GTN_PATH, "/home");

        //
        assertEquals(expectedParameters, router.route("/private/portal/classic/home"));
        assertEquals("/private/portal/classic/home", router.render(expectedParameters));
    }

    public void testSiteType() throws Exception {
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.GTN_HANDLER, "site");
        expectedParameters.put(Names.GTN_SITETYPE, "group");
        expectedParameters.put(Names.GTN_SITENAME, "platform");
        expectedParameters.put(Names.GTN_PATH, "/administration/registry");

        //
        assertEquals(expectedParameters, router.route("/private/group/platform/administration/registry"));
        assertEquals("/private/group/platform/administration/registry", router.render(expectedParameters));

        Map<QualifiedName, String> expectedParameters1 = new HashMap<QualifiedName, String>();
        expectedParameters1.put(Names.GTN_HANDLER, "site");
        expectedParameters1.put(Names.GTN_SITETYPE, "user");
        expectedParameters1.put(Names.GTN_SITENAME, "root");
        expectedParameters1.put(Names.GTN_PATH, "/tab_0");

        //
        assertEquals(expectedParameters1, router.route("/private/user/root/tab_0"));
        assertEquals("/private/user/root/tab_0", router.render(expectedParameters1));
    }
}
