package org.exoplatform.jpa;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.settings.jpa.SettingContextDAO;
import org.exoplatform.settings.jpa.SettingScopeDAO;
import org.exoplatform.settings.jpa.SettingsDAO;
import org.junit.AfterClass;
import org.junit.BeforeClass;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.settings-configuration-local.xml")
})
public class CommonsDAOJPAImplTest extends BaseTest {
  protected SettingContextDAO settingContextDAO;

  protected SettingScopeDAO   settingScopeDAO;

  protected SettingsDAO       settingsDAO;


  public void setUp() {
    super.setUp();

    // make sure data are well initialized for each test

    // Init DAO
    settingContextDAO = getService(SettingContextDAO.class);
    settingScopeDAO = getService(SettingScopeDAO.class);
    settingsDAO = getService(SettingsDAO.class);

    // Clean Data
    cleanDB();
  }

  public void testInit() {
    assertNotNull(settingContextDAO);
    assertNotNull(settingScopeDAO);
    assertNotNull(settingsDAO);
  }

  public void tearDown() {
    // Clean Data
    cleanDB();
    super.tearDown();
  }

  @BeforeClass
  @Override
  protected void beforeRunBare() {
    if (System.getProperty("gatein.test.output.path") == null) {
      System.setProperty("gatein.test.output.path", System.getProperty("java.io.tmpdir"));
    }
    super.beforeRunBare();
  }

  @AfterClass
  @Override
  protected void afterRunBare() {
    super.afterRunBare();
  }

  private void cleanDB() {
    settingsDAO.deleteAll();
    settingScopeDAO.deleteAll();
    settingContextDAO.deleteAll();
  }
}
