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

package org.exoplatform.portal.webui;

import java.io.Serializable;

import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;

import nl.captcha.Captcha;

import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.AbstractValidator;

/**
 * @author <a href="mailto:theute@redhat.com">Thomas Heute</a> Validator for Captcha content. Checks that the user input is
 *         equals to the content displayed by the distorted image.
 */
public class CaptchaValidator extends AbstractValidator implements Serializable {

    @Override
    protected boolean isValid(String value, UIFormInput uiInput) {
        PortletRequestContext ctx = PortletRequestContext.getCurrentInstance();
        PortletRequest req = ctx.getRequest();
        PortletSession session = req.getPortletSession();

        Captcha captcha = (Captcha) session.getAttribute(Captcha.NAME);

        return ((captcha != null) && (captcha.isCorrect(value)));
    }

    protected String getMessageLocalizationKey() {
        return "CaptchaValidator.msg.Invalid-input";
    }

    @Override
    protected Object[] getMessageArgs(String value, UIFormInput uiInput) throws Exception {
        String label = getLabelFor(uiInput);
        if (label.charAt(label.length() - 1) == ':') {
            label = label.substring(0, label.length() - 1);
        }
        return new Object[] { label, uiInput.getBindingField() };
    }
}
