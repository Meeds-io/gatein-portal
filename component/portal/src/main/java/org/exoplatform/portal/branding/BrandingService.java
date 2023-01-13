package org.exoplatform.portal.branding;

import java.util.Map;

import org.exoplatform.commons.file.model.FileItem;

public interface BrandingService {

  Branding getBrandingInformation();

  /**
   * Update the branding information Missing information in the branding object
   * are not updated.
   * 
   * @param branding The new branding information
   */
  void updateBrandingInformation(Branding branding);

  String getCompanyName();

  String getSiteName();

  String getCompanyLink();

  void updateCompanyName(String companyName);

  void updateCompanyLink(String companyLink);

  void updateSiteName(String siteName);

  String getTopBarTheme();

  Long getLogoId();

  /**
   * @return Branding Favicon {@link FileItem} technical identifier
   */
  Long getFaviconId();

  Logo getLogo();

  /**
   * @return configured custom {@link Favicon} else returns default one
   */
  Favicon getFavicon();

  /**
   * @return {@link Favicon} URL to retrieve favicon image
   */
  String getFaviconPath();

  void updateTopBarTheme(String style);

  /**
   * Update branding logo. If the logo object contains the image data, they are
   * used, otherwise if the uploadId exists it is used to retrieve the uploaded
   * resource. If there is no data, nor uploadId, the logo is deleted.
   * 
   * @param logo The logo object
   */
  void updateLogo(Logo logo);

  /**
   * @return CSS content of colors for theme
   */
  String getThemeCSSContent();

  /**
   * Updated last updated time of one of Branding properties
   * 
   * @param lastUpdatedTimestamp Timestamp in milliseconds
   */
  void updateLastUpdatedTime(long lastUpdatedTimestamp);

  /**
   * @return last updated time in milliseconds
   */
  long getLastUpdatedTime();

  /**
   * Updates CSS variables that defines the chosen theme and that will be
   * applied and server on UI
   * 
   * @param themeColors {@link Map} with variable name as key and variable value
   *          as map value
   */
  void updateThemeColors(Map<String, String> themeColors);

  /**
   * @return {@link Map} with variable name as key and variable value
   *          as map value
   */
  Map<String, String> getThemeColors();

}
