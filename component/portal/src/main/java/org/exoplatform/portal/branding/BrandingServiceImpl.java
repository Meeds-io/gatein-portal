/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.portal.branding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import com.github.sommeri.less4j.Less4jException;
import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessCompiler.Configuration;
import com.github.sommeri.less4j.core.ThreadUnsafeLessCompiler;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.branding.model.Background;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.BrandingFile;
import org.exoplatform.portal.branding.model.Favicon;
import org.exoplatform.portal.branding.model.Logo;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

@SuppressWarnings("unchecked")
public class BrandingServiceImpl implements BrandingService, Startable {

  private static final Log     LOG                               = ExoLogger.getExoLogger(BrandingServiceImpl.class);

  public static final String   BRANDING_RESET_ATTACHMENT_ID      = "0";

  public static final String   BRANDING_LOGO_BASE_PATH           = "/portal/rest/v1/platform/branding/logo?v=";            // NOSONAR

  public static final String   BRANDING_FAVICON_BASE_PATH        = "/portal/rest/v1/platform/branding/favicon?v=";         // NOSONAR

  public static final String   BRANDING_LOGIN_BG_BASE_PATH       = "/portal/rest/v1/platform/branding/loginBackground?v="; // NOSONAR

  public static final String   BRANDING_COMPANY_NAME_INIT_PARAM  = "exo.branding.company.name";

  // Will be used in Mail notification Footer by example
  public static final String   BRANDING_SITE_NAME_INIT_PARAM     = "exo.branding.company.siteName";

  // Will be used in Mail notification Footer by example
  public static final String   BRANDING_COMPANY_LINK_INIT_PARAM  = "exo.branding.company.link";

  public static final String   BRANDING_LOGIN_TITLE_PARAM        = "authentication.title";

  public static final String   BRANDING_LOGIN_SUBTITLE_PARAM     = "authentication.subtitle";

  public static final String   BRANDING_LOGIN_BG_PARAM           = "authentication.background";

  public static final String   BRANDING_LOGO_INIT_PARAM          = "exo.branding.company.logo";

  public static final String   BRANDING_COMPANY_NAME_SETTING_KEY = "exo.branding.company.name";

  public static final String   BRANDING_SITE_NAME_SETTING_KEY    = "exo.branding.company.siteName";

  public static final String   BRANDING_LOGIN_TEXT_COLOR_KEY     = "authentication.loginBackgroundTextColor";

  public static final String   BRANDING_TITLE_SETTING_KEY        = "authentication.title.";

  public static final String   BRANDING_SUBTITLE_SETTING_KEY     = "authentication.subtitle.";

  public static final String   BRANDING_COMPANY_LINK_SETTING_KEY = "exo.branding.company.link";

  public static final String   BRANDING_THEME_LESS_PATH          = "exo.branding.theme.path";

  public static final String   BRANDING_THEME_VARIABLES          = "exo.branding.theme.variables";

  public static final String   BRANDING_TOPBAR_THEME_SETTING_KEY = "bar_navigation_style";

  public static final String   BRANDING_LOGO_ID_SETTING_KEY      = "exo.branding.company.id";

  public static final String   BRANDING_FAVICON_ID_SETTING_KEY   = "exo.branding.company.favicon.id";

  public static final String   BRANDING_LOGIN_BG_ID_SETTING_KEY  = "authentication.background";

  public static final String   BRANDING_LAST_UPDATED_TIME_KEY    = "branding.lastUpdatedTime";

  public static final String   FILE_API_NAME_SPACE               = "CompanyBranding";

  public static final String   LOGO_NAME                         = "logo.png";

  public static final String   FAVICON_NAME                      = "favicon.ico";

  public static final String   LOGIN_BACKGROUND_NAME             = "loginBackground.png";

  public static final String   BRANDING_DEFAULT_LOGO_PATH        = "/skin/images/logo/DefaultLogo.png";                    // NOSONAR

