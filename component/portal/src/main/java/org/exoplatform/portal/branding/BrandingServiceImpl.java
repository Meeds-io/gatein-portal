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

import java.io.*;
import java.nio.file.Files;
import java.util.*;

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
import org.exoplatform.container.xml.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

@SuppressWarnings("unchecked")
public class BrandingServiceImpl implements BrandingService, Startable {
  private static final Log     LOG                               = ExoLogger.getExoLogger(BrandingServiceImpl.class);

  public static final String   BRANDING_COMPANY_NAME_INIT_PARAM  = "exo.branding.company.name";

  public static final String   BRANDING_COMPANY_LINK_INIT_PARAM  = "exo.branding.company.link";

  public static final String   BRANDING_LOGO_INIT_PARAM          = "exo.branding.company.logo";

  public static final String   BRANDING_COMPANY_NAME_SETTING_KEY = "exo.branding.company.name";

  public static final String   BRANDING_COMPANY_LINK_SETTING_KEY = "exo.branding.company.link";

  public static final String   BRANDING_THEME_LESS_PATH          = "exo.branding.theme.path";
  
  public static final String   BRANDING_THEME_VARIABLES          = "exo.branding.theme.variables";

  public static final String   BRANDING_TOPBAR_THEME_SETTING_KEY = "bar_navigation_style";

  public static final String   BRANDING_LOGO_ID_SETTING_KEY      = "exo.branding.company.id";

  public static final String   BRANDING_LAST_UPDATED_TIME_KEY    = "branding.lastUpdatedTime";

  public static final String   FILE_API_NAME_SPACE               = "CompanyBranding";

  public static final String   LOGO_NAME                         = "logo.png";

  public static final String   BRANDING_DEFAULT_LOGO_PATH        = "/images/logo/DefaultLogo.png";

  public static final Context  BRANDING_CONTEXT                  = Context.GLOBAL.id("BRANDING");

  public static final Scope    BRANDING_SCOPE                    = Scope.APPLICATION.id("BRANDING");

  public static final long     DEFAULT_LAST_MODIFED              = System.currentTimeMillis();

  private SettingService       settingService;

  private FileService          fileService;

  private UploadService        uploadService;

  private ConfigurationManager configurationManager;

  private String               defaultCompanyName                = "";

  private String               defaultCompanyLink                = "";

  private String               defaultTopbarTheme                = "Dark";

  private String               defaultConfiguredLogoPath         = null;

  private String               lessFilePath                      = null;

  private Map<String, String>  themeVariables                    = null;

  private String               lessThemeContent                  = null;

  private String               themeCSSContent                   = null;
  
  private Logo                 logo                       = null;

