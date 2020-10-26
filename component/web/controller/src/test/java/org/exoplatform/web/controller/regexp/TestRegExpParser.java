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

package org.exoplatform.web.controller.regexp;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class TestRegExpParser extends TestCase {

    public void testParseDisjunction() {
        parseParse("|");
        parseParse("a|");
        parseParse("|a");
        parseParse("a|b");
    }

    public void testParseAlternative() {
        parseParse("ab");
        parseParse("^a$");
    }

    public void testParseAssertion() {
        parseParse("^");
        parseParse("$");
    }

    public void testParseAny() {
        parseParse(".");
    }

    public void testParseCharacterLiteral() {
        parseParse("a");
        parseParse("-");
        parseParse("]");
        parseParse("\\$");
        parseParse("\\00");
        parseParse("\\01");
        parseParse("\\018");
        parseParse("\\011");
        parseParse("\\0311");
        parseParse("\\x00");
        parseParse("\\xFF");
        parseParse("\\xff");
        parseParse("\\u0000");
        parseParse("\\uFFFF");

        //
        failFail("\\");
        failFail("\\k");
        failFail("\\0");
        failFail("\\08");
        failFail("\\x1");
        failFail("\\x1G");
        failFail("\\u1");
        failFail("\\u12");
        failFail("\\u123");
        failFail("\\u123G");
    }

    public void testCharacterClass() throws Exception {
        parseParse("[a]");
        parseParse("[{]");
        parseParse("[a{]");
        parseParse("[]a]");
        parseParse("[[a]]");
        parseParse("[a[b]]");

        //
        failFail("[a");
        failFail("[]");
    }

    public void testCharacterClassNegation() throws Exception {
        parseParse("[^a]");
        parseParse("[^]a]");
        parseParse("[^[a]]");
        parseParse("[^a[b]]");

        //
        failFail("[^a");
        failFail("[]");
    }

    public void testCharacterClassRange() {
        parseParse("[a-b]");
        parseParse("[-]");
        parseParse("[ --]");
        parseParse("[ --b]");
        parseParse("[--/]");
        parseParse("[a-]");
        parseParse("[---]");
        parseParse("[--]");

        //
        parseFail("[--[ab]]"); // Parse - or a or b
    }

    public void testCharacterClassAlternative() {
        parseParse("[&]");
        parseParse("[a&&b]");
        parseParse("[a&&]");
        parseParse("[a&&[b]]");

        //
        failFail("[&&]");
        failFail("[&&&]");
        failFail("[&&&&]");

        //
        parseFail("[&&b]");
    }

    public void testCharacterClassEscape() {
        parseParse("[\\\\]");
        parseParse("[\\[]");
        parseParse("[\\]]");
        parseParse("[\\.]");
        parseParse("[\\-]");

        //
        failFail("[\\k]");
    }

    public void testCharacterClassAny() {
        parseParse("[.]");
        parseParse("[^.]");
    }

    public void testCharacterClassAssert() {
        parseParse("[$]");
        parseParse("[^$]");
        parseParse("[^^]");
        parseParse("[$^]");
    }

    public void testParseGroup() {
        parseParse("()");
        parseParse("(?)");
        parseParse("(a)");
        parseParse("(|)");
        parseParse("(a|)");
        parseParse("(|a)");
        parseParse("(a|b)");
        parseParse("(()())");

        //
        parseParse("(?:)");
        parseParse("(?=)");
        parseParse("(?!)");
        parseParse("(?<=)");
        parseParse("(?<!)");

        //
        failFail("(?a)");
        failFail("(");
        failFail(")");
        failFail("(?<)");
        failFail("(?<a)");
    }

    public void testParseQuantifier() {
        parseParse("^?");
        parseParse("$?");
        parseParse("a?");
        parseParse("()?");
        parseParse("[a]?");
        parseParse("^*");
        parseParse("$*");
        parseParse("a*");
        parseParse("()*");
        parseParse("[a]*");
        parseParse("^+");
        parseParse("$+");
        parseParse("a+");
        parseParse("()+");
        parseParse("[a]+");
        parseParse("a{0}");
        parseParse("a{0,}");
        parseParse("a{0,1}");

        //
        failFail("?");
        failFail("+");
        failFail("*");
        failFail("{");
        failFail("a{");
        failFail("a{}");
        failFail("a{b");
        failFail("a{0");
        failFail("a{0,");
        failFail("a{0,1");
    }

    public void testParseQuantifierMode() {
        parseParse("a??");
        parseParse("a?+");
        parseParse("a+?");
        parseParse("a++");
        parseParse("a*?");
        parseParse("a*+");
        parseParse("a{0}?");
        parseParse("a{0}+");
    }

    void parseFail(String s) {
        parse(s, false, true);
    }

    void parseParse(String s) {
        parse(s, false, false);
    }

    void failFail(String s) {
        parse(s, true, true);
    }

    void parse(String s, boolean javaFail, boolean javaccFail) {
        try {
            Pattern.compile(s);
            if (javaFail) {
                throw new AssertionFailedError("Was expecting " + s + " to not be compilable");
            }
        } catch (PatternSyntaxException e) {
            if (!javaFail) {
                AssertionFailedError afe = new AssertionFailedError("Was expecting " + s + " to be compilable");
                afe.initCause(e);
                throw afe;
            }
        }
        try {
            Stream stream = new Stream(s);
            Lexer lexer = new Lexer(stream);
            REParser parser = new REParser(lexer);
            parser.parse();
            assertEquals(s.length(), stream.getIndex());
            if (lexer.hasNext()) {
                throw new SyntaxException();
            }
            if (javaccFail) {
                throw new AssertionFailedError("Was expecting " + s + " to not be compilable");
            }
        } catch (SyntaxException e) {
            if (!javaccFail) {
                AssertionFailedError afe = new AssertionFailedError("Was expecting " + s + " to be compilable");
                afe.initCause(e);
                throw afe;
            }
        }
    }
}
