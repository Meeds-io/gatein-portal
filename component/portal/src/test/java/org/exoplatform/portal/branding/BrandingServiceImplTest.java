package org.exoplatform.portal.branding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.branding.model.Background;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.Favicon;
import org.exoplatform.portal.branding.model.Logo;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

@SuppressWarnings({
    "rawtypes", "unchecked"
})
public class BrandingServiceImplTest {

  @Test
  public void shouldGetDefaultBrandingInformationWhenNoUpdate() {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);
    ValueParam companyLink = new ValueParam();
    companyLink.setName(BrandingServiceImpl.BRANDING_COMPANY_LINK_INIT_PARAM);
    companyLink.setValue("https://meeds.io");
    initParams.addParam(companyLink);
    ValueParam siteName = new ValueParam();
    siteName.setName(BrandingServiceImpl.BRANDING_SITE_NAME_INIT_PARAM);
    siteName.setValue("Meeds");
    initParams.addParam(siteName);

    BrandingService brandingService = new BrandingServiceImpl(container,
                                                              configurationManager,
                                                              settingService,
                                                              fileService,
                                                              uploadService,
                                                              localeConfigService,
                                                              initParams);

    // When
    Branding brandingInformation = brandingService.getBrandingInformation();

    // Then
    assertNotNull(brandingInformation);
    assertEquals("Default Company Name", brandingInformation.getCompanyName());
    assertEquals("https://meeds.io", brandingInformation.getCompanyLink());
    assertEquals("Meeds", brandingInformation.getSiteName());
  }

  @Test
  public void shouldGetDefaultBrandingThemeColorsWhenNoUpdate() {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);

    String primaryColor = "#3f8487";
    String primaryBackground = "#f0f0f0";
    String secondaryColor = "#000000";
    String secondaryBackground = "#e25d5d";

    InitParams initParams = new InitParams();
    ValuesParam colorsTheme = new ValuesParam();
    colorsTheme.setName(BrandingServiceImpl.BRANDING_THEME_VARIABLES);
    List<String> variables = new ArrayList<>();
    variables.add("primaryColor:" + primaryColor);
    variables.add("primaryBackground:" + primaryBackground);
    variables.add("secondaryColor:" + secondaryColor);
    variables.add("secondaryBackground:" + secondaryBackground);
    colorsTheme.setValues(variables);

    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");

    initParams.addParam(companyName);
    initParams.addParam(colorsTheme);

    BrandingServiceImpl brandingService = new BrandingServiceImpl(container,
                                                                  configurationManager,
                                                                  settingService,
                                                                  fileService,
                                                                  uploadService,
                                                                  localeConfigService,
                                                                  initParams);
    brandingService.start();

    // When
    Branding brandingInformation = brandingService.getBrandingInformation();

    // Then
    assertNotNull(brandingInformation);
    assertNotNull("Default Theme colors shouldn't be null", brandingInformation.getThemeColors());

    assertEquals(4, brandingInformation.getThemeColors().size());
    assertEquals(primaryColor, brandingInformation.getThemeColors().get("primaryColor"));
    assertEquals(primaryBackground, brandingInformation.getThemeColors().get("primaryBackground"));
    assertEquals(secondaryColor, brandingInformation.getThemeColors().get("secondaryColor"));
    assertEquals(secondaryBackground, brandingInformation.getThemeColors().get("secondaryBackground"));
  }

  @Test
  public void shouldGetSavedBrandingThemeColorsWhenUpdate() {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    String primaryColorNewValue = "#3f8488";
    String primaryBackgroundNewValue = "#f0f0f1";
    String secondaryColorNewValue = "#000001";
    String secondaryBackgroundNewValue = "#e25d5e";

    when(settingService.get(BrandingServiceImpl.BRANDING_CONTEXT,
                            BrandingServiceImpl.BRANDING_SCOPE,
                            "primaryColor")).thenReturn((SettingValue) SettingValue.create(primaryColorNewValue));
    when(settingService.get(BrandingServiceImpl.BRANDING_CONTEXT,
                            BrandingServiceImpl.BRANDING_SCOPE,
                            "primaryBackground")).thenReturn((SettingValue) SettingValue.create(primaryBackgroundNewValue));
    when(settingService.get(BrandingServiceImpl.BRANDING_CONTEXT,
                            BrandingServiceImpl.BRANDING_SCOPE,
                            "secondaryColor")).thenReturn((SettingValue) SettingValue.create(secondaryColorNewValue));
    when(settingService.get(BrandingServiceImpl.BRANDING_CONTEXT,
                            BrandingServiceImpl.BRANDING_SCOPE,
                            "secondaryBackground")).thenReturn((SettingValue) SettingValue.create(secondaryBackgroundNewValue));

    String primaryColor = "#3f8487";
    String primaryBackground = "#f0f0f0";
    String secondaryColor = "#000000";
    String secondaryBackground = "#e25d5d";

    InitParams initParams = new InitParams();

    ValuesParam colorsTheme = new ValuesParam();
    colorsTheme.setName(BrandingServiceImpl.BRANDING_THEME_VARIABLES);
    List<String> variables = new ArrayList<>();
    variables.add("primaryColor:" + primaryColor);
    variables.add("primaryBackground:" + primaryBackground);
    variables.add("secondaryColor:" + secondaryColor);
    variables.add("secondaryBackground:" + secondaryBackground);
    colorsTheme.setValues(variables);

    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");

    initParams.addParam(companyName);
    initParams.addParam(colorsTheme);

    BrandingServiceImpl brandingService = new BrandingServiceImpl(container,
                                                                  configurationManager,
                                                                  settingService,
                                                                  fileService,
                                                                  uploadService,
                                                                  localeConfigService,
                                                                  initParams);
    brandingService.start();

    // When
    Branding brandingInformation = brandingService.getBrandingInformation();

    // Then
    assertNotNull(brandingInformation);
    assertNotNull("Default Theme colors shouldn't be null", brandingInformation.getThemeColors());

    assertEquals(4, brandingInformation.getThemeColors().size());
    assertEquals(primaryColorNewValue, brandingInformation.getThemeColors().get("primaryColor"));
    assertEquals(primaryBackgroundNewValue, brandingInformation.getThemeColors().get("primaryBackground"));
    assertEquals(secondaryColorNewValue, brandingInformation.getThemeColors().get("secondaryColor"));
    assertEquals(secondaryBackgroundNewValue, brandingInformation.getThemeColors().get("secondaryBackground"));
  }

  @Test
  public void shouldGetUpdatedBrandingInformationWhenInformationUpdated() {
    // Given
    SettingService settingService = mock(SettingService.class);
    when(settingService.get(Context.GLOBAL,
                            Scope.GLOBAL,
                            BrandingServiceImpl.BRANDING_COMPANY_NAME_SETTING_KEY)).thenReturn(new SettingValue("Updated Company Name"));
    when(settingService.get(Context.GLOBAL,
                            Scope.GLOBAL,
                            BrandingServiceImpl.BRANDING_COMPANY_LINK_SETTING_KEY)).thenReturn(new SettingValue("https://investors.meeds.io"));
    when(settingService.get(Context.GLOBAL,
                            Scope.GLOBAL,
                            BrandingServiceImpl.BRANDING_SITE_NAME_SETTING_KEY)).thenReturn(new SettingValue("Meeds.io"));
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);
    ValueParam companyLink = new ValueParam();
    companyLink.setName(BrandingServiceImpl.BRANDING_COMPANY_LINK_INIT_PARAM);
    companyLink.setValue("https://meeds.io");
    initParams.addParam(companyLink);
    ValueParam siteName = new ValueParam();
    siteName.setName(BrandingServiceImpl.BRANDING_SITE_NAME_INIT_PARAM);
    siteName.setValue("Meeds");
    initParams.addParam(siteName);

    BrandingServiceImpl brandingService = new BrandingServiceImpl(container,
                                                                  configurationManager,
                                                                  settingService,
                                                                  fileService,
                                                                  uploadService,
                                                                  localeConfigService,
                                                                  initParams);

    // When
    Branding brandingInformation = brandingService.getBrandingInformation();

    // Then
    assertNotNull(brandingInformation);
    assertEquals("Updated Company Name", brandingInformation.getCompanyName());
    assertEquals("https://investors.meeds.io", brandingInformation.getCompanyLink());
    assertEquals("Meeds.io", brandingInformation.getSiteName());
  }

  @Test
  public void shouldUpdateCompanyNameAndTopBarThemeWhenInformationUpdated() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    String primaryColor = "#3f8487";
    String primaryBackground = "#f0f0f0";
    String secondaryColor = "#000000";
    String secondaryBackground = "#e25d5d";

    String primaryColorNewValue = "#3f8488";
    String primaryBackgroundNewValue = "#f0f0f1";
    String secondaryColorNewValue = null;
    String secondaryBackgroundNewValue = null;

    ValuesParam colorsTheme = new ValuesParam();
    colorsTheme.setName(BrandingServiceImpl.BRANDING_THEME_VARIABLES);
    List<String> variables = new ArrayList<>();
    variables.add("primaryColor:" + primaryColor);
    variables.add("primaryBackground:" + primaryBackground);
    variables.add("secondaryColor:" + secondaryColor);
    variables.add("secondaryBackground:" + secondaryBackground);
    colorsTheme.setValues(variables);
    initParams.addParam(colorsTheme);

    BrandingServiceImpl brandingService = new BrandingServiceImpl(container,
                                                                  configurationManager,
                                                                  settingService,
                                                                  fileService,
                                                                  uploadService,
                                                                  localeConfigService,
                                                                  initParams);

    Branding newBranding = new Branding();
    newBranding.setCompanyName("New Company Name");
    newBranding.setTopBarTheme("Pink");
    newBranding.setThemeColors(new HashMap<String, String>());
    newBranding.getThemeColors().put("primaryColor", primaryColorNewValue);
    newBranding.getThemeColors().put("primaryBackground", primaryBackgroundNewValue);
    newBranding.getThemeColors().put("secondaryColor", secondaryColorNewValue);
    newBranding.getThemeColors().put("secondaryBackground", secondaryBackgroundNewValue);

    ArgumentCaptor<Context> settingContext = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> settingScope = ArgumentCaptor.forClass(Scope.class);
    ArgumentCaptor<String> settingKey = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SettingValue> settingValue = ArgumentCaptor.forClass(SettingValue.class);

    // When
    brandingService.updateBrandingInformation(newBranding);

    // Then
    verify(settingService, times(5)).set(settingContext.capture(),
                                         settingScope.capture(),
                                         settingKey.capture(),
                                         settingValue.capture());
    verify(settingService, times(1)).remove(BrandingServiceImpl.BRANDING_CONTEXT,
                                            BrandingServiceImpl.BRANDING_SCOPE,
                                            "secondaryColor");
    verify(settingService, times(1)).remove(BrandingServiceImpl.BRANDING_CONTEXT,
                                            BrandingServiceImpl.BRANDING_SCOPE,
                                            "secondaryBackground");

    List<Context> contexts = settingContext.getAllValues();
    List<Scope> scopes = settingScope.getAllValues();
    List<String> keys = settingKey.getAllValues();
    List<SettingValue> values = settingValue.getAllValues();

    assertEquals(Context.GLOBAL, contexts.get(0));
    assertEquals(Scope.GLOBAL, scopes.get(0));
    assertEquals(BrandingServiceImpl.BRANDING_COMPANY_NAME_SETTING_KEY, keys.get(0));
    assertEquals("New Company Name", values.get(0).getValue());

    assertEquals(Context.GLOBAL, contexts.get(1));
    assertEquals(Scope.GLOBAL, scopes.get(1));
    assertEquals(BrandingServiceImpl.BRANDING_TOPBAR_THEME_SETTING_KEY, keys.get(1));
    assertEquals("Pink", values.get(1).getValue());

    assertEquals(BrandingServiceImpl.BRANDING_CONTEXT, contexts.get(2));
    assertEquals(BrandingServiceImpl.BRANDING_SCOPE, scopes.get(2));
    assertEquals(BrandingServiceImpl.BRANDING_CONTEXT, contexts.get(3));
    assertEquals(BrandingServiceImpl.BRANDING_SCOPE, scopes.get(3));

    String firstThemeColorParam = keys.get(2);
    String firstThemeColorValue = values.get(2).getValue().toString();

    String secondThemeColorParam = keys.get(3);
    String secondThemeColorValue = values.get(3).getValue().toString();

    assertEquals("primaryColor", secondThemeColorParam);
    assertEquals("primaryBackground", firstThemeColorParam);
    assertEquals(primaryColorNewValue, secondThemeColorValue);
    assertEquals(primaryBackgroundNewValue, firstThemeColorValue);
  }

  @Test
  public void shouldUpdateLogoWhenLogoUpdatedByUploadId() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    FileInfo fileInfo = new FileInfo(2L,
                                     "myLogo",
                                     "image/png",
                                     BrandingServiceImpl.FILE_API_NAME_SPACE,
                                     "myLogo".getBytes().length,
                                     new Date(),
                                     "john",
                                     null,
                                     false);
    FileItem fileItem = new FileItem(fileInfo, null);
    when(fileService.writeFile(any(FileItem.class))).thenReturn(fileItem);
    when(fileService.getFileInfo(anyLong())).thenReturn(fileInfo);
    String uploadId = "1";
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    UploadResource uploadResource = new UploadResource(uploadId);
    URL resource = this.getClass().getResource("/branding/logo.png");
    uploadResource.setStoreLocation(resource.getPath());
    when(uploadService.getUploadResource(uploadId)).thenReturn(uploadResource);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    BrandingServiceImpl brandingService = new BrandingServiceImpl(container,
                                                                  configurationManager,
                                                                  settingService,
                                                                  fileService,
                                                                  uploadService,
                                                                  localeConfigService,
                                                                  initParams);

    Branding newBranding = new Branding();
    Logo logo = new Logo();
    logo.setUploadId(uploadId);
    newBranding.setLogo(logo);

    ArgumentCaptor<Context> settingContextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> settingScopeArgumentCaptor = ArgumentCaptor.forClass(Scope.class);
    ArgumentCaptor<String> settingKeyArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SettingValue> settingValueArgumentCaptor = ArgumentCaptor.forClass(SettingValue.class);
    ArgumentCaptor<FileItem> fileItemArgumentCaptor = ArgumentCaptor.forClass(FileItem.class);

    // When
    brandingService.updateBrandingInformation(newBranding);

    // Then
    verify(settingService, times(2)).set(settingContextArgumentCaptor.capture(),
                                         settingScopeArgumentCaptor.capture(),
                                         settingKeyArgumentCaptor.capture(),
                                         settingValueArgumentCaptor.capture());
    verify(fileService, times(1)).writeFile(fileItemArgumentCaptor.capture());
    List<Context> contexts = settingContextArgumentCaptor.getAllValues();
    List<Scope> scopes = settingScopeArgumentCaptor.getAllValues();
    List<String> keys = settingKeyArgumentCaptor.getAllValues();
    List<SettingValue> values = settingValueArgumentCaptor.getAllValues();
    assertEquals(Context.GLOBAL, contexts.get(0));
    assertEquals(Scope.GLOBAL, scopes.get(0));
    assertEquals(BrandingServiceImpl.BRANDING_LOGO_ID_SETTING_KEY, keys.get(0));
    assertEquals("2", values.get(0).getValue());
    List<FileItem> fileItems = fileItemArgumentCaptor.getAllValues();
    assertTrue(Arrays.equals(IOUtils.toByteArray(resource), fileItems.get(0).getAsByte()));
  }

  @Test
  public void shouldUpdateFaviconWhenFaviconUpdatedByUploadId() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);

    FileInfo fileInfo = new FileInfo(2L,
                                     "myFavicon",
                                     "image/png",
                                     BrandingServiceImpl.FILE_API_NAME_SPACE,
                                     "myFavicon".getBytes().length,
                                     new Date(),
                                     "john",
                                     null,
                                     false);
    FileItem fileItem = new FileItem(fileInfo, null);
    when(fileService.writeFile(any(FileItem.class))).thenReturn(fileItem);
    when(fileService.getFileInfo(anyLong())).thenReturn(fileInfo);
    String uploadId = "1";
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    UploadResource uploadResource = new UploadResource(uploadId);
    URL resource = this.getClass().getResource("/branding/favicon.ico");
    uploadResource.setStoreLocation(resource.getPath());
    when(uploadService.getUploadResource(uploadId)).thenReturn(uploadResource);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    BrandingServiceImpl brandingService = new BrandingServiceImpl(container,
                                                                  configurationManager,
                                                                  settingService,
                                                                  fileService,
                                                                  uploadService,
                                                                  localeConfigService,
                                                                  initParams);

    Branding newBranding = new Branding();
    Favicon favicon = new Favicon();
    favicon.setUploadId(uploadId);
    newBranding.setFavicon(favicon);

    ArgumentCaptor<Context> settingContextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> settingScopeArgumentCaptor = ArgumentCaptor.forClass(Scope.class);
    ArgumentCaptor<String> settingKeyArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SettingValue> settingValueArgumentCaptor = ArgumentCaptor.forClass(SettingValue.class);
    ArgumentCaptor<FileItem> fileItemArgumentCaptor = ArgumentCaptor.forClass(FileItem.class);

    // When
    brandingService.updateBrandingInformation(newBranding);

    // Then
    verify(settingService, times(2)).set(settingContextArgumentCaptor.capture(),
                                         settingScopeArgumentCaptor.capture(),
                                         settingKeyArgumentCaptor.capture(),
                                         settingValueArgumentCaptor.capture());
    verify(fileService, times(1)).writeFile(fileItemArgumentCaptor.capture());
    List<Context> contexts = settingContextArgumentCaptor.getAllValues();
    List<Scope> scopes = settingScopeArgumentCaptor.getAllValues();
    List<String> keys = settingKeyArgumentCaptor.getAllValues();
    List<SettingValue> values = settingValueArgumentCaptor.getAllValues();
    assertEquals(Context.GLOBAL, contexts.get(0));
    assertEquals(Scope.GLOBAL, scopes.get(0));
    assertEquals(BrandingServiceImpl.BRANDING_FAVICON_ID_SETTING_KEY, keys.get(0));
    assertEquals("2", values.get(0).getValue());
    List<FileItem> fileItems = fileItemArgumentCaptor.getAllValues();
    assertTrue(Arrays.equals(IOUtils.toByteArray(resource), fileItems.get(0).getAsByte()));
  }

  @Test
  public void testGetDefaultLoginBackground() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    InitParams initParams = new InitParams();

    BrandingServiceImpl brandingService = new BrandingServiceImpl(container,
                                                                  configurationManager,
                                                                  settingService,
                                                                  fileService,
                                                                  uploadService,
                                                                  localeConfigService,
                                                                  initParams);

    assertNull(brandingService.getLoginBackgroundPath());

    String imagePath = "loginBgPath";

    ValueParam loginBgPath = new ValueParam();
    loginBgPath.setName(BrandingServiceImpl.BRANDING_LOGIN_BG_PARAM);
    loginBgPath.setValue(imagePath);
    initParams.addParam(loginBgPath);

    brandingService = new BrandingServiceImpl(container,
                                              configurationManager,
                                              settingService,
                                              fileService,
                                              uploadService,
                                              localeConfigService,
                                              initParams);

    ServletContext context = mock(ServletContext.class);
    when(container.getPortalContext()).thenReturn(context);
    when(context.getResourceAsStream(imagePath)).thenReturn(new ByteArrayInputStream(new byte[] {
        1, 2, 3
    }));
    Background loginBackground = brandingService.getLoginBackground();
    assertNotNull(loginBackground);
    assertEquals(3, loginBackground.getSize());
  }

  @Test
  public void testUpdateLoginBackground() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    PortalContainer container = mock(PortalContainer.class);
    UploadService uploadService = mock(UploadService.class);
    LocaleConfigService localeConfigService = mock(LocaleConfigService.class);
    InitParams initParams = new InitParams();

    BrandingServiceImpl brandingService = new BrandingServiceImpl(container,
                                                                  configurationManager,
                                                                  settingService,
                                                                  fileService,
                                                                  uploadService,
                                                                  localeConfigService,
                                                                  initParams);

    assertNull(brandingService.getLoginBackgroundPath());

    String uploadId = "uploadId";
    long fileId = 2l;

    UploadResource uploadResource = new UploadResource(uploadId, "fileName.png");
    uploadResource.setStoreLocation(this.getClass().getClassLoader().getResource("branding/favicon.ico").getPath());
    when(uploadService.getUploadResource(uploadId)).thenReturn(uploadResource);
    when(fileService.writeFile(any())).thenAnswer(invocation -> {
      FileItem fileItem = invocation.getArgument(0);
      fileItem.getFileInfo().setId(fileId);
      return fileItem;
    });

    Background loginBackground = new Background(uploadId, 0, null, 0);
    brandingService.updateLoginBackground(loginBackground);

    verify(fileService, times(1)).writeFile(argThat(fileItem -> {
      assertNotNull(fileItem);
      assertNotNull(fileItem.getFileInfo());
      assertEquals(BrandingServiceImpl.LOGIN_BACKGROUND_NAME, fileItem.getFileInfo().getName());
      assertEquals("image/png", fileItem.getFileInfo().getMimetype());
      assertEquals(BrandingServiceImpl.FILE_API_NAME_SPACE, fileItem.getFileInfo().getNameSpace());
      return true;
    }));

    verify(settingService, times(1)).set(any(),
                                         any(),
                                         eq(BrandingServiceImpl.BRANDING_LOGIN_BG_ID_SETTING_KEY),
                                         argThat(value -> value != null && value.getValue() != null
                                             && StringUtils.equals(value.getValue().toString(), String.valueOf(fileId))));
  }

}
