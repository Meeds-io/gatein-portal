package org.exoplatform.portal.config;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.*;

import org.junit.Test;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.*;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

public class DynamicPortalLayoutMatcherPluginTest {

  private static final String PORTAL_SITE_NAME = "testPortal";

  private static final String GROUP_SITE_NAME  = "testGroup";

  private static final String USER_SITE_NAME   = "testUser";

  private InputStream         portalLayoutInputStream;

  @Test
  public void testInitParams() {
    boolean enabled = true;
    boolean useCurrentPortalLayout = true;
    String layoutTemplatePath = "layoutPath";
    DynamicPortalLayoutMatcher matcher = mock(DynamicPortalLayoutMatcher.class);

    DynamicPortalLayoutMatcherPlugin dynamicPortalLayoutMatcherPlugin = mockPlugin(enabled,
                                                                                   useCurrentPortalLayout,
                                                                                   layoutTemplatePath,
                                                                                   matcher);

    assertEquals(matcher, dynamicPortalLayoutMatcherPlugin.getDynamicLayoutMatcher());
    assertEquals(enabled, dynamicPortalLayoutMatcherPlugin.isEnabled());
    assertEquals(useCurrentPortalLayout, dynamicPortalLayoutMatcherPlugin.isUseCurrentPortalLayout());
    assertEquals(layoutTemplatePath, dynamicPortalLayoutMatcherPlugin.getLayoutTemplatePath());
    assertEquals(layoutTemplatePath, dynamicPortalLayoutMatcherPlugin.getLayoutTemplatePath());

    enabled = !enabled;
    useCurrentPortalLayout = !useCurrentPortalLayout;
    dynamicPortalLayoutMatcherPlugin = mockPlugin(enabled,
                                                  useCurrentPortalLayout,
                                                  layoutTemplatePath,
                                                  matcher);

    assertEquals(matcher, dynamicPortalLayoutMatcherPlugin.getDynamicLayoutMatcher());
    assertEquals(enabled, dynamicPortalLayoutMatcherPlugin.isEnabled());
    assertEquals(useCurrentPortalLayout, dynamicPortalLayoutMatcherPlugin.isUseCurrentPortalLayout());
    assertEquals(layoutTemplatePath, dynamicPortalLayoutMatcherPlugin.getLayoutTemplatePath());
    assertEquals(layoutTemplatePath, dynamicPortalLayoutMatcherPlugin.getLayoutTemplatePath());
  }

  @Test
  public void testPluginInit() {
    DynamicPortalLayoutMatcher matcher = mock(DynamicPortalLayoutMatcher.class);

    DynamicPortalLayoutMatcherPlugin dynamicPortalLayoutMatcherPlugin = mockPlugin(true,
                                                                                   true,
                                                                                   null,
                                                                                   matcher);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

    dynamicPortalLayoutMatcherPlugin.init(configurationManager);
    assertTrue(dynamicPortalLayoutMatcherPlugin.isInitialized());
    assertNull(dynamicPortalLayoutMatcherPlugin.getLayoutTemplate());

    dynamicPortalLayoutMatcherPlugin = mockPlugin(true,
                                                  true,
                                                  "NonExistingPath",
                                                  matcher);
    dynamicPortalLayoutMatcherPlugin.init(configurationManager);
    assertFalse(dynamicPortalLayoutMatcherPlugin.isEnabled());
    assertTrue(dynamicPortalLayoutMatcherPlugin.isInitialized());
    assertNull(dynamicPortalLayoutMatcherPlugin.getLayoutTemplate());

    try {
      when(configurationManager.getInputStream("NonExistingPath")).thenThrow(new IllegalStateException("Just Testing, no bug !") {
        private static final long serialVersionUID = -3930575563406989213L;

        @Override
        public StackTraceElement[] getStackTrace() {
          return new StackTraceElement[0];
        }
      });
    } catch (Exception e) {
      throw new AssertionError("Error while mocking configurationManager", e);
    }

    dynamicPortalLayoutMatcherPlugin = mockPlugin(true,
                                                  true,
                                                  "NonExistingPath",
                                                  matcher);
    dynamicPortalLayoutMatcherPlugin.init(configurationManager);
    assertFalse(dynamicPortalLayoutMatcherPlugin.isEnabled());
    assertTrue(dynamicPortalLayoutMatcherPlugin.isInitialized());
    assertNull(dynamicPortalLayoutMatcherPlugin.getLayoutTemplate());

    try {
      reset(configurationManager);
      when(configurationManager.getInputStream(eq("ExistingPath"))).thenReturn(getPortalLayoutInputStream());
    } catch (Exception e) {
      throw new AssertionError("Error while mocking configurationManager", e);
    }

    dynamicPortalLayoutMatcherPlugin = mockPlugin(true,
                                                  true,
                                                  "ExistingPath",
                                                  matcher);
    dynamicPortalLayoutMatcherPlugin.init(configurationManager);
    assertTrue(dynamicPortalLayoutMatcherPlugin.isEnabled());
    assertTrue(dynamicPortalLayoutMatcherPlugin.isInitialized());
    assertNotNull(dynamicPortalLayoutMatcherPlugin.getLayoutTemplate());
    assertNotNull(dynamicPortalLayoutMatcherPlugin.getLayoutTemplate().getChildren());
    assertEquals(5, dynamicPortalLayoutMatcherPlugin.getLayoutTemplate().getChildren().size());
  }

