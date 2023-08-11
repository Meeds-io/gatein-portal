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

package org.exoplatform.services.organization.idm;

import java.io.InputStream;
import java.net.URL;

import javax.naming.InitialContext;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.database.HibernateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.gatein.portal.idm.impl.store.attribute.ExtendedAttributeManager;
import org.infinispan.Cache;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.IdentitySessionFactory;
import org.picketlink.idm.api.SecureRandomProvider;
import org.picketlink.idm.api.cfg.IdentityConfiguration;
import org.picketlink.idm.cache.APICacheProvider;
import org.picketlink.idm.common.exception.IdentityConfigurationException;
import org.picketlink.idm.impl.api.session.IdentitySessionImpl;
import org.picketlink.idm.impl.configuration.IdentityConfigurationImpl;
import org.picketlink.idm.impl.configuration.jaxb2.JAXB2IdentityConfiguration;
import org.picketlink.idm.impl.credential.DatabaseReadingSaltEncoder;
import org.picketlink.idm.spi.cache.IdentityStoreCacheProvider;
import org.picketlink.idm.spi.configuration.metadata.IdentityConfigurationMetaData;
import org.picocontainer.Startable;

/**
 * @author <a href="mailto:boleslaw.dawidowicz@redhat.com">Boleslaw Dawidowicz</a>
 */
public class PicketLinkIDMServiceImpl implements PicketLinkIDMService, Startable {

    private static Log log = ExoLogger.getLogger(PicketLinkIDMServiceImpl.class);

    public static final String PARAM_CONFIG_OPTION = "config";

    public static final String PARAM_JNDI_NAME_OPTION = "jndiName";

    public static final String PARAM_SKIP_EXPIRATION_STRUCTURE_CACHE_ENTRIES = "skipExpirationOfStructureCacheEntries";

    public static final String PARAM_USE_SECURE_RANDOM_SERVICE = "useSecureRandomService";

    public static final String PARAM_STALE_CACHE_NODES_LINKS_CLEANER_DELAY = "staleCacheNodesLinksCleanerDelay";

    public static final int DEFAULT_STALE_CACHE_NODES_LINKS_CLEANER_DELAY = 120000;

    public static final String REALM_NAME_OPTION = "portalRealm";

    public static final String CACHE_CONFIG_API_OPTION = "apiCacheConfig";

    public static final String CACHE_CONFIG_STORE_OPTION = "storeCacheConfig";

    private IdentitySessionFactory identitySessionFactory;

    private String config;

    private String realmName = "idm_realm";

    private IdentityConfiguration identityConfiguration;

    private IdentityConfigurationMetaData configMD;

    private HibernateService hibernateService;
    private ExtendedAttributeManager extendedAttributeManager;

