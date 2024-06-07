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

package org.exoplatform.webui.core;

import java.util.List;

import org.exoplatform.webui.form.UIForm;

/**
 * A component that represents a wizard
 */
public abstract class UIWizard extends UIContainer {
    /**
     * The current step in the wizard process
     */
    private int currentStep = 1;

    /**
     * The selected step
     */
    private int selectedStep = 1;

    public UIWizard() {
    }

    public String url(String name) throws Exception {
        UIComponent renderedChild = getChild(currentStep - 1);
        if (!(renderedChild instanceof UIForm))
            return super.event(name);

        org.exoplatform.webui.config.Event event = config.getUIComponentEventConfig(name);
        if (event == null)
            return "??config??";

        UIForm uiForm = (UIForm) renderedChild;
        return uiForm.event(name);
    }

    public void viewStep(int step) {
        if (selectedStep < getChildren().size() + 1 && step > currentStep)
            selectedStep++;
        currentStep = step < selectedStep ? step : selectedStep;
        step = currentStep - 1;
        List<UIComponent> children = getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (i == step) {
                children.get(i).setRendered(true);
            } else {
                children.get(i).setRendered(false);
            }
        }
        // WebuiRequestContext context = WebuiRequestContext.getCurrentInstance() ;
        // context.addUIComponentToUpdateByAjax(this) ;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public int getSelectedStep() {
        return selectedStep;
    }

}
