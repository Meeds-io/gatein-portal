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

package org.exoplatform.web.security;

import java.security.SecureRandom;

import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.web.security.security.SecureRandomService;

/**
 *
 * @author <a href="mailto:ppalaga@redhat.com">Peter Palaga</a>
 *
 */

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/secure-random-service-configuration.xml")})
public class TestSecureRandomService extends AbstractKernelTest {
    protected SecureRandomService service;

    protected void setUp() throws Exception {
        PortalContainer container = getContainer();
        service = (SecureRandomService) container.getComponentInstanceOfType(SecureRandomService.class);
    }

    public void testGetSecureRandom() {
        /* let's look if the threading internals work */
        SecureRandom r = service.getSecureRandom();
        /* have we gotten anything at all? */
        assertNotNull(r);

        /* how can random be tested? */
        r.nextInt();
        r.nextInt();
    }

}
