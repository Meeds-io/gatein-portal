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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
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

import org.exoplatform.commons.api.settings.ExoFeatureService;
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
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import lombok.SneakyThrows;

@SuppressWarnings("unchecked")
public class BrandingServiceImpl implements BrandingService, Startable {

  private static final Log     LOG                               = ExoLogger.getExoLogger(BrandingServiceImpl.class);

  public static final String   BRANDING_RESET_ATTACHMENT_ID      = "0";

  public static final String   BRANDING_LOGO_BASE_PATH           = "/portal/rest/v1/platform/branding/logo?v=";            // NOSONAR

  public static final String   BRANDING_FAVICON_BASE_PATH        = "/portal/rest/v1/platform/branding/favicon?v=";         // NOSONAR

  public static final String   BRANDING_LOGIN_BG_BASE_PATH       = "/portal/rest/v1/platform/branding/loginBackground?v="; // NOSONAR

  public static final String   BRANDING_PAGE_BG_BASE_PATH        = "/portal/rest/v1/platform/branding/pageBackground?v=";  // NOSONAR

  public static final String   BRANDING_COMPANY_NAME_INIT_PARAM  = "exo.branding.company.name";

  // Will be used in Mail notification Footer by example
  public static final String   BRANDING_SITE_NAME_INIT_PARAM     = "exo.branding.company.siteName";

  // Will be used in Mail notification Footer by example
  public static final String   BRANDING_COMPANY_LINK_INIT_PARAM  = "exo.branding.company.link";

  public static final String   BRANDING_LOGIN_TITLE_PARAM        = "authentication.title";

  public static final String   BRANDING_LOGIN_SUBTITLE_PARAM     = "authentication.subtitle";

  public static final String   BRANDING_LOGIN_BG_INIT_PARAM      = "authentication.background";

  public static final String   BRANDING_LOGO_INIT_PARAM          = "exo.branding.company.logo";

  public static final String   BRANDING_FAVICON_INIT_PARAM       = "exo.branding.company.favicon";

  public static final String   BRANDING_COMPANY_NAME_SETTING_KEY = "exo.branding.company.name";

  public static final String   BRANDING_SITE_NAME_SETTING_KEY    = "exo.branding.company.siteName";

  public static final String   BRANDING_LOGIN_TEXT_COLOR_KEY     = "authentication.loginBackgroundTextColor";

  public static final String   BRANDING_TITLE_SETTING_KEY        = "authentication.title.";

  public static final String   BRANDING_SUBTITLE_SETTING_KEY     = "authentication.subtitle.";

  public static final String   BRANDING_COMPANY_LINK_SETTING_KEY = "exo.branding.company.link";

  public static final String   BRANDING_THEME_LESS_PATH          = "exo.branding.theme.path";

  public static final String   BRANDING_THEME_VARIABLES          = "exo.branding.theme.variables";

  public static final String   BRANDING_LOGO_ID_SETTING_KEY      = "exo.branding.company.id";

  public static final String   BRANDING_FAVICON_ID_SETTING_KEY   = "exo.branding.company.favicon.id";

  public static final String   BRANDING_LOGIN_BG_ID_SETTING_KEY  = "authentication.background";

  public static final String   BRANDING_PAGE_BG_ID_SETTING_KEY   = "page.background";

  public static final String   BRANDING_PAGE_BG_COLOR_KEY        = "page.backgroundColor";

  public static final String   BRANDING_PAGE_BG_REPEAT_KEY       = "page.backgroundRepeat";

  public static final String   BRANDING_PAGE_WIDTH_KEY           = "page.width";

  public static final String   BRANDING_CUSTOM_CSS               = "page.customCss";

  public static final String   BRANDING_PAGE_BG_POSITION_KEY     = "page.backgroundPosition";

  public static final String   BRANDING_PAGE_BG_SIZE_KEY         = "page.backgroundSize";

  public static final String   BRANDING_LAST_UPDATED_TIME_KEY    = "branding.lastUpdatedTime";

  public static final String   FILE_API_NAME_SPACE               = "CompanyBranding";

  public static final String   LOGO_NAME                         = "logo.png";

