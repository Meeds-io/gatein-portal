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

package org.exoplatform.webui.form.validator;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInput;

/**
 * Created by The eXo Platform SARL Author : Tran The Trong trongtt@gmail.com May 15, 2007
 *
 * Validates whether a date is in a correct format
 */

public class DateTimeValidator extends AbstractValidator implements Serializable {
    @Override
    protected String getMessageLocalizationKey() {
        return "DateTimeValidator.msg.Invalid-input";
    }

    @Override
    protected boolean isValid(String value, UIFormInput uiInput) {
        UIFormDateTimeInput uiDateInput = (UIFormDateTimeInput) uiInput;
        SimpleDateFormat sdf = new SimpleDateFormat(uiDateInput.getDatePattern_().trim());
        // Specify whether or not date/time parsing is to be lenient.
        sdf.setLenient(false);
        try {
            sdf.parse(value);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    @Override
    protected String trimmedValueOrNullIfBypassed(String value, UIFormInput uiInput, boolean exceptionOnMissingMandatory,
            boolean trimValue) throws Exception {
        if (!(uiInput instanceof UIFormDateTimeInput)) {
            return null;
        } else {
            return super.trimmedValueOrNullIfBypassed(value, uiInput, exceptionOnMissingMandatory, trimValue);
        }
    }

    @Override
    protected Object[] getMessageArgs(String value, UIFormInput uiInput) throws Exception {
        return new Object[] { getLabelFor(uiInput), value };
    }
}