  @Test
  public void testGetDynamicLayout() {
    DynamicPortalLayoutMatcher matcher = mock(DynamicPortalLayoutMatcher.class);
    DynamicPortalLayoutMatcherPlugin dynamicPortalLayoutMatcherPlugin = mockPlugin(true,
                                                                                   true,
                                                                                   "ExistingPath",
                                                                                   matcher);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    try {
      reset(configurationManager);
      when(configurationManager.getInputStream(eq("ExistingPath"))).thenReturn(getPortalLayoutInputStream());
    } catch (Exception e) {
      throw new AssertionError("Error while mocking configurationManager", e);
    }
    dynamicPortalLayoutMatcherPlugin.init(configurationManager);

    try {
      dynamicPortalLayoutMatcherPlugin.getPortalConfigWithDynamicLayout(null, null, null);
      fail("Should throw IllegalArgumentException when siteKey is null");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    PortalConfig result = dynamicPortalLayoutMatcherPlugin.getPortalConfigWithDynamicLayout(SiteKey.group(GROUP_SITE_NAME),
                                                                                            null,
                                                                                            null);
    assertNull(result);

    PortalConfig userPortalConfig = new PortalConfig(SiteType.USER.getName(), USER_SITE_NAME);
    userPortalConfig.useMetaPortalLayout();
    userPortalConfig.setDefaultLayout(false);
    result = dynamicPortalLayoutMatcherPlugin.getPortalConfigWithDynamicLayout(SiteKey.user(USER_SITE_NAME),
                                                                               userPortalConfig,
                                                                               null);
    assertNull(result);

    PortalConfig sitePortalConfig = new PortalConfig(SiteType.PORTAL.getName(), PORTAL_SITE_NAME);
    result = dynamicPortalLayoutMatcherPlugin.getPortalConfigWithDynamicLayout(SiteKey.user(USER_SITE_NAME),
                                                                               userPortalConfig,
                                                                               sitePortalConfig);
    assertNull(result);

    when(matcher.matches(SiteKey.user(USER_SITE_NAME), PORTAL_SITE_NAME)).thenReturn(true);
    result = dynamicPortalLayoutMatcherPlugin.getPortalConfigWithDynamicLayout(SiteKey.user(USER_SITE_NAME),
                                                                               userPortalConfig,
                                                                               sitePortalConfig);
    assertNotNull(result);
    assertEquals(userPortalConfig.getType(), result.getType());
    assertEquals(userPortalConfig.getName(), result.getName());
    assertNotEquals(sitePortalConfig.getPortalLayout(), result.getPortalLayout());
    assertEquals(sitePortalConfig.getPortalLayout().getChildren().size(), result.getPortalLayout().getChildren().size());
    assertNotEquals(userPortalConfig.getPortalLayout().getChildren().size(), result.getPortalLayout().getChildren().size());

    dynamicPortalLayoutMatcherPlugin = mockPlugin(true,
                                                  false,
                                                  "ExistingPath",
                                                  matcher);
    try {
      reset(configurationManager);
      when(configurationManager.getInputStream(eq("ExistingPath"))).thenReturn(getPortalLayoutInputStream());
    } catch (Exception e) {
      throw new AssertionError("Error while mocking configurationManager", e);
    }
    dynamicPortalLayoutMatcherPlugin.init(configurationManager);
    assertNotNull(dynamicPortalLayoutMatcherPlugin.getLayoutTemplate());

    when(matcher.matches(SiteKey.user(USER_SITE_NAME), PORTAL_SITE_NAME)).thenReturn(true);

    result = dynamicPortalLayoutMatcherPlugin.getPortalConfigWithDynamicLayout(SiteKey.user(USER_SITE_NAME),
                                                                               userPortalConfig,
                                                                               sitePortalConfig);
    assertNotNull(result);
    assertEquals(userPortalConfig.getType(), result.getType());
    assertEquals(userPortalConfig.getName(), result.getName());
    assertEquals(5, result.getPortalLayout().getChildren().size());
  }

  private InputStream getPortalLayoutInputStream() {
    try {
      if (portalLayoutInputStream == null) {
        try (InputStream is = getClass().getResourceAsStream("/org/exoplatform/portal/mop/user/portal/test/portal.xml")) {
          portalLayoutInputStream = new ByteArrayInputStream(IOUtil.getStreamContentAsBytes(is));
        }
      } else {
        portalLayoutInputStream.reset();
      }
    } catch (Exception e) {
      throw new AssertionError("Error reading portal layout file content", e);
    }
    return portalLayoutInputStream;
  }

  private DynamicPortalLayoutMatcherPlugin mockPlugin(boolean enabled,
                                                      boolean useCurrentPortalLayout,
                                                      String layoutTemplatePath,
                                                      DynamicPortalLayoutMatcher matcher) {
    InitParams params = new InitParams();

    ValueParam enabledParam = new ValueParam();
    enabledParam.setName("enabled");
    enabledParam.setValue(String.valueOf(enabled));
    params.addParameter(enabledParam);

    ValueParam useCurrentPortalLayoutParam = new ValueParam();
    useCurrentPortalLayoutParam.setName("useCurrentPortalLayout");
    useCurrentPortalLayoutParam.setValue(String.valueOf(useCurrentPortalLayout));
    params.addParameter(useCurrentPortalLayoutParam);

    ValueParam layoutTemplatePathParam = new ValueParam();
    layoutTemplatePathParam.setName("layoutTemplatePath");
    layoutTemplatePathParam.setValue(layoutTemplatePath);
    params.addParameter(layoutTemplatePathParam);

    ObjectParameter matcherParam = new ObjectParameter();
    matcherParam.setName("matcher");
    matcherParam.setObject(matcher);
    params.addParameter(matcherParam);

    return new DynamicPortalLayoutMatcherPlugin(params);
  }

}
