/*
 * JBoss, a division of Red Hat
 * Copyright 2013, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
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

package org.gatein.security.oauth.webapi;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.security.oauth.spi.OAuthProviderTypeRegistry;
import org.gatein.sso.agent.filter.api.SSOInterceptor;
import org.gatein.sso.integration.SSOFilterIntegratorImpl;

/**
 * Kernel component, which holds references to configured {@link org.gatein.sso.agent.filter.api.SSOInterceptor}
 * instances for OAuth integration
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthFilterIntegratorImpl extends SSOFilterIntegratorImpl implements OAuthFilterIntegrator {

  private static final Log                log = ExoLogger.getLogger(OAuthFilterIntegratorImpl.class);

    private final OAuthProviderTypeRegistry oAuthProviderTypeRegistry;

    public OAuthFilterIntegratorImpl(OAuthProviderTypeRegistry oAuthProviderTypeRegistry) {
        this.oAuthProviderTypeRegistry = oAuthProviderTypeRegistry;
    }

    @Override
    public Map<SSOInterceptor, String> getOAuthInterceptors() {
        if (oAuthProviderTypeRegistry.isOAuthEnabled()) {
            return getSSOInterceptors();
        } else {
            // return empty map if oauth is disabled (we don't have any OAuthProviders configured)
            log.debug("OAuth2 is disabled as there are not any OAuthProviderTypes configured. OAuth interceptors will be skipped");
            return new HashMap<SSOInterceptor, String>();
        }
    }
}
