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

import java.io.IOException;
import java.io.StringReader;

import org.exoplatform.component.test.AbstractGateInTest;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class TestPropertyResolverReader extends AbstractGateInTest {

    public void testEmpty() throws Exception {
        testIncremental("", "");
        testSame("", "");
        testLarger("", "");
    }

    public void testChars() throws Exception {
        testIncremental("abc", "abc");
        testSame("abc", "abc");
        testLarger("abc", "abc");
    }

    public void testDollar1() throws Exception {
        testIncremental("$", "$");
        testSame("$", "$");
        testLarger("$", "$");
    }

    public void testDollar2() throws Exception {
        testIncremental("$a", "$a");
        testSame("$a", "$a");
        testLarger("$a", "$a");
    }

    public void testOpenProperty1() throws Exception {
        testIncremental("${", "${");
        testSame("${", "${");
        testLarger("${", "${");
    }

    public void testOpenProperty2() throws Exception {
        testIncremental("${a", "${a");
        testSame("${a", "${a");
        testLarger("${a", "${a");
    }

    public void testProperty1() throws Exception {
        testIncremental("a", "${a}");
        testSame("a", "${a}");
        testLarger("a", "${a}");
    }

    public void testProperty2() throws Exception {
        testIncremental("ab", "${a}b");
        testSame("ab", "${a}b");
        testLarger("ab", "${a}b");
    }

    public void testProperty3() throws Exception {
        // This force a corner case as the resolver will try to
        // insert the 6 chars in the buffer that should be equals
        // to "${a}b . It will not have the room (only 4 letters ${a})
        // so it needs to shift the b char 2 letters to the right
        testIncremental("6charsb", new PropertyResolverReader(new StringReader("${a}b")) {
            @Override
            protected String resolve(String name) {
                return "6chars";
            }
        });
        testSame("6charsb", new PropertyResolverReader(new StringReader("${a}b")) {
            @Override
            protected String resolve(String name) {
                return "6chars";
            }
        });
        testLarger("6charsb", new PropertyResolverReader(new StringReader("${a}b")) {
            @Override
            protected String resolve(String name) {
                return "6chars";
            }
        });
    }

    public void testProperty4() throws Exception {
        // Same like testProperty3 but we allocate a small buffer
        // to force a reallocation after the property is resolved
        testIncremental("6charsb", new PropertyResolverReader(new StringReader("${a}b"), 5) {
            @Override
            protected String resolve(String name) {
                return "6chars";
            }
        });
        testSame("6charsb", new PropertyResolverReader(new StringReader("${a}b"), 5) {
            @Override
            protected String resolve(String name) {
                return "6chars";
            }
        });
        testLarger("6charsb", new PropertyResolverReader(new StringReader("${a}b"), 5) {
            @Override
            protected String resolve(String name) {
                return "6chars";
            }
        });
    }

    public void testNullProperty() throws Exception {
        testIncremental("${a}", new PropertyResolverReader(new StringReader("${a}")) {
            @Override
            protected String resolve(String name) {
                return null;
            }
        });
        testSame("${a}", new PropertyResolverReader(new StringReader("${a}")) {
            @Override
            protected String resolve(String name) {
                return null;
            }
        });
        testLarger("${a}", new PropertyResolverReader(new StringReader("${a}")) {
            @Override
            protected String resolve(String name) {
                return null;
            }
        });
    }

    public void testEmptyProperty() throws Exception {
        testIncremental("", new PropertyResolverReader(new StringReader("${a}")) {
            @Override
            protected String resolve(String name) {
                return "";
            }
        });
        testSame("", new PropertyResolverReader(new StringReader("${a}")) {
            @Override
            protected String resolve(String name) {
                return "";
            }
        });
        testLarger("", new PropertyResolverReader(new StringReader("${a}")) {
            @Override
            protected String resolve(String name) {
                return "";
            }
        });
    }

    private void testIncremental(String expected, String test) throws IOException {
        testIncremental(expected, new PropertyResolverReader(new StringReader(test)));
    }

    private void testIncremental(String expected, PropertyResolverReader r) throws IOException {
        char[] buffer = new char[expected.length()];
        for (int i = 0; i < expected.length(); i++) {
            assertEquals(1, r.read(buffer, i, 1));
        }
        assertEquals(-1, r.read(new char[0], 0, 1));
        assertEquals(expected, new String(buffer, 0, expected.length()));
    }

    private void testSame(String expected, String test) throws IOException {
        testSame(expected, new PropertyResolverReader(new StringReader(test)));
    }

    private void testSame(String expected, PropertyResolverReader r) throws IOException {
        char[] buffer = new char[expected.length()];
        int len = r.read(buffer, 0, expected.length());
        if (expected.length() == 0) {
            assertTrue(len == 0 || len == -1);
        } else {
            assertEquals(expected.length(), len);
        }
        assertEquals(-1, r.read(new char[0], 0, 1));
        assertEquals(expected, new String(buffer, 0, expected.length()));
    }

    private void testLarger(String expected, String test) throws IOException {
        testSame(expected, new PropertyResolverReader(new StringReader(test)));
    }

    private void testLarger(String expected, PropertyResolverReader r) throws IOException {
        char[] buffer = new char[expected.length() + 1];
        int len = r.read(buffer, 0, buffer.length);
        if (expected.length() == 0) {
            assertTrue(len == 0 || len == -1);
        } else {
            assertEquals(expected.length(), len);
        }
        assertEquals(-1, r.read(new char[0], 0, 1));
        assertEquals(expected, new String(buffer, 0, expected.length()));
    }
}
