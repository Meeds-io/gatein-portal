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
import java.io.OutputStream;
import java.nio.charset.Charset;


/**
 * A text encoder that encodes text to an output stream. No assumptions must be made about the statefullness nature of an
 * encoder as some encoder may be statefull and some encoder may be stateless.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public interface TextEncoder {

    /**
     * Returns the charset that will perform the encoding.
     *
     * @return the charset for encoding
     */
    Charset getCharset();

    void encode(char c, OutputStream out) throws IOException;

    void encode(char[] chars, int off, int len, OutputStream out) throws IOException;

    void encode(String str, int off, int len, OutputStream out) throws IOException;
}
