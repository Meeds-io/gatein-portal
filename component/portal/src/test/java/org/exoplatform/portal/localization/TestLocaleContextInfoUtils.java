package org.exoplatform.portal.localization;

import org.apache.commons.lang.LocaleUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.organization.impl.UserProfileImpl;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.impl.LocaleConfigImpl;
import org.gatein.common.i18n.LocaleFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest(LocaleContextInfoUtils.class)
public class TestLocaleContextInfoUtils {

  POMSessionManager pomSessionManager;
  LocaleConfigService localeConfigService;
  OrganizationService organizationService;
  UserProfileHandler userProfileHandler;
  UserPortalConfigService userPortalConfigService;
  DataStorage dataStorage;
  HttpServletRequest request;
  ExoContainer exoContainer;

  @Before
  public void setup() {
    exoContainer = mock(ExoContainer.class);
    pomSessionManager = mock(POMSessionManager.class);
    localeConfigService = mock(LocaleConfigService.class);
    organizationService = mock(OrganizationService.class);
    userProfileHandler = mock(UserProfileHandler.class);
    userPortalConfigService = mock(UserPortalConfigService.class);
    dataStorage = mock(DataStorage.class);
    request = Mockito.mock(HttpServletRequest.class);

    ExoContainerContext.setCurrentContainer(exoContainer);

    when(exoContainer.getComponentInstanceOfType(eq(POMSessionManager.class))).thenReturn(pomSessionManager);
    when(exoContainer.getComponentInstanceOfType(eq(LocaleConfigService.class))).thenReturn(localeConfigService);
    when(exoContainer.getComponentInstanceOfType(eq(OrganizationService.class))).thenReturn(organizationService);
    when(exoContainer.getComponentInstanceOfType(eq(UserPortalConfigService.class))).thenReturn(userPortalConfigService);
    when(organizationService.getUserProfileHandler()).thenReturn(userProfileHandler);
    when(userPortalConfigService.getDataStorage()).thenReturn(dataStorage);
    when(pomSessionManager.getSession()).thenReturn(null);
  }

  @Test
  public void testBuildLocaleContextInfoWithRequestAndNoPortalLocaleAndNoUserLocale() throws Exception {
    // Given
    when(localeConfigService.getLocalConfigs()).thenReturn(getSupportedLocalesOneLanguage());
    when(request.getCookies()).thenReturn(new Cookie[0]);
    when(request.getLocales()).thenReturn(Collections.enumeration(Collections.EMPTY_LIST));
    when(request.getRemoteUser()).thenReturn("root");
    
    when(userPortalConfigService.getDefaultPortal()).thenReturn("intranet");
    when(dataStorage.getPortalConfig(anyString())).thenReturn(null);
    when(userProfileHandler.findUserProfileByName(anyString()))
            .thenReturn(getUserProfileInstanceWithGivenLocale(null));

    // when
    LocaleContextInfo localCtx = LocaleContextInfoUtils.buildLocaleContextInfo(request);

    // Then
    assertNotNull(localCtx);
    assertEquals(localCtx.getRemoteUser(), "root");
    // return JVM locale if Portal locale and user locale are null
    assertEquals(localCtx.getPortalLocale(), Locale.getDefault());
    assertEquals(localCtx.getCookieLocales(), new ArrayList<Locale>());
    assertEquals(localCtx.getSessionLocale(), null);
    assertEquals(localCtx.getSupportedLocales().size(), 1);
    assertEquals(localCtx.getSupportedLocales().iterator().next().getLanguage(), "es");
    assertEquals(localCtx.getUserProfileLocale(), null);
    assertEquals(localCtx.getBrowserLocales(), new ArrayList<Locale>());
  }

