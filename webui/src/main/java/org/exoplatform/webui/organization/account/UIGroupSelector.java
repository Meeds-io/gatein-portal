/**
 * Copyright (C) 2019 eXo Platform SAS.
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

import java.util.*;
import java.util.stream.Collectors;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIBreadcumbs.LocalPath;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Author : dang.tung tungcnw@gmail.com Nov 22, 2008
 */
@ComponentConfigs({
        @ComponentConfig(template = "system:/groovy/webui/organization/account/UIGroupSelector.gtmpl", events = {
                @EventConfig(phase = Phase.DECODE, listeners = UIGroupSelector.ChangeNodeActionListener.class, csrfCheck = false),
                @EventConfig(phase = Phase.DECODE, listeners = UIGroupSelector.SelectGroupActionListener.class, csrfCheck = false),
                @EventConfig(phase = Phase.DECODE, listeners = UIGroupSelector.SelectPathActionListener.class, csrfCheck = false) }),
        @ComponentConfig(type = UITree.class, id = "UITreeGroupSelector", template = "system:/groovy/webui/core/UITree.gtmpl", events = @EventConfig(phase = Phase.DECODE, listeners = UITree.ChangeNodeActionListener.class, csrfCheck = false)),
        @ComponentConfig(type = UIBreadcumbs.class, id = "BreadcumbGroupSelector", template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl", events = @EventConfig(phase = Phase.DECODE, listeners = UIBreadcumbs.SelectPathActionListener.class)) })
@Serialized
public class UIGroupSelector extends UIContainer {

    private OrganizationService organizationService;

    private UserACL userACL;

    private Group selectGroup_;

    @SuppressWarnings("unchecked")
    public UIGroupSelector() throws Exception {
        organizationService = getApplicationComponent(OrganizationService.class);
        userACL = getApplicationComponent(UserACL.class);

        UIBreadcumbs uiBreadcumbs = addChild(UIBreadcumbs.class, "BreadcumbGroupSelector", "BreadcumbGroupSelector");
        UITree tree = addChild(UITree.class, "UITreeGroupSelector", "TreeGroupSelector");

        tree.setSibbling(getGroups(null));
        tree.setIcon("GroupAdminIcon");
        tree.setSelectedIcon("PortalIcon");
        tree.setBeanIdField("id");
        tree.setBeanLabelField("label");
        tree.setEscapeHTML(true);
        uiBreadcumbs.setBreadcumbsStyle("UIExplorerHistoryPath");
    }

    public Group getCurrentGroup() {
        return selectGroup_;
    }

    @SuppressWarnings("unchecked")
    public void changeGroup(String groupId) throws Exception {
        UIBreadcumbs uiBreadcumb = getChild(UIBreadcumbs.class);
        uiBreadcumb.setPath(getPath(null, groupId));

        UITree tree = getChild(UITree.class);

        if (groupId == null) {
            tree.setSibbling(getGroups(null));
            tree.setChildren(null);
            tree.setSelected(null);
            selectGroup_ = null;
            return;
        }

        selectGroup_ = organizationService.getGroupHandler().findGroupById(groupId);
        String parentGroupId = null;
        if (selectGroup_ != null) {
            parentGroupId = selectGroup_.getParentId();
        }
        Group parentGroup = null;
        if (parentGroupId != null) {
            parentGroup = organizationService.getGroupHandler().findGroupById(parentGroupId);
        }

        tree.setSibbling(getGroups(selectGroup_));
        if(parentGroup != null) {
            tree.setChildren(getGroups(parentGroup));
        }
        tree.setSelected(selectGroup_);
        tree.setParentSelected(parentGroup);
    }

    private List<LocalPath> getPath(List<LocalPath> list, String id) throws Exception {
        if (list == null)
            list = new ArrayList<>(5);
        if (id == null)
            return list;
        Group group = organizationService.getGroupHandler().findGroupById(id);
        if (group == null)
            return list;
        list.add(0, new LocalPath(group.getId(), group.getGroupName()));
        getPath(list, group.getParentId());
        return list;
    }

    @SuppressWarnings("unchecked")
    public List<String> getListGroup() throws Exception {
        if (getCurrentGroup() == null) {
            return null;
        }

        return getGroups(getCurrentGroup()).stream().map(Group::getId).collect(Collectors.toList());
    }

    protected List<Group> getGroups(Group parentGroup) throws Exception {
        ConversationState conversationState = ConversationState.getCurrent();
        if(conversationState != null && conversationState.getIdentity() != null) {
            return organizationService.getGroupHandler().findGroups(parentGroup)
                    .stream()
                    .filter(group -> userACL.hasPermission(conversationState.getIdentity(), group, "default"))
                    .collect(Collectors.toList());
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public String event(String name, String beanId) throws Exception {
        UIForm uiForm = getAncestorOfType(UIForm.class);
        if (uiForm != null)
            return uiForm.event(name, getId(), beanId);
        return super.event(name, beanId);
    }

    public static class ChangeNodeActionListener extends EventListener<UITree> {
        public void execute(Event<UITree> event) throws Exception {
            UIGroupSelector uiGroupSelector = event.getSource().getAncestorOfType(UIGroupSelector.class);
            String groupId = event.getRequestContext().getRequestParameter(OBJECTID);
            uiGroupSelector.changeGroup(groupId);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiGroupSelector);
        }
    }

    public static class SelectGroupActionListener extends EventListener<UIGroupSelector> {
        public void execute(Event<UIGroupSelector> event) throws Exception {
            UIGroupSelector uiSelector = event.getSource();
            UIComponent uiPermission = uiSelector.<UIComponent> getParent().getParent();
            WebuiRequestContext pcontext = event.getRequestContext();

            UIPopupWindow uiPopup = uiSelector.getParent();
            UIForm uiForm = event.getSource().getAncestorOfType(UIForm.class);
            if (uiForm != null) {
                event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
            } else {
                event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
            }
            if (uiSelector.getCurrentGroup() == null) {
                UIApplication uiApp = pcontext.getUIApplication();
                uiApp.addMessage(new ApplicationMessage("UIGroupSelector.msg.selectGroup", null));
                uiPopup.setShow(true);
                return;
            }

            uiPermission.broadcast(event, event.getExecutionPhase());
            uiPopup.setShow(false);

        }
    }

    public static class SelectPathActionListener extends EventListener<UIBreadcumbs> {
        public void execute(Event<UIBreadcumbs> event) throws Exception {
            UIBreadcumbs uiBreadcumbs = event.getSource();
            UIGroupSelector uiSelector = uiBreadcumbs.getParent();
            String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
            uiBreadcumbs.setSelectPath(objectId);
            String selectGroupId = uiBreadcumbs.getSelectLocalPath().getId();
            uiSelector.changeGroup(selectGroupId);

            UIPopupWindow uiPopup = uiSelector.getParent();
            uiPopup.setShow(true);

            UIForm uiForm = event.getSource().getAncestorOfType(UIForm.class);
            if (uiForm != null) {
                event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
            } else {
                event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
            }
        }
    }

}
