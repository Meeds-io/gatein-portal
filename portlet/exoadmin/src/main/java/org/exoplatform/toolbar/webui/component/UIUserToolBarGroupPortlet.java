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

package org.exoplatform.toolbar.webui.component;

import java.util.LinkedList;
import java.util.List;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.gatein.common.util.ParameterValidation;

/**
 * Created by The eXo Platform SAS Author : Pham Thanh Tung thanhtungty@gmail.com May 26, 2009
 */
@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/groovy/admintoolbar/webui/component/UIUserToolBarGroupPortlet.gtmpl", events = { @EventConfig(listeners = UIUserToolBarGroupPortlet.NavigationChangeActionListener.class) })
public class UIUserToolBarGroupPortlet extends BasePartialUpdateToolbar {

    private static final String SPLITTER_STRING = "::";

    public UIUserToolBarGroupPortlet() throws Exception {
        UserNodeFilterConfig.Builder builder = UserNodeFilterConfig.builder();
        builder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
        builder.withTemporalCheck();
        toolbarFilterConfig = builder.build();
    }

    public List<UserNavigation> getGroupNavigations() {
        UserPortal userPortal = getUserPortal();
        List<UserNavigation> allNavs = userPortal.getNavigations();

        List<UserNavigation> groupNav = new LinkedList<UserNavigation>();
        for (UserNavigation nav : allNavs) {
            if (nav.getKey().getType().equals(SiteType.GROUP)) {
                groupNav.add(nav);
            }
        }
        return groupNav;
    }

    @Override
    protected String getResourceIdFromNode(UserNode node, String navId) {
        return navId + SPLITTER_STRING + node.getURI();
    }

    @Override
    protected UserNode getNodeFromResourceID(String resourceId) throws Exception {
        String[] parsedId = parseResourceId(resourceId);
        if (parsedId == null) {
            throw new IllegalArgumentException("resourceId " + resourceId + " is invalid");
        }
        String groupId = parsedId[0];
        String nodeURI = parsedId[1];

        UserNavigation grpNav = getNavigation(SiteKey.group(groupId));
        if (grpNav == null)
            return null;

        UserNode node = getUserPortal().resolvePath(grpNav, toolbarFilterConfig, nodeURI);
        if (node != null && node.getURI().equals(nodeURI)) {
            return node;
        }
        return null;
    }

    private String[] parseResourceId(String resourceId) {
        if (!ParameterValidation.isNullOrEmpty(resourceId)) {
            String[] parsedId = resourceId.split(SPLITTER_STRING);
            if (parsedId.length == 2) {
                return parsedId;
            }
        }
        return null;
    }

    /**
     * An empty action listener for trigger update portlet by ajax via Portlet Event
     *
     * @author <a href="trongtt@gmail.com">Trong Tran</a>
     * @version $Revision$
     */
    public static class NavigationChangeActionListener extends EventListener<UIUserToolBarGroupPortlet> {
        @Override
        public void execute(Event<UIUserToolBarGroupPortlet> event) throws Exception {
            // This event is only a trick for updating the Toolbar group portlet
        }
    }
}
