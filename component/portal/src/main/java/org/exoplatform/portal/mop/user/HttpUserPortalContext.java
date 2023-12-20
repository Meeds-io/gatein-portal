package org.exoplatform.portal.mop.user;

import java.util.Locale;
import java.util.ResourceBundle;

import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.ResourceBundleManager;

public class HttpUserPortalContext implements UserPortalContext {

  private HttpServletRequest httpRequest;

  public HttpUserPortalContext(HttpServletRequest servletRequest) {
    this.httpRequest = servletRequest;
  }

  @Override
  public ResourceBundle getBundle(UserNavigation navigation) {
    ResourceBundleManager rbMgr = ExoContainerContext.getService(ResourceBundleManager.class);
    return rbMgr.getNavigationResourceBundle(getLocaleAsString(),
                                             getSiteType(navigation),
                                             getSiteName(navigation));
  }

  private String getSiteName(UserNavigation navigation) {
    return navigation
                     .getKey()
                     .getName();
  }

  private String getSiteType(UserNavigation navigation) {
    return navigation.getKey()
                     .getTypeName();
  }

  private String getLocaleAsString() {
    return LocaleContextInfo.getLocaleAsString(getUserLocale());
  }

  @Override
  public Locale getUserLocale() {
    return LocaleContextInfoUtils.computeLocale(httpRequest);
  }
}
