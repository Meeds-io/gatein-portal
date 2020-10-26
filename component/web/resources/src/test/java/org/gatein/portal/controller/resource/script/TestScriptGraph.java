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

package org.gatein.portal.controller.resource.script;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.exoplatform.component.test.AbstractGateInTest;
import org.gatein.common.util.Tools;
import org.gatein.portal.controller.resource.ResourceId;
import org.gatein.portal.controller.resource.ResourceScope;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class TestScriptGraph extends AbstractGateInTest {

    /** . */
    private static final ResourceId A = new ResourceId(ResourceScope.SHARED, "A");

    /** . */
    private static final ResourceId B = new ResourceId(ResourceScope.SHARED, "B");

    /** . */
    private static final ResourceId C = new ResourceId(ResourceScope.SHARED, "C");

    /** . */
    private static final ResourceId D = new ResourceId(ResourceScope.PORTAL, "D");

    public void testDetectCycle1() {
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A);
        ScriptResource b = graph.addResource(B);
        a.addDependency(B);
        try {
            b.addDependency(A);
            fail();
        } catch (IllegalStateException ignore) {
        }
    }

    public void testDetectCycle2() {
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A);
        ScriptResource b = graph.addResource(B);
        ScriptResource c = graph.addResource(C);
        a.addDependency(B);
        b.addDependency(C);
        try {
            c.addDependency(A);
            fail();
        } catch (IllegalStateException ignore) {
        }
    }

    public void testClosure() {
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A);
        a.addDependency(B);
        ScriptResource b = graph.addResource(B);
        b.addDependency(C);
        ScriptResource c = graph.addResource(C);
        assertEquals(Tools.toSet(B, C), a.getClosure());
        assertEquals(Tools.toSet(C), b.getClosure());
        assertEquals(Collections.emptySet(), c.getClosure());
    }

    /**
     * Closure of any node depends on node relationships in graph but does not depend on the order of building graph nodes
     */
    public void testBuildingOrder() {
        ScriptGraph graph = new ScriptGraph();

        // A -> B
        ScriptResource a = graph.addResource(A);
        a.addDependency(B);

        // C -> D
        ScriptResource c = graph.addResource(C);
        c.addDependency(D);

        // B -> C
        ScriptResource b = graph.addResource(B);
        b.addDependency(C);

        ScriptResource d = graph.addResource(D);

        assertEquals(Tools.toSet(D), c.getClosure());

        assertEquals(Tools.toSet(C, D), b.getClosure());

        assertEquals(Tools.toSet(B, C, D), a.getClosure());
    }

    public void testFetchMode() {
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A, FetchMode.ON_LOAD);
        ScriptResource b = graph.addResource(B, FetchMode.IMMEDIATE);
        ScriptResource c = graph.addResource(C, FetchMode.IMMEDIATE);
        try {
            a.addDependency(C);
            fail();
        } catch (IllegalStateException e) {
        }
        b.addDependency(C);

        //
        Map<ScriptResource, FetchMode> resolution = graph.resolve(Collections.<ResourceId, FetchMode> singletonMap(A, null));
        assertResultOrder(resolution.keySet());
        assertEquals(1, resolution.size());
        assertEquals(Tools.toSet(a), resolution.keySet());

        //
        resolution = graph.resolve(Collections.<ResourceId, FetchMode> singletonMap(B, null));
        assertResultOrder(resolution.keySet());
        assertEquals(2, resolution.size());
        assertEquals(Tools.toSet(b, c), resolution.keySet());
        assertEquals(FetchMode.IMMEDIATE, resolution.get(b));
        assertEquals(FetchMode.IMMEDIATE, resolution.get(c));

        //
        LinkedHashMap<ResourceId, FetchMode> pairs = new LinkedHashMap<ResourceId, FetchMode>();
        pairs.put(A, null);
        pairs.put(B, null);
        resolution = graph.resolve(pairs);
        assertResultOrder(resolution.keySet());
        assertEquals(3, resolution.size());
        assertEquals(Tools.toSet(a, b, c), resolution.keySet());
        assertEquals(FetchMode.ON_LOAD, resolution.get(a));
        assertEquals(FetchMode.IMMEDIATE, resolution.get(b));
        assertEquals(FetchMode.IMMEDIATE, resolution.get(c));
    }

    // ********

    public void testResolveDefaultOnLoadFetchMode() {
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A, FetchMode.ON_LOAD);

        // Use default fetch mode
        Map<ScriptResource, FetchMode> test = graph.resolve(Collections.<ResourceId, FetchMode> singletonMap(A, null));
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(a), test.keySet());
        assertEquals(FetchMode.ON_LOAD, test.get(a));

        // Get resource with with same fetch-mode
        test = graph.resolve(Collections.<ResourceId, FetchMode> singletonMap(A, FetchMode.ON_LOAD));
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(a), test.keySet());
        assertEquals(FetchMode.ON_LOAD, test.get(a));

        // Don't get resource with other fetch-mode
        test = graph.resolve(Collections.<ResourceId, FetchMode> singletonMap(A, FetchMode.IMMEDIATE));
        assertEquals(0, test.size());
    }

    public void testResolveDefaultImmediateFetchMode() {
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A, FetchMode.IMMEDIATE);

        // Use default fetch mode
        Map<ScriptResource, FetchMode> test = graph.resolve(Collections.<ResourceId, FetchMode> singletonMap(A, null));
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(a), test.keySet());
        assertEquals(FetchMode.IMMEDIATE, test.get(a));

        // Dont' get resource with other fetch-mode
        test = graph.resolve(Collections.<ResourceId, FetchMode> singletonMap(A, FetchMode.ON_LOAD));
        assertEquals(0, test.keySet().size());

        // Get resource with the same fetch-mode
        test = graph.resolve(Collections.<ResourceId, FetchMode> singletonMap(A, FetchMode.IMMEDIATE));
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(a), test.keySet());
        assertEquals(FetchMode.IMMEDIATE, test.get(a));
    }

    public void testResolveDependency1() {
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A, FetchMode.IMMEDIATE);
        ScriptResource b = graph.addResource(B, FetchMode.ON_LOAD);
        try {
            a.addDependency(B);
            fail();
        } catch (IllegalStateException ex) {
        }

        //
        LinkedHashMap<ResourceId, FetchMode> pairs = new LinkedHashMap<ResourceId, FetchMode>();
        pairs.put(A, null);
        pairs.put(B, null);
        Map<ScriptResource, FetchMode> test = graph.resolve(pairs);
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(a, b), test.keySet());
        assertEquals(FetchMode.IMMEDIATE, test.get(a));
        assertEquals(FetchMode.ON_LOAD, test.get(b));

        //
        pairs = new LinkedHashMap<ResourceId, FetchMode>();
        pairs.put(B, null);
        pairs.put(A, null);
        test = graph.resolve(pairs);
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(a, b), test.keySet());
        assertEquals(FetchMode.IMMEDIATE, test.get(a));
        assertEquals(FetchMode.ON_LOAD, test.get(b));

        //
        pairs = new LinkedHashMap<ResourceId, FetchMode>();
        pairs.put(B, null);
        test = graph.resolve(pairs);
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(b), test.keySet());
        assertEquals(FetchMode.ON_LOAD, test.get(b));
    }

    public void testResolveDependency2() {
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A, FetchMode.ON_LOAD);
        ScriptResource b = graph.addResource(B, FetchMode.IMMEDIATE);
        try {
            a.addDependency(B);
            fail();
        } catch (IllegalStateException ex) {
        }

        //
        LinkedHashMap<ResourceId, FetchMode> pairs = new LinkedHashMap<ResourceId, FetchMode>();
        pairs.put(A, null);
        Map<ScriptResource, FetchMode> test = graph.resolve(pairs);
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(a), test.keySet());
        assertEquals(FetchMode.ON_LOAD, test.get(a));

        //
        pairs = new LinkedHashMap<ResourceId, FetchMode>();
        pairs.put(A, null);
        pairs.put(B, null);
        test = graph.resolve(pairs);
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(a, b), test.keySet());
        assertEquals(FetchMode.ON_LOAD, test.get(a));
        assertEquals(FetchMode.IMMEDIATE, test.get(b));

        //
        pairs = new LinkedHashMap<ResourceId, FetchMode>();
        pairs.put(B, null);
        pairs.put(A, null);
        test = graph.resolve(pairs);
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(a, b), test.keySet());
        assertEquals(FetchMode.ON_LOAD, test.get(a));
        assertEquals(FetchMode.IMMEDIATE, test.get(b));

        //
        pairs = new LinkedHashMap<ResourceId, FetchMode>();
        pairs.put(B, null);
        test = graph.resolve(pairs);
        assertResultOrder(test.keySet());
        assertEquals(Tools.toSet(b), test.keySet());
        assertEquals(FetchMode.IMMEDIATE, test.get(b));
    }

    public void testResolveDisjointDependencies() {
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A, FetchMode.IMMEDIATE);
        ScriptResource b = graph.addResource(B, FetchMode.IMMEDIATE);
        ScriptResource c = graph.addResource(C, FetchMode.IMMEDIATE);
        a.addDependency(C);

        // Yes all permutations
        ResourceId[][] samples = { { A }, { A, B }, { B, A }, { A, B, C }, { A, C, B }, { B, A, C }, { B, C, A }, { C, A, B },
                { C, B, A }, };

        //
        LinkedHashMap<ResourceId, FetchMode> pairs = new LinkedHashMap<ResourceId, FetchMode>();
        for (ResourceId[] sample : samples) {
            pairs.clear();
            for (ResourceId id : sample) {
                pairs.put(id, null);
            }
            Map<ScriptResource, FetchMode> test = graph.resolve(pairs);
            assertResultOrder(test.keySet());
        }
    }

    public void testCrossDependency() {
        // Scripts and Module can't depend on each other
        ScriptGraph graph = new ScriptGraph();
        ScriptResource a = graph.addResource(A, FetchMode.IMMEDIATE);
        graph.addResource(B, FetchMode.ON_LOAD);
        try {
            a.addDependency(B);
            fail();
        } catch (IllegalStateException ignore) {
        }

        graph = new ScriptGraph();
        a = graph.addResource(A, FetchMode.ON_LOAD);
        a.addDependency(B);
        try {
            graph.addResource(B, FetchMode.IMMEDIATE);
            fail();
        } catch (IllegalStateException ignore) {
        }
    }

    public void testDuplicateResource() {
        ScriptGraph graph = new ScriptGraph();
        ResourceId shared = new ResourceId(ResourceScope.SHARED, "foo");

        graph.addResource(shared);
        try {
            graph.addResource(shared);
            fail();
        } catch (IllegalStateException ignore) {
        }

        ResourceId portlet = new ResourceId(ResourceScope.PORTLET, "foo");
        graph.addResource(portlet);
        try {
            graph.addResource(portlet);
            fail();
        } catch (IllegalStateException ignore) {
        }

        ResourceId portal = new ResourceId(ResourceScope.PORTAL, "foo");
        graph.addResource(portal);
        try {
            graph.addResource(portal);
            fail();
        } catch (IllegalStateException ignore) {
        }
    }

    /**
     * Test that each script of the test collection has no following script that belongs to its closure.
     *
     * @param test the test
     */
    private void assertResultOrder(Collection<ScriptResource> test) {
        ScriptResource[] array = test.toArray(new ScriptResource[test.size()]);
        for (int i = 0; i < array.length; i++) {
            ScriptResource resource = array[i];
            for (int j = i + 1; j < array.length; j++) {
                if (resource.closure.contains(array[j].getId()) && resource.fetchMode.equals(array[j].fetchMode)) {
                    throw failure("Was not expecting result order " + test, new Exception());
                }
            }
        }
    }
}
