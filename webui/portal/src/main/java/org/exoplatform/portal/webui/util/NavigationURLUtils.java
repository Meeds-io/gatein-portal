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
package org.exoplatform.portal.webui.util;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

/**
 * Contains some utility methods to compute URLs of pages
 */
public class NavigationURLUtils {

  /**
   * @param uri
   * @return URL of the page with selected URI in the current portal
   */
  public static String getURLInCurrentPortal(String uri) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL, Util.getPortalRequestContext().getPortalOwner(), uri);
    return nodeURL.setResource(resource).toString();
  }

  /**
   * @return URL of the current portal
   */
  public static String getCurrentPortalURL() {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteKey.portal(Util.getPortalRequestContext().getPortalOwner()), null);
    return nodeURL.setResource(resource).toString();
  }

  /**
   * @param portalName
   * @param uri
   * @return URL of the page with selected URI in the selected portal
   */
  public static String getPortalURL(String portalName, String uri) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteKey.portal(portalName), uri);
    return nodeURL.setResource(resource).toString();
  }

  /**
   * @param siteKey
   * @param uri
   * @return URL of the page with selected URI in the selected navigation
   */
  public static String getURL(SiteKey siteKey, String uri) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(siteKey, uri);
    return nodeURL.setResource(resource).toString();
  }

  /**
   * @param node
   * @return URL of the selected UserNode
   */
  public static String getURL(UserNode node) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    return nodeURL.setNode(node).toString();
  }
}