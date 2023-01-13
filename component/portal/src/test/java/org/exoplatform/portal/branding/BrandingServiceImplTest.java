package org.exoplatform.portal.branding;

import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.*;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.junit.Test;

import org.exoplatform.commons.api.settings.SettingService;
import org.mockito.ArgumentCaptor;

import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BrandingServiceImplTest {

  @Test
  public void shouldGetDefaultBrandingInformationWhenNoUpdate() {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

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

    BrandingService brandingService = new BrandingServiceImpl(configurationManager, settingService, fileService, uploadService, initParams);

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

    BrandingServiceImpl brandingService = new BrandingServiceImpl(configurationManager, settingService, fileService, uploadService, initParams);
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

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void shouldGetSavedBrandingThemeColorsWhenUpdate() {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

    String primaryColorNewValue = "#3f8488";
    String primaryBackgroundNewValue = "#f0f0f1";
    String secondaryColorNewValue = "#000001";
    String secondaryBackgroundNewValue = "#e25d5e";

    when(settingService.get(eq(BrandingServiceImpl.BRANDING_CONTEXT),
                            eq(BrandingServiceImpl.BRANDING_SCOPE),
                            eq("primaryColor"))).thenReturn((SettingValue) SettingValue.create(primaryColorNewValue));
    when(settingService.get(eq(BrandingServiceImpl.BRANDING_CONTEXT),
                            eq(BrandingServiceImpl.BRANDING_SCOPE),
                            eq("primaryBackground"))).thenReturn((SettingValue) SettingValue.create(primaryBackgroundNewValue));
    when(settingService.get(eq(BrandingServiceImpl.BRANDING_CONTEXT),
                            eq(BrandingServiceImpl.BRANDING_SCOPE),
                            eq("secondaryColor"))).thenReturn((SettingValue) SettingValue.create(secondaryColorNewValue));
    when(settingService.get(eq(BrandingServiceImpl.BRANDING_CONTEXT),
                            eq(BrandingServiceImpl.BRANDING_SCOPE),
                            eq("secondaryBackground"))).thenReturn((SettingValue) SettingValue.create(secondaryBackgroundNewValue));

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

    BrandingServiceImpl brandingService = new BrandingServiceImpl(configurationManager, settingService, fileService, uploadService, initParams);
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
    when(settingService.get(eq(Context.GLOBAL), eq(Scope.GLOBAL), eq(BrandingServiceImpl.BRANDING_COMPANY_NAME_SETTING_KEY))).thenReturn(new SettingValue("Updated Company Name"));
    when(settingService.get(eq(Context.GLOBAL), eq(Scope.GLOBAL), eq(BrandingServiceImpl.BRANDING_COMPANY_LINK_SETTING_KEY))).thenReturn(new SettingValue("https://investors.meeds.io"));
    when(settingService.get(eq(Context.GLOBAL), eq(Scope.GLOBAL), eq(BrandingServiceImpl.BRANDING_SITE_NAME_SETTING_KEY))).thenReturn(new SettingValue("Meeds.io"));
    FileService fileService = mock(FileService.class);
    UploadService uploadService = mock(UploadService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

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

    BrandingService brandingService = new BrandingServiceImpl(configurationManager, settingService, fileService, uploadService, initParams);

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
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

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

    BrandingService brandingService = new BrandingServiceImpl(configurationManager, settingService, fileService, uploadService, initParams);

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
    verify(settingService, times(5)).set(settingContext.capture(), settingScope.capture(), settingKey.capture(), settingValue.capture());
    verify(settingService, times(1)).remove(eq(BrandingServiceImpl.BRANDING_CONTEXT), eq(BrandingServiceImpl.BRANDING_SCOPE), eq("secondaryColor"));
    verify(settingService, times(1)).remove(eq(BrandingServiceImpl.BRANDING_CONTEXT), eq(BrandingServiceImpl.BRANDING_SCOPE), eq("secondaryBackground"));

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
  public void shouldUpdateLogoWhenLogoUpdatedByData() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    FileInfo fileInfo = new FileInfo(1L, "myLogo", "image/png",
            BrandingServiceImpl.FILE_API_NAME_SPACE, "myLogo".getBytes().length, new Date(), "john", null, false);
    FileItem fileItem = new FileItem(fileInfo, null);
    when(fileService.writeFile(any(FileItem.class))).thenReturn(fileItem);
    when(fileService.getFileInfo(anyLong())).thenReturn(fileInfo);
    UploadService uploadService = mock(UploadService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    BrandingService brandingService = new BrandingServiceImpl(configurationManager, settingService, fileService, uploadService, initParams);

    Branding newBranding = new Branding();
    Logo logo = new Logo();
    logo.setData("myLogo".getBytes());
    logo.setSize(logo.getData().length);
    newBranding.setLogo(logo);

    ArgumentCaptor<Context> settingContextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> settingScopeArgumentCaptor = ArgumentCaptor.forClass(Scope.class);
    ArgumentCaptor<String> settingKeyArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<SettingValue> settingValueArgumentCaptor = ArgumentCaptor.forClass(SettingValue.class);
    ArgumentCaptor<FileItem> fileItemArgumentCaptor = ArgumentCaptor.forClass(FileItem.class);

    // When
    brandingService.updateBrandingInformation(newBranding);

    // Then
    verify(settingService, times(2)).set(settingContextArgumentCaptor.capture(), settingScopeArgumentCaptor.capture(), settingKeyArgumentCaptor.capture(), settingValueArgumentCaptor.capture());
    verify(fileService, times(1)).writeFile(fileItemArgumentCaptor.capture());
    List<Context> contexts = settingContextArgumentCaptor.getAllValues();
    List<Scope> scopes = settingScopeArgumentCaptor.getAllValues();
    List<String> keys = settingKeyArgumentCaptor.getAllValues();
    List<SettingValue> values = settingValueArgumentCaptor.getAllValues();
    assertEquals(Context.GLOBAL, contexts.get(0));
    assertEquals(Scope.GLOBAL, scopes.get(0));
    assertEquals(BrandingServiceImpl.BRANDING_LOGO_ID_SETTING_KEY, keys.get(0));
    assertEquals("1", values.get(0).getValue());
    List<FileItem> fileItems = fileItemArgumentCaptor.getAllValues();
    assertEquals("myLogo", new String(fileItems.get(0).getAsByte()));
  }

  @Test
  public void shouldUpdateLogoWhenLogoUpdatedByUploadId() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

    FileInfo fileInfo = new FileInfo(2L, "myLogo", "image/png",
            BrandingServiceImpl.FILE_API_NAME_SPACE, "myLogo".getBytes().length, new Date(), "john", null, false);
    FileItem fileItem = new FileItem(fileInfo, null);
    when(fileService.writeFile(any(FileItem.class))).thenReturn(fileItem);
    when(fileService.getFileInfo(anyLong())).thenReturn(fileInfo);
    String uploadId = "1";
    UploadService uploadService = mock(UploadService.class);
    UploadResource uploadResource = new UploadResource(uploadId);
    URL resource = this.getClass().getResource("/branding/logo.png");
    uploadResource.setStoreLocation(resource.getPath());
    when(uploadService.getUploadResource(eq(uploadId))).thenReturn(uploadResource);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    BrandingService brandingService = new BrandingServiceImpl(configurationManager, settingService, fileService, uploadService, initParams);

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
    verify(settingService, times(2)).set(settingContextArgumentCaptor.capture(), settingScopeArgumentCaptor.capture(), settingKeyArgumentCaptor.capture(), settingValueArgumentCaptor.capture());
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
  public void shouldUpdateFaviconWhenFaviconUpdatedByData() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    FileInfo fileInfo = new FileInfo(1L,
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
    UploadService uploadService = mock(UploadService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    BrandingService brandingService = new BrandingServiceImpl(configurationManager,
                                                              settingService,
                                                              fileService,
                                                              uploadService,
                                                              initParams);

    Branding newBranding = new Branding();
    Favicon favicon = new Favicon();
    favicon.setData("myFavicon".getBytes());
    favicon.setSize(favicon.getData().length);
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
    assertEquals("1", values.get(0).getValue());
    List<FileItem> fileItems = fileItemArgumentCaptor.getAllValues();
    assertEquals("myFavicon", new String(fileItems.get(0).getAsByte()));
  }

  @Test
  public void shouldUpdateFaviconWhenFaviconUpdatedByUploadId() throws Exception {
    // Given
    SettingService settingService = mock(SettingService.class);
    FileService fileService = mock(FileService.class);
    ConfigurationManager configurationManager = mock(ConfigurationManager.class);

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
    UploadResource uploadResource = new UploadResource(uploadId);
    URL resource = this.getClass().getResource("/branding/favicon.ico");
    uploadResource.setStoreLocation(resource.getPath());
    when(uploadService.getUploadResource(eq(uploadId))).thenReturn(uploadResource);

    InitParams initParams = new InitParams();
    ValueParam companyName = new ValueParam();
    companyName.setName(BrandingServiceImpl.BRANDING_COMPANY_NAME_INIT_PARAM);
    companyName.setValue("Default Company Name");
    initParams.addParam(companyName);

    BrandingService brandingService = new BrandingServiceImpl(configurationManager,
                                                              settingService,
                                                              fileService,
                                                              uploadService,
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
}
