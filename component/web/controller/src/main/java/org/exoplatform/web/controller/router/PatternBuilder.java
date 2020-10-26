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

import org.exoplatform.web.controller.regexp.Literal;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class PatternBuilder {

    /** . */
    private final StringBuilder buffer = new StringBuilder();

    /** . */
    PatternBuilder() {
    }

    public PatternBuilder expr(CharSequence s) {
        if (s == null) {
            throw new NullPointerException("No null expression allowed");
        }
        buffer.append(s);
        return this;
    }

    public PatternBuilder expr(char s) {
        buffer.append(s);
        return this;
    }

    public PatternBuilder litteral(String s, int from, int to) {
        if (from < 0) {
            throw new IllegalArgumentException("No negative from argument");
        }
        if (to > s.length()) {
            throw new IllegalArgumentException("No to argument greater than the string length");
        }
        if (from > to) {
            throw new IllegalArgumentException("The to argument cannot be greater than the from argument");
        }
        if (from < to) {
            for (int i = from; i < to; i++) {
                char c = s.charAt(i);
                if (Literal.isEscaped(c)) {
                    buffer.append('\\');
                }
                buffer.append(c);
            }
        }
        return this;
    }

    public PatternBuilder literal(String s, int from) {
        return litteral(s, from, s.length());
    }

    public PatternBuilder literal(String s) {
        return litteral(s, 0, s.length());
    }

    public PatternBuilder literal(char c) {
        return literal(Character.toString(c));
    }

    public String build() {
        return buffer.toString();
    }
}
