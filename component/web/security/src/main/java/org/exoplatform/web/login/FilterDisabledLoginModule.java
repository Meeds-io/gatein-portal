/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.web.login;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.gatein.sso.agent.tomcat.ServletAccess;

import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.jaas.AbstractLoginModule;

public class FilterDisabledLoginModule extends AbstractLoginModule {

  private static final Log   log                = ExoLogger.getLogger(FilterDisabledLoginModule.class);

  public static final String DISABLED_USER_NAME = "_disabledUserName";

  @Override
  public boolean login() throws LoginException {
    log.debug("In login of FilterDisabledLoginModule.");

    try {
      Callback[] callbacks = new Callback[] {
          new NameCallback("Username")
      };
      callbackHandler.handle(callbacks);

      String username = ((NameCallback) callbacks[0]).getName();
      if (username != null) {
        OrganizationService organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
        begin(organizationService);
        try {
          UserHandler uHandler = organizationService.getUserHandler();
          User user = uHandler.findUserByName(username, UserStatus.ANY);

          if (user == null) {
            log.debug("user {0} doesn't exists. FilterDisabledLoginModule will be ignored.", username);
          } else if (!user.isEnabled()) {
            HttpServletRequest request = getCurrentHttpServletRequest();
            if (request != null) {
              request.setAttribute(DISABLED_USER_NAME, username);
            }

            throw new LoginException("Can't authenticate. user " + username + " is disabled");
          }
        } finally {
          end(organizationService);
        }
      } else {
        log.debug("No username has been committed. FilterDisabledLoginModule will be ignored.");
      }

      return true;
    } catch (final Exception e) {
      log.warn(e.getMessage());
      throw new LoginException(e.getMessage());
    }
  }

  protected HttpServletRequest getCurrentHttpServletRequest() {
    return ServletAccess.getRequest();
  }

  @Override
  public boolean commit() throws LoginException {
    return true;
  }

  @Override
  public boolean abort() throws LoginException {
    return true;
  }

  @Override
  public boolean logout() throws LoginException {
    return true;
  }

  @Override
  protected Log getLogger() {
    return log;
  }

  private void begin(OrganizationService orgService) {
    if (orgService instanceof ComponentRequestLifecycle componentRequestLifecycle) {
      RequestLifeCycle.begin(componentRequestLifecycle);
    }
  }

  private void end(OrganizationService orgService) {
    if (orgService instanceof ComponentRequestLifecycle) {
      RequestLifeCycle.end();
    }
  }
}