  public static final String   FAVICON_NAME                      = "favicon.ico";

  public static final String   LOGIN_BACKGROUND_NAME             = "loginBackground.png";

  public static final String   PAGE_BACKGROUND_NAME              = "pageBackground.png";

  public static final String   BRANDING_CUSTOM_STYLE_FEATURE     = "customStylesheet";

  public static final String   BRANDING_DEFAULT_LOGO_PATH        = "/skin/images/logo/DefaultLogo.png";                    // NOSONAR

  public static final String   BRANDING_DEFAULT_FAVICON_PATH     = "/skin/images/favicon.ico";                             // NOSONAR

  public static final Context  BRANDING_CONTEXT                  = Context.GLOBAL.id("BRANDING");

  public static final Scope    BRANDING_SCOPE                    = Scope.APPLICATION.id("BRANDING");

  public static final long     DEFAULT_LAST_MODIFED              = System.currentTimeMillis();

  private PortalContainer      container;

  private LocaleConfigService  localeConfigService;

  private SettingService       settingService;

  private FileService          fileService;

  private UploadService        uploadService;

  private ConfigurationManager configurationManager;

  private ExoFeatureService    featureService;

  private ListenerService      listenerService;

  private String               defaultCompanyName                = "";

  private String               defaultSiteName                   = "";

  private String               defaultCompanyLink                = "";

  private String               defaultConfiguredLogoPath         = null;

  private String               defaultConfiguredFaviconPath      = null;

  private String               defaultConfiguredLoginBgPath      = null;

  private String               lessFilePath                      = null;

  private Map<String, String>  themeVariables                    = null;

  private String               defaultLoginTitle                 = null;

  private String               defaultLoginSubtitle              = null;

  private Map<String, String>  loginTitle                        = null;

  private Map<String, String>  loginSubtitle                     = null;

  private Map<String, String>  supportedLanguages                = null;

  private String               lessThemeContent                  = null;

  private String               themeCSSContent                   = null;

  private String               customCss                         = null;

  private Logo                 logo                              = null;

  private Favicon              favicon                           = null;

  private Background           loginBackground                   = null;

  private Background           pageBackground                    = null;

  public BrandingServiceImpl(PortalContainer container, // NOSONAR
                             ConfigurationManager configurationManager,
                             SettingService settingService,
                             FileService fileService,
                             UploadService uploadService,
                             LocaleConfigService localeConfigService,
                             ListenerService listenerService,
                             InitParams initParams) {
    this.container = container;
    this.configurationManager = configurationManager;
    this.settingService = settingService;
    this.fileService = fileService;
    this.uploadService = uploadService;
    this.localeConfigService = localeConfigService;
    this.listenerService = listenerService;

    this.loadLanguages();
    this.loadInitParams(initParams);
  }

  @Override
  public void start() {
    computeThemeCSS();
    listenerService.addListener(ExoFeatureService.FEATURE_STATUS_CHANGED_EVENT, e -> {
      if (StringUtils.equals(BRANDING_CUSTOM_STYLE_FEATURE, (String) e.getSource())) {
        this.triggerBrandingUpdated(true, true);
      }
    });
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
    return getBrandingInformation(true);
  }

