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

package org.exoplatform.portal.webui.component;

import java.util.List;

import javax.portlet.MimeResponse;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceURL;

import org.exoplatform.portal.mop.navigation.GenericScope;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.navigation.TreeNode;
import org.exoplatform.portal.webui.navigation.UIPortalNavigation;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minhdv81@yahoo.com Jul 3, 2006
 */
@ComponentConfigs({
        @ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "system:/groovy/webui/core/UISitemap.gtmpl"),
        @ComponentConfig(type = UIPortalNavigation.class, id = "UISiteMap", events = {
                @EventConfig(listeners = UIPortalNavigation.CollapseAllNodeActionListener.class),
                @EventConfig(listeners = UIPortalNavigation.CollapseNodeActionListener.class) }) })
public class UISitemapPortlet extends UIPortletApplication {

    public static final int DEFAULT_LEVEL = 2;

    public UISitemapPortlet() throws Exception {

        PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
        PortletRequest prequest = context.getRequest();
        PortletPreferences prefers = prequest.getPreferences();
        String template = prefers.getValue("template", "system:/groovy/webui/core/UISitemapTree.gtmpl");

        UIPortalNavigation uiPortalNavigation = addChild(UIPortalNavigation.class, "UISiteMap", null);
        uiPortalNavigation.setTemplate(template);
        uiPortalNavigation.setUseAjax(isUseAjax());

        int level = DEFAULT_LEVEL;
        try {
            level = Integer.valueOf(prefers.getValue("level", String.valueOf(DEFAULT_LEVEL)));
        } catch (Exception ex) {
            log.warn("Preference for navigation level can only be integer");
        }

        if (level <= 0) {
            uiPortalNavigation.setScope(Scope.ALL);
        } else {
            uiPortalNavigation.setScope(GenericScope.treeShape(level));
        }
    }

    @Override
    public void serveResource(WebuiRequestContext context) throws Exception {
        super.serveResource(context);

        ResourceRequest req = context.getRequest();
        String nodeID = req.getResourceID();

        JSONArray jsChilds = getChildrenAsJSON(nodeID);
        if (jsChilds == null) {
            return;
        }

        MimeResponse res = context.getResponse();
        res.setContentType("text/json");
        res.getWriter().write(jsChilds.toString());
    }

    private JSONArray getChildrenAsJSON(String nodeID) throws Exception {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        List<TreeNode> childs = null;

        UIPortalNavigation uiPortalNavigation = getChild(UIPortalNavigation.class);
        TreeNode tnode = uiPortalNavigation.getTreeNodes().findNodes(nodeID);
        if (tnode != null) {
            UserNode userNode = uiPortalNavigation.updateNode(tnode.getNode());
            if (userNode != null) {
                tnode.setExpanded(true);
                tnode.setChildren(userNode.getChildren());
                childs = tnode.getChildren();
            }
        }

        JSONArray jsChilds = new JSONArray();
        if (childs == null) {
            return null;
        }
        MimeResponse res = context.getResponse();
        for (TreeNode child : childs) {
            jsChilds.put(toJSON(child, res));
        }
        return jsChilds;
    }

    private JSONObject toJSON(TreeNode tnode, MimeResponse res) throws Exception {
        UIPortalNavigation uiPortalNavigation = getChild(UIPortalNavigation.class);
        JSONObject json = new JSONObject();
        UserNode node = tnode.getNode();
        String nodeId = node.getId();

        json.put("label", node.getEncodedResolvedLabel());
        json.put("hasChild", tnode.hasChild());
        json.put("isExpanded", tnode.isExpanded());
        json.put("collapseURL", uiPortalNavigation.url("CollapseNode", nodeId));

        ResourceURL rsURL = res.createResourceURL();
        rsURL.setResourceID(nodeId);
        json.put("getNodeURL", rsURL.toString());

        if (node.getPageRef() != null) {
            NavigationResource resource = new NavigationResource(node);
            NodeURL url = Util.getPortalRequestContext().createURL(NodeURL.TYPE, resource);
            url.setAjax(isUseAjax());
            json.put("actionLink", url.toString());
        }

        JSONArray childs = new JSONArray();
        for (TreeNode child : tnode.getChildren()) {
            childs.put(toJSON(child, res));
        }
        json.put("childs", childs);
        return json;
    }

    public boolean isUseAjax() {
        PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
        PortletRequest prequest = context.getRequest();
        PortletPreferences prefers = prequest.getPreferences();
        return Boolean.valueOf(prefers.getValue("useAJAX", "true"));
    }
}
