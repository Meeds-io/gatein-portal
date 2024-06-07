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

package org.exoplatform.webui.bean;

import java.lang.reflect.Method;
import java.util.List;

import org.exoplatform.util.ReflectionUtil;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormInputSet;

/**
 * A stateless object that must be obtained via the {@link #getInstance()} method.
 *
 * Author : Nhu Dinh Thuan thuan.nhu@exoplatform.com Oct 13, 2006
 */
public class ReflectionDataMapping implements BeanDataMapping {

    /** . */
    private static final ReflectionDataMapping instance = new ReflectionDataMapping();

    public static ReflectionDataMapping getInstance() {
        return instance;
    }

    private ReflectionDataMapping() {
    }

    public void mapBean(Object bean, UIForm uiForm) throws Exception {
        List<UIComponent> children = uiForm.getChildren();
        for (UIComponent uichild : children) {
            if (uichild instanceof UIFormInput) {
                invokeSetBindingField(bean, (UIFormInput) uichild);
            } else if (uichild instanceof UIFormInputSet) {
                mapBean(bean, (UIFormInputSet) uichild);
            }
        }
    }

    public void mapBean(Object bean, UIFormInputSet uiFormInputSet) throws Exception {
        List<UIComponent> children = uiFormInputSet.getChildren();
        for (UIComponent uichild : children) {
            if (uichild instanceof UIFormInput) {
                invokeSetBindingField(bean, (UIFormInput) uichild);
            }
        }
    }

    public void mapField(UIForm uiForm, Object bean) throws Exception {
        List<UIComponent> children = uiForm.getChildren();
        for (UIComponent uichild : children) {
            if (uichild instanceof UIFormInput) {
                invokeGetBindingField((UIFormInput) uichild, bean);
            } else if (uichild instanceof UIFormInputSet) {
                mapField((UIFormInputSet) uichild, bean);
            }
        }
    }

    public void mapField(UIFormInputSet uiFormInputSet, Object bean) throws Exception {
        List<UIComponent> children = uiFormInputSet.getChildren();
        for (UIComponent uichild : children) {
            if (uichild instanceof UIFormInput) {
                invokeGetBindingField((UIFormInput) uichild, bean);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void invokeGetBindingField(UIFormInput uiFormInput, Object bean) throws Exception {
        String bindingField = uiFormInput.getBindingField();
        if (bindingField == null)
            return;
        Method method = ReflectionUtil.getGetBindingMethod(bean, bindingField);
        Object value = method.invoke(bean, ReflectionUtil.EMPTY_ARGS);
        if (value == null)
            return;
        uiFormInput.setValue(value);
    }

    private void invokeSetBindingField(Object bean, UIFormInput uiFormInput) throws Exception {
        // Do not execute in case uiFormInput is not editable
        if (uiFormInput instanceof UIFormInputBase) {
            UIFormInputBase uiStringBase = (UIFormInputBase) uiFormInput;
            if (uiStringBase.isReadOnly() || uiStringBase.isDisabled()) {
                return;
            }
        }
        String bindingField = uiFormInput.getBindingField();
        if (bindingField == null)
            return;
        Class[] classes = new Class[] { uiFormInput.getTypeValue() };
        Method method = ReflectionUtil.getSetBindingMethod(bean, bindingField, classes);
        method.invoke(bean, new Object[] { uiFormInput.getValue() });
    }

}