  public static final String   BRANDING_DEFAULT_FAVICON_PATH     = "/skin/images/favicon.ico";                             // NOSONAR

  public static final Context  BRANDING_CONTEXT                  = Context.GLOBAL.id("BRANDING");

  public static final Scope    BRANDING_SCOPE                    = Scope.APPLICATION.id("BRANDING");

  public static final long     DEFAULT_LAST_MODIFED              = System.currentTimeMillis();

  private PortalContainer      container;

  private SettingService       settingService;

  private FileService          fileService;

  private UploadService        uploadService;

  private ConfigurationManager configurationManager;

  private String               defaultCompanyName                = "";

  private String               defaultSiteName                   = "";

  private String               defaultCompanyLink                = "";

  private String               defaultTopbarTheme                = "Dark";

  private String               defaultConfiguredLogoPath         = null;

  private String               defaultConfiguredFaviconPath      = null;

  private String               defaultConfiguredLoginBgPath      = null;

  private String               lessFilePath                      = null;

  private Map<String, String>  themeVariables                    = null;

  private String               defaultLoginTitle                 = null;

  private String               defaultLoginSubtitle              = null;

  private Map<String, String>  loginTitle                        = null;

  private Map<String, String>  loginSubtitle                     = null;

  private String               defaultLanguage                   = null;

  private Map<String, String>  supportedLanguages                = null;

  private String               lessThemeContent                  = null;

  private String               themeCSSContent                   = null;

  private Logo                 logo                              = null;

  private Favicon              favicon                           = null;

  private Background           loginBackground                   = null;

  public BrandingServiceImpl(PortalContainer container,
                             ConfigurationManager configurationManager,
                             SettingService settingService,
                             FileService fileService,
                             UploadService uploadService,
                             LocaleConfigService localeConfigService,
                             InitParams initParams) {
    this.container = container;
    this.configurationManager = configurationManager;
    this.settingService = settingService;
    this.fileService = fileService;
    this.uploadService = uploadService;

    this.loadLanguages(localeConfigService);
    this.loadInitParams(initParams);
  }

  @Override
  public void start() {
    computeThemeCSS();
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  @Override
  public String getThemeCSSContent() {
    if (themeCSSContent == null) {
      this.computeThemeCSS();
    }
    return themeCSSContent;
  }

  /**
   * Get all the branding information
   * 
   * @return The branding object containing all information
   */
  @Override
  public Branding getBrandingInformation() {
    Branding branding = new Branding();
    branding.setDefaultLanguage(defaultLanguage);
    branding.setSupportedLanguages(Collections.unmodifiableMap(supportedLanguages));
    branding.setCompanyName(getCompanyName());
    branding.setCompanyLink(getCompanyLink());
    branding.setSiteName(getSiteName());
    branding.setTopBarTheme(getTopBarTheme());
    branding.setLogo(getLogo());
    branding.setFavicon(getFavicon());
    branding.setLoginBackground(getLoginBackground());
    branding.setLoginBackgroundTextColor(getLoginBackgroundTextColor());
    branding.setThemeColors(getThemeColors());
    branding.setLoginTitle(getLoginTitle());
    branding.setLoginSubtitle(getLoginSubtitle());
    branding.setLastUpdatedTime(getLastUpdatedTime());
    return branding;
  }

  @Override
  public long getLastUpdatedTime() {
    SettingValue<?> lastUpdatedTime = settingService.get(Context.GLOBAL, Scope.GLOBAL, BRANDING_LAST_UPDATED_TIME_KEY);
    if (lastUpdatedTime == null || lastUpdatedTime.getValue() == null) {
      return DEFAULT_LAST_MODIFED;
    }
    return Long.parseLong(lastUpdatedTime.getValue().toString());
  }

  @Override
  public void updateBrandingInformation(Branding branding) {
    try {
      updateCompanyName(branding.getCompanyName(), false);
      updateSiteName(branding.getSiteName(), false);
      updateCompanyLink(branding.getCompanyLink(), false);
      updateTopBarTheme(branding.getTopBarTheme(), false);
      updateLogo(branding.getLogo(), false);
      updateFavicon(branding.getFavicon(), false);
      updateLoginBackground(branding.getLoginBackground(), false);
      updateLoginBackgroundTextColor(branding.getLoginBackgroundTextColor(), false);
      updateThemeColors(branding.getThemeColors(), false);
      updateLoginTitle(branding.getLoginTitle());
      updateLoginSubtitle(branding.getLoginSubtitle());
    } finally {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  @Override
  public String getCompanyName() {
    SettingValue<String> brandingCompanyName = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                                         Scope.GLOBAL,
                                                                                         BRANDING_COMPANY_NAME_SETTING_KEY);
    if (brandingCompanyName != null && StringUtils.isNotBlank(brandingCompanyName.getValue())) {
      return brandingCompanyName.getValue();
    } else {
      return defaultCompanyName;
    }
  }

  @Override
  public String getCompanyLink() {
    SettingValue<String> brandingCompanyLink = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                                         Scope.GLOBAL,
                                                                                         BRANDING_COMPANY_LINK_SETTING_KEY);
    if (brandingCompanyLink != null && StringUtils.isNotBlank(brandingCompanyLink.getValue())) {
      return brandingCompanyLink.getValue();
    } else {
      return defaultCompanyLink;
    }
  }

  @Override
  public String getSiteName() {
    SettingValue<String> brandingSiteName = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                                      Scope.GLOBAL,
                                                                                      BRANDING_SITE_NAME_SETTING_KEY);
    if (brandingSiteName != null && StringUtils.isNotBlank(brandingSiteName.getValue())) {
      return brandingSiteName.getValue();
    } else {
      return defaultSiteName;
    }
  }