  @Override
  public Branding getBrandingInformation(boolean retrieveBinaries) {
    Branding branding = new Branding();
    branding.setDefaultLanguage(getDefaultLanguage());
    branding.setDirection(getDefaultLocaleDirection());
    branding.setSupportedLanguages(loadLanguages());
    branding.setCompanyName(getCompanyName());
    branding.setCompanyLink(getCompanyLink());
    branding.setSiteName(getSiteName());
    branding.setLogo(getLogo());
    branding.setFavicon(getFavicon());
    branding.setLoginBackground(getLoginBackground());
    branding.setLoginBackgroundTextColor(getLoginBackgroundTextColor());
    branding.setPageBackground(getPageBackground());
    branding.setPageBackgroundColor(getPageBackgroundColor());
    branding.setPageBackgroundPosition(getPageBackgroundPosition());
    branding.setPageBackgroundSize(getPageBackgroundSize());
    branding.setPageBackgroundRepeat(getPageBackgroundRepeat());
    branding.setPageWidth(getPageWidth());
    branding.setCustomCss(getCustomCss());
    branding.setThemeStyle(getThemeStyle());
    branding.setLoginTitle(getLoginTitle());
    branding.setLoginSubtitle(getLoginSubtitle());
    branding.setLastUpdatedTime(getLastUpdatedTime());
    if (!retrieveBinaries) {
      if (branding.getFavicon() != null && branding.getFavicon().getData() != null) {
        Favicon brandingFile = branding.getFavicon().clone();
        brandingFile.setData(null);
        branding.setFavicon(brandingFile);
      }
      if (branding.getLogo() != null && branding.getLogo().getData() != null) {
        Logo brandingFile = branding.getLogo().clone();
        brandingFile.setData(null);
        branding.setLogo(brandingFile);
      }
      if (branding.getLoginBackground() != null && branding.getLoginBackground().getData() != null) {
        Background brandingFile = branding.getLoginBackground().clone();
        brandingFile.setData(null);
        branding.setLoginBackground(brandingFile);
      }
      if (branding.getPageBackground() != null && branding.getPageBackground().getData() != null) {
        Background brandingFile = branding.getPageBackground().clone();
        brandingFile.setData(null);
        branding.setPageBackground(brandingFile);
      }
    }
    return branding;
  }

  @Override
  public long getLastUpdatedTime() {
    String lastUpdatedTime = getPropertyValue(BRANDING_LAST_UPDATED_TIME_KEY);
    if (lastUpdatedTime == null) {
      return DEFAULT_LAST_MODIFED;
    } else {
      return Long.parseLong(lastUpdatedTime);
    }
  }

