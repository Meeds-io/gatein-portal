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

import junit.framework.TestCase;

import org.exoplatform.web.controller.QualifiedName;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TestQualifiedName extends TestCase {

    public void testSame() {
        QualifiedName qd1 = QualifiedName.create("a", "bc");
        assertEquals("a", qd1.getQualifier());
        assertEquals("bc", qd1.getName());
        assertEquals("a:bc", qd1.getValue());

        //
        QualifiedName qd2 = QualifiedName.parse("a:bc");
        assertEquals("a", qd2.getQualifier());
        assertEquals("bc", qd2.getName());
        assertEquals("a:bc", qd2.getValue());

        //
        assertTrue(qd1.equals(qd2));
        assertTrue(qd2.equals(qd1));
        assertTrue(qd1.equals(qd1));
        assertTrue(qd2.equals(qd2));

        //
        assertEquals(qd1.hashCode(), qd2.hashCode());
    }

    public void testSameQualifierComparison() {
        QualifiedName qd1 = QualifiedName.create("a", "b");
        QualifiedName qd2 = QualifiedName.create("a", "c");
        assertEquals(Math.signum(qd1.compareTo(qd2)), Math.signum("b".compareTo("c")));
        assertEquals(Math.signum(qd2.compareTo(qd1)), Math.signum("c".compareTo("b")));
    }

    public void testEmptyQualifierComparison() {
        QualifiedName qd1 = QualifiedName.create("a");
        QualifiedName qd2 = QualifiedName.create("b");
        assertEquals(Math.signum(qd1.compareTo(qd2)), Math.signum("a".compareTo("b")));
        assertEquals(Math.signum(qd2.compareTo(qd1)), Math.signum("b".compareTo("a")));
    }

    public void testDifferentQualifierSameNameComparison() {
        QualifiedName qd1 = QualifiedName.create("a", "c");
        QualifiedName qd2 = QualifiedName.create("b", "c");
        assertEquals(Math.signum(qd1.compareTo(qd2)), Math.signum("a".compareTo("b")));
        assertEquals(Math.signum(qd2.compareTo(qd1)), Math.signum("b".compareTo("a")));
    }

    public void testSameWithEmptyQualifier() {
        QualifiedName qd1 = QualifiedName.create("abc");
        assertEquals("", qd1.getQualifier());
        assertEquals("abc", qd1.getName());
        assertEquals("abc", qd1.getValue());

        //
        QualifiedName qd2 = QualifiedName.parse("abc");
        assertEquals("", qd2.getQualifier());
        assertEquals("abc", qd2.getName());
        assertEquals("abc", qd2.getValue());

        //
        assertTrue(qd1.equals(qd2));
        assertTrue(qd2.equals(qd1));
        assertTrue(qd1.equals(qd1));
        assertTrue(qd2.equals(qd2));

        //
        assertEquals(qd1.hashCode(), qd2.hashCode());
    }

    public void testNPEInCtor() {
        try {
            QualifiedName.create(null);
            fail();
        } catch (NullPointerException ignore) {
        }
        try {
            QualifiedName.create("a", null);
            fail();
        } catch (NullPointerException ignore) {
        }
        try {
            QualifiedName.create(null, "a");
            fail();
        } catch (NullPointerException ignore) {
        }
        try {
            QualifiedName.create(null, null);
            fail();
        } catch (NullPointerException ignore) {
        }
        try {
            QualifiedName.parse(null);
            fail();
        } catch (NullPointerException ignore) {
        }
    }

    public void testIAEInCtor() {
        try {
            QualifiedName.create("a:b");
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            QualifiedName.create(":", "a");
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            QualifiedName.create("a", ":");
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            QualifiedName.create(":", ":");
            fail();
        } catch (IllegalArgumentException ignore) {
        }
        try {
            QualifiedName.parse("::");
            fail();
        } catch (IllegalArgumentException ignore) {
        }
    }
}
