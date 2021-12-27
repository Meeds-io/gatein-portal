package org.exoplatform.jpa;

import org.exoplatform.commons.testing.BaseTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public abstract class BaseTest extends BaseTestCase {
  public void setUp() {
    begin();
  }

  protected void tearDown() {
    end();
  }

  @BeforeClass
  @Override
  protected void beforeRunBare() {
    if(System.getProperty("gatein.test.output.path") == null) {
      System.setProperty("gatein.test.output.path", System.getProperty("java.io.tmpdir"));
    }
    super.beforeRunBare();
  }

  @AfterClass
  @Override
  protected void afterRunBare() {
    super.afterRunBare();
  }

  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }
}
