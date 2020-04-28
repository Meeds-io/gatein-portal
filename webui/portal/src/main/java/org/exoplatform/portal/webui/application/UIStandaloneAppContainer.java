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

package org.exoplatform.portal.webui.application;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.exoplatform.portal.application.StandaloneAppRequestContext;
import org.exoplatform.web.login.LoginServlet;
import org.exoplatform.web.login.LogoutControl;
import org.exoplatform.web.security.security.AbstractTokenService;
import org.exoplatform.web.security.security.CookieTokenService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(template = "system:/groovy/portal/webui/application/UIStandaloneAppContainer.gtmpl", events = { @EventConfig(listeners = UIStandaloneAppContainer.LogoutActionListener.class, csrfCheck = false) })
public class UIStandaloneAppContainer extends UIContainer {
    @Override
    public void processRender(WebuiRequestContext context) throws Exception {
        super.processRender(context);
    }

    public static class LogoutActionListener extends EventListener<UIComponent> {
        public void execute(Event<UIComponent> event) throws Exception {
            StandaloneAppRequestContext context = (StandaloneAppRequestContext) event.getRequestContext();
            HttpServletRequest req = context.getRequest();

            // Delete the token from store
            String token = getTokenCookie(req);
            if (token != null) {
                AbstractTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
                tokenService.deleteToken(token);
            }
            token = LoginServlet.getOauthRememberMeTokenCookie(req);
            if (token != null) {
                AbstractTokenService tokenService = AbstractTokenService.getInstance(CookieTokenService.class);
                tokenService.deleteToken(token);
            }

            LogoutControl.wantLogout();
            Cookie cookie = new Cookie(LoginServlet.COOKIE_NAME, "");
            cookie.setPath(req.getContextPath());
            cookie.setMaxAge(0);
            context.getResponse().addCookie(cookie);

            Cookie oauthCookie = new Cookie(LoginServlet.OAUTH_COOKIE_NAME, "");
            oauthCookie.setPath(req.getContextPath());
            oauthCookie.setMaxAge(0);
            context.getResponse().addCookie(oauthCookie);

            context.sendRedirect(req.getRequestURI());
        }

        private String getTokenCookie(HttpServletRequest req) {
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (LoginServlet.COOKIE_NAME.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        }

    }
}
