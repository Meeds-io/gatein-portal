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

package org.gatein.security.oauth.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.security.oauth.spi.AccessTokenContext;
import org.gatein.security.oauth.spi.OAuthProviderType;
import org.gatein.security.oauth.spi.OAuthProviderTypeRegistry;

/**
 * {@inheritDoc}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthProviderTypeRegistryImpl implements OAuthProviderTypeRegistry {

    // Key is String identifier of OauthProviderType (Key of this OAuthProviderType). Value is OAuthProviderType
    private final Map<String, OAuthProviderType> oauthProviderTypes = new LinkedHashMap<String, OAuthProviderType>();

    private static final Log                   log                = ExoLogger.getLogger(OAuthProviderTypeRegistryImpl.class);

    // Register OAuthProviderType into our list. It's called by kernel
    public void addPlugin(ComponentPlugin plugin) {
        if (plugin instanceof OauthProviderTypeRegistryPlugin) {
            OauthProviderTypeRegistryPlugin oauthPlugin = (OauthProviderTypeRegistryPlugin)plugin;

            OAuthProviderType oauthPrType = oauthPlugin.getOAuthProviderType();

            if (oauthPrType != null && oauthPrType.isEnabled()) {
                this.oauthProviderTypes.put(oauthPrType.getKey(), oauthPrType);
                log.debug("Added new OAuthProviderType " + oauthPrType);
            } else {
                log.debug("Skip OAuthProviderType " + oauthPrType + " because it's disabled");
            }
        } else {
            throw new RuntimeException("Invalid plugin type: " + plugin.getClass() + ", plugin: " + plugin);
        }
    }

    @Override
    public <T extends AccessTokenContext> OAuthProviderType<T> getOAuthProvider(String key, Class<T> accessTokenContextClass) {
        return (OAuthProviderType<T>)oauthProviderTypes.get(key);
    }

    @Override
    public Collection<OAuthProviderType> getEnabledOAuthProviders() {
        return Collections.unmodifiableCollection(oauthProviderTypes.values());
    }

    /**
     * @return true if at least one OAuth provider is enabled
     */
    @Override
    public boolean isOAuthEnabled() {
        return !oauthProviderTypes.isEmpty();
    }
}
