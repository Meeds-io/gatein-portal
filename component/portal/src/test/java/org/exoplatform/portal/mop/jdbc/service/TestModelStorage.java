package org.exoplatform.portal.mop.jdbc.service;

import java.util.*;

import javax.persistence.EntityTransaction;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.*;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.pom.data.ModelChange;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration-local.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/mop/navigation/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test.mop.portal.configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml") })
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
    super.end();
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

  public void testWindowMove1() throws Exception {
    Page page = storage_.getPage("portal::test::test4");
    Application<?> a1 = (Application<?>) page.getChildren().get(0);
    Container a2 = (Container) page.getChildren().get(1);
    Application<?> a3 = (Application<?>) a2.getChildren().get(0);
    Application<?> a4 = (Application<?>) a2.getChildren().remove(1);
    page.getChildren().add(1, a4);
    List<ModelChange> changes = storage_.save(page);

    //
    page = storage_.getPage("portal::test::test4");
    assertEquals(3, page.getChildren().size());
    Application<?> c1 = (Application<?>) page.getChildren().get(0);
    assertEquals(a1.getStorageId(), c1.getStorageId());
    Application<?> c2 = (Application<?>) page.getChildren().get(1);
    assertEquals(a4.getStorageId(), c2.getStorageId());
    Container c3 = (Container) page.getChildren().get(2);
    assertEquals(a2.getStorageId(), c3.getStorageId());
    assertEquals(1, c3.getChildren().size());
    Application<?> c4 = (Application<?>) c3.getChildren().get(0);
    assertEquals(a3.getStorageId(), c4.getStorageId());
  }

  public void testPageMerge() throws Exception {
    Page page = storage_.getPage("portal::test::test5");

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
    page = storage_.getPage("portal::test::test5");
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
    page = storage_.getPage("portal::test::test5");
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

  public void testGetAllPortalNames() throws Exception {
    testGetAllSiteNames("portal", "getAllPortalNames");
  }

  public void testGetAllGroupNames() throws Exception {
    testGetAllSiteNames("group", "getAllGroupNames");
  }

  private void testGetAllSiteNames(String siteType, final String methodName) throws Exception {
    final List<String> names = (List<String>) storage_.getClass().getMethod(methodName).invoke(storage_);

    // Create new portal
    storage_.create(new PortalConfig(siteType, "testGetAllSiteNames"));

    // Test during tx we see the good names
    List<String> transientNames = (List<String>) storage_.getClass().getMethod(methodName).invoke(storage_);
    assertTrue("Was expecting " + transientNames + " to contain " + names, transientNames.containsAll(names));
    transientNames.removeAll(names);
    assertEquals(Collections.singletonList("testGetAllSiteNames"), transientNames);

    // Now commit tx
    end(true);

    // We test we observe the change
    begin();
    List<String> afterNames = (List<String>) storage_.getClass().getMethod(methodName).invoke(storage_);
    assertTrue(afterNames.containsAll(names));
    afterNames.removeAll(names);
    assertEquals(Collections.singletonList("testGetAllSiteNames"), afterNames);

    // Then we remove the newly created portal
    storage_.remove(new PortalConfig(siteType, "testGetAllSiteNames"));

    // Test we are syeing the transient change
    transientNames.clear();
    transientNames = (List<String>) storage_.getClass().getMethod(methodName).invoke(storage_);
    assertEquals(names, transientNames);

    //
    end(true);

    // Now test it is still removed
    begin();
    afterNames = (List<String>) storage_.getClass().getMethod(methodName).invoke(storage_);
    assertEquals(new HashSet<String>(names), new HashSet<String>(afterNames));
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

  public void testGetPageChildrenFilteredByProfiles() throws Exception {
    Page page = storage_.getPage("portal::test::test6");
    assertNotNull(page.getChildren());
    assertEquals(1, page.getChildren().size());
    Container container = (Container) page.getChildren().get(0);

    assertEquals("test", container.getProfiles());
  }

  public void testGetContainerClass() throws Exception {
    Page page = storage_.getPage("portal::test::test6");
    assertNotNull(page.getChildren());
    assertEquals(1, page.getChildren().size());
    Container container = (Container) page.getChildren().get(0);

    assertEquals("testClass1 testClass2", container.getCssClass());
  }

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
