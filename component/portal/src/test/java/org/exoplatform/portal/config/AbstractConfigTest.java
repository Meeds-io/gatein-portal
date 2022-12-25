package org.exoplatform.portal.config;

import org.exoplatform.component.test.*;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration-local.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml")})
public abstract class AbstractConfigTest extends AbstractKernelTest {

  @Override
  protected void setUp() throws Exception {
    super.begin();
  }

  @Override
  protected void tearDown() throws Exception {
    super.end();
  }
}
