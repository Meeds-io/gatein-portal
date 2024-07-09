/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.portal.resource;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.xml.DocumentSource;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.component.test.web.ServletContextImpl;
import org.exoplatform.component.test.web.WebAppImpl;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.resource.config.tasks.PortalSkinTask;
import org.exoplatform.portal.resource.config.xml.SkinConfigParser;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.test.mocks.servlet.MockServletRequest;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.controller.QualifiedName;
import org.exoplatform.web.controller.router.Router;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.servlet.ServletContext;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-configuration.xml") })
public abstract class AbstractSkinServiceTest extends AbstractKernelTest {
  private static final String           DEFAULT_MODULE_NAME = "Core";

  // The value must be the same with the version property in
  // META-INF/maven/org.exoplatform.gatein.portal/exo.portal.component.web.resources/pom.properties
  protected static final String         ASSETS_VERSION = "PORTAL-VERSION";

  protected SkinService                 skinService;

  protected ControllerContext           controllerCtx;

  protected List<SkinKey>               skinsToDelete  = new ArrayList<>();

  private static ServletContext         mockServletContext;

  protected static MockResourceResolver resResolver;

  abstract boolean isDevelopingMode();

  abstract Router getRouter();

  abstract boolean setUpTestEnvironment();

  abstract void touchSetUp();

  @Override
  protected void setUp() throws Exception {
    // Set running mode at starting up
    PropertyManager.setProperty(PropertyManager.DEVELOPING, String.valueOf(isDevelopingMode()));

    PortalContainer portalContainer = getContainer();

    skinService = (SkinService) portalContainer.getComponentInstanceOfType(SkinService.class);

    controllerCtx = getControllerContext();

    if (setUpTestEnvironment()) {
      URL base = AbstractSkinServiceTest.class.getClassLoader().getResource("mockwebapp");
      File f = new File(base.toURI());
      assertTrue(f.exists());
      assertTrue(f.isDirectory());
      mockServletContext = new ServletContextImpl(f, "/mockwebapp", "mockwebapp");
      skinService.registerContext(new WebAppImpl(mockServletContext, Thread.currentThread().getContextClassLoader()));

      resResolver = new MockResourceResolver();
      skinService.addResourceResolver(resResolver);

      URL url = mockServletContext.getResource("/gatein-resources.xml");
      SkinConfigParser.processConfigResource(DocumentSource.create(url), skinService, mockServletContext);
      //
      touchSetUp();
    }
  }

  protected void tearDown() throws Exception {
    resResolver.reset();
    for (SkinKey key : skinsToDelete) {
      skinService.removeSupportedSkin(key.getName());
      skinService.removeSkin(key);
    }
    skinsToDelete.clear();
    skinService.reloadSkins();
  }

  public void testInitializing() throws Exception {
    assertEquals(1, skinService.getAvailableSkinNames().size());
    assertTrue(skinService.getAvailableSkinNames().contains("TestSkin"));
    assertTrue(skinService.mainResolver.resolvers.contains(resResolver));
  }

  public void testPortalSkin() {
    String contextPath = mockServletContext.getContextPath();

    SkinConfig portalSkin = skinService.getPortalSkin(DEFAULT_MODULE_NAME, "TestSkin");
    assertNotNull(portalSkin);
    assertEquals(DEFAULT_MODULE_NAME, portalSkin.getModule());
    assertEquals(contextPath + "/skin/core/Stylesheet.css", portalSkin.getCSSPath());

    portalSkin = skinService.getPortalSkin("Module2", "TestSkin");
    assertNotNull(portalSkin);
    assertEquals("Module2", portalSkin.getModule());
    assertEquals(contextPath + "/skin/module2/Stylesheet.css", portalSkin.getCSSPath());

    portalSkin = skinService.getPortalSkin("Module3", "TestSkin");
    assertNotNull(portalSkin);
    assertEquals("Module3", portalSkin.getModule());
    assertEquals(contextPath + "/skin/module3/Stylesheet.css", portalSkin.getCSSPath());

    portalSkin = skinService.getPortalSkin("Module1", "TestSkin");
    assertNotNull(portalSkin);
    assertEquals("Module1", portalSkin.getModule());
    assertEquals(contextPath + "/skin/module1/Stylesheet.css", portalSkin.getCSSPath());
  }

  public void testRemovePortalSkin() {
    String skinName = "TestSkin";
    String module = "ToDeletePortalSkin";
    String cssPath = mockServletContext.getContextPath() + "/skin/core/Stylesheet.css";

    SkinConfig portalSkin = skinService.getPortalSkin(module, skinName);
    assertNull(portalSkin);

    skinService.addPortalSkin(module, skinName, cssPath);

    portalSkin = skinService.getPortalSkin(module, skinName);
    assertNotNull(portalSkin);
    assertEquals(cssPath, portalSkin.getCSSPath());

    skinService.removePortalSkin(module, skinName);
    portalSkin = skinService.getPortalSkin(module, skinName);
    assertNull(portalSkin);
  }