  @Test
  public void testBuildLocaleContextInfoWithRequestAndWithPortalLocaleAndNoUserLocale() throws Exception {
    // Given
    when(localeConfigService.getLocalConfigs()).thenReturn(getSupportedLocalesNoLanguage());
    when(request.getCookies()).thenReturn(new Cookie[0]);
    when(request.getLocales()).thenReturn(Collections.enumeration(Collections.EMPTY_LIST));
    when(request.getRemoteUser()).thenReturn("root");

    when(userPortalConfigService.getDefaultPortal()).thenReturn("intranet");

    //
    // return portal locale when it is set AND user locale is null
    //
    when(dataStorage.getPortalConfig(anyString()))
            .thenReturn(getPortalConfigInstanceWithGivenLocale(LocaleUtils.toLocale("fr")));
    when(userProfileHandler.findUserProfileByName(anyString()))
            .thenReturn(getUserProfileInstanceWithGivenLocale(null));

    // when
    LocaleContextInfo localCtx = LocaleContextInfoUtils.buildLocaleContextInfo(request);

    // then
    assertNotNull(localCtx);
    assertEquals(localCtx.getRemoteUser(), "root");
    assertEquals(localCtx.getPortalLocale().getLanguage(), "fr");
    assertEquals(localCtx.getCookieLocales(), new ArrayList<Locale>());
    assertEquals(localCtx.getSessionLocale(), null);
    assertEquals(localCtx.getSupportedLocales().size(), 0);
    assertEquals(localCtx.getUserProfileLocale(), null);
    assertEquals(localCtx.getBrowserLocales(), new ArrayList<Locale>());
  }

  @Test
  public void testBuildLocaleContextInfoWithRequestAndNoPortalLocaleAndWithUserLocale() throws Exception {
    // Given
    when(localeConfigService.getLocalConfigs()).thenReturn(getSupportedLocalesOneLanguage());
    when(request.getCookies()).thenReturn(new Cookie[0]);
    when(request.getLocales()).thenReturn(Collections.enumeration(Collections.EMPTY_LIST));
    when(request.getRemoteUser()).thenReturn("root");

    when(userPortalConfigService.getDefaultPortal()).thenReturn("intranet");

    //
    // return user locale if set
    //
    when(dataStorage.getPortalConfig(anyString())).thenReturn(null);
    when(userProfileHandler.findUserProfileByName(anyString()))
            .thenReturn(getUserProfileInstanceWithGivenLocale(LocaleUtils.toLocale("de")));

    // when
    LocaleContextInfo localCtx = LocaleContextInfoUtils.buildLocaleContextInfo(request);

    // then
    assertNotNull(localCtx);
    assertEquals(localCtx.getRemoteUser(), "root");
    assertEquals(localCtx.getPortalLocale().getLanguage(), Locale.getDefault().getLanguage());
    assertEquals(localCtx.getCookieLocales(), new ArrayList<Locale>());
    assertEquals(localCtx.getSessionLocale(), null);
    assertEquals(localCtx.getSupportedLocales().size(), 1);
    assertEquals(localCtx.getSupportedLocales().iterator().next().getLanguage(), "es");
    assertEquals(localCtx.getUserProfileLocale().getLanguage(), "de");
    assertEquals(localCtx.getBrowserLocales(), new ArrayList<Locale>());
  }
  
  @Test
  public void testBuildLocaleContextInfoWithUserIDAndNoPortalLocaleAndNoUserLocale() throws Exception {
    // Given
    when(localeConfigService.getLocalConfigs()).thenReturn(getSupportedLocalesOneLanguage());
    when(userPortalConfigService.getDefaultPortal()).thenReturn("intranet");
  
    String userId = "exo";
    
    //
    // return JVM locale if Portal locale and user locale are null
    //
    when(dataStorage.getPortalConfig(anyString())).thenReturn(null);
    when(userProfileHandler.findUserProfileByName(anyString()))
            .thenReturn(getUserProfileInstanceWithGivenLocale(null));
    
    // when
    LocaleContextInfo localCtx1 = LocaleContextInfoUtils.buildLocaleContextInfo(userId);

    // Then
    assertNotNull(localCtx1);
    assertEquals(localCtx1.getRemoteUser(), "exo");
    assertEquals(localCtx1.getPortalLocale(), Locale.getDefault());
    assertEquals(localCtx1.getCookieLocales(), null);
    assertEquals(localCtx1.getSessionLocale(), null);
    assertEquals(localCtx1.getSupportedLocales().size(), 1);
    assertEquals(localCtx1.getSupportedLocales().iterator().next().getLanguage(), "es");
    assertEquals(localCtx1.getUserProfileLocale(), null);
    assertEquals(localCtx1.getBrowserLocales(), null);
  }

