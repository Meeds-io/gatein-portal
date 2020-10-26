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
import junit.framework.TestCase;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TestBuildRoute extends TestCase {

    public void testParameterSegment() throws Exception {
        Router router = router().add(route("/{a}")).build();

        //
        assertEquals(0, router.root.getSegmentNames().size());
        assertEquals(1, router.root.getPatternSize());
        PatternRoute patternRoute = router.root.getPattern(0);
        assertEquals("^/(?:([^/]+))(?:(?<=^/)|(?=/)|$)", patternRoute.pattern.getPattern());
        assertEquals(1, patternRoute.params.length);
        assertEquals(Names.A, patternRoute.params[0].name);
        assertEquals("^(.+)$", patternRoute.params[0].matchingRegex[0].getPattern());
        assertEquals(EncodingMode.FORM, patternRoute.params[0].encodingMode);
        assertEquals(2, patternRoute.chunks.length);
        assertEquals("", patternRoute.chunks[0]);
        assertEquals("", patternRoute.chunks[1]);
    }

    public void testQualifiedParameterSegment() throws Exception {
        Router router = router().add(route("/{q:a}")).build();

        //
        assertEquals(0, router.root.getSegmentNames().size());
        assertEquals(1, router.root.getPatternSize());
        PatternRoute patternRoute = router.root.getPattern(0);
        assertEquals("^/(?:([^/]+))(?:(?<=^/)|(?=/)|$)", patternRoute.pattern.getPattern());
        assertEquals(1, patternRoute.params.length);
        assertEquals(Names.Q_A, patternRoute.params[0].name);
        assertEquals("^(.+)$", patternRoute.params[0].matchingRegex[0].getPattern());
        assertEquals(EncodingMode.FORM, patternRoute.params[0].encodingMode);
        assertEquals(2, patternRoute.chunks.length);
        assertEquals("", patternRoute.chunks[0]);
        assertEquals("", patternRoute.chunks[1]);
    }

    public void testPatternSegment() throws Exception {
        Router router = router().add(route("/{a}").with(pathParam("a").matchedBy(".*"))).build();

        //
        assertEquals(0, router.root.getSegmentNames().size());
        assertEquals(1, router.root.getPatternSize());
        PatternRoute patternRoute = router.root.getPattern(0);
        assertEquals("^/(?:([^/]*))(?:(?<=^/)|(?=/)|$)", patternRoute.pattern.getPattern());
        assertEquals(1, patternRoute.params.length);
        assertEquals(Names.A, patternRoute.params[0].name);
        assertEquals("^(.*)$", patternRoute.params[0].matchingRegex[0].getPattern());
        assertEquals(EncodingMode.FORM, patternRoute.params[0].encodingMode);
        assertEquals(2, patternRoute.chunks.length);
        assertEquals("", patternRoute.chunks[0]);
        assertEquals("", patternRoute.chunks[1]);
    }

    public void testSamePrefix() throws Exception {
        Router router = router().add(route("/public/foo")).add(route("/public/bar")).build();
        assertEquals(2, router.root.getSegmentSize("public"));
        Route publicRoute1 = router.root.getSegment("public", 0);
        assertEquals(1, publicRoute1.getSegmentSize("foo"));
        Route publicRoute2 = router.root.getSegment("public", 1);
        assertEquals(1, publicRoute2.getSegmentSize("bar"));
    }

    private void assertEquals(Route expectedRoute, Route route) {
        assertEquals(expectedRoute.getClass(), route.getClass());
        assertEquals(expectedRoute.getSegmentNames(), route.getSegmentNames());
        for (String segmentName : expectedRoute.getSegmentNames()) {
            assertEquals(expectedRoute.getSegmentSize(segmentName), route.getSegmentSize(segmentName));
            for (int segmentIndex = 0; segmentIndex < expectedRoute.getSegmentSize(segmentName); segmentIndex++) {
                SegmentRoute expectedSegmentRoute = expectedRoute.getSegment(segmentName, segmentIndex);
                SegmentRoute segmentRoute = route.getSegment(segmentName, segmentIndex);
                assertEquals(expectedSegmentRoute, segmentRoute);
            }
        }
        assertEquals(expectedRoute.getPatternSize(), route.getPatternSize());
        for (int i = 0; i < expectedRoute.getPatternSize(); i++) {
            assertEquals(expectedRoute.getPattern(i), route.getPattern(i));
        }
        if (route instanceof PatternRoute) {
            assertEquals(((PatternRoute) expectedRoute).pattern.toString(), ((PatternRoute) route).pattern.toString());
            assertEquals(((PatternRoute) expectedRoute).params.length, ((PatternRoute) route).params.length);
            for (int i = 0; i < ((PatternRoute) expectedRoute).params.length; i++) {
                PathParam expectedParam = ((PatternRoute) expectedRoute).params[i];
                PathParam param = ((PatternRoute) route).params[i];
                assertEquals(expectedParam.name, param.name);
                assertEquals(expectedParam.matchingRegex.length, param.matchingRegex.length);
                for (int j = 0; j < expectedParam.matchingRegex.length; j++) {
                    assertEquals(expectedParam.matchingRegex[j].toString(), param.matchingRegex[j].toString());
                }
                assertEquals(expectedParam.encodingMode, param.encodingMode);
            }
        } else if (route instanceof SegmentRoute) {
            assertEquals(((SegmentRoute) expectedRoute).name, ((SegmentRoute) route).name);
        }
    }
}
