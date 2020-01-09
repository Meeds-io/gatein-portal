package org.exoplatform.portal.config;

import org.exoplatform.container.PortalContainer;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public abstract class AbstractSiteDataImportTest extends AbstractDataImportTest {

  @Override
  protected final String getConfig1() {
    return "site1";
  }

  @Override
  protected final String getConfig2() {
    return "site2";
  }

  @Override
  protected final void afterSecondBoot(PortalContainer container) throws Exception {
    afterFirstBoot(container);
  }

  @Override
  protected void afterSecondBootWithWantReimport(PortalContainer container) throws Exception {
    afterSecondBootWithOverride(container);
  }

  @Override
  protected final void afterSecondBootWithNoMixin(PortalContainer container) throws Exception {
    afterSecondBoot(container);
  }
}
