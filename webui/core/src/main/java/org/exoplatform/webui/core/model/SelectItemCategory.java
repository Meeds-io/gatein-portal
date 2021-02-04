/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.webui.core.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by The eXo Platform SARL Author : Nguyen Thi Hoa hoa.nguyen@exoplatform.com Aug 10, 2006
 *
 * Represents a group of SelectItemOption, held in a UIFormInputItemSelector
 *
 * @see org.exoplatform.webui.form.UIFormInputItemSelector
 * @see SelectItemOption
 */
public class SelectItemCategory<T> {
    /**
     * The name of the category
     */
    private String name;

    /**
     * The label of the category
     */
    private String label;

    /**
     * The list of SelectItemOption that this category contains
     */
    private List<SelectItemOption<T>> options;

    /**
     * Whether this category is selected
     */
    protected boolean selected = false;

    public SelectItemCategory() {
    }

    public SelectItemCategory(String name) {
        this.name = name;
    }

    public SelectItemCategory(String name, boolean selected) {
        this.name = name;
        label = name;
        this.selected = selected;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean b) {
        selected = b;
    }

    public List<SelectItemOption<T>> getSelectItemOptions() {
        return options;
    }

    public void setSelectItemOptions(List<SelectItemOption<T>> options) {
        this.options = options;
    }

    public SelectItemCategory<T> addSelectItemOption(SelectItemOption<T> option) {
        if (options == null) {
            options = new ArrayList<>();
        }
        options.add(option);
        return this;
    }

    public SelectItemOption<T> getSelectedItemOption() {
        if (options == null)
            return null;
        for (SelectItemOption<T> item : options) {
            if (item.isSelected())
                return item;
        }
        return options.get(0);
    }
}
