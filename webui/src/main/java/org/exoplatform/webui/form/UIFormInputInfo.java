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

import java.io.Writer;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Represents a info text element
 */
@Serialized
public class UIFormInputInfo extends UIFormInputBase<String> {

    public UIFormInputInfo() {
    }

    public UIFormInputInfo(String name, String bindingExpression, String value) {
        super(name, bindingExpression, String.class);
        this.value_ = value;
        readonly_ = true;
    }

    @SuppressWarnings("unused")
    public void decode(Object input, WebuiRequestContext context) {
    }

    public void processRender(WebuiRequestContext context) throws Exception {
        Writer w = context.getWriter();
        w.append("<div id=\"").append(getId()).append("\" class=\"uiInputInfo\">");
        String value = getValue();
        if (value != null) {
            value = HTMLEntityEncoder.getInstance().encode(value);
            w.write(value);
        }
        w.write("</div>");
    }

}