  public BrandingServiceImpl(ConfigurationManager configurationManager,
                             SettingService settingService,
                             FileService fileService,
                             UploadService uploadService,
                             InitParams initParams) {
    this.configurationManager = configurationManager;
    this.settingService = settingService;
    this.fileService = fileService;
    this.uploadService = uploadService;

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
    branding.setCompanyName(getCompanyName());
    branding.setCompanyLink(getCompanyLink());
    branding.setTopBarTheme(getTopBarTheme());
    branding.setLogo(getLogo());
    branding.setThemeColors(getThemeColors());
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
      updateCompanyLink(branding.getCompanyLink(), false);
      updateTopBarTheme(branding.getTopBarTheme(), false);
      updateLogo(branding.getLogo(), false);
      updateThemeColors(branding.getThemeColors(), false);
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
  public void updateCompanyName(String companyName) {
    updateCompanyName(companyName, true);
  }

  @Override
  public void updateCompanyLink(String companyLink) {
    updateCompanyLink(companyLink, true);
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
  public Logo getLogo() {
    if (this.logo == null) {
      Long imageId = getLogoId();
      if (imageId != null) {
        try {
          FileItem fileItem = fileService.getFile(imageId);
          if (fileItem != null) {
            Logo storedLogo = new Logo();
            storedLogo.setData(fileItem.getAsByte());
            storedLogo.setSize(fileItem.getFileInfo().getSize());
            storedLogo.setUpdatedDate(fileItem.getFileInfo().getUpdatedDate().getTime());
            this.logo = storedLogo;
            return this.logo;
          }
        } catch (FileStorageException e) {
          LOG.error("Error while retrieving branding logo", e);
        }
      }
    }
    return this.getDefaultLogo();
  }

  @Override
  public Logo getDefaultLogo() {
    if (this.logo == null || this.logo.getData() == null) {
      String logoPath = defaultConfiguredLogoPath;
      if (StringUtils.isBlank(logoPath)) {
        logoPath = BRANDING_DEFAULT_LOGO_PATH;
      }
      try {
        File file = new File(logoPath);
        if (file.exists()) {
          this.logo = new Logo(null, Files.readAllBytes(file.toPath()), file.length(), file.lastModified());
        } else {
          InputStream is = PortalContainer.getInstance().getPortalContext().getResourceAsStream(logoPath);
          if (is != null) {
            byte[] streamContentAsBytes = IOUtil.getStreamContentAsBytes(is);
            this.logo = new Logo(null, streamContentAsBytes, streamContentAsBytes.length, DEFAULT_LAST_MODIFED);
          }
        }
      } catch (Exception e) {
        LOG.warn("The file of the default configured logo cannot be retrieved (" + logoPath + ")", e);
      }
    }
    return this.logo;
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

  /**
   * Load init params
   * 
   * @param initParams
   * @throws Exception
   */
  private void loadInitParams(InitParams initParams) {
    if (initParams != null) {
      ValueParam companyNameParam = initParams.getValueParam(BRANDING_COMPANY_NAME_INIT_PARAM);
      if (companyNameParam != null) {
        this.defaultCompanyName = companyNameParam.getValue();
      }

      ValueParam companyLinkParam = initParams.getValueParam(BRANDING_COMPANY_LINK_INIT_PARAM);
      if (companyLinkParam != null) {
        this.defaultCompanyLink = companyLinkParam.getValue();
      }

      ValueParam logoParam = initParams.getValueParam(BRANDING_LOGO_INIT_PARAM);
      if (logoParam != null) {
        this.defaultConfiguredLogoPath = logoParam.getValue();
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

  private void updateLogo(Logo logo, boolean updateLastUpdatedTime) {
    this.logo = null;
    if (logo == null || ((logo.getData() == null || logo.getData().length <= 0) && StringUtils.isBlank(logo.getUploadId()))) {
      Long logoId = this.getLogoId();
      if (logoId != null) {
        fileService.deleteFile(logoId);
        settingService.remove(Context.GLOBAL, Scope.GLOBAL, BRANDING_LOGO_ID_SETTING_KEY);
      }
    } else {
      try {
        InputStream inputStream;
        if (logo.getData() != null && logo.getData().length > 0) {
          inputStream = new ByteArrayInputStream(logo.getData());
        } else if (StringUtils.isNoneBlank(logo.getUploadId())) {
          inputStream = getUploadDataAsStream(logo.getUploadId());
        } else {
          throw new IllegalArgumentException("Cannot update branding logo, the logo object must contain the image data or an upload id");
        }
        String currentUserId = getCurrentUserId();
        FileItem fileItem;
        Long logoId = this.getLogoId();
        if (logoId == null) {
          fileItem = new FileItem(null,
                                  LOGO_NAME,
                                  "image/png",
                                  FILE_API_NAME_SPACE,
                                  logo.getSize(),
                                  new Date(),
                                  currentUserId,
                                  false,
                                  inputStream);
          fileItem = fileService.writeFile(fileItem);
          settingService.set(Context.GLOBAL,
                             Scope.GLOBAL,
                             BRANDING_LOGO_ID_SETTING_KEY,
                             SettingValue.create(String.valueOf(fileItem.getFileInfo().getId())));
        } else {
          fileItem = new FileItem(logoId,
                                  LOGO_NAME,
                                  "image/png",
                                  FILE_API_NAME_SPACE,
                                  logo.getSize(),
                                  new Date(),
                                  currentUserId,
                                  false,
                                  inputStream);
          fileService.updateFile(fileItem);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Error while updating logo", e);
      }
    }
    if (updateLastUpdatedTime) {
      updateLastUpdatedTime(System.currentTimeMillis());
    }
  }

  private String computeThemeCSS() {
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
          configuration.setLinkSourceMap(false);
          LessCompiler.CompilationResult result = compiler.compile(lessThemeContent, configuration);
          this.themeCSSContent  = result.getCss();
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
      try {
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

}
