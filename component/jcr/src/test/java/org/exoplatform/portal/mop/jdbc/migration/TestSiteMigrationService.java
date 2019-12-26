package org.exoplatform.portal.mop.jdbc.migration;

import java.util.Collections;

import javax.persistence.EntityTransaction;

import org.gatein.mop.api.workspace.*;
import org.gatein.mop.core.api.MOPService;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.*;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.AbstractPortalTest;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.jdbc.migration.SiteMigrationService;
import org.exoplatform.portal.mop.jdbc.service.PageServiceImpl;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;

@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/portal-configuration.xml"),

    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml") })
public class TestSiteMigrationService extends AbstractPortalTest {

  private POMDataStorage       pomStorage;

  private ModelDataStorage     modelStorage;

  private PageService          pageService;

  private POMSessionManager    manager;

  private org.exoplatform.portal.mop.page.PageServiceImpl      jcrPageService;

  private SiteMigrationService siteMigrationService;

  public TestSiteMigrationService(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    this.pomStorage = getContainer().getComponentInstanceOfType(POMDataStorage.class);
    this.modelStorage = getContainer().getComponentInstanceOfType(ModelDataStorage.class);
    this.pageService = getContainer().getComponentInstanceOfType(PageService.class);
    this.manager = getContainer().getComponentInstanceOfType(POMSessionManager.class);
    this.jcrPageService = new org.exoplatform.portal.mop.page.PageServiceImpl(manager);

    InitParams params = new InitParams();
    ValueParam v = new ValueParam();
    v.setName("workspace");
    v.setValue("portal-test");
    params.addParameter(v);

    this.siteMigrationService = new SiteMigrationService(params,
                                                         pomStorage,
                                                         modelStorage,
                                                         getContainer().getComponentInstanceOfType(ListenerService.class),
                                                         getContainer().getComponentInstanceOfType(RepositoryService.class),
                                                         getContainer().getComponentInstanceOfType(SettingService.class));

    begin();

    EntityManagerService managerService =
                                        getContainer().getComponentInstanceOfType(EntityManagerService.class);
    EntityTransaction transaction = managerService.getEntityManager().getTransaction();
    if (!transaction.isActive()) {
      transaction.begin();
    }
  }

  protected void tearDown() throws Exception {
    end(false);
  }

  public void testMigrate() throws Exception {
    MOPService mop = manager.getPOMService();
    Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "testSiteMigration");
    Navigation defaultNav = portal.getRootNavigation().addChild("default");
    defaultNav.addChild("a");

    portal.getRootPage().addChild("pages");
    portal.getRootPage().addChild("templates");

    ContainerData container = new ContainerData(null,
                                                null,
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                Collections.emptyList(),
                                                Collections.emptyList(),
                                                Collections.emptyList(),
                                                Collections.emptyList());
    PortalData portalData = new PortalData(null,
                                           "testSiteMigration",
                                           "portal",
                                           "en",
                                           "",
                                           "",
                                           Collections.emptyList(),
                                           "",
                                           null,
                                           "",
                                           container,
                                           Collections.emptyList());
    pomStorage.save(portalData);

    sync(true);

    siteMigrationService.doMigrate(new PortalKey(PortalConfig.PORTAL_TYPE, "testSiteMigration"));
    siteMigrationService.doRemove(new PortalKey(PortalConfig.PORTAL_TYPE, "testSiteMigration"));

    begin();

    // assertNotNull(pageService.loadPage(pageKey));
    assertNotNull(modelStorage.getPortalConfig(new PortalKey(PortalConfig.PORTAL_TYPE, "testSiteMigration")));
  }
}
