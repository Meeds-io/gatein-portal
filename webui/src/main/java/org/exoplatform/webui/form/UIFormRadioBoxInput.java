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

package org.exoplatform.webui.form;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.model.SelectItemOption;

/**
 * Represents a radio box element The selected box value is stored in the value_ property of UIFormInputBase
 */
@Serialized
public class UIFormRadioBoxInput extends UIFormInputBase<String> {

    public static int VERTICAL_ALIGN = 1;

    public static int HORIZONTAL_ALIGN = 2;

    /**
     * The list of radio boxes
     */
    private List<SelectItemOption<String>> options_;

    /**
     * Alignment of the element (vertical or horizontal)
     */
    private int align_;

    private int topRender_ = 0;

    public UIFormRadioBoxInput() {
    }

    public UIFormRadioBoxInput(String name, String value) {
        this(name, value, new ArrayList<SelectItemOption<String>>());
    }

    public UIFormRadioBoxInput(String name, String value, List<SelectItemOption<String>> options) {
        super(name, value, String.class);
        value_ = value;
        this.options_ = options;
        align_ = HORIZONTAL_ALIGN;
    }

    public final List<SelectItemOption<String>> getOptions() {
        return options_;
    }

    public final UIFormRadioBoxInput setOptions(List<SelectItemOption<String>> options) {
        this.options_ = options;
        return this;
    }

    public final UIFormRadioBoxInput setAlign(int val) {
        align_ = val;
        return this;
    }

    /**
     * Method set render one radio box in fois !
     *
     * @return : object of UIFormRadioBoxInput
     */
    public final UIFormRadioBoxInput setRenderOneRadioBox() {
        topRender_ = 1;
        return this;
    }

    @SuppressWarnings("unused")
    public void decode(Object input, WebuiRequestContext context) {
        if (isDisabled())
            return;
        if (input != null)
            value_ = (String) input;
    }

    public void processRender(WebuiRequestContext context) throws Exception {
        if (options_ == null)
            return;
        ResourceBundle res = context.getApplicationResourceBundle();
        Writer w = context.getWriter();
        if (value_ == null) {
            SelectItemOption<String> si = options_.get(0);
            value_ = si.getValue();
        }

        int index = 0;
        for (int i = index; i < options_.size(); i++) {
            SelectItemOption<String> si = options_.get(i);
            String checked = "";
            if (si.getValue().equals(value_))
                checked = " checked='checked'";
            // if(align_ == VERTICAL_ALIGN) w.write("<div style='overflow:hidden; width: 100%'>");
            // if(align_ == VERTICAL_ALIGN) w.write("<div style='clear:both;'><span></span></div>") ;
            if (align_ == VERTICAL_ALIGN)
                w.write("<div>");
            w.write("<label class=\"uiRadio\"><input class='radio' type='radio'");
            if (readonly_)
                w.write(" readonly ");
            if (isDisabled())
                w.write(" disabled ");
            w.write(checked);
            w.write(" name='");
            w.write(getName());
            w.write("'");
            w.write(" id='");
            w.write(getName());
            w.write("'");
            w.write(" value='");
            w.write(si.getValue());
            w.write("'/>");
            w.write(" <span>");
            String label = getId() + ".label." + si.getLabel();
            try {
                label = res.getString(label);
            } catch (MissingResourceException e) {
                label = si.getLabel();
            }
            w.write(label);
            w.write("</span></label>");
            if (align_ == VERTICAL_ALIGN)
                w.write("</div>");
            // if(align_ == VERTICAL_ALIGN) w.write("</div>");

            if (topRender_ == 1) {
                index = i + 1;
                if (index == options_.size())
                    index = 0;
                break;
            }
        }

    }

    public String renderWithId(int iteration) throws Exception {
        if (options_ == null)
            return "";
        Writer w = new StringWriter();
        if (value_ == null) {
            SelectItemOption<String> si = options_.get(0);
            value_ = si.getValue();
        }

        int index = 0;
        for (int i = index; i < options_.size(); i++) {
            SelectItemOption<String> si = options_.get(i);
            String checked = "";
            if (si.getValue().equals(value_))
                checked = " checked='checked'";
            // if(align_ == VERTICAL_ALIGN) w.write("<div style='overflow:hidden; width: 100%'>");
            // if(align_ == VERTICAL_ALIGN) w.write("<div style='clear:both;'><span></span></div>") ;
            if (align_ == VERTICAL_ALIGN)
                w.write("<div>");
            w.write("<input class='radio' type='radio'");
            if (readonly_)
                w.write(" readonly ");
            if (isDisabled())
                w.write(" disabled ");
            w.write(checked);
            w.write(" name='");
            w.write(getName());
            w.write("'");
            w.write(" id='");
            w.write(getName() + iteration);
            w.write("'");
            w.write(" value='");
            w.write(si.getValue());
            w.write("'/>");
            if (align_ == VERTICAL_ALIGN)
                w.write("</div>");
            // if(align_ == VERTICAL_ALIGN) w.write("</div>");

            if (topRender_ == 1) {
                index = i + 1;
                if (index == options_.size())
                    index = 0;
                break;
            }
        }
        return w.toString();
    }


}
