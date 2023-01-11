package org.exoplatform.portal.config;

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
import org.mockito.junit.MockitoJUnitRunner;

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

    String predefinedOwner = "global";

    HashSet<String> predefinedOwners = new HashSet<>();
    predefinedOwners.add(predefinedOwner);

    NewPortalConfig newPortalConfig = mock(NewPortalConfig.class);

    when(newPortalConfig.getImportMode()).thenReturn("overwrite");
    when(newPortalConfig.getOwnerType()).thenReturn(PortalConfig.PORTAL_TYPE);

    PageContext pageContext = mock(PageContext.class);
    PageKey pageKey = mock(PageKey.class);
    when(pageContext.getKey()).thenReturn(pageKey);
    when(pageKey.format()).thenReturn("pageKey");
    List<PageContext> allPages = new ArrayList<>();
    allPages.add(pageContext);

    when(pageService.loadPages(any(SiteKey.class))).thenReturn(allPages);

    newPortalConfigListener.createPage(newPortalConfig, predefinedOwner);

    verify(pageService, times(1)).destroyPage(pageKey);

    Mockito.reset(pageService);

    when(newPortalConfig.getImportMode()).thenReturn("merge");

    newPortalConfigListener.createPage(newPortalConfig, predefinedOwner);

    verify(pageService, times(0)).destroyPage(pageKey);
  }
}
