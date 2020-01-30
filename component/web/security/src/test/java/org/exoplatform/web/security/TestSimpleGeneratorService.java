/*
 * JBoss, a division of Red Hat
 * Copyright 2012, Red Hat Middleware, LLC, and individual
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

package org.exoplatform.web.security;

import org.gatein.wci.security.Credentials;

import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/tokenservice-configuration.xml") })
public class TestSimpleGeneratorService extends AbstractKernelTest {
    private SimpleGeneratorCookieTokenService service;

    protected void setUp() throws Exception {
        PortalContainer container = getContainer();
        service = (SimpleGeneratorCookieTokenService) container.getComponentInstanceOfType(SimpleGeneratorCookieTokenService.class);
        Thread.sleep(1000); // for enough time initial database
        begin();
    }
    protected void tearDown() {
        end();
    }

    /**
     * Test that duplicated token is never generated
     */
    public void testDuplicatedTokenGeneration() throws Exception {
        String token1 = service.createToken(new Credentials("root1", "gtn1"));
        assertEquals("random0.rememberme0", token1);
        assertEquals(service.getCounter(), 1);

        String token2 = service.createToken(new Credentials("root2", "gtn2"));
        assertEquals("random1.rememberme1", token2);
        assertEquals(service.getCounter(), 2);

        String token3 = service.createToken(new Credentials("-root3", "gtn3"));
        assertEquals("random2.rememberme2", token3);
        // Counter should be 4 now due to duplicated token generation
        assertEquals(service.getCounter(), 4);

        assertEquals("root1", service.getToken(token1).getPayload().getUsername());
        assertEquals("root2", service.getToken(token2).getPayload().getUsername());
        assertEquals("-root3", service.getToken(token3).getPayload().getUsername());
    }
}
