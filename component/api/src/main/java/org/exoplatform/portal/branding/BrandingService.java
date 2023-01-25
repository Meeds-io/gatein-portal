/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.portal.branding;

import java.util.Locale;
import java.util.Map;

import org.exoplatform.portal.branding.model.Background;
import org.exoplatform.portal.branding.model.Branding;
import org.exoplatform.portal.branding.model.Favicon;
import org.exoplatform.portal.branding.model.Logo;

public interface BrandingService {

  Branding getBrandingInformation();

  Branding getBrandingInformation(boolean retrieveBinaries);

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
   * @return Branding Favicon File technical identifier
   */
  Long getFaviconId();

  /**
   * @return Login Background File technical identifier
   */
  Long getLoginBackgroundId();

  Logo getLogo();

  /**
   * @return configured custom {@link Favicon} else returns default one
   */
  Favicon getFavicon();

  /**
   * @return configured custom {@link Background} else returns default one if
   *         exists else null
   */
  Background getLoginBackground();

  /**
   * @return Login Text Color displayed on top of the Background
   */
  String getLoginBackgroundTextColor();

  /**
   * @return {@link Logo} URL to retrieve logo image
   */
  String getLogoPath();

  /**
   * @return {@link Favicon} URL to retrieve favicon image
   */
  String getFaviconPath();

  /**
   * @return {@link Background} URL to retrieve login background
   */
  String getLoginBackgroundPath();

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
   * Update branding favicon. If the favicon object contains the image data,
   * they are used, otherwise if the uploadId exists it is used to retrieve the
   * uploaded resource. If there is no data, nor uploadId, the favicon is
   * deleted.
   * 
   * @param favicon The {@link Favicon} object to update
   */
  void updateFavicon(Favicon favicon);

  /**
   * Update login background. If the object contains the image data, they are
   * used, otherwise if the uploadId exists it is used to retrieve the uploaded
   * resource. If there is no data, nor uploadId, the login background is
   * deleted.
   * 
   * @param loginBackground The {@link Background} object to update
   */
  void updateLoginBackground(Background loginBackground);

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
   *                      as map value
   */
  void updateThemeColors(Map<String, String> themeColors);

  /**
   * Updates the login background text color
   *
   * @param textColor text color in hex representation like #fff
   */
  void updateLoginBackgroundTextColor(String textColor);

  /**
   * @return {@link Map} with variable name as key and variable value as map
   *         value
   */
  Map<String, String> getThemeColors();

  /**
   * @return {@link Map} of login titles per language
   */
  Map<String, String> getLoginTitle();

  /**
   * @param  locale user request {@link Locale}
   * @return        corresponding title to the {@link Locale}
   */
  String getLoginTitle(Locale locale);

  /**
   * @return {@link Map} of login subtitles per language
   */
  Map<String, String> getLoginSubtitle();

  /**
   * @param  locale user request {@link Locale}
   * @return        corresponding subtitle to the {@link Locale}
   */
  String getLoginSubtitle(Locale locale);

}
