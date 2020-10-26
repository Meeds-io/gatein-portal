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

package org.exoplatform.organization.webui.component;

import java.io.Writer;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL Author : chungnv nguyenchung136@yahoo.com Jun 23, 2006 10:07:15 AM
 */
@ComponentConfig()
@Serialized
public class UIMembershipManagement extends UIContainer {

    public UIMembershipManagement() throws Exception {
        addChild(UIListMembershipType.class, null, null);
        addChild(UIMembershipTypeForm.class, null, null);
    }

    /** Returns currently selected GroupMembershipForm under the Group tab * */
    private UIGroupMembershipForm getGroupMembershipForm() {
        UIOrganizationPortlet uiParent = getParent();
        UIGroupManagement groupManagement = uiParent.getChild(UIGroupManagement.class);
        UIGroupDetail groupDetail = groupManagement.getChild(UIGroupDetail.class);
        UIGroupInfo groupInfo = groupDetail.getChild(UIGroupInfo.class);
        UIUserInGroup userIngroup = groupInfo.getChild(UIUserInGroup.class);
        return userIngroup.getChild(UIGroupMembershipForm.class);
    }

    public void addOptions(MembershipType option) {
        UIGroupMembershipForm membershipFormUnderGroupTab = getGroupMembershipForm();
        if (membershipFormUnderGroupTab != null) {
            membershipFormUnderGroupTab.addOptionMembershipType(option);
        }
    }

    public void deleteOptions(MembershipType option) {
        UIGroupMembershipForm membershipFormUnderGroupTab = getGroupMembershipForm();
        if (membershipFormUnderGroupTab != null) {
            membershipFormUnderGroupTab.removeOptionMembershipType(option);
        }
    }

    @SuppressWarnings("unused")
    public void processRender(WebuiRequestContext context) throws Exception {
        Writer w = context.getWriter();
        w.write("<div id=\"UIMembershipManagement\" class=\"UIMembershipManagement\">");
        renderChildren();
        w.write("</div>");
    }

}
