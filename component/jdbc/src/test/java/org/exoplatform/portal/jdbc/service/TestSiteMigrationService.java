package org.exoplatform.portal.jdbc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.persistence.EntityTransaction;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.AbstractPortalTest;
import org.exoplatform.portal.jdbc.migration.PageMigrationService;
import org.exoplatform.portal.jdbc.migration.SiteMigrationService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.page.PageServiceImpl;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;
import org.gatein.mop.api.workspace.Navigation;
import org.gatein.mop.api.workspace.ObjectType;
import org.gatein.mop.api.workspace.Site;
import org.gatein.mop.core.api.MOPService;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/portal-configuration.xml"),

        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml")})
public class TestSiteMigrationService extends AbstractPortalTest {

  private POMDataStorage pomStorage;

  private ModelDataStorage modelStorage;

  private PageService pageService;

  private POMSessionManager manager;

  private PageServiceImpl jcrPageService;

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
    this.jcrPageService = new PageServiceImpl(manager);

    InitParams params = new InitParams();
    ValueParam v = new ValueParam();
    v.setName("workspace");
    v.setValue("portal-test");
    params.addParameter(v);

    this.siteMigrationService = new SiteMigrationService(params, pomStorage, modelStorage,
            getContainer().getComponentInstanceOfType(ListenerService.class),
            getContainer().getComponentInstanceOfType(RepositoryService.class),
            getContainer().getComponentInstanceOfType(SettingService.class),
            getContainer().getComponentInstanceOfType(EntityManagerService.class));

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

    ContainerData container = new ContainerData(null, null, "", "", "", "", "",
            "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    PortalData portalData = new PortalData(null, "testSiteMigration", "portal",
            "en", "", "", Collections.emptyList(), "", null, "", container, Collections.emptyList());
    pomStorage.save(portalData);

    sync(true);

    siteMigrationService.doMigration();
    siteMigrationService.doRemove();

    begin();

//    assertNotNull(pageService.loadPage(pageKey));
    assertNotNull(modelStorage.getPortalConfig(new PortalKey("portal", "testSiteMigration")));
  }
}
