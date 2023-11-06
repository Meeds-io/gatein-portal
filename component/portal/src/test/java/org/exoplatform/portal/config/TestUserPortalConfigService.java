/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.portal.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.exoplatform.portal.mop.SiteFilter;
import org.gatein.common.util.Tools;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PageBody;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.mop.storage.SiteStorage;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;

import junit.framework.AssertionFailedError;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration-local.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "org/exoplatform/portal/config/conf/configuration.xml")})
public class TestUserPortalConfigService extends AbstractConfigTest {

  /** . */
  private UserPortalConfigService userPortalConfigSer_;

  /** . */
  private OrganizationService     orgService_;

  /** . */
  private LayoutService           layoutService;

  /** . */
  private PageStorage             pageStorage;

  /** . */
  private Authenticator           authenticator;

  /** . */
  private ListenerService         listenerService;

  /** . */
  private LinkedList<Event>       events;

  /** . */
  private boolean                 registered;

  /** . */
  private SiteStorage             siteStorage;

  public TestUserPortalConfigService() {
    registered = false;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    Listener listener = new Listener() {
      @Override
      public void onEvent(Event event) throws Exception {
        events.add(event);
      }
    };

    PortalContainer container = getContainer();
    userPortalConfigSer_ = (UserPortalConfigService) container.getComponentInstanceOfType(UserPortalConfigService.class);
    orgService_ = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
    authenticator = (Authenticator) container.getComponentInstanceOfType(Authenticator.class);
    listenerService = (ListenerService) container.getComponentInstanceOfType(ListenerService.class);
    events = new LinkedList<Event>();
    layoutService = container.getComponentInstanceOfType(LayoutService.class);
    pageStorage = container.getComponentInstanceOfType(PageStorage.class);
    siteStorage = container.getComponentInstanceOfType(SiteStorage.class);

    // Register only once for all unit tests
    if (!registered) {
      // I'm using this due to crappy design of
      // org.exoplatform.services.listener.ListenerService
      listenerService.addListener(LayoutService.PAGE_CREATED, listener);
      listenerService.addListener(EventType.PAGE_DESTROYED, listener);
      listenerService.addListener(EventType.PAGE_UPDATED, listener);
      listenerService.addListener(EventType.NAVIGATION_CREATED, listener);
      listenerService.addListener(EventType.NAVIGATION_DESTROYED, listener);
      listenerService.addListener(EventType.NAVIGATION_UPDATED, listener);
    }
  }

  private static Map<String, UserNavigation> toMap(UserPortal cfg) {
    return toMap(cfg.getNavigations());
  }

  private static Map<String, UserNavigation> toMap(List<UserNavigation> navigations) {
    Map<String, UserNavigation> map = new HashMap<String, UserNavigation>();
    for (UserNavigation nav : navigations) {
      map.put(nav.getKey().getType().getName() + "::" + nav.getKey().getName(), nav);
    }
    return map;
  }

  public void testUpdatePortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        assertEquals("en", portalCfg.getLocale());
        portalCfg.setLocale("fr");

        layoutService.save(portalCfg);

        userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
        portalCfg = userPortalCfg.getPortalConfig();
        assertEquals("fr", portalCfg.getLocale());
      }
    }.execute("root");
  }

  public void testEnforcedReimporting() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        assertEquals("en", portalCfg.getLocale());
        portalCfg.setLocale("fr");

        layoutService.save(portalCfg);

        userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
        portalCfg = userPortalCfg.getPortalConfig();
        assertEquals("fr", portalCfg.getLocale());

        // Re-import site config from configuration
        userPortalConfigSer_.start();

        userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
        portalCfg = userPortalCfg.getPortalConfig();
        assertEquals("en", portalCfg.getLocale());
      }
    }.execute("root");
  }

  public void testRootGetUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals("expected to have 4 navigations instead of " + navigations, 4, navigations.size());
        assertTrue(navigations.containsKey("portal::classic"));
        assertTrue(navigations.containsKey("group::/platform/administrators"));
        assertTrue(navigations.containsKey("group::/platform/users"));
        assertTrue(navigations.containsKey("group::/organization/management/executive-board"));
      }
    }.execute("root");
  }

  public void testGetAllPortalNames() {
    new UnitTest() {
      public void execute() throws Exception {
        assertTrue(userPortalConfigSer_.getSiteNames(SiteType.PORTAL, 0, 0).contains("system"));

        String originalGlobalPortal = userPortalConfigSer_.globalPortal_;
        userPortalConfigSer_.globalPortal_ = "system";
        try {
          assertFalse(userPortalConfigSer_.getSiteNames(SiteType.PORTAL, 0, 0).contains("system"));
        } finally {
          userPortalConfigSer_.globalPortal_ = originalGlobalPortal;
        }
      }
    }.execute("root");
  }

  public void testGetSites() {
    new UnitTest() {
      public void execute() throws Exception {
        userPortalConfigSer_.removeUserPortalConfig("jazz");
        assertEquals(4, userPortalConfigSer_.getUserPortalDisplayedSites().size());

        String originalGlobalPortal = userPortalConfigSer_.globalPortal_;
        userPortalConfigSer_.globalPortal_ = "system";
        try {
          assertEquals(3, userPortalConfigSer_.getUserPortalDisplayedSites().size());
        } finally {
          userPortalConfigSer_.globalPortal_ = originalGlobalPortal;
        }
      }
    }.execute("root");
  }

  public void testGetGlobalUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "john");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertTrue(navigations.containsKey("portal::classic"));
        assertFalse(navigations.containsKey("portal::" + userPortalConfigSer_.getGlobalPortal()));

        String originalGlobalPortal = userPortalConfigSer_.globalPortal_;
        userPortalConfigSer_.globalPortal_ = "system";
        try {
          userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
          userPortal = userPortalCfg.getUserPortal();
          navigations = toMap(userPortal);

          assertTrue(navigations.containsKey("portal::classic"));
          assertTrue(navigations.containsKey("portal::system"));
        } finally {
          userPortalConfigSer_.globalPortal_ = originalGlobalPortal;
        }
      }
    }.execute("root");
  }

  public void testGetGlobalUserNodes() {
    new UnitTest() {
      public void execute() throws Exception {
        UserNodeFilterConfig.Builder filterConfigBuilder = UserNodeFilterConfig.builder();
        filterConfigBuilder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
        filterConfigBuilder.withTemporalCheck();
        UserNodeFilterConfig filterConfig = filterConfigBuilder.build();

        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "john");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        Collection<UserNode> nodes = userPortal.getNodes(SiteType.PORTAL, Scope.ALL, filterConfig);
        assertNotNull(nodes);

        int initialNodesSize = nodes.size();
        assertTrue(initialNodesSize > 0);

        String originalGlobalPortal = userPortalConfigSer_.globalPortal_;
        userPortalConfigSer_.globalPortal_ = "systemtest";
        try {
          userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
          portalCfg = userPortalCfg.getPortalConfig();
          userPortal = userPortalCfg.getUserPortal();
          nodes = userPortal.getNodes(SiteType.PORTAL, Scope.ALL, filterConfig);
          assertNotNull(nodes);

          assertEquals(initialNodesSize + 1, nodes.size());
          UserNode homeNode = nodes.iterator().next();
          assertEquals("home", homeNode.getName());
          assertEquals("classic", homeNode.getNavigation().getKey().getName());

          UserNode lastUserNode = new ArrayList<>(nodes).get(initialNodesSize);
          assertEquals("systemhome", lastUserNode.getName());
          assertEquals("systemtest", lastUserNode.getNavigation().getKey().getName());
        } finally {
          userPortalConfigSer_.globalPortal_ = originalGlobalPortal;
        }
      }
    }.execute("root");
  }
  
  public void testGetUserNodesGlobalNotIncluded() {
    new UnitTest() {
      public void execute() throws Exception {
        UserNodeFilterConfig.Builder filterConfigBuilder = UserNodeFilterConfig.builder();
        filterConfigBuilder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
        filterConfigBuilder.withTemporalCheck();
        UserNodeFilterConfig filterConfig = filterConfigBuilder.build();

        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "john");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        Collection<UserNode> nodes = userPortal.getNodes(SiteType.PORTAL, Scope.ALL, filterConfig, false);
        assertNotNull(nodes);

        int initialNodesSize = nodes.size();
        assertTrue(initialNodesSize > 0);

        String originalGlobalPortal = userPortalConfigSer_.globalPortal_;
        userPortalConfigSer_.globalPortal_ = "systemtest";
        try {
          userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
          portalCfg = userPortalCfg.getPortalConfig();
          userPortal = userPortalCfg.getUserPortal();
          nodes = userPortal.getNodes(SiteType.PORTAL, Scope.ALL, filterConfig, false);
          assertNotNull(nodes);

          assertEquals(initialNodesSize, nodes.size());
          UserNode homeNode = nodes.iterator().next();
          assertEquals("home", homeNode.getName());
          assertEquals("classic", homeNode.getNavigation().getKey().getName());

          UserNode lastUserNode = new ArrayList<>(nodes).get(nodes.size() - 1);
          assertEquals("webexplorer", lastUserNode.getName());
          assertEquals("classic", lastUserNode.getNavigation().getKey().getName());
        } finally {
          userPortalConfigSer_.globalPortal_ = originalGlobalPortal;
        }
      }
    }.execute("root");
  }

  public void testGetGlobalUserNode() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "john");
        assertNotNull(userPortalCfg);

        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);

        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal);

        UserNodeFilterConfig.Builder filterConfigBuilder = UserNodeFilterConfig.builder();
        filterConfigBuilder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL);
        filterConfigBuilder.withTemporalCheck();
        UserNodeFilterConfig filterConfig = filterConfigBuilder.build();

        UserNode userNode = userPortal.resolvePath(filterConfig, "systemhome");
        assertNotNull(userNode);
        assertEquals("home", userNode.getName());

        String originalGlobalPortal = userPortalConfigSer_.globalPortal_;
        userPortalConfigSer_.globalPortal_ = "systemtest";
        try {
          userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "john");
          portalCfg = userPortalCfg.getPortalConfig();
          userPortal = userPortalCfg.getUserPortal();
          userNode = userPortal.resolvePath(filterConfig, "systemhome");

          assertNotNull(userNode);
          assertEquals("systemhome", userNode.getName());
        } finally {
          userPortalConfigSer_.globalPortal_ = originalGlobalPortal;
        }
      }
    }.execute("root");
  }

  public void testJohnGetUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "john");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals("expected to have 4 navigations instead of " + navigations, 4, navigations.size());
        assertTrue(navigations.containsKey("portal::classic"));
        assertTrue(navigations.containsKey("group::/platform/administrators"));
        assertTrue(navigations.containsKey("group::/platform/users"));
      }
    }.execute("john");
  }

  public void testMaryGetUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "mary");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals(2, navigations.size());
        assertTrue(navigations.containsKey("portal::classic"));
        assertTrue(navigations.containsKey("group::/platform/users"));
      }
    }.execute("mary");
  }

  public void testGuestGetUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", null);
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("classic", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals("" + navigations, 1, navigations.size());
        assertTrue(navigations.containsKey("portal::classic"));
      }
    }.execute(null);
  }

  public void testGetDefaultPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        PortalConfig portalConfig = userPortalConfigSer_.getDefaultPortalConfig();
        assertNotNull(portalConfig);
        assertEquals(PortalConfig.PORTAL_TYPE, portalConfig.getType());
        assertEquals("classic", portalConfig.getName());
      }
    }.execute(null);
  }

  public void testNavigationOrder() {
    new UnitTest() {
      public void execute() throws Exception {
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("classic", "root");
        UserPortal userPortal = userPortalCfg.getUserPortal();
        List<UserNavigation> navigations = userPortal.getNavigations();
        assertEquals("expected to have 4 navigations instead of " + navigations, 4, navigations.size());
        assertEquals("classic", navigations.get(0).getKey().getName());
        assertEquals("/platform/administrators", navigations.get(1).getKey().getName());
        assertEquals("/organization/management/executive-board", navigations.get(2).getKey().getName());
        assertEquals("/platform/users", navigations.get(3).getKey().getName());
      }
    }.execute("root");
  }

  public void testCreateUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        userPortalConfigSer_.createUserPortalConfig(PortalConfig.PORTAL_TYPE, "jazz", "test");
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("jazz", "root");
        assertNotNull(userPortalCfg);
        PortalConfig portalCfg = userPortalCfg.getPortalConfig();
        assertNotNull(portalCfg);
        assertEquals(PortalConfig.PORTAL_TYPE, portalCfg.getType());
        assertEquals("jazz", portalCfg.getName());
        UserPortal userPortal = userPortalCfg.getUserPortal();
        assertNotNull(userPortal.getNavigations());
        Map<String, UserNavigation> navigations = toMap(userPortal);
        assertEquals("expected to have 4 navigations instead of " + navigations, 4, navigations.size());
        assertTrue(navigations.containsKey("portal::jazz"));
        assertTrue(navigations.containsKey("group::/platform/administrators"));
        assertTrue(navigations.containsKey("group::/organization/management/executive-board"));
        assertTrue(navigations.containsKey("group::/platform/users"));

        queryPage();
      }

      private void queryPage() {
        try {
          pageStorage.findPages(0, 10, SiteType.PORTAL, null, null, null);
        } catch (Exception ex) {
          assertTrue("Exception while querying pages with new portal", false);
        }
      }

    }.execute("root");
  }

  public void testCreateGroupPortalConfigWithDefaultTemplate() {
    new UnitTest() {
      public void execute() throws Exception {
        String originalDefaultGroupSiteTemplate = userPortalConfigSer_.getDefaultGroupSiteTemplate();

        // Test creating group site with default template having a predefined
        // group.xml
        userPortalConfigSer_.setDefaultGroupSiteTemplate("group");
        try {
          String groupId = "/groupTemplate101";
          userPortalConfigSer_.createGroupSite(groupId);

          PortalConfig groupPortalConfig = layoutService.getPortalConfig(PortalConfig.GROUP_TYPE, groupId);

          assertNotNull(groupPortalConfig);
          assertNotNull(groupPortalConfig.getPortalLayout());
          assertNotNull(groupPortalConfig.getPortalLayout().getChildren());
          assertEquals(4, groupPortalConfig.getPortalLayout().getChildren().size());
          assertFalse(groupPortalConfig.isDefaultLayout());
        } finally {
          userPortalConfigSer_.setDefaultGroupSiteTemplate(originalDefaultGroupSiteTemplate);
        }

        // Test creating group site with not existing template
        userPortalConfigSer_.setDefaultGroupSiteTemplate("fake");
        try {
          String groupId = "/groupTemplate102";
          userPortalConfigSer_.createGroupSite(groupId);

          PortalConfig groupPortalConfig = layoutService.getPortalConfig(PortalConfig.GROUP_TYPE, groupId);

          assertNotNull(groupPortalConfig);
          assertNotNull(groupPortalConfig.getPortalLayout());
          assertNotNull(groupPortalConfig.getPortalLayout().getChildren());
          assertEquals(1, groupPortalConfig.getPortalLayout().getChildren().size());
          assertEquals(PageBody.class, groupPortalConfig.getPortalLayout().getChildren().get(0).getClass());
          assertTrue(groupPortalConfig.isDefaultLayout());
        } finally {
          userPortalConfigSer_.setDefaultGroupSiteTemplate(originalDefaultGroupSiteTemplate);
        }

        // Test creating group site with null template
        userPortalConfigSer_.setDefaultGroupSiteTemplate(null);
        try {
          String groupId = "/groupTemplate103";
          userPortalConfigSer_.createGroupSite(groupId);

          PortalConfig groupPortalConfig = layoutService.getPortalConfig(PortalConfig.GROUP_TYPE, groupId);

          assertNotNull(groupPortalConfig);
          assertNotNull(groupPortalConfig.getPortalLayout());
          assertNotNull(groupPortalConfig.getPortalLayout().getChildren());
          assertEquals(1, groupPortalConfig.getPortalLayout().getChildren().size());
          assertEquals(PageBody.class, groupPortalConfig.getPortalLayout().getChildren().get(0).getClass());
          assertTrue(groupPortalConfig.isDefaultLayout());
        } finally {
          userPortalConfigSer_.setDefaultGroupSiteTemplate(originalDefaultGroupSiteTemplate);
        }
      }
    }.execute("root");
  }

  public void testCreateUserPortalConfigWithDefaultTemplate() {
    new UnitTest() {
      public void execute() throws Exception {
        String originalDefaultUserSiteTemplate = userPortalConfigSer_.getDefaultUserSiteTemplate();

        // Test creating user site with default template having a predefined
        // user.xml
        userPortalConfigSer_.setDefaultUserSiteTemplate("user");
        try {
          String userId = "/userTemplate101";
          userPortalConfigSer_.createUserSite(userId);

          PortalConfig userPortalConfig = layoutService.getPortalConfig(PortalConfig.USER_TYPE, userId);

          assertNotNull(userPortalConfig);
          assertNotNull(userPortalConfig.getPortalLayout());
          assertNotNull(userPortalConfig.getPortalLayout().getChildren());
          assertEquals(2, userPortalConfig.getPortalLayout().getChildren().size());
          assertFalse(userPortalConfig.isDefaultLayout());
        } finally {
          userPortalConfigSer_.setDefaultUserSiteTemplate(originalDefaultUserSiteTemplate);
        }

        // Test creating user site with not existing template
        userPortalConfigSer_.setDefaultUserSiteTemplate("fake");
        try {
          String userId = "userTemplate102";
          userPortalConfigSer_.createUserSite(userId);

          PortalConfig userPortalConfig = layoutService.getPortalConfig(PortalConfig.USER_TYPE, userId);

          assertNotNull(userPortalConfig);
          assertNotNull(userPortalConfig.getPortalLayout());
          assertNotNull(userPortalConfig.getPortalLayout().getChildren());
          assertEquals(1, userPortalConfig.getPortalLayout().getChildren().size());
          assertEquals(PageBody.class, userPortalConfig.getPortalLayout().getChildren().get(0).getClass());
          assertTrue(userPortalConfig.isDefaultLayout());
        } finally {
          userPortalConfigSer_.setDefaultUserSiteTemplate(originalDefaultUserSiteTemplate);
        }

        // Test creating user site with null template
        userPortalConfigSer_.setDefaultUserSiteTemplate(null);
        try {
          String userId = "userTemplate103";
          userPortalConfigSer_.createUserSite(userId);

          PortalConfig userPortalConfig = layoutService.getPortalConfig(PortalConfig.USER_TYPE, userId);

          assertNotNull(userPortalConfig);
          assertNotNull(userPortalConfig.getPortalLayout());
          assertNotNull(userPortalConfig.getPortalLayout().getChildren());
          assertEquals(1, userPortalConfig.getPortalLayout().getChildren().size());
          assertEquals(PageBody.class, userPortalConfig.getPortalLayout().getChildren().get(0).getClass());
          assertTrue(userPortalConfig.isDefaultLayout());
        } finally {
          userPortalConfigSer_.setDefaultUserSiteTemplate(originalDefaultUserSiteTemplate);
        }
      }
    }.execute("root");
  }

  public void testRemoveUserPortalConfig() {
    new UnitTest() {
      public void execute() throws Exception {
        userPortalConfigSer_.createUserPortalConfig(PortalConfig.PORTAL_TYPE, "jazz", "test");
        UserPortalConfig userPortalCfg = userPortalConfigSer_.getUserPortalConfig("jazz", "root");
        assertNotNull(userPortalCfg);
        restartTransaction();
        userPortalConfigSer_.removeUserPortalConfig("jazz");
        restartTransaction();
        assertNull(userPortalConfigSer_.getUserPortalConfig("jazz", "root"));
      }
    }.execute("root");
  }

  public void testRootGetMakableNavigations() {
    new UnitTest() {
      public void execute() throws Exception {
        Set<String> navigations = new HashSet<String>(userPortalConfigSer_.getMakableNavigations("root", false));
        Set<String> expectedNavigations = Tools.toSet("/platform/users",
                                                      "/platform",
                                                      "/platform/guests",
                                                      "/platform/administrators",
                                                      "/organization",
                                                      "/organization/management",
                                                      "/organization/management/executive-board");
        for (String expectedNavigation : expectedNavigations) {
          assertTrue("Navigation not found : " + expectedNavigation, navigations.contains(expectedNavigation));
        }
      }
    }.execute(null);
  }

  public void testJohnGetMakableNavigations() {
    new UnitTest() {
      public void execute() throws Exception {
        Set<String> navigations = new HashSet<String>(userPortalConfigSer_.getMakableNavigations("john", false));
        Set<String> expectedNavigations = Tools.toSet("/organization/management/executive-board");
        assertEquals(expectedNavigations, navigations);
      }
    }.execute(null);
  }

  public void testMaryGetMakableNavigations() {
    new UnitTest() {
      public void execute() throws Exception {
        Set<String> navigations = new HashSet<String>(userPortalConfigSer_.getMakableNavigations("mary", false));
        Set<String> expectedNavigations = Collections.emptySet();
        assertEquals(expectedNavigations, navigations);
      }
    }.execute(null);
  }

  public void testHasMakableNavigations() {
    new UnitTest() {
      public void execute() throws Exception {
        assertEquals(true, userPortalConfigSer_.hasMakableNavigations("root", true)); // super
                                                                                      // user
        assertEquals(false, userPortalConfigSer_.hasMakableNavigations(null, true)); // remote
                                                                                     // user
                                                                                     // is
                                                                                     // null
        assertEquals(true, userPortalConfigSer_.hasMakableNavigations("john", true)); // john
                                                                                      // is
                                                                                      // an
                                                                                      // administrator
                                                                                      // so
                                                                                      // he
                                                                                      // has
                                                                                      // at
                                                                                      // least
                                                                                      // a
                                                                                      // membership
                                                                                      // in
                                                                                      // adminstration
                                                                                      // group
      }
    }.execute(null);
  }

  public void testRootGetPage() {
    new UnitTest() {
      public void execute() throws Exception {
        assertEquals("group::/platform/administrators::newAccount",
                     userPortalConfigSer_.getPage(PageKey.parse("group::/platform/administrators::newAccount"))
                                         .getKey()
                                         .format());
        assertEquals("group::/organization/management/executive-board::newStaff",
                     userPortalConfigSer_
                                         .getPage(PageKey.parse("group::/organization/management/executive-board::newStaff"))
                                         .getKey()
                                         .format());
      }
    }.execute("root");
  }

  public void testJohnGetPage() {
    new UnitTest() {
      public void execute() throws Exception {
        assertEquals(null, userPortalConfigSer_.getPage(PageKey.parse("group::/platform/administrators::newAccount")));
        assertEquals("group::/organization/management/executive-board::newStaff",
                     userPortalConfigSer_
                                         .getPage(PageKey.parse("group::/organization/management/executive-board::newStaff"))
                                         .getKey()
                                         .format());
      }
    }.execute("john");
  }

  public void testMaryGetPage() {
    new UnitTest() {
      public void execute() throws Exception {
        assertEquals(null, userPortalConfigSer_.getPage(PageKey.parse("group::/platform/administrators::newAccount")));
        assertEquals(null,
                     userPortalConfigSer_.getPage(PageKey
                                                         .parse("group::/organization/management/executive-board::newStaff")));
      }
    }.execute("mary");
  }

  public void testAnonymousGetPage() {
    new UnitTest() {
      public void execute() throws Exception {
        assertEquals(null, userPortalConfigSer_.getPage(PageKey.parse("group::/platform/administrators::newAccount")));
        assertEquals(null,
                     userPortalConfigSer_.getPage(PageKey
                                                         .parse("group::/organization/management/executive-board::newStaff")));
      }
    }.execute(null);
  }

  public void testOverwriteUserLayout() {
    new UnitTest() {
      public void execute() throws Exception {
        PortalConfig cfg = layoutService.getPortalConfig(PortalConfig.USER_TYPE, "overwritelayout");
        assertNotNull(cfg);

        Container container = cfg.getPortalLayout();
        assertNotNull(container);
        assertEquals(2, container.getChildren().size());
        assertTrue(container.getChildren().get(0) instanceof PageBody);
        assertTrue(((Application) container.getChildren().get(1)).getType() == ApplicationType.PORTLET);
        Application<Portlet> pa = (Application<Portlet>) container.getChildren().get(1);
        ApplicationState<Portlet> state = pa.getState();
        assertEquals("overwrite_application_ref/overwrite_portlet_ref", layoutService.getId(state));
      }
    }.execute(null);
  }

  public void testUserTemplate() {
    new UnitTest() {
      public void execute() throws Exception {
        assertNull(layoutService.getPortalConfig(PortalConfig.USER_TYPE, "user"));
        assertNull(layoutService.getPortalConfig(PortalConfig.USER_TYPE, "julien"));

        //
        UserHandler userHandler = orgService_.getUserHandler();
        User user = userHandler.createUserInstance("julien");
        user.setPassword("default");
        user.setFirstName("default");
        user.setLastName("default");
        user.setEmail("exo@exoportal.org");
        userHandler.createUser(user, true);

        //
        PortalConfig cfg = layoutService.getPortalConfig(PortalConfig.USER_TYPE, "julien");
        assertNotNull(cfg);
        Container container = cfg.getPortalLayout();
        assertNotNull(container);
        assertEquals(2, container.getChildren().size());
        assertTrue(container.getChildren().get(0) instanceof PageBody);
        assertTrue(((Application) container.getChildren().get(1)).getType() == ApplicationType.PORTLET);
        Application<Portlet> pa = (Application<Portlet>) container.getChildren().get(1);
        ApplicationState state = pa.getState();
        assertEquals("foo/bar", layoutService.getId(pa.getState()));
      }
    }.execute(null);
  }

  private abstract class UnitTest {

    /** . */
    protected final void execute(String userId) {
      Throwable failure = null;

      //
      begin();

      //
      ConversationState conversationState = null;
      if (userId != null) {
        try {
          conversationState = new ConversationState(authenticator.createIdentity(userId));
        } catch (Exception e) {
          failure = e;
        }
      }

      //
      if (failure == null) {
        //
        ConversationState.setCurrent(conversationState);
        try {
          execute();
        } catch (Exception e) {
          failure = e;
        } finally {
          ConversationState.setCurrent(null);
          end();
        }
      }

      // Report error as a junit assertion failure
      if (failure != null) {
        AssertionFailedError err = new AssertionFailedError();
        err.initCause(failure);
        throw err;
      }
    }

    protected abstract void execute() throws Exception;

  }
}
