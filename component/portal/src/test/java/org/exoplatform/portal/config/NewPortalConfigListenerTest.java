package org.exoplatform.portal.config;

import liquibase.pro.packaged.N;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.services.resources.LocaleConfigService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NewPortalConfigListenerTest {

  @Mock
  private UserPortalConfigService owner;

  @Mock
  private DataStorage             dataStorage;

  @Mock
  private PageService             pageService;

  @Mock
  private ConfigurationManager    configurationManager;

  @Mock
  private NavigationService       navigationService;

  @Mock
  private InitParams              initParams;

  @Mock
  private DescriptionService      descriptionService;

  @Mock
  private UserACL                 userACL;

  @Mock
  private LocaleConfigService     localeConfigService;

  @Test
  public void testInitPageDB() throws Exception {
    ValueParam valueParam = new ValueParam();
    valueParam.setName("default.portal");
    valueParam.setValue("classic");

    ValueParam valueParam1 = new ValueParam();
    valueParam.setName("page.templates.location");
    valueParam.setValue("war:/conf/portal/template/pages");

    when(initParams.getValueParam("default.portal")).thenReturn(valueParam);
    when(initParams.getValueParam("page.templates.location")).thenReturn(valueParam1);

    NewPortalConfigListener newPortalConfigListener = new NewPortalConfigListener(owner,
                                                                                  dataStorage,
                                                                                  pageService,
                                                                                  configurationManager,
                                                                                  initParams,
                                                                                  navigationService,
                                                                                  descriptionService,
                                                                                  userACL,
                                                                                  localeConfigService);

    newPortalConfigListener.createdOwners.add("global");
    HashSet<String> predefinedOwner = new HashSet<>();
    predefinedOwner.add("global");

    NewPortalConfig newPortalConfig = mock(NewPortalConfig.class);

    when(newPortalConfig.getPredefinedOwner()).thenReturn(predefinedOwner);
    when(newPortalConfig.getImportMode()).thenReturn("overwrite");
    when(newPortalConfig.getOwnerType()).thenReturn(PortalConfig.PORTAL_TYPE);

    PageContext pageContext = mock(PageContext.class);
    PageKey pageKey = mock(PageKey.class);
    when(pageContext.getKey()).thenReturn(pageKey);
    when(pageKey.format()).thenReturn("pageKey");
    List<PageContext> allPages = new ArrayList<>();
    allPages.add(pageContext);

    when(pageService.loadPages(any(SiteKey.class))).thenReturn(allPages);

    newPortalConfigListener.initPageDB(newPortalConfig);

    verify(pageService, times(1)).destroyPage(pageKey);

    Mockito.reset(pageService);

    when(newPortalConfig.getImportMode()).thenReturn("merge");

    newPortalConfigListener.initPageDB(newPortalConfig);

    verify(pageService, times(0)).destroyPage(pageKey);
  }

  @Test
  public void testReloadConfig() throws Exception {
    ValueParam valueParam = new ValueParam();
    valueParam.setName("default.portal");
    valueParam.setValue("classic");

    ValueParam valueParam1 = new ValueParam();
    valueParam.setName("page.templates.location");
    valueParam.setValue("war:/conf/portal/template/pages");

    when(initParams.getValueParam("default.portal")).thenReturn(valueParam);
    when(initParams.getValueParam("page.templates.location")).thenReturn(valueParam1);

    HashSet<String> predefinedOwner = new HashSet<>();
    predefinedOwner.add("global");

    NewPortalConfig newPortalConfig = mock(NewPortalConfig.class);

    when(newPortalConfig.getPredefinedOwner()).thenReturn(predefinedOwner);
    when(newPortalConfig.isPredefinedOwner(anyString())).thenReturn(true);
    when(newPortalConfig.getImportMode()).thenReturn("insert","merge");
    when(newPortalConfig.getOverrideMode()).thenReturn(false);
    when(newPortalConfig.getOwnerType()).thenReturn(PortalConfig.PORTAL_TYPE);
    String location = "war:/conf/webapps/test-location/portal";
    when(newPortalConfig.getLocation()).thenReturn(location);
    when(newPortalConfig.getTemplateLocation()).thenReturn(location);
    doCallRealMethod().when(newPortalConfig).setImportMode(anyString());
    doCallRealMethod().when(newPortalConfig).setOverrideMode(anyBoolean());
    List<NewPortalConfig> configs = new ArrayList<>();
    configs.add(newPortalConfig);
    when(initParams.getObjectParamValues(NewPortalConfig.class)).thenReturn(configs);

    when(dataStorage.getPortalConfig("portal","global")).thenReturn(new PortalConfig());

    Mockito.mockStatic(IOUtil.class);
    when(IOUtil.getStreamContentAsString(any())).thenReturn(null,
                                                            null,
                                                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<page-set "
                                                                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                + "  xsi:schemaLocation=\"http://www.exoplatform"
                                                                + ".org/xml/ns/gatein_objects_1_8 http://www.exoplatform"
                                                                + ".org/xml/ns/gatein_objects_1_8\"\n"
                                                                + "  xmlns=\"http://www.exoplatform"
                                                                + ".org/xml/ns/gatein_objects_1_8\"><page>\n"
                                                                + "    <name>profile</name>\n"
                                                                + "    <title>Profile</title>\n"
                                                                + "    <access-permissions>*:/platform/users;*:/platform/externals"
                                                                + "</access-permissions>\n"
                                                                + "    <edit-permission>manager:/platform/administrators"
                                                                + "</edit-permission></page></page-set>",
                                                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                                                + "<node-navigation>\n"
                                                                + "  <priority>2</priority>\n"
                                                                + "  <page-nodes>\n"
                                                                + "    <node>\n"
                                                                + "      <name>profile</name>\n"
                                                                + "      <label>#{portal.global.profile}</label>\n"
                                                                + "      <visibility>SYSTEM</visibility>\n"
                                                                + "      <page-reference>portal::global::profile</page"
                                                                + "-reference>\n"
                                                                + "    </node>\n"
                                                                + "  </page-nodes>\n"
                                                                + "</node-navigation>");

    NewPortalConfigListener newPortalConfigListener = new NewPortalConfigListener(owner,
                                                                                  dataStorage,
                                                                                  pageService,
                                                                                  configurationManager,
                                                                                  initParams,
                                                                                  navigationService,
                                                                                  descriptionService,
                                                                                  userACL,
                                                                                  localeConfigService);

    newPortalConfigListener.createdOwners.add("global");



    try {
      newPortalConfigListener.reloadConfig("portal", "global", location, "merge", true);
    } catch (Exception e) {
      //the navigation reload throws an exception due to incomplete data
      //as we want to test only if navigationService.saveNavigation is called
      //we catch the exception here, and only check that saveNavigation is called once
    }
    verify(pageService, times(1)).savePage(any());
    verify(navigationService, times(1)).saveNavigation(any());
  }
}
