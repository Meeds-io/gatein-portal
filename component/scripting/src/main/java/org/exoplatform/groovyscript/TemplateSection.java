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
package org.exoplatform.groovyscript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class TemplateSection {

    /** . */
    private final SectionType type;

    /** . */
    private final List<SectionItem> items;

    public TemplateSection(SectionType type, String text) {
        this(type, text, 0, 0);
    }

    public TemplateSection(SectionType type, String text, Position pos) {
        this(type, text, pos.getCol(), pos.getLine());
    }

    public TemplateSection(SectionType type, String text, int colNumber, int lineNumber) {
        if (type == null) {
            throw new NullPointerException();
        }
        if (text == null) {
            throw new NullPointerException();
        }

        // Now we process the line breaks
        ArrayList<SectionItem> sections = new ArrayList<SectionItem>();

        //
        int from = 0;
        while (true) {
            int to = text.indexOf('\n', from);

            //
            if (to != -1) {
                String chunk = text.substring(from, to);
                sections.add(new TextItem(new Position(colNumber, lineNumber), chunk));

                //
                sections.add(new LineBreakItem(new Position(colNumber + (to - from), lineNumber)));

                //
                from = to + 1;
                lineNumber++;
                colNumber = 1;
            } else {
                String chunk = text.substring(from);
                sections.add(new TextItem(new Position(colNumber, lineNumber), chunk));
                break;
            }
        }

        //
        this.type = type;
        this.items = Collections.unmodifiableList(sections);
    }

    public SectionType getType() {
        return type;
    }

    public List<SectionItem> getItems() {
        return items;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof TemplateSection) {
            TemplateSection that = (TemplateSection) obj;
            return type == that.type && items.equals(that.items);
        }
        return false;
    }

    @Override
    public String toString() {
        return "TextSection[type=" + type + ",text=" + items + "]";
    }
}
