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

package org.exoplatform.portal.webui.portal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.Properties;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UIPageBody;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.login.LoginUtils;
import org.exoplatform.web.login.LogoutControl;
import org.exoplatform.web.security.GateInToken;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;

@ComponentConfig(
                 lifecycle = UIPortalLifecycle.class,
                 template = "system:/groovy/portal/webui/page/UIPortal.gtmpl",
                 events = {
                            @EventConfig(listeners = UIPortal.LogoutActionListener.class,
                                         csrfCheck = false),
                 })
public class UIPortal extends UIContainer {

  @Getter
  @Setter
  private SiteKey               siteKey;

  @Getter
  @Setter
  private String                locale;

  @Getter
  @Setter
  private String                label;

  @Getter
  @Setter
  private String                editPermission;

  @Getter
  @Setter
  private String                skin;

  @Getter
  @Setter
  private Properties            properties;

  @Getter
  private Map<String, UIPage>   allUiPages       = new HashMap<>();

  @Getter
  @Setter
  private UIComponent           maximizedUIComponent;

  @Getter
  @Setter
  private Map<String, String[]> publicParameters = new HashMap<>();

  @Getter
  @Setter
  private boolean               useDynamicLayout;

  public UIPortal() {
    // Listen to storage to update cached pages when updated
    ListenerService listenerService = ExoContainerContext.getService(ListenerService.class);
    listenerService.addListener(LayoutService.PAGE_UPDATED, new RefreshUIPageListener());
  }

  public SiteType getSiteType() {
    return siteKey.getType();
  }

  public UserNode getNavPath() {
    PortalRequestContext prc = PortalRequestContext.getCurrentInstance();
    if (prc.getNavigationNode() == null) {
      UserPortal userPortal = prc.getUserPortalConfig().getUserPortal();
      UserNavigation navigation = userPortal.getNavigation(prc.getSiteKey());
      if (navigation == null) {
        setNavPath(userPortal.getDefaultPath(null));
      } else {
        setNavPath(userPortal.getDefaultPath(navigation, null));
      }
    }
    return prc.getNavigationNode();
  }

  public void setNavPath(UserNode nav) {
    PortalRequestContext.getCurrentInstance().setUserNode(nav);
  }

  /**
   * Return cached UIPage associated to the specified pageReference
   *
   * @param pageReference key whose associated UIPage is to be returned
   * @return the UIPage associated to the specified pageReference or null if not
   *         any
   */
  public UIPage getUIPage(String pageReference) {
    if (isDraftPage() || isNoCache()) {
      return null;
    } else {
      return this.allUiPages.get(pageReference);
    }
  }

  public void setUIPage(String pageReference, UIPage uiPage) {
    if (!isDraftPage()) {
      this.allUiPages.put(pageReference, uiPage);
    }
  }

  public boolean isDraftPage() {
    return ((PortalRequestContext) RequestContext.getCurrentInstance()).isDraftPage();
  }

  public boolean isNoCache() {
    return ((PortalRequestContext) RequestContext.getCurrentInstance()).isNoCache();
  }

  public void clearUIPage(String pageReference) {
    if (this.allUiPages != null)
      this.allUiPages.remove(pageReference);
  }

  public UserNavigation getUserNavigation() {
    PortalRequestContext prc = Util.getPortalRequestContext();
    return prc.getUserPortalConfig().getUserPortal().getNavigation(siteKey);
  }

  /**
   * Refresh the UIPage under UIPortal
   *
   * @throws Exception
   */
  public void refreshUIPage() throws Exception {
    UIPageBody uiPageBody = findFirstComponentOfType(UIPageBody.class);
    if (uiPageBody == null) {
      return;
    }

    uiPageBody.setPageBody(getSelectedUserNode(), this);
  }

  public UserNode getSelectedUserNode() {
    return getNavPath();
  }

  public String getProperty(String name, String defaultValue) {
    return ObjectUtils.firstNonNull(getProperty(name), defaultValue);
  }

  public String getProperty(String name) {
    if (properties == null) {
      return null;
    }
    return properties.get(name);
  }

  public static class LogoutActionListener extends EventListener<UIComponent> {
    public void execute(Event<UIComponent> event) throws Exception {
      PortalRequestContext prContext = Util.getPortalRequestContext();
      HttpServletRequest req = prContext.getRequest();

      // Delete the token from store
      String token = getTokenCookie(req);
      if (token != null) {
        AbstractTokenService<GateInToken, String> tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
        tokenService.deleteToken(token);
      }

      LogoutControl.wantLogout();
      Cookie cookie = new Cookie(LoginUtils.COOKIE_NAME, "");
      cookie.setPath(req.getContextPath());
      cookie.setMaxAge(0);
      prContext.getResponse().addCookie(cookie);

      prContext.sendRedirect("/");
    }

    private String getTokenCookie(HttpServletRequest req) {
      Cookie[] cookies = req.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (LoginUtils.COOKIE_NAME.equals(cookie.getName())) {
            return cookie.getValue();
          }
        }
      }
      return null;
    }

  }

  public void setHeaderAndFooterRendered(boolean headerAndFooterRendered) {
    List<UIComponent> list = getChildren();
    for (UIComponent child : list) {
      if (child instanceof UIPageBody) {
        /* do not touch the page body */
      } else if (child.isRendered() != headerAndFooterRendered) {
        child.setRendered(headerAndFooterRendered);
      }
    }
  }

  @Asynchronous
  public class RefreshUIPageListener extends Listener<LayoutService, Page> {
    @Override
    public void onEvent(org.exoplatform.services.listener.Event<LayoutService, Page> event) throws Exception {
      Page page = event.getData();
      if (page == null) {
        return;
      }
      PageKey pageKey = page.getPageKey();
      if (pageKey == null) {
        return;
      }
      String pageReference = pageKey.format();
      if (allUiPages != null && allUiPages.containsKey(pageReference)) {
        allUiPages.remove(pageReference);
      }
    }
  }
}
