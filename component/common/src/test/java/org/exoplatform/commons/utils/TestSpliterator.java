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

package org.exoplatform.commons.utils;

import org.exoplatform.component.test.AbstractGateInTest;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TestSpliterator extends AbstractGateInTest {

    public void testEmptyString() {
        Spliterator i = new Spliterator("", ' ');
        assertTrue(i.hasNext());
        assertEquals("", i.next());
        assertFalse(i.hasNext());
    }

    public void testSeparatorString() {
        Spliterator i = new Spliterator(" ", ' ');
        assertTrue(i.hasNext());
        assertEquals("", i.next());
        assertTrue(i.hasNext());
        assertEquals("", i.next());
        assertFalse(i.hasNext());
    }

    public void testEntireString() {
        Spliterator i = new Spliterator("a", ' ');
        assertTrue(i.hasNext());
        assertEquals("a", i.next());
        assertFalse(i.hasNext());
    }

    public void testNormal() {
        Spliterator i = new Spliterator("a b", ' ');
        assertTrue(i.hasNext());
        assertEquals("a", i.next());
        assertTrue(i.hasNext());
        assertEquals("b", i.next());
        assertFalse(i.hasNext());
    }
}
