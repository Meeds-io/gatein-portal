/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.web.controller.router;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.exoplatform.web.url.MimeType;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class URIHelper implements Appendable {

    /** . */
    private StringBuilder sb;

    /** . */
    final URIWriter writer;

    public URIHelper() {
        this(new StringBuilder());
    }

    public URIHelper(StringBuilder sb) {
        this.sb = sb;
        this.writer = new URIWriter(this, MimeType.PLAIN);
    }

    public String getPath() {
        if (sb != null) {
            int index = sb.indexOf("?");
            if (index != -1) {
                return sb.substring(0, index);
            } else {
                return sb.toString();
            }
        }
        return null;
    }

    public Map<String, String[]> getQueryParams() {
        if (sb != null) {
            int index = sb.indexOf("?");
            if (index != -1) {
                String query = sb.substring(index + 1);
                return Arrays.stream(query.split("&"))
                             .collect(Collectors.toMap(s -> s.split("=")[0],
                                                       s -> s.split("=").length > 1 ? new String[] { s.split("=")[1] }
                                                                                    : new String[] {""}));
              }
        }
        return null;
    }

    public void reset() {
        if (sb != null) {
            sb.setLength(0);
        }
        writer.reset(sb);
    }

    public Appendable append(CharSequence csq) throws IOException {
        sb.append(csq);
        return this;
    }

    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        sb.append(csq, start, end);
        return this;
    }

    public Appendable append(char c) throws IOException {
        sb.append(c);
        return this;
    }
}
