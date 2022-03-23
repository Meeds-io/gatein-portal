package org.exoplatform.portal.branding;

import java.io.Serializable;
import java.util.*;

public class Branding implements Serializable {

  private static final long serialVersionUID = 625471892955717717L;

  private String companyName;

  private String siteName;

  private String companyLink;

  private String topBarTheme;

  private Logo logo;

  private Map<String, String> themeColors      = new HashMap<>();

  private long                lastUpdatedTime;

  public Branding() {
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getCompanyLink() {
    return companyLink;
  }

  public void setCompanyLink(String companyLink) {
    this.companyLink = companyLink;
  }

  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public String getTopBarTheme() {
    return topBarTheme;
  }

  public void setTopBarTheme(String topBarTheme) {
    this.topBarTheme = topBarTheme;
  }
  
  public Logo getLogo() {
    return logo;
  }
  
  public void setLogo(Logo logo) {
    this.logo = logo;
  }

  public Map<String, String> getThemeColors() {
    return themeColors;
  }

  public void setThemeColors(Map<String, String> themeColors) {
    this.themeColors = themeColors;
  }

  public long getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  public void setLastUpdatedTime(long lastUpdatedTime) {
    this.lastUpdatedTime = lastUpdatedTime;
  }

  @Override
  public String toString() {
    return "Branding [companyName=" + companyName + ", topBarTheme=" + topBarTheme + "]";
  }
}
