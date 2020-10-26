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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.exoplatform.web.controller.QualifiedName;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TestMatch extends AbstractTestController {

    public void testRoot() throws Exception {
        Router router = router().add(route("/")).build();

        //
        assertNull(router.route(""));
        assertEquals(Collections.<QualifiedName, String> emptyMap(), router.route("/"));
        assertNull(router.route("/a"));
        assertNull(router.route("a"));
    }

    public void testA() throws Exception {
        Router router = router().add(route("/a")).build();

        //
        assertEquals(Collections.<QualifiedName, String> emptyMap(), router.route("/a"));
        assertNull(router.route("a"));
        assertNull(router.route("a/"));
        assertEquals(Collections.<QualifiedName, String> emptyMap(), router.route("/a/"));
        assertNull(router.route(""));
        assertNull(router.route("/"));
        assertNull(router.route("/b"));
        assertNull(router.route("b"));
        assertNull(router.route("/a/b"));
    }

    public void testAB() throws Exception {
        Router router = router().add(route("/a/b")).build();

        //
        assertNull(router.route("a/b"));
        assertEquals(Collections.<QualifiedName, String> emptyMap(), router.route("/a/b"));
        assertEquals(Collections.<QualifiedName, String> emptyMap(), router.route("/a/b/"));
        assertNull(router.route("a/b/"));
        assertNull(router.route(""));
        assertNull(router.route("/"));
        assertNull(router.route("/b"));
        assertNull(router.route("b"));
        assertNull(router.route("/a/b/c"));
    }

    public void testParameter() throws Exception {
        Router router = router().add(route("/{p}")).build();

        //
        assertEquals(Collections.singletonMap(Names.P, "a"), router.route("/a"));
    }

    public void testParameterPropagationToDescendants() throws Exception {
        Router router = router().add(route("/").with(routeParam("p").withValue("a")).sub(route("/a"))).build();

        //
        assertEquals(Collections.singletonMap(Names.P, "a"), router.route("/a"));
    }

    public void testSimplePattern() throws Exception {
        Router router = router().add(route("/{p}").with(pathParam("p").matchedBy("a"))).build();

        //
        assertEquals(Collections.singletonMap(Names.P, "a"), router.route("/a"));
        assertNull(router.route("a"));
        assertNull(router.route("/ab"));
        assertNull(router.route("ab"));
    }

    public void testPrecedence() throws Exception {
        Router router = router().add(route("/a")).add(route("/{p}/b").with(pathParam("p").matchedBy("a"))).build();

        //
        assertNull(router.route("a"));
        assertEquals(Collections.<QualifiedName, String> emptyMap(), router.route("/a"));
        assertEquals(Collections.<QualifiedName, String> emptyMap(), router.route("/a/"));
        assertEquals(Collections.singletonMap(Names.P, "a"), router.route("/a/b"));
    }

    public void testTwoRules1() throws Exception {
        Router router = router().add(route("/a").with(routeParam("b").withValue("b"))).add(route("/a/b")).build();

        //
        assertEquals(Collections.singletonMap(Names.B, "b"), router.route("/a"));
        assertEquals(Collections.<QualifiedName, String> emptyMap(), router.route("/a/b"));
    }

    public void testTwoRules2() throws Exception {
        Router router = router().add(route("/{a}").with(routeParam("b").withValue("b"))).add(route("/{a}/b")).build();

        //
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.A, "a");
        expectedParameters.put(Names.B, "b");
        assertEquals(expectedParameters, router.route("/a"));
        assertEquals(Collections.singletonMap(Names.A, "a"), router.route("/a/b"));
    }

    public void testLang() throws Exception {
        Router router = router().add(route("/{a}b").with(pathParam("a").matchedBy("(([A-Za-z]{2})/)?").preservePath())).build();

        //
        assertEquals(Collections.singletonMap(Names.A, "fr/"), router.route("/fr/b"));
        assertEquals(Collections.singletonMap(Names.A, ""), router.route("/b"));
    }

    public void testOptionalParameter() throws Exception {
        Router router = router().add(
                route("/{a}/b").with(pathParam("a").matchedBy("a?").preservePath(), routeParam("b").withValue("b"))).build();

        //
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.A, "a");
        expectedParameters.put(Names.B, "b");
        assertEquals(expectedParameters, router.route("/a/b"));
        assertEquals("/a/b", router.render(expectedParameters));

        //
        expectedParameters.put(Names.A, "");
        assertEquals(expectedParameters, router.route("/b"));
        assertEquals("/b", router.render(expectedParameters));
    }

    public void testAvoidMatchingPrefix() throws Exception {
        Router router = router().add(route("/{a}/ab/c").with(pathParam("a").matchedBy("a?").preservePath())).build();

        //
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.A, "");
        assertEquals(expectedParameters, router.route("/ab/c"));
        assertEquals("/ab/c", router.render(expectedParameters));
    }

    public void testPartialMatching() throws Exception {
        Router router = router().add(route("/{a}").with(pathParam("a").matchedBy("abc").preservePath())).build();

        //
        assertNull(router.route("/abcdef"));
    }

    /*
     * public void testLookAhead() throws Exception { Router router = router(). add(route("/{a}"). with(
     * pathParam("a").matchedBy("(.(?=/))?").preservingPath()). sub(route("/{b}").
     * with(pathParam("b").matchedBy(".").preservingPath())) ).build();
     *
     * // Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>(); expectedParameters.put(Names.A,
     * ""); expectedParameters.put(Names.B, "b"); assertEquals(expectedParameters, router.route("/b")); assertEquals("/b",
     * router.render(expectedParameters));
     *
     * // expectedParameters.put(Names.A, "a"); assertEquals(expectedParameters, router.route("/a/b")); assertEquals("/a/b",
     * router.render(expectedParameters)); }
     */

    public void testZeroOrOneFollowedBySubRoute() throws Exception {
        Router router = router().add(
                route("/{a}").with(pathParam("a").matchedBy("a?").preservePath()).sub(
                        route("/b").with(routeParam("b").withValue("b")))).build();

        //
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.A, "a");
        expectedParameters.put(Names.B, "b");
        assertEquals(expectedParameters, router.route("/a/b"));
        assertEquals("/a/b", router.render(expectedParameters));

        //
        expectedParameters.put(Names.A, "");
        assertEquals(expectedParameters, router.route("/b"));
        assertEquals("/b", router.render(expectedParameters));
    }

    public void testMatcher() throws Exception {
        Router router = router().add(route("/{a}")).add(route("/a").with(routeParam("b").withValue("b_value"))).build();

        Iterator<Map<QualifiedName, String>> i = router.root.route("/a", Collections.<String, String[]> emptyMap());
        Map<QualifiedName, String> s1 = i.next();
        assertEquals(Collections.singletonMap(Names.A, "a"), s1);
        Map<QualifiedName, String> s2 = i.next();
        assertEquals(Collections.singletonMap(Names.B, "b_value"), s2);
        assertFalse(i.hasNext());
    }

    public void testDisjunction() throws Exception {
        Router router = router().add(route("/{a}{b}").with(pathParam("a").matchedBy("a|b"))).build();

        //
        Map<QualifiedName, String> expectedParameters = new HashMap<QualifiedName, String>();
        expectedParameters.put(Names.A, "a");
        expectedParameters.put(Names.B, "c");

        //
        Iterator<Map<QualifiedName, String>> i = router.root.route("/ac", Collections.<String, String[]> emptyMap());
        Map<QualifiedName, String> s1 = i.next();
        assertEquals(expectedParameters, s1);
        assertFalse(i.hasNext());

        i = router.root.route("/bc", Collections.<String, String[]> emptyMap());
        s1 = i.next();
        expectedParameters.put(Names.A, "b");
        assertEquals(expectedParameters, s1);
        assertFalse(i.hasNext());
    }

    public void testCaptureGroup() throws Exception {
        Router router = router().add(route("/{a}").with(pathParam("a").matchedBy("a(.)c").captureGroup(true))).build();

        //
        Iterator<Map<QualifiedName, String>> i = router.root.route("/abc", Collections.<String, String[]> emptyMap());
        Map<QualifiedName, String> s1 = i.next();
        assertEquals(Collections.singletonMap(Names.A, "b"), s1);
        assertFalse(i.hasNext());
    }
}