  @Override
  public String getLoginBackgroundTextColor() {
    SettingValue<String> brandingLoginTextColor = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                                            Scope.GLOBAL,
                                                                                            BRANDING_LOGIN_TEXT_COLOR_KEY);
    if (brandingLoginTextColor != null && StringUtils.isNotBlank(brandingLoginTextColor.getValue())) {
      return brandingLoginTextColor.getValue();
    } else {
      return null;
    }
  }

  @Override
  public void updateCompanyName(String companyName) {
    updateCompanyName(companyName, true);
  }

  @Override
  public void updateCompanyLink(String companyLink) {
    updateCompanyLink(companyLink, true);
  }

  @Override
  public void updateSiteName(String siteName) {
    updateSiteName(siteName, true);
  }

  @Override
  public void updateLoginBackgroundTextColor(String textColor) {
    updateLoginBackgroundTextColor(textColor, true);
  }

  @Override
  public String getTopBarTheme() {
    SettingValue<String> topBarTheme = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                                 Scope.GLOBAL,
                                                                                 BRANDING_TOPBAR_THEME_SETTING_KEY);
    if (topBarTheme != null && StringUtils.isNotBlank(topBarTheme.getValue())) {
      return topBarTheme.getValue();
    } else {
      return defaultTopbarTheme;
    }
  }

  @Override
  public Long getLogoId() {
    SettingValue<String> logoId = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                            Scope.GLOBAL,
                                                                            BRANDING_LOGO_ID_SETTING_KEY);
    if (logoId != null && logoId.getValue() != null) {
      return Long.parseLong(logoId.getValue());
    } else {
      return null;
    }
  }

  @Override
  public Long getFaviconId() {
    SettingValue<String> faviconId = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                               Scope.GLOBAL,
                                                                               BRANDING_FAVICON_ID_SETTING_KEY);
    if (faviconId != null && faviconId.getValue() != null) {
      return Long.parseLong(faviconId.getValue());
    } else {
      return null;
    }
  }

  @Override
  public Long getLoginBackgroundId() {
    SettingValue<String> loginBackgroundId = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                                       Scope.GLOBAL,
                                                                                       BRANDING_LOGIN_BG_ID_SETTING_KEY);
    if (loginBackgroundId != null && loginBackgroundId.getValue() != null) {
      return Long.parseLong(loginBackgroundId.getValue());
    } else {
      return null;
    }
  }

  @Override
  public Logo getLogo() {
    if (this.logo == null) {
      try {
        Long imageId = getLogoId();
        if (imageId != null) {
          this.logo = retrieveStoredBrandingFile(imageId, new Logo());
        } else {
          this.logo = retrieveDefaultBrandingFile(defaultConfiguredLogoPath, new Logo());
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving logo", e);
      }
    }
    return this.logo;
  }

  @Override
  public Favicon getFavicon() {
    if (this.favicon == null) {
      try {
        Long imageId = getFaviconId();
        if (imageId != null) {
          this.favicon = retrieveStoredBrandingFile(imageId, new Favicon());
        } else {
          this.favicon = retrieveDefaultBrandingFile(defaultConfiguredFaviconPath, new Favicon());
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving favicon", e);
      }
    }
    return this.favicon;
  }

  @Override
  public Background getLoginBackground() {
    if (this.loginBackground == null) {
      try {
        Long imageId = getLoginBackgroundId();
        if (imageId != null) {
          this.loginBackground = retrieveStoredBrandingFile(imageId, new Background());
        } else if (StringUtils.isNotBlank(defaultConfiguredLoginBgPath)) {
          this.loginBackground = retrieveDefaultBrandingFile(defaultConfiguredLoginBgPath, new Background());
        } else {
          this.loginBackground = new Background();
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving login background", e);
      }
    }
    return this.loginBackground;
  }

  @Override
  public String getLogoPath() {
    Logo brandingLogo = getLogo();
    return BRANDING_LOGO_BASE_PATH + Objects.hash(brandingLogo.getUpdatedDate());
  }

  @Override
  public String getFaviconPath() {
    Favicon brandingFavicon = getFavicon();
    return BRANDING_FAVICON_BASE_PATH + Objects.hash(brandingFavicon.getUpdatedDate());
  }

  @Override
  public String getLoginBackgroundPath() {
    Background background = getLoginBackground();
    return background == null
        || background.getData() == null ? null : BRANDING_LOGIN_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public void updateTopBarTheme(String topBarTheme) {
    updateTopBarTheme(topBarTheme, true);
  }

  @Override
  public void updateLastUpdatedTime(long lastUpdatedTimestamp) {
    if (lastUpdatedTimestamp <= 0) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, BRANDING_LAST_UPDATED_TIME_KEY);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, BRANDING_LAST_UPDATED_TIME_KEY, SettingValue.create(lastUpdatedTimestamp));
    }
  }

  @Override
  public void updateLogo(Logo logo) {
    updateLogo(logo, true);
  }

  @Override
  public void updateFavicon(Favicon favicon) {
    updateFavicon(favicon, true);
  }

  @Override
  public void updateLoginBackground(Background loginBackground) {
    updateLoginBackground(loginBackground, true);
  }

  @Override
  public void updateThemeColors(Map<String, String> themeColors) {
    updateThemeColors(themeColors, true);
  }

  @Override
  public Map<String, String> getThemeColors() {
    if (themeVariables == null || themeVariables.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, String> themeColors = new HashMap<>();
    Set<String> variables = themeVariables.keySet();
    for (String themeVariable : variables) {
      SettingValue<?> storedColorValue = settingService.get(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable);
      String colorValue = storedColorValue == null
          || storedColorValue.getValue() == null ? themeVariables.get(themeVariable) : storedColorValue.getValue().toString();
      if (StringUtils.isNotBlank(colorValue)) {
        themeColors.put(themeVariable, colorValue);
      }
    }
    return themeColors;
  }

  @Override
  public Map<String, String> getLoginTitle() {
    if (this.loginTitle == null) {
      Map<String, String> valuePerLanguage = new HashMap<>();
      for (String language : supportedLanguages.keySet()) {
        SettingValue<?> storedValue = settingService.get(BRANDING_CONTEXT,
                                                         BRANDING_SCOPE,
                                                         BRANDING_TITLE_SETTING_KEY + language);
        if (storedValue != null && storedValue.getValue() != null) {
          valuePerLanguage.put(language, storedValue.getValue().toString());
        }
      }
      valuePerLanguage.computeIfAbsent(defaultLanguage, key -> this.defaultLoginTitle);
      this.loginTitle = valuePerLanguage;
    }
    return Collections.unmodifiableMap(this.loginTitle);
  }

  @Override
  public Map<String, String> getLoginSubtitle() {
    if (this.loginSubtitle == null) {
      Map<String, String> valuePerLanguage = new HashMap<>();
      for (String language : supportedLanguages.keySet()) {
        SettingValue<?> storedValue = settingService.get(BRANDING_CONTEXT,
                                                         BRANDING_SCOPE,
                                                         BRANDING_SUBTITLE_SETTING_KEY + language);
        if (storedValue != null && storedValue.getValue() != null) {
          valuePerLanguage.put(language, storedValue.getValue().toString());
        }
      }
      valuePerLanguage.computeIfAbsent(defaultLanguage, key -> this.defaultLoginSubtitle);
      this.loginSubtitle = valuePerLanguage;
    }
    return Collections.unmodifiableMap(this.loginSubtitle);
  }

  @Override
  public String getLoginTitle(Locale locale) {
    Map<String, String> valuePerLanguage = getLoginTitle();
    if (valuePerLanguage.containsKey(locale.getLanguage())) {
      return valuePerLanguage.get(locale.getLanguage());
    } else {
      return valuePerLanguage.getOrDefault(defaultLanguage, this.defaultLoginTitle);
    }
  }

  @Override
  public String getLoginSubtitle(Locale locale) {
    Map<String, String> valuePerLanguage = getLoginSubtitle();
    if (valuePerLanguage.containsKey(locale.getLanguage())) {
      return valuePerLanguage.get(locale.getLanguage());
    } else {
      return valuePerLanguage.getOrDefault(defaultLanguage, this.defaultLoginTitle);
    }
  }

  /**
   * Load init params
   * 
   * @param  initParams
   * @throws Exception
   */
  private void loadInitParams(InitParams initParams) { // NOSONAR
    if (initParams != null) {
      ValueParam companyNameParam = initParams.getValueParam(BRANDING_COMPANY_NAME_INIT_PARAM);
      if (companyNameParam != null) {
        this.defaultCompanyName = companyNameParam.getValue();
      }

      ValueParam companyLinkParam = initParams.getValueParam(BRANDING_COMPANY_LINK_INIT_PARAM);
      if (companyLinkParam != null) {
        this.defaultCompanyLink = companyLinkParam.getValue();
      }

      ValueParam siteNameParam = initParams.getValueParam(BRANDING_SITE_NAME_INIT_PARAM);
      if (siteNameParam != null) {
        this.defaultSiteName = siteNameParam.getValue();
      }

      ValueParam logoParam = initParams.getValueParam(BRANDING_LOGO_INIT_PARAM);
      if (logoParam != null) {
        this.defaultConfiguredLogoPath = logoParam.getValue();
      }

      ValueParam loginBackgroundParam = initParams.getValueParam(BRANDING_LOGIN_BG_PARAM);
      if (loginBackgroundParam != null) {
        this.defaultConfiguredLoginBgPath = loginBackgroundParam.getValue();
      }

      ValueParam loginTitleParam = initParams.getValueParam(BRANDING_LOGIN_TITLE_PARAM);
      if (loginTitleParam != null) {
        this.defaultLoginTitle = loginTitleParam.getValue();
      }

      ValueParam loginSubtitleParam = initParams.getValueParam(BRANDING_LOGIN_SUBTITLE_PARAM);
      if (loginSubtitleParam != null) {
        this.defaultLoginSubtitle = loginSubtitleParam.getValue();
      }

      ValueParam lessFileParam = initParams.getValueParam(BRANDING_THEME_LESS_PATH);
      if (lessFileParam != null) {
        this.lessFilePath = lessFileParam.getValue();
      }

      ValuesParam lessVariablesParam = initParams.getValuesParam(BRANDING_THEME_VARIABLES);
      if (lessVariablesParam != null) {
        List<String> variables = lessVariablesParam.getValues();
        this.themeVariables = new HashMap<>();
        for (String themeVariable : variables) {
          if (StringUtils.isBlank(themeVariable) || !themeVariable.contains(":")) {
            continue;
          }
          String[] themeVariablesPart = themeVariable.split(":");
          this.themeVariables.put(themeVariablesPart[0], themeVariablesPart[1]);
        }
      }
    }
  }

  private void loadLanguages(LocaleConfigService localeConfigService) {
    Locale defaultLocale = localeConfigService.getDefaultLocaleConfig() == null ? Locale.ENGLISH
                                                                                : localeConfigService.getDefaultLocaleConfig()
                                                                                                     .getLocale();
    this.defaultLanguage = defaultLocale.getLanguage();
    this.supportedLanguages =
                            localeConfigService.getLocalConfigs() == null ? Collections.singletonMap(defaultLocale.getLanguage(),
                                                                                                     getLocaleDisplayName(defaultLocale,
                                                                                                                          defaultLocale))
                                                                          : localeConfigService.getLocalConfigs()
                                                                                               .stream()
                                                                                               .filter(localeConfig -> !StringUtils.equals(localeConfig.getLocaleName(),
                                                                                                                                           "ma"))
                                                                                               .collect(Collectors.toMap(LocaleConfig::getLocaleName,
                                                                                                                         localeConfig -> getLocaleDisplayName(defaultLocale,
                                                                                                                                                              localeConfig.getLocale())));
  }

  private String getLocaleDisplayName(Locale defaultLocale, Locale locale) {
    return defaultLocale.equals(locale) ? defaultLocale.getDisplayName(defaultLocale)
                                        : locale.getDisplayName(defaultLocale) + " / " + locale.getDisplayName(locale);
  }

  private void updateTopBarTheme(String topBarTheme, boolean updateLastUpdatedTime) {
    if (StringUtils.isBlank(topBarTheme)) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, BRANDING_TOPBAR_THEME_SETTING_KEY);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, BRANDING_TOPBAR_THEME_SETTING_KEY, SettingValue.create(topBarTheme));
    }
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private void updateCompanyLink(String companyLink, boolean updateLastUpdatedTime) {
    if (StringUtils.isEmpty(companyLink)) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, BRANDING_COMPANY_LINK_SETTING_KEY);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, BRANDING_COMPANY_LINK_SETTING_KEY, SettingValue.create(companyLink));
    }
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private void updateSiteName(String siteName, boolean updateLastUpdatedTime) {
    if (StringUtils.isEmpty(siteName)) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, BRANDING_SITE_NAME_SETTING_KEY);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, BRANDING_SITE_NAME_SETTING_KEY, SettingValue.create(siteName));
    }
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private void updateLoginBackgroundTextColor(String textColor, boolean updateLastUpdatedTime) {
    if (StringUtils.isBlank(textColor)) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, BRANDING_LOGIN_TEXT_COLOR_KEY);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, BRANDING_LOGIN_TEXT_COLOR_KEY, SettingValue.create(textColor));
    }
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private void updateCompanyName(String companyName, boolean updateLastUpdatedTime) {
    if (StringUtils.isEmpty(companyName)) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, BRANDING_COMPANY_NAME_SETTING_KEY);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, BRANDING_COMPANY_NAME_SETTING_KEY, SettingValue.create(companyName));
    }
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private void updateThemeColors(Map<String, String> themeColors, boolean updateLastUpdatedTime) {
    if (themeVariables == null || themeVariables.isEmpty()) {
      return;
    }

    Set<String> variables = themeVariables.keySet();
    for (String themeVariable : variables) {
      if (themeColors != null && themeColors.get(themeVariable) != null) {
        String themeColor = themeColors.get(themeVariable);
        settingService.set(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable, SettingValue.create(themeColor));
      } else {
        settingService.remove(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable);
      }
    }

    // Refresh Theme
    computeThemeCSS();
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private void updateLoginTitle(Map<String, String> titles) {
    for (String language : supportedLanguages.keySet()) {
      if (titles.containsKey(language)) {
        String value = titles.get(language);
        settingService.set(BRANDING_CONTEXT,
                           BRANDING_SCOPE,
                           BRANDING_TITLE_SETTING_KEY + language,
                           SettingValue.create(value == null ? "" : value));
      } else {
        settingService.remove(BRANDING_CONTEXT,
                              BRANDING_SCOPE,
                              BRANDING_TITLE_SETTING_KEY + language);
      }
    }
    this.loginTitle = null;
  }

  private void updateLoginSubtitle(Map<String, String> subtitles) {
    for (String language : supportedLanguages.keySet()) {
      if (subtitles.containsKey(language)) {
        String value = subtitles.get(language);
        settingService.set(BRANDING_CONTEXT,
                           BRANDING_SCOPE,
                           BRANDING_SUBTITLE_SETTING_KEY + language,
                           SettingValue.create(value == null ? "" : value));
      } else {
        settingService.remove(BRANDING_CONTEXT,
                              BRANDING_SCOPE,
                              BRANDING_SUBTITLE_SETTING_KEY + language);
      }
    }
    this.loginSubtitle = null;
  }

  private void updateLogo(Logo logo, boolean updateLastUpdatedTime) { // NOSONAR
    updateBrandingFile(logo, LOGO_NAME, this.getLogoId(), BRANDING_LOGO_ID_SETTING_KEY);
    this.logo = null;
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private void updateFavicon(Favicon favicon, boolean updateLastUpdatedTime) {
    updateBrandingFile(favicon, FAVICON_NAME, this.getFaviconId(), BRANDING_FAVICON_ID_SETTING_KEY);
    this.favicon = null;
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private void updateLoginBackground(Background loginBackground, boolean updateLastUpdatedTime) {
    updateBrandingFile(loginBackground, LOGIN_BACKGROUND_NAME, this.getLoginBackgroundId(), BRANDING_LOGIN_BG_ID_SETTING_KEY);
    this.loginBackground = null;
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private void updateBrandingFile(BrandingFile brandingFile, String fileName, Long fileId, String settingKey) {
    String uploadId = brandingFile == null ? null : brandingFile.getUploadId();
    if (StringUtils.isNotBlank(uploadId)) {
      try {
        if (StringUtils.equals(BRANDING_RESET_ATTACHMENT_ID, uploadId)) {
          removeBrandingFile(fileId, settingKey);
        } else {
          updateBrandingFileByUploadId(uploadId, fileName, fileId, settingKey);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Error while updating login background", e);
      }
    }
  }

  private void removeBrandingFile(Long fileId, String settingKey) {
    if (fileId != null) {
      fileService.deleteFile(fileId);
      settingService.remove(Context.GLOBAL,
                            Scope.GLOBAL,
                            settingKey);
    }
  }

  private void updateBrandingFileByUploadId(String uploadId,
                                            String fileName,
                                            Long fileId,
                                            String settingKey) throws Exception {
    InputStream inputStream = getUploadDataAsStream(uploadId);
    if (inputStream == null) {
      throw new IllegalArgumentException("Cannot update " + fileName
          + ", the object must contain the image data or an upload id");
    }
    int size = inputStream.available();
    FileItem fileItem = new FileItem(fileId,
                                     fileName,
                                     "image/png",
                                     FILE_API_NAME_SPACE,
                                     size,
                                     new Date(),
                                     getCurrentUserId(),
                                     false,
                                     inputStream);
    if (fileId == null) {
      fileItem = fileService.writeFile(fileItem);
      settingService.set(Context.GLOBAL,
                         Scope.GLOBAL,
                         settingKey,
                         SettingValue.create(String.valueOf(fileItem.getFileInfo().getId())));
    } else {
      fileService.updateFile(fileItem);
    }
  }

  private String computeThemeCSS() {// NOSONAR
    if (StringUtils.isNotBlank(lessFilePath)) {
      try {
        InputStream inputStream = configurationManager.getInputStream(lessFilePath);
        lessThemeContent = IOUtil.getStreamContentAsString(inputStream);
      } catch (Exception e) {
        LOG.warn("Error retrieving less file content", e);
      }
    }

    if (themeVariables != null && !themeVariables.isEmpty()) {
      Set<String> variables = themeVariables.keySet();
      for (String themeVariable : variables) {
        SettingValue<?> storedColorValue = settingService.get(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable);
        String colorValue = storedColorValue == null
            || storedColorValue.getValue() == null ? themeVariables.get(themeVariable) : storedColorValue.getValue().toString();
        if (StringUtils.isNotBlank(colorValue) && StringUtils.isNotBlank(lessThemeContent)) {
          lessThemeContent = lessThemeContent.replaceAll("@" + themeVariable + ":[ #a-zA-Z0-9]*;?\r?\n",
                                                         "@" + themeVariable + ": " + colorValue + ";\n");
        }
      }

      if (StringUtils.isNotBlank(lessThemeContent)) {
        LessCompiler compiler = new ThreadUnsafeLessCompiler();
        try {
          Configuration configuration = new Configuration();
          configuration.getSourceMapConfiguration().setLinkSourceMap(false);
          LessCompiler.CompilationResult result = compiler.compile(lessThemeContent, configuration);
          this.themeCSSContent = result.getCss();
        } catch (Less4jException e) {
          LOG.warn("Error compiling less file content", e);
        }
      }
    }

    return this.themeCSSContent;
  }

  private InputStream getUploadDataAsStream(String uploadId) throws FileNotFoundException {
    UploadResource uploadResource = uploadService.getUploadResource(uploadId);
    if (uploadResource == null) {
      return null;
    } else {
      try {// NOSONAR
        return new FileInputStream(new File(uploadResource.getStoreLocation()));
      } finally {
        uploadService.removeUploadResource(uploadId);
      }
    }
  }

  private String getCurrentUserId() {
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState != null && conversationState.getIdentity() != null) {
      return conversationState.getIdentity().getUserId();
    }
    return null;
  }

  private <T extends BrandingFile> T retrieveStoredBrandingFile(Long imageId, T brandingFile) throws FileStorageException {
    FileItem fileItem = fileService.getFile(imageId);
    if (fileItem != null) {
      brandingFile.setData(fileItem.getAsByte());
      brandingFile.setSize(fileItem.getFileInfo().getSize());
      brandingFile.setUpdatedDate(fileItem.getFileInfo().getUpdatedDate().getTime());
    }
    return brandingFile;
  }

  private <T extends BrandingFile> T retrieveDefaultBrandingFile(String imagePath, T brandingFile) throws IOException {
    if (StringUtils.isNotBlank(imagePath)) {
      File file = new File(imagePath);
      if (file.exists()) {
        brandingFile.setData(Files.readAllBytes(file.toPath()));
        brandingFile.setSize(file.length());
        brandingFile.setUpdatedDate(file.lastModified());
      } else {
        InputStream is = container.getPortalContext().getResourceAsStream(imagePath);
        if (is != null) {
          byte[] streamContentAsBytes = IOUtil.getStreamContentAsBytes(is);
          brandingFile.setData(streamContentAsBytes);
          brandingFile.setSize(streamContentAsBytes.length);
          brandingFile.setUpdatedDate(DEFAULT_LAST_MODIFED);
        }
      }
    }
    return brandingFile;
  }

}
