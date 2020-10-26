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

package org.exoplatform.portal.resource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.exoplatform.commons.utils.CharEncoder;
import org.exoplatform.commons.utils.CharsetCharEncoder;

/**
 * A codec for names. It is a modified version of the percent encoding algorithm that translates underscores to their percent
 * counterpart and slash to underscores. Therefore slash chars are never seen as the %2F string as it can cause some issues on
 * tomcat when it is used in an URI.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class Codec {

    private Codec() {
    }

    private static final char[][] table = new char[256][];

    static {
        char[] a = "0123456789ABCDEF".toCharArray();
        for (int b = 0; b < 256; b++) {
            int b1 = (b & 0xF0) >> 4;
            int b2 = b & 0x0F;
            table[b] = new char[] { a[b1], a[b2] };
        }
    }

    public static String decode(String s) {
        try {
            s = s.replace("_", "%2F");
            return URLDecoder.decode(s, "UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public static void encode(Appendable appendable, String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                appendable.append(c);
            } else {
                switch (c) {
                    case 'A':
                    case '.':
                    case '-':
                    case '*':
                        appendable.append(c);
                        break;
                    case ' ':
                        appendable.append('+');
                        break;
                    case '/':
                        appendable.append('_');
                        break;
                    default:
                        CharEncoder encoder = CharsetCharEncoder.getUTF8();
                        byte[] bytes = encoder.encode(c);
                        appendable.append('%');
                        for (byte b : bytes) {
                            for (char cc : table[b]) {
                                appendable.append(cc);
                            }
                        }
                }
            }
        }
    }

}
