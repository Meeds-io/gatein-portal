/*
 * Copyright (C) 2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.jdbc.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.gatein.common.util.Tools;

import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.jdbc.dao.DescriptionDAO;
import org.exoplatform.portal.mop.Described;
import org.exoplatform.portal.mop.Described.State;
import org.exoplatform.portal.mop.description.DescriptionService;

@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.portal.jdbc.test.configuration.xml") })
public class TestDescriptionService extends AbstractKernelTest {

  private DescriptionService service;
  
  private DescriptionDAO descDAO;
  
  private String id = "testId";
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    //
    PortalContainer container = PortalContainer.getInstance();
    service = container.getComponentInstanceOfType(DescriptionService.class);
    descDAO = getContainer().getComponentInstanceOfType(DescriptionDAO.class);

    //
    begin();
  }

  @Override
  protected void tearDown() throws Exception {
    descDAO.deleteAll();
    end();
    super.tearDown();
  }

  public void testResolveNoDescription() throws Exception {
    assertEquals(null, service.resolveDescription("notExists", null, Locale.ENGLISH));
  }

  public void testResolveLocalizedDescription() throws Exception {
    Map<Locale, Described.State> descriptions = new HashMap<Locale, Described.State>();
    descriptions.put(Locale.ENGLISH, new State("name_en", null));
    descriptions.put(Locale.UK, new State("name_en_GB", null));
    service.setDescriptions(id, descriptions);

    //
    assertEquals(null, service.resolveDescription(id, null, Locale.GERMAN));
    assertEquals(null, service.resolveDescription(id, null, new Locale("", "GB")));
    assertEquals(new Described.State("name_en", null), service.resolveDescription(id, Locale.ENGLISH, Locale.GERMAN));
    assertEquals(new Described.State("name_en_GB", null), service.resolveDescription(id, Locale.UK, Locale.GERMAN));
    assertEquals(new Described.State("name_en", null), service.resolveDescription(id, Locale.US, Locale.GERMAN));
    assertEquals(new Described.State("name_en", null), service.resolveDescription(id, null, Locale.ENGLISH));
    assertEquals(new Described.State("name_en", null), service.resolveDescription(id, null, Locale.US));
    assertEquals(new Described.State("name_en_GB", null), service.resolveDescription(id, null, Locale.UK));
  }

  public void testResolveDescription() throws Exception {
    Map<Locale, Described.State> descriptions = new HashMap<Locale, Described.State>();
    descriptions.put(Locale.ENGLISH, new State("name_en", null));
    descriptions.put(Locale.UK, new State("name_en_GB", null));

    service.setDescription(id, new State("name", null));
    service.setDescriptions(id, descriptions);

    //
    assertEquals(null, service.resolveDescription(id, null, Locale.GERMAN));
    assertEquals(new Described.State("name_en", null), service.resolveDescription(id, Locale.ENGLISH, Locale.GERMAN));
    assertEquals(new Described.State("name_en_GB", null), service.resolveDescription(id, Locale.UK, Locale.GERMAN));
    assertEquals(new Described.State("name_en", null), service.resolveDescription(id, Locale.US, Locale.GERMAN));
    assertEquals(new Described.State("name_en", null), service.resolveDescription(id, null, Locale.ENGLISH));
    assertEquals(new Described.State("name_en", null), service.resolveDescription(id, null, Locale.US));
    assertEquals(new Described.State("name_en_GB", null), service.resolveDescription(id, null, Locale.UK));
  }

  public void testGetDefaultDescription() throws Exception {
    service.setDescription(id, new State("foo_name", null));

    //
    assertEquals(new Described.State("foo_name", null), service.getDescription(id));
  }

  public void testRemoveDefaultDescription() throws Exception {
    service.setDescription(id, new State("foo_name", null));
    assertEquals(new Described.State("foo_name", null), service.getDescription(id));
    
    service.setDescription(id, null);
    assertNull(service.getDescription(id));
  }

  public void testSetLocalizedDescription() throws Exception {    
    service.setDescription(id, Locale.ENGLISH, new Described.State("foo_english", null));

    //
    assertEquals(new State("foo_english", null), service.getDescription(id, Locale.ENGLISH));
  }

  public void testSetInvalidLocaleDescription() throws Exception {
    try {
      service.setDescription(id, new Locale("", "GB"), new Described.State("foo_invalid", null));
      fail();
    } catch (IllegalArgumentException e) {
    }

    //
    try {
      service.setDescription(id, new Locale("en", "GB", "variant"), new Described.State("foo_invalid", null));
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  public void testAddLocalizedDescription() throws Exception {
    service.setDescription(id, Locale.ENGLISH, new Described.State("add_english", null));
    service.setDescription(id, Locale.FRENCH, new Described.State("add_french", null));

    //
    State desc = service.getDescription(id, Locale.ENGLISH);
    assertNotNull(desc);
    assertEquals("add_english", desc.getName());
    desc = service.getDescription(id, Locale.FRENCH);
    assertNotNull(desc);
    assertEquals("add_french", desc.getName());
  }

  public void testGetDescriptions() throws Exception {
    assertNull(service.getDescriptions(id));
    
    Map<Locale, Described.State> descriptions = new HashMap<Locale, Described.State>();
    descriptions.put(Locale.ENGLISH, new State("foo_english", null));
    descriptions.put(Locale.FRENCH, new State("foo_french", null));
    service.setDescriptions(id, descriptions);

    //
    Map<Locale, Described.State> description = service.getDescriptions(id);
    assertEquals(Tools.toSet(Locale.ENGLISH, Locale.FRENCH), description.keySet());
    assertEquals(new Described.State("foo_english", null), description.get(Locale.ENGLISH));
    assertEquals(new Described.State("foo_french", null), description.get(Locale.FRENCH));
  }

  public void testSetDescriptions() throws Exception {
    assertNull(service.getDescriptions(id));

    //
    Map<Locale, Described.State> description = new HashMap<Locale, Described.State>();
    description.put(Locale.ENGLISH, new Described.State("bar_english", null));
    description.put(Locale.FRENCH, new Described.State("bar_french", null));
    service.setDescriptions(id, description);

    //
    description = service.getDescriptions(id);
    assertEquals(Tools.toSet(Locale.ENGLISH, Locale.FRENCH), description.keySet());
    assertEquals(new Described.State("bar_english", null), description.get(Locale.ENGLISH));
    assertEquals(new Described.State("bar_french", null), description.get(Locale.FRENCH));

    //
    description = new HashMap<Locale, Described.State>();
    description.put(Locale.ENGLISH, new Described.State("bar_english_2", null));
    service.setDescriptions(id, description);

    //
    description = service.getDescriptions(id);
    assertEquals(Tools.toSet(Locale.ENGLISH), description.keySet());
    assertEquals(new Described.State("bar_english_2", null), description.get(Locale.ENGLISH));
  }

  public void testSetInvalidLocaleDescriptions() throws Exception {
    try {
      service.setDescriptions(id,
                          Collections.singletonMap(new Locale("", "GB"), new Described.State("bar_invalid", null)));
      fail();
    } catch (IllegalArgumentException e) {
    }
    try {
      service.setDescriptions(id,
                          Collections.singletonMap(new Locale("en", "GB", "variant"), new Described.State("bar_invalid", null)));
      fail();
    } catch (IllegalArgumentException e) {
    }
  }
}
