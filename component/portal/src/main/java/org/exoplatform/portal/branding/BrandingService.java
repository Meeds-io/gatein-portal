package org.exoplatform.portal.branding;

import java.util.List;
import java.util.Map;

public interface BrandingService {

  Branding getBrandingInformation();

  void updateBrandingInformation(Branding branding);

  String getCompanyName();

  void updateCompanyName(String companyName);

  String getTopBarTheme();

  Long getLogoId();

  Logo getLogo();

  Logo getDefaultLogo();

  void updateTopBarTheme(String style);

  void updateLogo(Logo logo);

  /**
   * @return CSS content of colors for theme
   */
  String getThemeCSSContent();

}
