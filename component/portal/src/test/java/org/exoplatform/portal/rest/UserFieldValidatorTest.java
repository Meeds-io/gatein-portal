package org.exoplatform.portal.rest;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.resources.ResourceBundleService;

public class UserFieldValidatorTest {

  @BeforeClass
  public static void setup() {
    PortalContainer container = PortalContainer.getInstance();
    ResourceBundleService resourceBundleService = container.getComponentInstanceOfType(ResourceBundleService.class);
    if (resourceBundleService == null) {
      resourceBundleService = Mockito.mock(ResourceBundleService.class);
      container.registerComponentInstance(resourceBundleService);
    }
    ExoContainerContext.setCurrentContainer(container);
  }

  @Test
  public void testValidateFieldLength() {
    UserFieldValidator fieldValidator = new UserFieldValidator("field", false,false);
    assertNull(fieldValidator.validate(Locale.ENGLISH, "123"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, null));

    fieldValidator = new UserFieldValidator("field", false,false, 8, 10);
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "1234567"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "12345678"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "12345678901"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "1234567890"));
  }

  @Test
  public void testValidateUsername() {
    UserFieldValidator fieldValidator = new UserFieldValidator("field", true,false);
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "123"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "1aa"));
    assertNotNull(fieldValidator.validate(Locale.ENGLISH, "aaA"));
    assertNull(fieldValidator.validate(Locale.ENGLISH, "aaa"));
  }

  @Test
  public void testValidateFieldRegex() {
    System.setProperty("gatein.validators.fieldregexp.regexp", "[1-9]*");

    try {
      UserFieldValidator fieldValidator = new UserFieldValidator("fieldregexp", false,false);
      assertNull(fieldValidator.validate(Locale.ENGLISH, "123"));
      assertNotNull(fieldValidator.validate(Locale.ENGLISH, "1aa"));

      String message = "FORMAT_MESSAGE";
      System.setProperty("gatein.validators.fieldregexp.format.message", message);
      fieldValidator = new UserFieldValidator("fieldregexp", false,false);
      assertEquals(message, fieldValidator.validate(Locale.ENGLISH, "1aa"));
    } finally {
      System.setProperty("gatein.validators.fieldregexp.regexp", "");
    }
  }

}
