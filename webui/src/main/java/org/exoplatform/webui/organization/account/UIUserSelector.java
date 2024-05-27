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

package org.exoplatform.webui.organization.account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.search.UserSearchService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL Author : Pham Tuan phamtuanchip@gmail.com Dec 11, 2007 Modified: dang.tung tungcnw@gmail.com
 * Nov 22, 2008
 */
@SuppressWarnings("deprecation")
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/organization/account/UIUserSelector.gtmpl", events = {
        @EventConfig(listeners = UIUserSelector.AddActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIUserSelector.AddUserActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIUserSelector.SearchActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIUserSelector.ShowPageActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIUserSelector.CloseActionListener.class, phase = Phase.DECODE) })
@Serialized
public class UIUserSelector extends UIForm implements UIPopupComponent {
    public static final String FIELD_KEYWORD = "Quick Search";

    protected Map<String, User> userData_ = new HashMap<String, User>();

    private boolean isShowSearch_ = false;

    private boolean isShowSearchUser = true;

    private UIPageIterator uiIterator_;

    private String selectedUsers;

    private boolean multi = true;

    public UIUserSelector() throws Exception {
        addUIFormInput(new UIFormStringInput(FIELD_KEYWORD, FIELD_KEYWORD, null));
        isShowSearch_ = true;
        uiIterator_ = new UIPageIterator();
        computePageList(null);
        uiIterator_.setId("UISelectUserPage");
    }

    private void computePageList(String keyword) {
      UserSearchService userSearchService = getApplicationComponent(UserSearchService.class);
      try {
        ListAccess<User> userList = userSearchService.searchUsers(keyword);
        uiIterator_.setPageList(new LazyPageList<>(userList, 10));
      } catch (Exception e) {
        UIApplication uiApp = getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIUserSelector.errorListingUsers", null));
      }
    }

    @SuppressWarnings({ "unchecked" })
    public List<User> getData() throws Exception {
        if (getMulti()) {
            for (Object obj : uiIterator_.getCurrentPageData()) {
                User user = (User) obj;
                UIFormCheckBoxInput<Boolean> uiFormCheckBoxInput = getUIFormCheckBoxInput(user.getUserName());
                if (uiFormCheckBoxInput == null) {
                    uiFormCheckBoxInput = new UIFormCheckBoxInput<Boolean>(user.getUserName(), user.getUserName(), false);
                    addUIFormInput(uiFormCheckBoxInput);
                }

                uiFormCheckBoxInput.setChecked(uiIterator_.isSelectedItem(user.getUserName()));
            }
        }
        return new ArrayList<User>(uiIterator_.getCurrentPageData());
    }

    public String getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(String selectedUsers) {
        this.selectedUsers = selectedUsers;
    }

    public void setMulti(boolean multi) {
        this.multi = multi;
    }

    public boolean getMulti() {
        return multi;
    }

    public UIPageIterator getUIPageIterator() {
        return uiIterator_;
    }

    public long getAvailablePage() {
        return uiIterator_.getAvailablePage();
    }

    public long getCurrentPage() {
        return uiIterator_.getCurrentPage();
    }

    public String[] getActions() {
        return new String[] { "Add", "Close" };
    }

    public void activate() {
    }

    public void deActivate() {
    }

    public String getLabel(String id) {
        try {
            return super.getLabel(id);
        } catch (Exception e) {
            return id;
        }
    }

    public void setShowSearch(boolean isShowSearch) {
        this.isShowSearch_ = isShowSearch;
    }

    public boolean isShowSearch() {
        return isShowSearch_;
    }

    public void setShowSearchUser(boolean isShowSearchUser) {
        this.isShowSearchUser = isShowSearchUser;
    }

    public void search(String keyword) throws Exception {
        computePageList(keyword);
    }

    public boolean isShowSearchUser() {
        return isShowSearchUser;
    }

    public static class AddActionListener extends EventListener<UIUserSelector> {
        public void execute(Event<UIUserSelector> event) throws Exception {
            UIUserSelector uiForm = event.getSource();
            // Used to compute checked combo box
            uiForm.setSelectedItem();
            StringBuilder sb = new StringBuilder();
            // get item from selected item map
            Set<String> items = uiForm.uiIterator_.getSelectedItems();
            if (items.size() == 0) {
                UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
                uiApp.addMessage(new ApplicationMessage("UIUserSelector.msg.user-required", null));
                return;
            }
            String[] arrItems = items.toArray(new String[items.size()]);
            Arrays.sort(arrItems);

            for (String key : arrItems) {
                if (sb.toString() != null && sb.toString().trim().length() != 0)
                    sb.append(",");
                sb.append(key);
            }

            uiForm.setSelectedUsers(sb.toString());
            uiForm.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());
        }
    }

    public static class AddUserActionListener extends EventListener<UIUserSelector> {
        public void execute(Event<UIUserSelector> event) throws Exception {
            UIUserSelector uiForm = event.getSource();
            String userName = event.getRequestContext().getRequestParameter(OBJECTID);
            uiForm.setSelectedUsers(userName);
            uiForm.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());
        }
    }

    protected void updateCurrentPage(int page) throws Exception {
        uiIterator_.setCurrentPage(page);
    }

    public void setKeyword(String value) {
        getUIStringInput(FIELD_KEYWORD).setValue(value);
    }

    @SuppressWarnings({ "unchecked" })
    private void setSelectedItem() throws Exception {
        for (Object o : this.uiIterator_.getCurrentPageData()) {
            User u = (User) o;
            UIFormCheckBoxInput<Boolean> input = this.getUIFormCheckBoxInput(u.getUserName());
            if (input != null) {
                this.uiIterator_.setSelectedItem(u.getUserName(), input.isChecked());
            }
        }
    }

    public static class SearchActionListener extends EventListener<UIUserSelector> {
        public void execute(Event<UIUserSelector> event) throws Exception {
            UIUserSelector uiForm = event.getSource();

            String keyword = uiForm.getUIStringInput(FIELD_KEYWORD).getValue();
            uiForm.search(keyword);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
        }
    }

    public static class CloseActionListener extends EventListener<UIUserSelector> {
        public void execute(Event<UIUserSelector> event) throws Exception {
            UIUserSelector uiForm = event.getSource();
            uiForm.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());
        }
    }

    public static class ShowPageActionListener extends EventListener<UIUserSelector> {
        public void execute(Event<UIUserSelector> event) throws Exception {
            UIUserSelector uiSelectUserForm = event.getSource();
            uiSelectUserForm.setSelectedItem();

            int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
            uiSelectUserForm.updateCurrentPage(page);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiSelectUserForm);
        }
    }
}
