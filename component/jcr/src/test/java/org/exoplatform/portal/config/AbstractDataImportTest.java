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

package org.exoplatform.portal.config;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.mop.importer.ImportMode;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public abstract class AbstractDataImportTest extends AbstractGateInTest {
  private Set<String> clearProperties = new HashSet<String>();

  protected abstract ImportMode getMode();

  protected abstract String getConfig2();

  protected abstract String getConfig1();

  protected void afterOneBootWithExtention(PortalContainer container) throws Exception {
  }

  protected void afterFirstBoot(PortalContainer container) throws Exception {
  }

  protected abstract void afterSecondBoot(PortalContainer container) throws Exception;

  protected abstract void afterSecondBootWithOverride(PortalContainer container) throws Exception;

  protected abstract void afterSecondBootWithWantReimport(PortalContainer container) throws Exception;

  protected abstract void afterSecondBootWithNoMixin(PortalContainer container) throws Exception;

  protected void setSystemProperty(String key, String value) {
    clearProperties.add(key);
    System.setProperty(key, value);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    for (String key : clearProperties) {
      System.clearProperty(key);
    }
    clearProperties.clear();
  }

  public void testOneBootWithExtension() throws Exception {
    KernelBootstrap bootstrap = new KernelBootstrap();
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.test.jcr-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.identity-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.portal-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport1-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport2-configuration.xml");

    //
    setSystemProperty("override.1", "false");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());
    setSystemProperty("override_2", "false");
    setSystemProperty("import.mode_2", getMode().toString());
    setSystemProperty("import.portal_2", getConfig2());

    //
    bootstrap.boot();
    PortalContainer container = bootstrap.getContainer();
    afterOneBootWithExtention(container);
    bootstrap.dispose();
  }

  public void testOneBoot() throws Exception {
    KernelBootstrap bootstrap = new KernelBootstrap();
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.test.jcr-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.identity-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.portal-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport1-configuration.xml");

    //
    setSystemProperty("override.1", "false");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());

    //
    bootstrap.boot();
    PortalContainer container = bootstrap.getContainer();
    afterFirstBoot(container);
    bootstrap.dispose();
  }

  public void testTwoBoots() throws Exception {
    KernelBootstrap bootstrap = new KernelBootstrap();
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.test.jcr-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.identity-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.portal-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport1-configuration.xml");

    //
    setSystemProperty("override.1", "false");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());

    bootstrap.boot();
    PortalContainer container = bootstrap.getContainer();
    afterFirstBoot(container);
    bootstrap.dispose();

    //
    setSystemProperty("import.portal.1", getConfig2());

    bootstrap.boot();
    container = bootstrap.getContainer();
    afterSecondBoot(container);
    bootstrap.dispose();
  }

  public void testTwoBootsWithOverride() throws Exception {
    KernelBootstrap bootstrap = new KernelBootstrap();
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.test.jcr-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.identity-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.portal-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport1-configuration.xml");

    //
    setSystemProperty("override.1", "true");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());

    bootstrap.boot();
    PortalContainer container = bootstrap.getContainer();
    afterFirstBoot(container);
    bootstrap.dispose();

    //
    setSystemProperty("import.portal.1", getConfig2());

    bootstrap.boot();
    container = bootstrap.getContainer();
    afterSecondBootWithOverride(container);
    bootstrap.dispose();
  }

  public void testTwoBootsWithPortalConfigOverrideFlag() throws Exception {
    KernelBootstrap bootstrap = new KernelBootstrap();
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.test.jcr-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.identity-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "conf/exo.portal.component.portal-configuration.xml");
    bootstrap.addConfiguration(ContainerScope.PORTAL, "org/exoplatform/portal/config/TestImport3-configuration.xml");

    //
    setSystemProperty("override.1", "true");
    setSystemProperty("override.2", "false");
    setSystemProperty("import.mode.1", getMode().toString());
    setSystemProperty("import.portal.1", getConfig1());

    bootstrap.boot();
    PortalContainer container = bootstrap.getContainer();
    afterFirstBoot(container);
    bootstrap.dispose();

    //
    setSystemProperty("import.portal.1", getConfig2());

    bootstrap.boot();
    container = bootstrap.getContainer();
    afterSecondBoot(container);
    bootstrap.dispose();
  }
}
