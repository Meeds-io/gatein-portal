/*
 * Copyright (C) 2011 eXo Platform SAS.
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

package org.exoplatform.portal.mop.navigation;

import java.util.ArrayList;
import java.util.Collections;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.mop.AbstractMOPTest;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.description.DescriptionServiceImpl;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PortalData;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/mop/navigation/configuration.xml") })
public abstract class AbstractTestNavigationService extends AbstractMOPTest {

    /** . */
    protected POMSessionManager mgr;

    /** . */
    protected NavigationService service;

    /** . */
    protected DataStorage dataStorage;

    /** . */
    protected DescriptionService descriptionService;

    protected ModelDataStorage modelDataStorage;

    @Override
    protected void setUp() throws Exception {
        PortalContainer container = PortalContainer.getInstance();
        mgr = (POMSessionManager) container.getComponentInstanceOfType(POMSessionManager.class);
        service = getNavigationService();
        descriptionService = new DescriptionServiceImpl(mgr);
        dataStorage = (DataStorage) container.getComponentInstanceOfType(DataStorage.class);
        modelDataStorage = (ModelDataStorage) container.getComponentInstanceOfType(ModelDataStorage.class);

        // Clear the cache for each test
        if (service instanceof  NavigationServiceImpl) {
            ((NavigationServiceImpl)service).clearCache();
        }

        //
        super.setUp();
    }

    protected NavigationService getNavigationService() {
        return new NavigationServiceImpl(mgr);
    }

    protected void createSite(SiteType siteType, String siteName) throws Exception {
        ContainerData container = new ContainerData(null, "test", "", "", "", "", "",
                "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        modelDataStorage.create(new PortalData(null,
                siteName, siteType.getName(), null, null,
                null, new ArrayList<>(), null, null, null, container, null));
    }

    protected void createNavigation(SiteType siteType, String siteName) throws Exception {
        createSite(siteType, siteName);
        service.saveNavigation(new NavigationContext(new SiteKey(siteType, siteName), new NavigationState(1)));
    }
}
