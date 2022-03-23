package org.exoplatform.portal.branding;

import java.util.Map;

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

  String getCompanyLink();

  void updateCompanyName(String companyName);

  void updateCompanyLink(String companyLink);

  String getTopBarTheme();

  Long getLogoId();

  Logo getLogo();

  Logo getDefaultLogo();

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
