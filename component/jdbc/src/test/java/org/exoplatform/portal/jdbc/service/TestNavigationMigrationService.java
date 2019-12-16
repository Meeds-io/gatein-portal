package org.exoplatform.portal.jdbc.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import javax.persistence.EntityTransaction;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.AbstractPortalTest;
import org.exoplatform.portal.config.TestDataStorage;
import org.exoplatform.portal.jdbc.migration.NavigationMigrationService;
import org.exoplatform.portal.jdbc.migration.SiteMigrationService;
import org.exoplatform.portal.mop.Described;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.description.DescriptionServiceImpl;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NavigationServiceImpl;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.navigation.SimpleDataCache;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;
import org.gatein.mop.api.workspace.Navigation;
import org.gatein.mop.api.workspace.ObjectType;
import org.gatein.mop.api.workspace.Site;
import org.gatein.mop.core.api.MOPService;
import org.junit.After;
import org.junit.Before;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/portal-configuration.xml"),

        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml")})
public class TestNavigationMigrationService extends AbstractPortalTest {

  private NavigationService navService;

  private DescriptionService descriptionService;

  private DescriptionServiceImpl jcrDescriptionService;

  private NavigationServiceImpl jcrNavService;

  private POMSessionManager manager;

  private NavigationMigrationService migrationService;

  private ModelDataStorage modelStorage;

  private POMDataStorage pomStorage;

  public TestNavigationMigrationService(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

    this.pomStorage = getContainer().getComponentInstanceOfType(POMDataStorage .class);
    this.navService = getContainer().getComponentInstanceOfType(NavigationService.class);
    this.manager = getContainer().getComponentInstanceOfType(POMSessionManager.class);
    this.modelStorage = getContainer().getComponentInstanceOfType(ModelDataStorage.class);

    SimpleDataCache cache = new SimpleDataCache();
    this.jcrNavService = new NavigationServiceImpl(manager, cache);

    this.descriptionService = getContainer().getComponentInstanceOfType(DescriptionService.class);
    this.jcrDescriptionService = new DescriptionServiceImpl(manager);

    InitParams params = new InitParams();
    ValueParam v = new ValueParam();
    v.setName("workspace");
    v.setValue("portal-test");
    params.addParameter(v);
    this.migrationService = new NavigationMigrationService(params, pomStorage, navService, descriptionService,
            getService(POMSessionManager.class),
            getService(ListenerService.class),
            getService(RepositoryService.class),
            getService(EntityManagerService.class));

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

  public <T> T getService(Class<T> clazz) {
    return getContainer().getComponentInstanceOfType(clazz);
  }

  @Override
  @After
  protected void end(boolean save) {
    EntityManagerService managerService =
            getContainer().getComponentInstanceOfType(EntityManagerService.class);
    EntityTransaction transaction = managerService.getEntityManager().getTransaction();
    if (transaction.isActive()) {
      if (save) {
        transaction.commit();
      } else {
        transaction.rollback();
      }
    }
    super.end(save);
  }


  public void testMigrate() throws Exception {
    MOPService mop = manager.getPOMService();
    Site portal = mop.getModel().getWorkspace().addSite(ObjectType.PORTAL_SITE, "testMigrate");
    Navigation defaultNav = portal.getRootNavigation().addChild("default");
    defaultNav.addChild("a");

    NavigationContext nav = jcrNavService.loadNavigation(SiteKey.portal("testMigrate"));
    nav.setState(new NavigationState(1));
    jcrNavService.saveNavigation(nav);
    NodeContext root = jcrNavService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    NodeContext child = root.get("a");
    assertNotNull(nav);

    jcrDescriptionService.setDescription(child.getId(), Locale.ENGLISH, new Described.State("testDescribe", "testDescribe"));

    sync(true);

    ContainerData container = new ContainerData(null, "test", "", "", "", "", "",
    "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    modelStorage.create(new PortalData(null,
            "testMigrate", SiteType.PORTAL.getName(), null, null,
            null, new ArrayList<>(), null, null, null, container, null));

    sync(true);

    migrationService.doMigration();
    migrationService.doRemove();

    nav = navService.loadNavigation(SiteKey.portal("testMigrate"));
    assertNotNull(nav);

    root = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    assertNotNull(root);
    child = root.get("a");
    assertNotNull(child);
    assertNotNull(descriptionService.getDescription(child.getId(), Locale.ENGLISH));


    jcrNavService.clearCache();
    nav = jcrNavService.loadNavigation(SiteKey.portal("testMigrate"));
    assertNull(nav);

    // Remove site
    PortalData portalData = new PortalData(null, "testMigrate", "portal",
            "en", "", "", Collections.emptyList(), "", null, "", container, Collections.emptyList());
    this.pomStorage.remove(portalData);
    this.pomStorage.save();
    sync(true);
  }
}
