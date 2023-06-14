package org.exoplatform.commons.utils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.branding.BrandingService;

public class MailUtils {

  public static final String DEFAULT_FROM_EMAIL = "noreply@meeds.io";

  public static final String SENDER_NAME_PARAM  = "exo:notificationSenderName";

  public static final String SENDER_EMAIL_PARAM = "exo:notificationSenderEmail";

  private MailUtils() {
    // Util Class contianing static calls
  }

  public static String getSenderName() {
    SettingValue<?> name = getSettingService().get(Context.GLOBAL, Scope.GLOBAL.id(null), SENDER_NAME_PARAM);
    return name != null ? (String) name.getValue() : getBrandingCompanyName();
  }

  public static String getSenderEmail() {
    SettingValue<?> mail = getSettingService().get(Context.GLOBAL, Scope.GLOBAL.id(null), SENDER_EMAIL_PARAM);
    return mail != null ? (String) mail.getValue() : System.getProperty("gatein.email.smtp.from", DEFAULT_FROM_EMAIL);
  }

  private static SettingService getSettingService() {
    SettingService settingService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SettingService.class);
    if (settingService == null) {
      settingService = PortalContainer.getInstance().getComponentInstanceOfType(SettingService.class);
    }
    return settingService;
  }

  private static String getBrandingCompanyName() {
    BrandingService brandingService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(BrandingService.class);
    if (brandingService != null) {
      return brandingService.getCompanyName();
    }
    return null;
  }

}
