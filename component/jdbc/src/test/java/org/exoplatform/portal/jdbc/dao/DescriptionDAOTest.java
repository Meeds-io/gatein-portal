package org.exoplatform.portal.jdbc.dao;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.exoplatform.commons.utils.I18N;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.portal.jdbc.entity.DescriptionEntity;
import org.exoplatform.portal.jdbc.entity.DescriptionState;

@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.portal.jdbc.test.configuration.xml") })
public class DescriptionDAOTest extends AbstractKernelTest {
  private DescriptionDAO descDAO;

  @Override
  protected void setUp() throws Exception {
    begin();
    super.setUp();
    this.descDAO = getContainer().getComponentInstanceOfType(DescriptionDAO.class);
  }

  @Override
  protected void tearDown() throws Exception {
    descDAO.deleteAll();
    super.tearDown();
    end();
  }

  public void testCreate() {
    DescriptionEntity desc = createDescription("testCreate");
    descDAO.create(desc);
    end();
    begin();

    DescriptionEntity result = descDAO.find(desc.getId());
    assertNotNull(result);
    assertDescription(desc, result);
  }

  public void testSave() {
    DescriptionEntity desc = createDescription("testSave");
    descDAO.create(desc);
    end();
    begin();

    descDAO.saveDescription("testSave", new DescriptionState("testName2", "testDesc2"));
    end();
    begin();
    DescriptionEntity result = descDAO.find(desc.getId());
    assertEquals("testName2", result.getState().getName());
    assertEquals("testDesc2", result.getState().getDescription());
  }

  public void testSave2() {
    DescriptionEntity desc = createDescription("testSave2");
    descDAO.create(desc);
    end();
    begin();

    Map<String, DescriptionState> localized = new HashMap<String, DescriptionState>();
    localized.put(I18N.toTagIdentifier(Locale.FRENCH), new DescriptionState("name1", "desc1"));
    localized.put(I18N.toTagIdentifier(Locale.ITALY), new DescriptionState("name2", "desc2"));
    descDAO.saveDescriptions("testSave2", localized);
    end();
    begin();

    DescriptionEntity result = descDAO.find(desc.getId());
    assertNotNull(result.getLocalized());
    Map<String, DescriptionState> resultLocalized = result.getLocalized();
    assertEquals(2, resultLocalized.size());
    assertEquals(new DescriptionState("name1", "desc1"), resultLocalized.get(I18N.toTagIdentifier(Locale.FRENCH)));
    assertEquals(new DescriptionState("name2", "desc2"), resultLocalized.get(I18N.toTagIdentifier(Locale.ITALY)));
  }
  
  public void testDelete() {
    DescriptionEntity desc = createDescription("testDelete");
    descDAO.create(desc);
    end();
    begin();
    
    descDAO.deleteByRefId("testDelete");
    end();
    begin();
    assertNull(descDAO.find(desc.getId()));
    assertNull(descDAO.getByRefId("testDelete"));
  }

  private void assertDescription(DescriptionEntity expected, DescriptionEntity result) {
    assertEquals(expected.getReferenceId(), result.getReferenceId());
    assertEquals(expected.getId(), result.getId());
    assertEquals(expected.getState(), result.getState());
    if (expected.getLocalized() == null) {
      assertNull(result.getLocalized());
    } else {
      assertEquals(expected.getLocalized().size(), result.getLocalized().size());
      for (String locale : expected.getLocalized().keySet()) {
        assertEquals(expected.getLocalized().get(locale), result.getLocalized().get(locale));
      }
    }
  }

  private DescriptionEntity createDescription(String refId) {
    DescriptionEntity desc = new DescriptionEntity();
    desc.setReferenceId(refId);
    DescriptionState state = new DescriptionState("testName", "testDesc");
    desc.setState(state);
    Map<String, DescriptionState> localized = new HashMap<String, DescriptionState>();
    localized.put(I18N.toTagIdentifier(Locale.FRENCH), new DescriptionState("testName1", "testDesc1"));
    localized.put(I18N.toTagIdentifier(Locale.ENGLISH), new DescriptionState("testName2", "testDesc2"));
    desc.setLocalized(localized);
    return desc;
  }

}
