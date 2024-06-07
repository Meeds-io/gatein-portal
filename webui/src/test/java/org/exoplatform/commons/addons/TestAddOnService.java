/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.commons.addons;

import java.util.List;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

@ConfiguredBy({
   @ConfigurationUnit(
     scope = ContainerScope.PORTAL,
     path = "conf/portal/test-configuration.xml"
   )
})
public class TestAddOnService extends AbstractKernelTest {

    private AddOnService service;
    
    @Override
    protected void setUp() throws Exception {
        PortalContainer container = getContainer();
        service = container.getComponentInstanceOfType(AddOnService.class);        
    }

    public void testGetApplications() {
        List<Application<Portlet>> apps = service.getApplications("AddOnContainer");
        assertEquals(1, apps.size());
        
        assertEquals("SampleAddOn/helloAddOn", ((TransientApplicationState)apps.get(0).getState()).getContentId());
    }

}