    public PicketLinkIDMServiceImpl(ExoContainerContext exoContainerContext, InitParams initParams,
            HibernateService hibernateService, ConfigurationManager confManager,
            InitialContextInitializer dependency) throws Exception {

        ValueParam config = null;

        ValueParam directoryTypeValueParam = initParams.getValueParam("ldap.type");
        String directoryType = null;
        if(directoryTypeValueParam != null) {
            directoryType = directoryTypeValueParam.getValue();
        }
        if(StringUtils.isNotBlank(directoryType)) {
            config = initParams.getValueParam(PARAM_CONFIG_OPTION + "." + directoryType);
        }

        if(config == null) {
            config = initParams.getValueParam(PARAM_CONFIG_OPTION);
        }

        ValueParam jndiName = initParams.getValueParam(PARAM_JNDI_NAME_OPTION);
        ValueParam canExpireStructureCacheEntriesParam = initParams
                .getValueParam(PARAM_SKIP_EXPIRATION_STRUCTURE_CACHE_ENTRIES);
        ValueParam staleCacheNodesLinksCleanerDelayParam = initParams
                .getValueParam(PARAM_STALE_CACHE_NODES_LINKS_CLEANER_DELAY);
        ValueParam realmName = initParams.getValueParam(REALM_NAME_OPTION);
        ValueParam apiCacheConfig = initParams.getValueParam(CACHE_CONFIG_API_OPTION);
        ValueParam storeCacheConfig = initParams.getValueParam(CACHE_CONFIG_STORE_OPTION);
        ValueParam useSecureRandomService = initParams.getValueParam(PARAM_USE_SECURE_RANDOM_SERVICE);

        this.hibernateService = hibernateService;

        if (config == null && jndiName == null) {
            throw new IllegalStateException("Either '" + PARAM_CONFIG_OPTION + "' or '" + PARAM_JNDI_NAME_OPTION
                    + "' parameter must " + "be specified");
        }
        if (realmName != null) {
            this.realmName = realmName.getValue();
        }

        long staleCacheNodesLinksCleanerDelay = staleCacheNodesLinksCleanerDelayParam == null ? DEFAULT_STALE_CACHE_NODES_LINKS_CLEANER_DELAY
                : Long.parseLong(staleCacheNodesLinksCleanerDelayParam.getValue());

        boolean skipExpirationOfStructureCacheEntries = canExpireStructureCacheEntriesParam != null
                && "true".equals(canExpireStructureCacheEntriesParam.getValue());

        if (config != null) {
            this.config = config.getValue();
            URL configURL = confManager.getURL(this.config);

            if (configURL == null) {
                throw new IllegalStateException("Cannot fine resource: " + this.config);
            }

            this.configMD = JAXB2IdentityConfiguration.createConfigurationMetaData(confManager
                    .getInputStream(this.config));

            identityConfiguration = new IdentityConfigurationImpl().configure(this.configMD);

            identityConfiguration.getIdentityConfigurationRegistry().register(hibernateService.getSessionFactory(),
                    "hibernateSessionFactory");

            if (apiCacheConfig != null) {
                log.warn("The parameter 'apiCacheProvider' has been deprecated. It has been replaced by caches in Organization Service top layer. Thus, the parameter should be removed.");

                InputStream configStream = confManager.getInputStream(apiCacheConfig.getValue());

                if (configStream == null) {
                    throw new IllegalArgumentException("Infinispan configuration InputStream is null");
                }

                configStream.close();
            }

            if (storeCacheConfig != null) {
                log.warn("The parameter 'storeCacheProvider' has been deprecated. It has been replaced by caches in Organization Service top layer. Thus, the parameter should be removed.");

                InputStream configStream = confManager.getInputStream(storeCacheConfig.getValue());

                if (configStream == null) {
                    throw new IllegalArgumentException("Infinispan configuration InputStream is null");
                }
            }

            if (useSecureRandomService != null && "true".equals(useSecureRandomService.getValue())) {
                SecureRandomProvider secureRandomProvider = (SecureRandomProvider)exoContainerContext.getContainer().getComponentInstanceOfType(SecureRandomProvider.class);
                identityConfiguration.getIdentityConfigurationRegistry().register(secureRandomProvider, DatabaseReadingSaltEncoder.DEFAULT_SECURE_RANDOM_PROVIDER_REGISTRY_NAME);
            }
        } else {
            identitySessionFactory = (IdentitySessionFactory) new InitialContext().lookup(jndiName.getValue());
        }

    }

    public void start() {
        if (identitySessionFactory == null) {
            try {
                identitySessionFactory = identityConfiguration.buildIdentitySessionFactory();
            } catch (IdentityConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
    }

    public IdentitySessionFactory getIdentitySessionFactory() {
        return identitySessionFactory;
    }

    public IdentitySession getIdentitySession() throws Exception {
        if(getIdentitySessionFactory() != null) {
            return getIdentitySessionFactory().getCurrentIdentitySession(realmName);
        } else {
            return null;
        }
    }

    public IdentitySession getIdentitySession(String realm) throws Exception {
        if (realm == null) {
            throw new IllegalArgumentException("Realm name cannot be null");
        }
        return getIdentitySessionFactory().getCurrentIdentitySession(realm);
    }

    @Override
    public IdentityConfiguration getIdentityConfiguration() {
        return identityConfiguration;
    }

    @Override
    public ExtendedAttributeManager getExtendedAttributeManager() throws Exception {
      if (this.extendedAttributeManager == null) {
          this.extendedAttributeManager = new ExtendedAttributeManager((IdentitySessionImpl) getIdentitySession());
      }
      return this.extendedAttributeManager;
    }

    public String getRealmName() {
        return realmName;
    }

    public HibernateService getHibernateService() {
        return hibernateService;
    }

    public IdentityConfigurationMetaData getConfigMD() {
        return this.configMD;
    }

    public void setConfigMD(IdentityConfigurationMetaData configMD) {
        this.configMD = configMD;
    }
}