  public void testPortalSkinAndPriority() {
    Collection<SkinConfig> portalSkinConfigs = skinService.getPortalSkins("TestSkin");
    String contextPath = mockServletContext.getContextPath();
    assertNotNull(portalSkinConfigs);
    assertEquals(4, portalSkinConfigs.size());

    SkinConfig[] array = new SkinConfig[4];
    portalSkinConfigs.toArray(array);

    SkinConfig portalSkin = array[0];
    assertNotNull(portalSkin);
    assertEquals(DEFAULT_MODULE_NAME, portalSkin.getModule());
    assertEquals(contextPath + "/skin/core/Stylesheet.css", portalSkin.getCSSPath());

    portalSkin = array[1];
    assertNotNull(portalSkin);
    assertEquals("Module2", portalSkin.getModule());
    assertEquals(contextPath + "/skin/module2/Stylesheet.css", portalSkin.getCSSPath());

    portalSkin = array[2];
    assertNotNull(portalSkin);
    assertEquals("Module3", portalSkin.getModule());
    assertEquals(contextPath + "/skin/module3/Stylesheet.css", portalSkin.getCSSPath());

    portalSkin = array[3];
    assertNotNull(portalSkin);
    assertEquals("Module1", portalSkin.getModule());
    assertEquals(contextPath + "/skin/module1/Stylesheet.css", portalSkin.getCSSPath());
  }

  public void testPortalSkinVisitor() {
    String contextPath = mockServletContext.getContextPath();
    Collection<SkinConfig> portalSkinConfigs = skinService.findSkins(new SkinVisitor() {
      @Override
      public Collection<SkinConfig> getSkins(Set<Entry<SkinKey, SkinConfig>> portalSkins,
                                             Set<Entry<SkinKey, SkinConfig>> skinConfigs) {
        Collection<SkinConfig> list = new HashSet<>();
        for (Entry<SkinKey, SkinConfig> entry : portalSkins) {
          if (entry.getKey().getModule().equals(DEFAULT_MODULE_NAME)) {
            list.add(entry.getValue());
          }
        }
        return list;
      }
    });

    assertNotNull(portalSkinConfigs);
    assertEquals(1, portalSkinConfigs.size());
    SkinConfig[] arr = portalSkinConfigs.toArray(new SkinConfig[1]);
    SkinConfig portalSkin = arr[0];
    assertNotNull(portalSkin);
    assertEquals(DEFAULT_MODULE_NAME, portalSkin.getModule());
    assertEquals(contextPath + "/skin/core/Stylesheet.css", portalSkin.getCSSPath());
  }

  public void testPortletSkin() {
    SkinConfig portletSkin = skinService.getSkin("mockwebapp/FirstPortlet", "TestSkin");
    String contextPath = mockServletContext.getContextPath();
    assertNotNull(portletSkin);
    assertNotNull(portletSkin.getAdditionalModules());
    assertEquals(1, portletSkin.getAdditionalModules().size());
    assertEquals("AdditionalModule", portletSkin.getAdditionalModules().get(0));
    assertEquals(contextPath + "/skin/FirstPortlet.css", portletSkin.getCSSPath());

    portletSkin = skinService.getSkin("mockwebapp/SecondPortlet", "TestSkin");
    assertNotNull(portletSkin);
    assertNull(portletSkin.getAdditionalModules());
    assertEquals(contextPath + "/skin/SecondPortlet.css", portletSkin.getCSSPath());
  }

  public void testThemes() {
    Map<String, Set<String>> themeStyles = skinService.getPortletThemes();
    Set<String> themes = themeStyles.get("Simple");
    assertNotNull(themes);
    assertTrue(themes.contains("SimpleBlue"));
    assertTrue(themes.contains("SimpleViolet"));

    assertNotNull(themeStyles.get("VistaStyle"));
  }

  public void testCustomSkinKey() {
    skinService.addSkin("jcr/foo", "bar", "/path/to/customkey.css", -1, false);
    skinsToDelete.add(new SkinKey("jcr/foo", "bar"));
    SkinConfig skin = skinService.getSkin("foo", "bar");
    assertNull(skin);
    skin = skinService.getSkin("jcr/foo", "bar");
    assertNotNull(skin);
    assertEquals("/path/to/customkey.css", skin.getCSSPath());

    Collection<SkinConfig> list = skinService.findSkins(new SkinVisitor() {
      @Override
      public Collection<SkinConfig> getSkins(Set<Entry<SkinKey, SkinConfig>> portalSkins,
                                             Set<Entry<SkinKey, SkinConfig>> skinConfigs) {
        Collection<SkinConfig> list = new HashSet<SkinConfig>();
        for (Entry<SkinKey, SkinConfig> entry : portalSkins) {
          if (entry.getKey().getModule().startsWith("jcr/")) {
            list.add(entry.getValue());
          }
        }
        for (Entry<SkinKey, SkinConfig> entry : skinConfigs) {
          if (entry.getKey().getModule().startsWith("jcr/")) {
            list.add(entry.getValue());
          }
        }
        return list;
      }
    });

    assertNotNull(list);
    assertEquals(1, list.size());
    SkinConfig next = list.iterator().next();
    assertEquals("/path/to/customkey.css", next.getCSSPath());
  }

  ControllerContext getControllerContext() {
    try {
      return newControllerContext(getRouter());
    } catch (Exception e) {
      throw new IllegalArgumentException("The controller context is not initialized properly", e);
    }
  }

  public static ControllerContext newControllerContext(Router router) {
    return newControllerContext(router, "/portal");
  }

  public static ControllerContext newControllerContext(Router router, String requestURI) {
    try {
      MockServletRequest request = new MockServletRequest(null,
                                                          new URL("http://localhost" + requestURI),
                                                          "/portal",
                                                          null,
                                                          false);
      String portalPath = request.getRequestURI().substring(request.getContextPath().length());

      //
      Iterator<Map<QualifiedName, String>> matcher = router.matcher(portalPath, request.getParameterMap());
      Map<QualifiedName, String> parameters = null;
      if (matcher.hasNext()) {
        parameters = matcher.next();
      }
      return new ControllerContext(null, router, request, null, parameters);
    } catch (MalformedURLException e) {
      return null;
    }
  }
}
