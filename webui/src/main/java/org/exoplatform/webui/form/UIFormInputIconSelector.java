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

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.Param;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.config.annotation.ParamConfig;
import org.exoplatform.webui.core.UIDropDownControl;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL Author : Tuan Nguyen tuan08@users.sourceforge.net Jun 26, 2006
 *
 * Represents an icon selector
 */
@ComponentConfigs({
        @ComponentConfig(template = "system:/groovy/webui/form/UIFormInputIconSelector.gtmpl", events = {
                @EventConfig(listeners = UIFormInputIconSelector.ChangeOptionSetActionListener.class, phase = Phase.DECODE),
                @EventConfig(listeners = UIFormInputIconSelector.ChangeIconCategoryActionListener.class, phase = Phase.DECODE),
                @EventConfig(listeners = UIFormInputIconSelector.SelectIconActionListener.class, phase = Phase.DECODE) }, initParams = { @ParamConfig(name = "IconSet16x16", value = "app:/WEB-INF/conf/uiconf/webui/component/IconSet16x16.groovy") }),
        @ComponentConfig(type = UIDropDownControl.class, id = "IconDropDown", template = "system:/groovy/webui/core/UIDropDownControlSelector.gtmpl", events = { @EventConfig(listeners = UIFormInputIconSelector.SelectItemActionListener.class) }) })
public class UIFormInputIconSelector extends UIFormInputBase<String> {

    private List<String> optionSets = new ArrayList<String>();

    private List<IconSet> iconSets = new ArrayList<IconSet>();

    private String paramDefault = "IconSet16x16";

    private CategoryIcon selectedIconCategory;

    private IconSet selectedIconSet;

    private String selectedIcon;

    private String selectType = "page";

    public static final String[] SELECT_TYPE = { "portal", "page" };

    public UIFormInputIconSelector(String name, String bindingField) throws Exception {
        super(name, bindingField, String.class);
        setComponentConfig(UIFormInputIconSelector.class, null);
        addChild(UIDropDownControl.class, "IconDropDown", null);
        this.setValues(paramDefault);
        selectType = "page";
    }

    private List<SelectItemOption<String>> getDropOptions() {
        List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
        for (String s : optionSets) {
            options.add(new SelectItemOption<String>(s));
        }
        return options;
    }

    public void setType(String type) {
        selectType = type;
    }

    public String getType() {
        return selectType;
    }

    public void setValues(String paramName) throws Exception {
        selectedIconCategory = null;
        selectedIconSet = null;
        selectedIcon = null;
        iconSets.clear();
        optionSets.clear();
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        if (context instanceof PortletRequestContext) {
            context = (WebuiRequestContext) context.getParentAppRequestContext();
        }
        for (Param param : getComponentConfig().getInitParams().getParams()) {
            if (param.getName().equals(paramName)) {
                CategoryIcon categoryIconSet = (CategoryIcon) param.getMapGroovyObject(context);
                if (selectedIconCategory == null)
                    selectedIconCategory = categoryIconSet;
                for (IconSet iconset : categoryIconSet.getCategory()) {
                    if (selectedIconSet == null)
                        setSelectedIconSet(iconset);
                    IconCategory iconCategory = iconset.getIconCategory();
                    if (selectedIcon == null)
                        setSelectedIcon(iconCategory.getValue().get(0));
                    iconSets.add(iconset);
                }
            }
            optionSets.add(param.getName());
            getChild(UIDropDownControl.class).setOptions(getDropOptions());
        }
    }

    public List<String> getOptionSets() {
        return optionSets;
    }

    public List<IconSet> getListIconSet() {
        return iconSets;
    }

    public CategoryIcon getSelectedCategory() {
        return selectedIconCategory;
    }

    public void setSelectedCategory(CategoryIcon category) {
        selectedIconCategory = category;
    }

    public IconSet getSelectedIconSet() {
        return selectedIconSet;
    }

    public void setSelectedIconSet(IconSet iconset) {
        selectedIconSet = iconset;
    }

    public List<String> getListIcon(IconSet set) {
        return set.getIconCategory().getValue();
    }

    public String getValue() {
        return getSelectedIcon();
    }