  @Override
  public void updateBrandingInformation(Branding branding) {
    validateCSSInputs(branding);
    try {
      updateCompanyName(branding.getCompanyName(), false);
      updateSiteName(branding.getSiteName(), false);
      updateCompanyLink(branding.getCompanyLink(), false);
      updateLogo(branding.getLogo(), false);
      updateFavicon(branding.getFavicon(), false);
      updateLoginBackground(branding.getLoginBackground(), false);
      updateLoginBackgroundTextColor(branding.getLoginBackgroundTextColor(), false);
      updatePageBackground(branding.getPageBackground(), false);
      updatePageBackgroundColor(branding.getPageBackgroundColor(), false);
      updatePageBackgroundSize(branding.getPageBackgroundSize(), false);
      updatePageBackgroundPosition(branding.getPageBackgroundPosition(), false);
      updatePageBackgroundRepeat(branding.getPageBackgroundRepeat(), false);
      updatePageWidth(branding.getPageWidth(), false);
      updateCustomCss(branding.getCustomCss(), false);
      updateThemeStyle(branding.getThemeStyle(), false);
      updateLoginTitle(branding.getLoginTitle());
      updateLoginSubtitle(branding.getLoginSubtitle());
    } finally {
      triggerBrandingUpdated(true, true);
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
    return getPropertyValue(BRANDING_COMPANY_LINK_SETTING_KEY, defaultCompanyLink);
  }

  @Override
  public String getSiteName() {
    return getPropertyValue(BRANDING_SITE_NAME_SETTING_KEY, defaultSiteName);
  }

  @Override
  public String getPageBackgroundColor() {
    return getPropertyValue(BRANDING_PAGE_BG_COLOR_KEY);
  }

  @Override
  public String getPageBackgroundRepeat() {
    return getPropertyValue(BRANDING_PAGE_BG_REPEAT_KEY);
  }

  @Override
  public String getPageWidth() {
    return getPropertyValue(BRANDING_PAGE_WIDTH_KEY);
  }

  @Override
  public String getCustomCss() {
    return getPropertyValue(BRANDING_CUSTOM_CSS);
  }

  @Override
  public String getPageBackgroundPosition() {
    return getPropertyValue(BRANDING_PAGE_BG_POSITION_KEY);
  }

  @Override
  public String getPageBackgroundSize() {
    return getPropertyValue(BRANDING_PAGE_BG_SIZE_KEY);
  }

  @Override
  public String getLoginBackgroundTextColor() {
    return getPropertyValue(BRANDING_LOGIN_TEXT_COLOR_KEY);
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
  public Long getLogoId() {
    return getPropertyValueLong(BRANDING_LOGO_ID_SETTING_KEY);
  }

  @Override
  public Long getFaviconId() {
    return getPropertyValueLong(BRANDING_FAVICON_ID_SETTING_KEY);
  }

  @Override
  public Long getLoginBackgroundId() {
    return getPropertyValueLong(BRANDING_LOGIN_BG_ID_SETTING_KEY);
  }

  @Override
  public Long getPageBackgroundId() {
    return getPropertyValueLong(BRANDING_PAGE_BG_ID_SETTING_KEY);
  }

  @Override
  public Logo getLogo() {
    if (this.logo == null) {
      try {
        Long imageId = getLogoId();
        if (imageId != null && imageId > 0) {
          this.logo = retrieveStoredBrandingFile(imageId, new Logo());
        } else {
          this.logo = retrieveDefaultBrandingFile(defaultConfiguredLogoPath, new Logo(), LOGO_NAME, BRANDING_LOGO_ID_SETTING_KEY);
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
          this.favicon = retrieveDefaultBrandingFile(defaultConfiguredFaviconPath, new Favicon(), FAVICON_NAME, BRANDING_FAVICON_ID_SETTING_KEY);
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
          this.loginBackground = retrieveDefaultBrandingFile(defaultConfiguredLoginBgPath, new Background(), LOGIN_BACKGROUND_NAME, BRANDING_LOGIN_BG_ID_SETTING_KEY);
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
  public Background getPageBackground() {
    if (this.pageBackground == null) {
      try {
        Long imageId = getPageBackgroundId();
        if (imageId != null) {
          this.pageBackground = retrieveStoredBrandingFile(imageId, new Background());
        } else {
          this.pageBackground = new Background();
        }
      } catch (Exception e) {
        LOG.warn("Error retrieving page background", e);
      }
    }
    return this.pageBackground;
  }

  @Override
  public String getLogoPath() {
    Logo brandingLogo = getLogo();
    return brandingLogo == null
           || brandingLogo.getData() == null ? null : BRANDING_LOGO_BASE_PATH + Objects.hash(brandingLogo.getUpdatedDate());
  }

  @Override
  public String getFaviconPath() {
    Favicon brandingFavicon = getFavicon();
    return brandingFavicon == null
           || brandingFavicon.getData() == null ? null :
                                                BRANDING_FAVICON_BASE_PATH + Objects.hash(brandingFavicon.getUpdatedDate());
  }

  @Override
  public String getLoginBackgroundPath() {
    Background background = getLoginBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_LOGIN_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public String getPageBackgroundPath() {
    Background background = getPageBackground();
    return background == null
           || background.getData() == null ? null : BRANDING_PAGE_BG_BASE_PATH + Objects.hash(background.getUpdatedDate());
  }

  @Override
  public void updateLastUpdatedTime(long lastUpdatedTimestamp) {
    if (lastUpdatedTimestamp <= 0) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, BRANDING_LAST_UPDATED_TIME_KEY);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, BRANDING_LAST_UPDATED_TIME_KEY, SettingValue.create(lastUpdatedTimestamp));
    }
    this.themeCSSContent = null;
    this.customCss = null;
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
  public void updateThemeStyle(Map<String, String> themeStyle) {
    updateThemeStyle(themeStyle, true);
  }

  @Override
  public Map<String, String> getThemeStyle() {
    if (themeVariables == null || themeVariables.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, String> themeStyleVariables = new HashMap<>();
    Set<String> variables = themeVariables.keySet();
    for (String themeVariable : variables) {
      SettingValue<?> storedStyleValue = settingService.get(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable);
      String styleValue = storedStyleValue == null
                          || storedStyleValue.getValue() == null ? themeVariables.get(themeVariable) :
                                                                 storedStyleValue.getValue().toString();
      if (StringUtils.isNotBlank(styleValue)) {
        themeStyleVariables.put(themeVariable, styleValue);
      }
    }
    return themeStyleVariables;
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
      valuePerLanguage.computeIfAbsent(getDefaultLanguage(), key -> this.defaultLoginTitle);
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
      valuePerLanguage.computeIfAbsent(getDefaultLanguage(), key -> this.defaultLoginSubtitle);
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
      return valuePerLanguage.getOrDefault(getDefaultLanguage(), this.defaultLoginTitle);
    }
  }

  @Override
  public String getLoginSubtitle(Locale locale) {
    Map<String, String> valuePerLanguage = getLoginSubtitle();
    if (valuePerLanguage.containsKey(locale.getLanguage())) {
      return valuePerLanguage.get(locale.getLanguage());
    } else {
      return valuePerLanguage.getOrDefault(getDefaultLanguage(), this.defaultLoginTitle);
    }
  }

  @Override
  public String getDefaultLanguage() {
    return getDefaultLocale().toLanguageTag();
  }

  /**
   * Load init params
   * 
   * @param initParams
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

      ValueParam favIconParam = initParams.getValueParam(BRANDING_FAVICON_INIT_PARAM);
      if (favIconParam != null) {
        this.defaultConfiguredFaviconPath = favIconParam.getValue();
      }

      ValueParam loginBackgroundParam = initParams.getValueParam(BRANDING_LOGIN_BG_INIT_PARAM);
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

  private Map<String, String> loadLanguages() {
    Locale defaultLocale = getDefaultLocale();
    this.supportedLanguages = localeConfigService.getLocalConfigs()
        == null ?
                Collections.singletonMap(defaultLocale.getLanguage(),
                                         getLocaleDisplayName(defaultLocale,
                                                              defaultLocale)) :
                localeConfigService.getLocalConfigs()
                                   .stream()
                                   .filter(localeConfig -> !StringUtils.equals(localeConfig.getLocaleName(),
                                                                               "ma"))
                                   .collect(Collectors.toMap(LocaleConfig::getLocaleName,
                                                             localeConfig -> getLocaleDisplayName(defaultLocale,
                                                                                                  localeConfig.getLocale())));
    return this.supportedLanguages;
  }

  private Locale getDefaultLocale() {
    return localeConfigService.getDefaultLocaleConfig() == null ? Locale.getDefault() :
                                                                localeConfigService.getDefaultLocaleConfig()
                                                                                   .getLocale();
  }

  private String getDefaultLocaleDirection() {
    LocaleConfig defaultLocaleConfig = localeConfigService.getDefaultLocaleConfig();
    return defaultLocaleConfig == null
           || defaultLocaleConfig.getOrientation() == null
           || !defaultLocaleConfig.getOrientation()
                                  .isRT() ? "ltr" : "rtl";
  }

  private String getLocaleDisplayName(Locale defaultLocale, Locale locale) {
    return defaultLocale.equals(locale) ? defaultLocale.getDisplayName(defaultLocale) :
                                        locale.getDisplayName(defaultLocale) + " / " + locale.getDisplayName(locale);
  }

  private void updateCompanyLink(String companyLink, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_COMPANY_LINK_SETTING_KEY, companyLink, updateLastUpdatedTime);
  }

  private void updateSiteName(String siteName, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_SITE_NAME_SETTING_KEY, siteName, updateLastUpdatedTime);
  }

  private void updateLoginBackgroundTextColor(String textColor, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_LOGIN_TEXT_COLOR_KEY, textColor, updateLastUpdatedTime);
  }

  private void updatePageBackgroundColor(String color, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_COLOR_KEY, color, updateLastUpdatedTime);
  }

  private void updatePageBackgroundSize(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_SIZE_KEY, value, updateLastUpdatedTime);
  }

  private void updatePageBackgroundPosition(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_POSITION_KEY, value, updateLastUpdatedTime);
  }

  private void updatePageBackgroundRepeat(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_BG_REPEAT_KEY, value, updateLastUpdatedTime);
  }

  private void updatePageWidth(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_PAGE_WIDTH_KEY, value, updateLastUpdatedTime);
  }

  private void updateCustomCss(String value, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_CUSTOM_CSS, value, updateLastUpdatedTime);
  }

  private void updateCompanyName(String companyName, boolean updateLastUpdatedTime) {
    updatePropertyValue(BRANDING_COMPANY_NAME_SETTING_KEY, companyName, updateLastUpdatedTime);
  }

  private void updateThemeStyle(Map<String, String> themeStyles, boolean updateLastUpdatedTime) {
    if (themeVariables == null || themeVariables.isEmpty()) {
      return;
    }

    Set<String> variables = themeVariables.keySet();
    for (String themeVariable : variables) {
      if (themeStyles != null && themeStyles.get(themeVariable) != null) {
        String themeStyle = themeStyles.get(themeVariable);
        settingService.set(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable, SettingValue.create(themeStyle));
      } else {
        settingService.remove(BRANDING_CONTEXT, BRANDING_SCOPE, themeVariable);
      }
    }

    // Refresh Theme
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
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
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateFavicon(Favicon favicon, boolean updateLastUpdatedTime) {
    updateBrandingFile(favicon, FAVICON_NAME, this.getFaviconId(), BRANDING_FAVICON_ID_SETTING_KEY);
    this.favicon = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateLoginBackground(Background loginBackground, boolean updateLastUpdatedTime) {
    updateBrandingFile(loginBackground, LOGIN_BACKGROUND_NAME, this.getLoginBackgroundId(), BRANDING_LOGIN_BG_ID_SETTING_KEY);
    this.loginBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updatePageBackground(Background background, boolean updateLastUpdatedTime) {
    updateBrandingFile(background, PAGE_BACKGROUND_NAME, this.getPageBackgroundId(), BRANDING_PAGE_BG_ID_SETTING_KEY);
    this.pageBackground = null;
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void updateBrandingFile(BrandingFile brandingFile, String fileName, Long fileId, String settingKey) {
    String uploadId = brandingFile == null ? null : brandingFile.getUploadId();
    updateBrandingFile(uploadId,
                       fileName,
                       fileId,
                       settingKey);
    if (fileId != null
        && !StringUtils.equals(BRANDING_RESET_ATTACHMENT_ID, uploadId)) {
      // Cleanup old fileId
      fileService.deleteFile(fileId);
    }
  }

  private void updateBrandingFile(String uploadId, String fileName, Long fileId, String settingKey) {
    try {
      if (StringUtils.equals(BRANDING_RESET_ATTACHMENT_ID, uploadId)) {
        removeBrandingFile(fileId, settingKey);
      } else if (StringUtils.isNotBlank(uploadId)) {
        updateBrandingFileByUploadId(uploadId, fileName, settingKey);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error while updating login background", e);
    }
  }

  private void removeBrandingFile(Long fileId, String settingKey) {
    if (fileId != null && fileId > 0) {
      fileService.deleteFile(fileId);
      settingService.remove(Context.GLOBAL,
                            Scope.GLOBAL,
                            settingKey);
    }
  }

  @SneakyThrows
  private void updateBrandingFileByUploadId(String uploadId,
                                            String fileName,
                                            String settingKey) {
    InputStream inputStream = getUploadDataAsStream(uploadId);
    if (inputStream == null) {
      throw new IllegalArgumentException("Cannot update " + fileName +
          ", the object must contain the image data or an upload id");
    }
    updateBrandingFileByInputStream(inputStream, fileName, settingKey);
  }

  @SneakyThrows
  private FileItem updateBrandingFileByInputStream(InputStream inputStream, String fileName, String settingKey)  {
    int size = inputStream.available();
    FileItem fileItem = new FileItem(0l,
                                     fileName,
                                     "image/png",
                                     FILE_API_NAME_SPACE,
                                     size,
                                     new Date(),
                                     getCurrentUserId(),
                                     false,
                                     inputStream);
    fileItem = fileService.writeFile(fileItem);
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL,
                       settingKey,
                       SettingValue.create(String.valueOf(fileItem.getFileInfo().getId())));
    return fileItem;
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
                            || storedColorValue.getValue() == null ? themeVariables.get(themeVariable) :
                                                                   storedColorValue.getValue().toString();
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
    if (StringUtils.isNotBlank(getCustomCssContent())
        && getFeatureService() != null
        && getFeatureService().isActiveFeature(BRANDING_CUSTOM_STYLE_FEATURE)) {
      this.themeCSSContent += "\n" + this.customCss;
    }
    return this.themeCSSContent;
  }

  private String getCustomCssContent() {
    if (this.customCss == null) {
      this.customCss = getCustomCss();
      if (this.customCss == null) {
        this.customCss = "";
      }
    }
    return this.customCss;
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

  private <T extends BrandingFile> T retrieveStoredBrandingFile(long imageId, T brandingFile) throws FileStorageException {
    FileItem fileItem = fileService.getFile(imageId);
    if (fileItem != null) {
      brandingFile.setData(fileItem.getAsByte());
      brandingFile.setSize(fileItem.getFileInfo().getSize());
      brandingFile.setUpdatedDate(fileItem.getFileInfo().getUpdatedDate().getTime());
      brandingFile.setFileId(imageId);
    }
    return brandingFile;
  }

  private <T extends BrandingFile> T retrieveDefaultBrandingFile(String imagePath, T brandingFile, String fileName, String settingKey) throws IOException {
    if (StringUtils.isNotBlank(imagePath)) {
      byte[] bytes = null;
      long lastModified = DEFAULT_LAST_MODIFED;
      File file = new File(imagePath);
      if (file.exists()) {
        bytes = Files.readAllBytes(file.toPath());
        lastModified = file.lastModified();
      } else {
        try (InputStream is = container.getPortalContext().getResourceAsStream(imagePath)) {
          if (is != null) {
            bytes = IOUtil.getStreamContentAsBytes(is);
          }
        }
      }
      if (bytes != null) {
        FileItem fileItem = updateBrandingFileByInputStream(new ByteArrayInputStream(bytes), fileName, settingKey);
        brandingFile.setFileId(fileItem.getFileInfo().getId());
        brandingFile.setData(bytes);
        brandingFile.setSize(bytes.length);
        brandingFile.setUpdatedDate(lastModified);
      }
    }
    return brandingFile;
  }

  private String getPropertyValue(String key) {
    return getPropertyValue(key, null);
  }

  private Long getPropertyValueLong(String key) {
    String value = getPropertyValue(key, null);
    return StringUtils.isBlank(value) ? null : Long.parseLong(value);
  }

  private String getPropertyValue(String key, String defaultValue) {
    SettingValue<?> value = settingService.get(Context.GLOBAL,
                                               Scope.GLOBAL,
                                               key);
    if (value != null
        && value.getValue() != null
        && StringUtils.isNotBlank(value.getValue().toString())) {
      return value.getValue().toString();
    } else {
      return defaultValue;
    }
  }

  private void updatePropertyValue(String key, String value, boolean updateLastUpdatedTime) {
    if (StringUtils.isBlank(value)) {
      settingService.remove(Context.GLOBAL, Scope.GLOBAL, key);
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, key, SettingValue.create(value));
    }
    triggerBrandingUpdated(updateLastUpdatedTime, updateLastUpdatedTime);
  }

  private void validateCSSInputs(Branding branding) { // NOSONAR
    Arrays.asList(branding.getCustomCss(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundPosition(),
                  branding.getPageBackgroundRepeat(),
                  branding.getPageBackgroundSize(),
                  branding.getLoginBackgroundTextColor(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundColor(),
                  branding.getPageBackgroundColor())
          .forEach(this::validateCSSStyleValue);
    branding.getThemeStyle().values().forEach(this::validateCSSStyleValue);
  }

  private void validateCSSStyleValue(String value) {
    if (StringUtils.isNotBlank(value)
        && (value.contains("javascript") || value.contains("eval"))) {
      throw new IllegalArgumentException(String.format("Invalid css value input %s",
                                                       value));
    }
  }

  private void triggerBrandingUpdated(boolean updateLastUpdatedTime, boolean triggerEvent) {
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
    if (triggerEvent) {
      listenerService.broadcast(BRANDING_UPDATED_EVENT, null, getBrandingInformation(false));
    }
  }

  private ExoFeatureService getFeatureService() {
    if (featureService == null) {
      featureService = container.getComponentInstanceOfType(ExoFeatureService.class);
    }
    return featureService;
  }

}