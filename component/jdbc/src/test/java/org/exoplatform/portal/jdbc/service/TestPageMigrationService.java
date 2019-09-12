package org.exoplatform.portal.jdbc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.persistence.EntityTransaction;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
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
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.services.listener.ListenerService;
import org.gatein.mop.api.workspace.ObjectType;
import org.gatein.mop.api.workspace.Site;
import org.gatein.mop.core.api.MOPService;
import org.junit.After;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/portal-configuration.xml"),

        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml")})
public class TestPageMigrationService extends AbstractPortalTest {

  private POMDataStorage pomStorage;

  private ModelDataStorage modelStorage;

  private PageService pageService;

  private POMSessionManager manager;

  private PageServiceImpl jcrPageService;

  private PageMigrationService pageMigrationService;

  private SiteMigrationService siteMigrationService;

  public TestPageMigrationService(String name) {
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
    this.pageMigrationService = new PageMigrationService(null, pomStorage, modelStorage, pageService, jcrPageService,
            getContainer().getComponentInstanceOfType(ListenerService.class), getContainer().getComponentInstanceOfType(EntityManagerService.class));
    this.siteMigrationService = new SiteMigrationService(null, pomStorage, modelStorage,
            getContainer().getComponentInstanceOfType(ListenerService.class), getContainer().getComponentInstanceOfType(EntityManagerService.class));

    super.begin();

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
    Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "testPageMigrationSite");
    portal.getRootPage().addChild("pages");

    PageKey pageKey = new PageKey(SiteKey.portal("testPageMigrationSite"), "testPageMigration");
    PageState state = new PageState("", "", false, "",
            Collections.emptyList(), "", Collections.emptyList(), Collections.emptyList());
    PageContext pageContext = new PageContext(pageKey, state);
    jcrPageService.savePage(pageContext);

    ContainerData container = new ContainerData(null, "test", "", "", "", "", "",
            "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    pomStorage.save(new PageData(null, null,
            "testPageMigration", null, null, null, "testPageMigration", "", "", "",
            Collections.emptyList(), Arrays.asList(container), "portal", "testPageMigrationSite", "", false,
            Collections.emptyList(), Collections.emptyList()));

    sync(true);

    container = new ContainerData(null, "test", "", "", "", "", "",
            "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    modelStorage.create(new PortalData(null,
            "testPageMigrationSite", SiteType.PORTAL.getName(), null, null,
            null, new ArrayList<>(), null, null, null, container, null));

    pageMigrationService.doMigration();
    pageMigrationService.doRemove();

    begin();

    assertNull(jcrPageService.loadPage(pageKey));
    assertNotNull(pageService.loadPage(pageKey));

    // Clean up portal
    PortalData portalData = new PortalData(null, "testPageMigrationSite", "portal",
            "en", "", "", Collections.emptyList(), "", null, "", container, Collections.emptyList());
    this.pomStorage.remove(portalData);
    sync(true);
  }
}
