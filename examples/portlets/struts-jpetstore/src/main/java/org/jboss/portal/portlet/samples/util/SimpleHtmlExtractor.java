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
package org.jboss.portal.portlet.samples.util;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class SimpleHtmlExtractor {
    public static String extractAttribute(String htmlFragment, String attr) {
        String srch = " " + attr + "=";
        int pos = htmlFragment.indexOf(srch);
        if (pos == -1) {
            return null;
        }
        String[] segments = htmlFragment.substring(pos + srch.length() + 1).split("[\"\']");
        if (segments.length == 0) {
            return null;
        }

        return segments[0];
    }

    public static String removeElements(String htmlFragment, String... elements) {
        if (elements.length == 0) {
            StringBuilder sb = new StringBuilder(" ");
            String[] noTags = htmlFragment.split("<[^>]+>");
            for (int i = 0; i < noTags.length; i++) {
                if (!"".equals(noTags[i])) {
                    sb.append(noTags[i]);
                }
            }
            return sb.toString();
        }

        return htmlFragment;
    }
}
