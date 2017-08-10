package org.exoplatform.portal.jdbc.service;

import javax.persistence.EntityTransaction;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.TestDataStorage;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Dashboard;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.pom.spi.gadget.Gadget;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;

@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/portal-configuration.xml"),

    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.portal.jdbc.test.configuration.xml") })
public class TestModelStorage extends TestDataStorage {

  private PageService pageService;

  private DataStorage storage_;

  public TestModelStorage(String name) {
    super(name);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.pageService = getContainer().getComponentInstanceOfType(PageService.class);
    storage_ = (DataStorage) getContainer().getComponentInstanceOfType(DataStorage.class);
  }

  @Override
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

  @Override
  protected void begin() {
    super.begin();

    EntityManagerService managerService =
                                        getContainer().getComponentInstanceOfType(EntityManagerService.class);
    EntityTransaction transaction = managerService.getEntityManager().getTransaction();
    if (!transaction.isActive()) {
      transaction.begin();
    }
  }

  public void testWindowMove1() {

  }

  public void testPageMerge() throws Exception {
    Page page = storage_.getPage("portal::test::test4");

    String app1Id = page.getChildren().get(0).getStorageId();
    Container container = (Container) page.getChildren().get(1);
    String containerId = container.getStorageId();
    String app2Id = container.getChildren().get(0).getStorageId();
    String app3Id = container.getChildren().get(1).getStorageId();

    // Add an application
    Application<Portlet> groovyApp = Application.createPortletApplication();
    ApplicationState<Portlet> state = new TransientApplicationState<Portlet>("web/GroovyPortlet");
    groovyApp.setState(state);
    ((Container) page.getChildren().get(1)).getChildren().add(1, groovyApp);

    // Save
    storage_.save(page);

    // Check it is existing at the correct location
    // and also that the ids are still the same
    page = storage_.getPage("portal::test::test4");
    assertEquals(2, page.getChildren().size());
    // assertEquals(PortletState.create("portal#test:/web/BannerPortlet/banner"),
    // ((Application)page.getChildren().get(0)).getInstanceState());
    assertEquals(app1Id, page.getChildren().get(0).getStorageId());
    container = (Container) page.getChildren().get(1);
    assertEquals(3, container.getChildren().size());
    assertEquals(containerId, container.getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/BannerPortlet/banner"),
    // ((Application)container.getChildren().get(0)).getInstanceState());
    assertEquals(app2Id, container.getChildren().get(0).getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/GroovyPortlet/groovyportlet"),
    // ((Application)container.getChildren().get(1)).getInstanceState());
    assertNotNull(container.getChildren().get(0).getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/FooterPortlet/footer"),
    // ((Application)container.getChildren().get(2)).getInstanceState());
    assertEquals(app3Id, container.getChildren().get(2).getStorageId());

    // Now remove the element
    container.getChildren().remove(1);
    storage_.save(page);

    // Check it is removed
    // and also that the ids are still the same
    page = storage_.getPage("portal::test::test4");
    assertEquals(2, page.getChildren().size());
    // assertEquals(PortletState.create("portal#test:/web/BannerPortlet/banner"),
    // ((Application)page.getChildren().get(0)).getInstanceState());
    assertEquals(app1Id, page.getChildren().get(0).getStorageId());
    container = (Container) page.getChildren().get(1);
    assertEquals(2, container.getChildren().size());
    assertEquals(containerId, container.getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/BannerPortlet/banner"),
    // ((Application)container.getChildren().get(0)).getInstanceState());
    assertEquals(app2Id, container.getChildren().get(0).getStorageId());
    // assertEquals(PortletState.create("portal#test:/web/FooterPortlet/footer"),
    // ((Application)container.getChildren().get(1)).getInstanceState());
    assertEquals(app3Id, container.getChildren().get(1).getStorageId());
  }

  public void testAccessMixin() throws Exception {

  }

  public void testModifyMixin() throws Exception {

  }

  public void testJTA() throws Exception {

  }

  public void testNullPageReferenceDeletes() throws Exception {

  }

  public void testGettingGadgetInDashboard() throws Exception {
    Page page = new Page();
    page.setPageId("portal::test::foo");
    Application<Portlet> app = Application.createPortletApplication();
    app.setState(new TransientApplicationState<Portlet>("dashboard/DashboardPortlet"));
    page.getChildren().add(app);
    pageService.savePage(new PageContext(page.getPageKey(), null));
    storage_.save(page);
    page = storage_.getPage("portal::test::foo");
    String id = page.getChildren().get(0).getStorageId();

    // Load the dashboard itself
    Dashboard dashboard = storage_.loadDashboard(id);

    // Put a gadget in one container
    Container row0 = (Container) dashboard.getChildren().get(0);
    Application<Gadget> gadgetApp = Application.createGadgetApplication();
    gadgetApp.setState(new TransientApplicationState<Gadget>("foo"));
    row0.getChildren().add(gadgetApp);

    // Save the dashboard
    storage_.saveDashboard(dashboard);

    // Load again the persisted version
    dashboard = storage_.loadDashboard(id);

    row0 = (Container) dashboard.getChildren().get(0);
    Application<Gadget> gadget = (Application<Gadget>) row0.getChildren().get(0);
    String storageId = gadget.getStorageId();

    // Now get the gadget by StorageId
    Application<Object> applicationModel = storage_.getApplicationModel(storageId);
    assertEquals(gadget.getId(), applicationModel.getId());
  }

  public void testSiteLayout() throws Exception {
    PortalConfig pConfig = storage_.getPortalConfig(PortalConfig.PORTAL_TYPE, "classic");
    assertNotNull(pConfig);
    assertNotNull("The Group layout of " + pConfig.getName() + " is null",
                  pConfig.getPortalLayout());

    pConfig = storage_.getPortalConfig(PortalConfig.GROUP_TYPE, "/platform/administrators");
    assertNotNull(pConfig);
    assertNotNull("The Group layout of " + pConfig.getName() + " is null",
                  pConfig.getPortalLayout());
    assertTrue(pConfig.getPortalLayout().getChildren() != null
        && pConfig.getPortalLayout().getChildren().size() > 1);
    pConfig.getPortalLayout().getChildren().clear();
    storage_.save(pConfig);

    pConfig = storage_.getPortalConfig(PortalConfig.GROUP_TYPE, "/platform/administrators");
    assertNotNull(pConfig);
    assertNotNull("The Group layout of " + pConfig.getName() + " is null",
                  pConfig.getPortalLayout());
    assertTrue(pConfig.getPortalLayout().getChildren() != null
        && pConfig.getPortalLayout().getChildren().size() == 0);
  }

  public void testGroupLayout() throws Exception {
  }
  
  public void testUserLayout() throws Exception {
  }
  
  public void testGroupNavigation() throws Exception {    
  }
  
  // We need to investigate why when the pref is null we need to replace it with
  // empty string
  // in chromattic, it's done by
  // org.exoplatform.portal.pom.spi.portlet.PortletState
  public void testNullPreferenceValue() throws Exception {
    Page page = storage_.getPage("portal::test::test4");
    Application<Portlet> app = (Application<Portlet>) page.getChildren().get(0);
    PersistentApplicationState<Portlet> state = (PersistentApplicationState) app.getState();

    //
    Portlet prefs = storage_.load(state, ApplicationType.PORTLET);

    //
    prefs.setValue("template", null);
    storage_.save(state, prefs);

    //
    prefs = storage_.load(state, ApplicationType.PORTLET);
    assertNotNull(prefs);
    // assertEquals(new PortletBuilder().add("template", "").build(), prefs);
  }
}
