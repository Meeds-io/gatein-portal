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

package org.exoplatform.webui.form.wysiwyg;

import java.io.Writer;

import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.form.UIFormInputBase;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
/**
 * The Class UIFormWYSIWYGInput.
 */
public class UIFormWYSIWYGInput extends UIFormInputBase<String> {
    public static final String BASIC_TOOLBAR = "Basic".intern();

    public static final String DEFAULT_TOOLBAR = "Default".intern();

    private FCKEditorConfig fckConfig;

    @Deprecated
    private boolean useBasicToolbar = true;

    private boolean sourceModeOnStartup = false;

    private String toolBarName;

    private String width;

    private String height;

    /**
     * Instantiates a new uI form wysiwyg input.
     *
     * @param name the name
     * @param bindingField the binding field
     * @param value the value
     * @param isBasic the is basic
     */
    @Deprecated
    public UIFormWYSIWYGInput(String name, String bindingField, String value, boolean isBasic) {
        super(name, bindingField, String.class);
        useBasicToolbar = isBasic;
        this.value_ = value;
    }

    public UIFormWYSIWYGInput(String name, String bindingField, String value) {
        super(name, bindingField, String.class);
        this.value_ = value;
    }

    /**
     * Gets the fCK config.
     *
     * @return the fCK config
     */
    public FCKEditorConfig getFCKConfig() {
        return fckConfig;
    }

    /**
     * Sets the fCK config.
     *
     * @param config the new fCK config
     */
    public void setFCKConfig(FCKEditorConfig config) {
        this.fckConfig = config;
    }

    public boolean getSourceModeOnStartup() {
        return this.sourceModeOnStartup;
    }

    public void setSourceModeOnStartup(boolean b) {
        this.sourceModeOnStartup = b;
    }

    public String getToolBarName() {
        return this.toolBarName;
    }

    public void setToolBarName(String name) {
        this.toolBarName = name;
    }

    public String getWidth() {
        return this.width;
    }

    public void setWidth(String s) {
        this.width = s;
    }

    public String getHeight() {
        return this.height;
    }

    public void setHeight(String s) {
        this.height = s;
    };

    /*
     * (non-Javadoc)
     *
     * @see org.exoplatform.webui.form.UIFormInputBase#decode(java.lang.Object,
     * org.exoplatform.webui.application.WebuiRequestContext)
     */
    public void decode(Object input, WebuiRequestContext context) {
        value_ = (String) input;
        if (value_ != null && value_.length() == 0)
            value_ = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
     */
    public void processRender(WebuiRequestContext context) throws Exception {
        HttpServletRequest request = ((WebuiRequestContext) context.getParentAppRequestContext()).getRequest();
        FCKEditor editor = new FCKEditor(request, getName());
        if (fckConfig == null) {
            FCKEditorConfig editorConfig = new FCKEditorConfig();
            configureFCKConfig(editorConfig);
            editor.setConfig(editorConfig);
        } else {
            configureFCKConfig(fckConfig);
            editor.setConfig(fckConfig);
        }
        if (toolBarName != null && toolBarName.length() > 0) {
            editor.setToolbarSet(toolBarName);
        } else {
            editor.setToolbarSet(BASIC_TOOLBAR);
        }
        if (value_ == null)
            value_ = "";
        if (height != null && height.length() > 0) {
            editor.setHeight(height);
        }
        if (width != null && width.length() > 0) {
            editor.setWidth(width);
        }
        editor.setValue(value_);
        Writer w = context.getWriter();
        w.write(editor.createHtml());
        if (this.isMandatory())
            w.write(" *");
    }

    private void configureFCKConfig(final FCKEditorConfig config) {
        WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
        String currentLanguage = requestContext.getLocale().getLanguage();
        config.put("AutoDetectLanguage", "false");
        config.put("DefaultLanguage", currentLanguage);
        config.put("SourceModeOnStartup", Boolean.toString(sourceModeOnStartup));
    }
}
