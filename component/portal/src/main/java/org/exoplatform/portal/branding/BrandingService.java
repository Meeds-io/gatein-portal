package org.exoplatform.portal.branding;

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

}
