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

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.UsernameCredential;
import org.exoplatform.services.security.j2ee.TomcatLoginModule;
import org.exoplatform.web.security.security.CookieTokenService;
import org.gatein.sso.agent.tomcat.ServletAccess;

public class RememberMeLoginModule extends TomcatLoginModule {

  private static final Log   log                = ExoLogger.getLogger(RememberMeLoginModule.class);

  @Override
  public boolean login() throws LoginException {
    log.debug("In login of RemembermeLoginModule.");

    try {

      HttpServletRequest servletRequest = ServletAccess.getRequest();
      if (servletRequest == null) {
        LOG.warn("HttpServletRequest is null. RemembermeLoginModule will be ignored.");
        return false;
      }
      String token = LoginUtils.getRememberMeTokenCookie(servletRequest);

      if (token!=null) {
        CookieTokenService tokenservice = getContainer().getComponentInstanceOfType(CookieTokenService.class);
        String username = tokenservice.validateToken(token, false);
        if (username!=null) {
          Authenticator authenticator = getContainer().getComponentInstanceOfType(Authenticator.class);
          if (authenticator == null)
          {
            throw new LoginException("No Authenticator component found, check your configuration");
          }
          identity = authenticator.createIdentity(username);
          sharedState.put("javax.security.auth.login.name", username);
          sharedState.put("exo.security.identity", identity);
          subject.getPublicCredentials().add(new UsernameCredential(username));

        } else {
          log.debug("Rememberme cookie is not valid. RemembermeLoginModule will be ignored.");

        }

      } else {
        log.debug("No username has been committed. RemembermeLoginModule will be ignored.");
      }

      return true;
    } catch (final Exception e) {
      log.warn(e.getMessage());
      throw new LoginException(e.getMessage());
    }
  }


  @Override
  public boolean commit() throws LoginException
  {
    if (identity!=null) {
      super.commit();
    }
    return true;
  }
}
