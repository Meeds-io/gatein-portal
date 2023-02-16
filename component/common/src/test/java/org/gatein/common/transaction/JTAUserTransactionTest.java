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

package org.gatein.common.transaction;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;

/**
 * test for {@link JTAUserTransactionLifecycleService}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.settings-configuration-local-jta.xml"),
})
public class JTAUserTransactionTest extends AbstractKernelTest {

    private JTAUserTransactionLifecycleService jtaUserTransactionLifecycleService;

    @Override
    protected void setUp() throws Exception {
        PortalContainer container = PortalContainer.getInstance();
        jtaUserTransactionLifecycleService = (JTAUserTransactionLifecycleService) container
                .getComponentInstanceOfType(JTAUserTransactionLifecycleService.class);
    }

    public void testTransactionLifecycle() throws Exception {
        UserTransaction tx = jtaUserTransactionLifecycleService.getUserTransaction();
        assertNotNull(tx);

        // Test normal workflow with begin/commit
        assertStatus(Status.STATUS_NO_TRANSACTION);

        jtaUserTransactionLifecycleService.beginJTATransaction();
        assertStatus(Status.STATUS_ACTIVE);

        jtaUserTransactionLifecycleService.finishJTATransaction();
        assertStatus(Status.STATUS_NO_TRANSACTION);

        // Test workflow with setRollBackOnly
        jtaUserTransactionLifecycleService.beginJTATransaction();
        assertStatus(Status.STATUS_ACTIVE);

        tx.setRollbackOnly();
        jtaUserTransactionLifecycleService.finishJTATransaction();
        assertStatus(Status.STATUS_NO_TRANSACTION);
    }

    public void testListener() throws Exception {
        CounterListener counterListener = new CounterListener();
        jtaUserTransactionLifecycleService.registerListener(counterListener);

        UserTransaction tx = jtaUserTransactionLifecycleService.getUserTransaction();
        assertNotNull(tx);

        assertEquals(counterListener.getBeforeBeginCounter(), 0);
        assertEquals(counterListener.getAfterBeginCounter(), 0);

        jtaUserTransactionLifecycleService.beginJTATransaction();
        assertEquals(counterListener.getBeforeBeginCounter(), 1);
        assertEquals(counterListener.getAfterBeginCounter(), 1);

        jtaUserTransactionLifecycleService.finishJTATransaction();
        jtaUserTransactionLifecycleService.beginJTATransaction();
        assertEquals(counterListener.getBeforeBeginCounter(), 2);
        assertEquals(counterListener.getAfterBeginCounter(), 2);

        jtaUserTransactionLifecycleService.finishJTATransaction();
        assertEquals(counterListener.getBeforeBeginCounter(), 2);
        assertEquals(counterListener.getAfterBeginCounter(), 2);
    }

    private void assertStatus(int expectedStatus) throws Exception {
        int status = jtaUserTransactionLifecycleService.getUserTransaction().getStatus();
        assertEquals(expectedStatus, status);
    }
}
