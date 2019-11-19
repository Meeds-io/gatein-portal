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

package org.exoplatform.organization.webui.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIBreadcumbs.LocalPath;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.UIGroup;

/**
 * Created by The eXo Platform SARL Author : chungnv nguyenchung136@yahoo.com Jun 23, 2006 10:07:15 AM
 */
@ComponentConfig(events = @EventConfig(listeners = UIGroupExplorer.ChangeNodeActionListener.class, csrfCheck = false))
@Serialized
public class UIGroupExplorer extends UIContainer {

    private Group selectedGroup_;

    @SuppressWarnings("unchecked")
    private Collection sibblingsGroup_;

    @SuppressWarnings("unchecked")
    private Collection childrenGroup_;

    @SuppressWarnings("unchecked")
    public UIGroupExplorer() throws Exception {
        UITree tree = addChild(UITree.class, null, "TreeGroupExplorer");
        OrganizationService service = getApplicationComponent(OrganizationService.class);
        sibblingsGroup_ = service.getGroupHandler().findGroups(null);

        // if not administrator
        if (!GroupManagement.isAdministrator(null))
            sibblingsGroup_ = GroupManagement.getRelatedGroups(null, sibblingsGroup_);

        tree.setSibbling((List) convertGroups(sibblingsGroup_));
        tree.setIcon("GroupAdminIcon");
        tree.setSelectedIcon("PortalIcon");
        tree.setBeanIdField("id");
        // tree.setBeanLabelField("groupName");
        tree.setBeanLabelField("label");
        tree.setEscapeHTML(true);
        tree.setMaxTitleCharacter(25);
    }

    @SuppressWarnings("unchecked")
    public void changeGroup(String groupId) throws Exception {
        OrganizationService service = getApplicationComponent(OrganizationService.class);

        UIGroupManagement uiGroupManagement = this.getParent();
        UIBreadcumbs uiBreadcumb = uiGroupManagement.getChild(UIBreadcumbs.class);
        uiBreadcumb.setPath(getPath(null, groupId));

        UITree uiTree = getChild(UITree.class);
        UIGroupDetail uiGroupDetail = uiGroupManagement.getChild(UIGroupDetail.class);
        UIGroupInfo uiGroupInfo = uiGroupDetail.getChild(UIGroupInfo.class);

        if (groupId == null) {
            sibblingsGroup_ = service.getGroupHandler().findGroups(null);
            // if not administrator
            if (!GroupManagement.isAdministrator(null))
                sibblingsGroup_ = GroupManagement.getRelatedGroups(null, sibblingsGroup_);
            uiTree.setSibbling((List) convertGroups(sibblingsGroup_));
            uiTree.setSelected(null);
            uiTree.setChildren(null);
            uiTree.setParentSelected(null);
            selectedGroup_ = null;
            uiGroupInfo.setGroup(null);
            return;
        }

        if (groupId != null) {
            selectedGroup_ = service.getGroupHandler().findGroupById(groupId);
        } else {
            selectedGroup_ = null;
        }

        String parentGroupId = null;
        if (selectedGroup_ != null)
            parentGroupId = selectedGroup_.getParentId();
        Group parentGroup = null;
        if (parentGroupId != null)
            parentGroup = service.getGroupHandler().findGroupById(parentGroupId);
        childrenGroup_ = service.getGroupHandler().findGroups(selectedGroup_);
        sibblingsGroup_ = service.getGroupHandler().findGroups(parentGroup);

        // if not administrator
        if (!GroupManagement.isAdministrator(null)) {
            childrenGroup_ = GroupManagement.getRelatedGroups(null, childrenGroup_);
            sibblingsGroup_ = GroupManagement.getRelatedGroups(null, sibblingsGroup_);
        }

        for (Object group : sibblingsGroup_) {
            if (selectedGroup_ != null && ((Group) group).getId().equals(selectedGroup_.getId())) {
                selectedGroup_ = (Group) group;
                break;
            }
        }
        uiGroupInfo.setGroup(selectedGroup_);

        uiTree.setSibbling((List) convertGroups(sibblingsGroup_));
        uiTree.setChildren((List) convertGroups(childrenGroup_));
        uiTree.setSelected(new UIGroup(selectedGroup_));
        uiTree.setParentSelected(parentGroup);
    }

    public List<LocalPath> getPath(List<LocalPath> list, String id) throws Exception {
        if (list == null)
            list = new ArrayList<LocalPath>(5);
        if (id == null)
            return list;
        OrganizationService service = getApplicationComponent(OrganizationService.class);
        Group group = service.getGroupHandler().findGroupById(id);
        if (group == null)
            return list;
        // list.add(0, new LocalPath(group.getId(), group.getGroupName()));
        list.add(0, new LocalPath(group.getId(), group.getLabel()));
        getPath(list, group.getParentId());
        return list;
    }

    public Group getCurrentGroup() {
        if (selectedGroup_ != null) {
            OrganizationService service = getApplicationComponent(OrganizationService.class);
            try {
                selectedGroup_ = service.getGroupHandler().findGroupById(selectedGroup_.getId());
            } catch (Exception e) {
                selectedGroup_ = null;
            }
        }
        return selectedGroup_;
    }

    public void setCurrentGroup(Group g) {
        selectedGroup_ = g;
    }

    @SuppressWarnings("unchecked")
    public Collection getChildrenGroup() {
        return childrenGroup_;
    }

    @SuppressWarnings("unchecked")
    public Collection getSibblingGroups() {
        return sibblingsGroup_;
    }

    @SuppressWarnings("unchecked")
    public void setChildGroup(Collection childrendGroup) {
        childrenGroup_ = childrendGroup;
    }

    @SuppressWarnings("unused")
    public void processRender(WebuiRequestContext context) throws Exception {
        if (selectedGroup_ != null) {
            changeGroup(selectedGroup_.getId());            
        } else {
            changeGroup(null);
        }
        renderChildren();
    }

    public static class ChangeNodeActionListener extends EventListener<UITree> {
        public void execute(Event<UITree> event) throws Exception {
            UIGroupExplorer uiGroupExplorer = event.getSource().getParent();
            String groupId = event.getRequestContext().getRequestParameter(OBJECTID);
            uiGroupExplorer.changeGroup(groupId);
            UIGroupManagement uiGroupManagement = uiGroupExplorer.getParent();
            UIGroupDetail uiGroupDetail = uiGroupManagement.getChild(UIGroupDetail.class);
            uiGroupDetail.getChild(UIGroupForm.class).setGroup(null);
            uiGroupDetail.setRenderedChild(UIGroupInfo.class);
        }
    }

    private Collection<UIGroup> convertGroups(Collection<Group> groups) {
        Collection<UIGroup> result = new ArrayList();
        for (Group group : groups) {
            result.add(new UIGroup(group));
        }
        return result;
    }

}
