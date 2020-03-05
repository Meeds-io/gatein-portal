package org.exoplatform.portal.config;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.mop.SiteKey;

/**
 * <p>
 * A class that will be used to check if a site portal should have dynamic site
 * layout retrieved into it or not.
 * </p>
 * <p>
 * This check is made swicth multiple conditions and regex.
 * </p>
 */
public class DynamicPortalLayoutMatcher {

  private String  currentSiteNameRegex   = null;

  private String  siteNameRegex          = null;

  private String  siteTypeRegex          = null;

  private Pattern currentSiteNamePattern = null;

  private Pattern siteNamePattern        = null;

  private Pattern siteTypePattern        = null;

  public void setCurrentSiteNameRegex(String currentSiteNameRegex) {
    this.currentSiteNameRegex = currentSiteNameRegex;
  }

  public void setSiteNameRegex(String siteNameRegex) {
    this.siteNameRegex = siteNameRegex;
  }

  public void setSiteTypeRegex(String siteTypeRegex) {
    this.siteTypeRegex = siteTypeRegex;
  }

  public Pattern getSiteNamePattern() {
    if (siteNamePattern == null && StringUtils.isNotBlank(siteNameRegex)) {
      this.siteNamePattern = Pattern.compile(siteNameRegex);
    }
    return siteNamePattern;
  }

  public Pattern getSiteTypePattern() {
    if (siteTypePattern == null && StringUtils.isNotBlank(siteTypeRegex)) {
      this.siteTypePattern = Pattern.compile(siteTypeRegex);
    }
    return siteTypePattern;
  }

  public Pattern getCurrentSiteNamePattern() {
    if (currentSiteNamePattern == null && StringUtils.isNotBlank(currentSiteNameRegex)) {
      this.currentSiteNamePattern = Pattern.compile(currentSiteNameRegex);
    }
    return currentSiteNamePattern;
  }

  /**
   * Checks whether currently displaying site (designated by siteKey) should be
   * retrieved with dynamic site layout or not.
   * 
   * @param siteKey currently displaying site
   * @param currentPortalSiteName last displayed site name of type PORTAL
   * @return true if condifitons of current Matcher matches, else false.
   */
  public boolean matches(SiteKey siteKey, String currentPortalSiteName) {
    if (siteKey == null || StringUtils.isBlank(currentPortalSiteName)) {
      return false;
    }

    if (getCurrentSiteNamePattern() != null && !getCurrentSiteNamePattern().matcher(currentPortalSiteName).matches()) {
      return false;
    }
    if (getSiteNamePattern() != null && !getSiteNamePattern().matcher(siteKey.getName()).matches()) {
      return false;
    }
    if (getSiteTypePattern() != null && !getSiteTypePattern().matcher(siteKey.getTypeName()).matches()) { // NOSONAR
      return false;
    }
    return true;
  }
}