    public UIFormInput setValue(String value) {
        selectedIcon = value;
        return this;
    }

    public void setSelectedIcon(String name) {
        selectedIcon = name;
    }

    public String getSelectedIcon() {
        if (selectedIcon != null)
            return selectedIcon;
        IconSet set = getSelectedIconSet();
        IconCategory iconCategory = set.getIconCategory();
        selectedIcon = iconCategory.getValue().get(0);
        return selectedIcon;
    }

    public void decode(Object input, WebuiRequestContext context) {
        if (input == null || String.valueOf(input).length() < 1)
            return;
        selectedIcon = (String) input;
    }

    public static class ChangeOptionSetActionListener extends EventListener<UIFormInputIconSelector> {
        public void execute(Event<UIFormInputIconSelector> event) throws Exception {
            UIFormInputIconSelector uiForm = event.getSource();
            String paramName = event.getRequestContext().getRequestParameter(OBJECTID);
            uiForm.setValues(paramName);
        }
    }

    public static class ChangeIconCategoryActionListener extends EventListener<UIFormInputIconSelector> {
        public void execute(Event<UIFormInputIconSelector> event) throws Exception {
            UIFormInputIconSelector uiIconSelector = event.getSource();
            String setName = event.getRequestContext().getRequestParameter(OBJECTID);
            uiIconSelector.setSelectedIcon(null);
            for (IconSet set : uiIconSelector.getListIconSet()) {
                if (set.getName().equals(setName)) {
                    uiIconSelector.setSelectedIconSet(set);
                }
            }

            UIForm uiForm = uiIconSelector.getAncestorOfType(UIForm.class);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
        }
    }

    public static class SelectIconActionListener extends EventListener<UIFormInputIconSelector> {
        public void execute(Event<UIFormInputIconSelector> event) throws Exception {
            UIFormInputIconSelector uiIconSelector = event.getSource();
            String iconName = event.getRequestContext().getRequestParameter(OBJECTID);
            if (iconName.equals("Default")) {
                uiIconSelector.setSelectedIcon("Default");
                for (IconSet set : uiIconSelector.getListIconSet()) {
                    if (set.getName().equals("misc")) {
                        uiIconSelector.setSelectedIconSet(set);
                    }
                }

                UIForm uiForm = uiIconSelector.getAncestorOfType(UIForm.class);
                event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
                return;
            }
            uiIconSelector.setSelectedIcon(iconName);

            UIForm uiForm = uiIconSelector.getAncestorOfType(UIForm.class);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
        }
    }

    public static class CategoryIcon {

        private String name;

        private String sizeOption;

        private List<IconSet> category = new ArrayList<IconSet>();

        public CategoryIcon(String n, String s) {
            name = n;
            sizeOption = s;
        }

        public String getSizeOption() {
            return sizeOption;
        }

        public String getName() {
            return name;
        }

        public CategoryIcon addCategory(IconSet set) {
            category.add(set);
            return this;
        }

        public List<IconSet> getCategory() {
            return category;
        }

    }

    public static class IconSet {

        private String name;

        private IconCategory iconcate_ = null;

        private IconSet set_ = null;

        public IconSet(String n) {
            name = n;
        }

        public String getName() {
            return name;
        }

        public IconCategory getIconCategory() {
            return iconcate_;
        }

        public IconSet addCategories(IconCategory iconCate) {
            iconcate_ = iconCate;
            return this;
        }

        public IconSet getIconSet() {
            return set_;
        }

        public void addSets(IconSet set) {
            set_ = set;
        }

    }

    public static class IconCategory extends SelectItemOption<List<String>> {

        public IconCategory(String name) {
            super(name, new ArrayList<String>());
        }

        public IconCategory addIcon(String icon) {
            this.value.add(icon);
            return this;
        }

    }

    // Just for using UIDropDownControl correctly.
    public static class SelectItemActionListener extends EventListener<UIDropDownControl> {
        public void execute(Event<UIDropDownControl> event) throws Exception {
            UIDropDownControl uiDropDown = event.getSource();
            event.getRequestContext().addUIComponentToUpdateByAjax(uiDropDown);
        }
    }
}
