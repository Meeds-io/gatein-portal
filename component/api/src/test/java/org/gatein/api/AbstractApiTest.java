/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.gatein.api;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.HashSet;

import junit.framework.AssertionFailedError;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.component.test.KernelLifeCycle;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.gatein.api.navigation.NodePath;
import org.gatein.api.oauth.OAuthProviderAccessor;
import org.gatein.api.page.PageId;
import org.gatein.api.security.Permission;
import org.gatein.api.security.User;
import org.gatein.api.site.SiteId;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/mop/navigation/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.resources-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.web.oauth-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.api-configuration.xml") })
public class AbstractApiTest {

    protected static final Log LOG = ExoLogger.getLogger(AbstractApiTest.class);

    @ClassRule
    public static KernelLifeCycle kernelLifeCycle = new KernelLifeCycle();

    protected PortalContainer container;

    protected Portal portal;

    protected SiteId defaultSiteId;
    
    protected IdentityRegistry identityRegistry;

    @After
    public void after() throws Exception {
        BasicPortalRequest.setInstance(null);
        try {
            cleanup();
        } finally {
            RequestLifeCycle.end();
        }
    }

    @Before
    public void before() throws Exception {
        try {
          defaultSiteId = new SiteId("classic2");
          container = kernelLifeCycle.getContainer();
          portal = (Portal) container.getComponentInstanceOfType(Portal.class);
          assertNotNull("Portal component not found in container", portal);
          identityRegistry = (IdentityRegistry) container.getComponentInstanceOfType(IdentityRegistry.class);

          ExoContainerContext.setCurrentContainer(container);
          RequestLifeCycle.begin(container);
          HashSet<MembershipEntry> memberships = new HashSet<MembershipEntry>();
          memberships.add(new MembershipEntry("/platform/users", "member"));
          Identity john = new Identity("john", memberships);
          identityRegistry.register(john);

          BasicPortalRequest.setInstance(new BasicPortalRequest(new User("john"), defaultSiteId, NodePath.root(), Locale.ENGLISH,
                  portal, new BasicPortalRequest.BasicURIResolver("/portal")));
        } catch (Exception e) {
          LOG.error("Error starting container", e);
        }
    }

    protected void createSite(SiteId siteId, String... pages) {
        createSite(siteId, true, pages);
    }

    protected void createSite(SiteId siteId, boolean createNav, String... pages) {
        try {
            DataStorage dataStorage = container.getComponentInstanceOfType(DataStorage.class);
            NavigationService navService = container.getComponentInstanceOfType(NavigationService.class);

            SiteKey siteKey = Util.from(siteId);

            PortalConfig config = new PortalConfig(siteKey.getTypeName(), siteKey.getName());
            config.setAccessPermissions(Util.from(Permission.everyone()));

            dataStorage.create(config);

            if (createNav) {
                NavigationContext nav = new NavigationContext(new SiteKey(siteKey.getTypeName(), siteKey.getName()),
                        new NavigationState(null));
                navService.saveNavigation(nav);
            }

            createPage(siteId, pages);
        } catch (Exception e) {
            LOG.error("Error while creating site", e);
            AssertionFailedError afe = new AssertionFailedError();
            afe.initCause(e);
            throw afe;
        }
    }

    protected void createPage(SiteId siteId, String... pages) {
        PageService pageService = container.getComponentInstanceOfType(PageService.class);

        SiteKey siteKey = Util.from(siteId);
        for (String page : pages) {
            pageService.savePage(new PageContext(new PageKey(siteKey, page), new PageState("displayName", "description", false,
                    null, Arrays.asList("Everyone"), "Everyone", Arrays.asList("Everyone"), Arrays.asList("Everyone"))));
        }
    }

    protected void setPermission(PageId pageId, String editPermission, String... accessPermissions) {
        PageKey pageKey = Util.from(pageId);
        PageService pageService = container.getComponentInstanceOfType(PageService.class);
        PageContext p = pageService.loadPage(pageKey);
        p.setState(p.getState().builder().editPermission(editPermission).accessPermissions(accessPermissions).build());
        pageService.savePage(p);
    }

    private void cleanup() throws Exception {
        DataStorage dataStorage = container.getComponentInstanceOfType(DataStorage.class);
        SiteType[] types = new SiteType[] { SiteType.PORTAL, SiteType.GROUP, SiteType.USER };

        for (SiteType type : types) {
            Query<PortalConfig> q = new Query<PortalConfig>(type.getName(), null, PortalConfig.class);
            ListAccess<PortalConfig> la = dataStorage.find2(q);
            for (PortalConfig portalConfig : la.load(0, la.getSize())) {
                dataStorage.remove(portalConfig);
            }
        }
    }
}