  @Test
  public void testBuildLocaleContextInfoWithUserIDAndWithPortalLocaleAndNoUserLocale() throws Exception {
    // Given
    when(localeConfigService.getLocalConfigs()).thenReturn(getSupportedLocalesOneLanguage());
    when(userPortalConfigService.getDefaultPortal()).thenReturn("intranet");

    String userId = "exo";

    //
    // return portal locale when it is set AND user locale is null
    //
    when(dataStorage.getPortalConfig(anyString()))
            .thenReturn(getPortalConfigInstanceWithGivenLocale(LocaleUtils.toLocale("pt_BR")));
    when(userProfileHandler.findUserProfileByName(anyString()))
            .thenReturn(getUserProfileInstanceWithGivenLocale(null));

    // when
    LocaleContextInfo localCtx1 = LocaleContextInfoUtils.buildLocaleContextInfo(userId);

    // then
    assertNotNull(localCtx1);
    assertEquals(localCtx1.getRemoteUser(), "exo");
    Locale portalLocale = localCtx1.getPortalLocale();
    assertEquals(portalLocale.getLanguage(),"pt");
    assertEquals(portalLocale.getCountry(),"BR");
    assertEquals(localCtx1.getCookieLocales(), null);
    assertEquals(localCtx1.getSessionLocale(), null);
    assertEquals(localCtx1.getSupportedLocales().size(), 1);
    Locale supportedLocale = localCtx1.getSupportedLocales().iterator().next();
    assertEquals(supportedLocale.getLanguage(), "es");
    assertEquals(supportedLocale.getCountry(), "ES");
    assertEquals(localCtx1.getUserProfileLocale(), null);
    assertEquals(localCtx1.getBrowserLocales(), null);
  }

  @Test
  public void testBuildLocaleContextInfoWithUserIDAndNoPortalLocaleAndWithUserLocale() throws Exception {
    // Given
    when(localeConfigService.getLocalConfigs()).thenReturn(getSupportedLocalesNoLanguage());
    when(userPortalConfigService.getDefaultPortal()).thenReturn("intranet");

    String userId = "exo";

    //
    // return user locale if set
    //
    when(dataStorage.getPortalConfig(anyString())).thenReturn(null);
    when(userProfileHandler.findUserProfileByName(anyString()))
            .thenReturn(getUserProfileInstanceWithGivenLocale(LocaleUtils.toLocale("de")));

    // when
    LocaleContextInfo localCtx1 = LocaleContextInfoUtils.buildLocaleContextInfo(userId);

    // then
    assertNotNull(localCtx1);
    assertEquals(localCtx1.getRemoteUser(), "exo");
    assertEquals(localCtx1.getPortalLocale().getLanguage(), Locale.getDefault().getLanguage());
    assertEquals(localCtx1.getCookieLocales(), null);
    assertEquals(localCtx1.getSessionLocale(), null);
    assertEquals(localCtx1.getSupportedLocales().size(), 0);
    assertEquals(localCtx1.getUserProfileLocale().getLanguage(), "de");
    assertEquals(localCtx1.getBrowserLocales(), null);
  }

  private Collection<LocaleConfig> getSupportedLocalesNoLanguage() {
    return new HashSet<LocaleConfig>();
  }

  private Collection<LocaleConfig> getSupportedLocalesOneLanguage() {
    Set<LocaleConfig> supportedLocales = new HashSet<LocaleConfig>();
    LocaleConfigImpl localeConfig = new LocaleConfigImpl();
    localeConfig.setLocale("es_ES");
    supportedLocales.add(localeConfig);
    return supportedLocales;
  }
  
  private UserProfile getUserProfileInstanceWithGivenLocale(Locale locale) {
    UserProfile profile = new UserProfileImpl();
    if (locale != null) {
      profile.setAttribute(Constants.USER_LANGUAGE, locale.getLanguage());
    }
    return  profile;
  }

  private PortalConfig getPortalConfigInstanceWithGivenLocale(Locale locale) {
    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setLocale(locale.toString());
    return portalConfig;
  }
  
}
