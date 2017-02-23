package org.exoplatform.portal.localization;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.gatein.common.i18n.LocaleFactory;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * This class is used to ease {@link LocaleContextInfo} object build
 */
public class LocaleContextInfoUtils {
  
  private static final String LOCALE_COOKIE = "LOCALE";
  
  private static final String PREV_LOCALE_SESSION_ATTR = "org.gatein.LAST_LOCALE";
  
  private static final String LOCALE_SESSION_ATTR = "org.gatein.LOCALE";
  
  private static final Logger LOG = LoggerFactory.getLogger(LocaleContextInfoUtils.class);
  
  /**
   *  Helper method for setters invocation on {@link LocaleContextInfo} object
   * @param request
   * @return a built {@link LocaleContextInfo} object
   */
  public static LocaleContextInfo buildLocaleContextInfo(HttpServletRequest request) {
    LocaleContextInfo localeCtx = new LocaleContextInfo();
    // start with setting supported locales and and portal locale
    localeCtx.setSupportedLocales(getSupportedLocales());
    localeCtx.setPortalLocale(getPortalLocale());
    // check request nullability before proceeding
    if (request == null) {
      return localeCtx;
    }
    //
    String username = request.getRemoteUser();
    // get session locale
    String lastLocaleLangauge = getPreviousLocale(request) == null ? null : getPreviousLocale(request).toString();
    Locale sessionLocale = lastLocaleLangauge == null ? getSessionLocale(request) : new Locale(lastLocaleLangauge);
    localeCtx.setSessionLocale(sessionLocale);
    // continue setting localCtx with data fetched from request
    localeCtx.setUserProfileLocale(getUserLocale(username));
    localeCtx.setBrowserLocales(Collections.list(request.getLocales()));
    localeCtx.setCookieLocales(getCookieLocales(request));
    localeCtx.setRemoteUser(username);
    return localeCtx;
  }
  
  /**
   * Helper method for setters invocation on {@link LocaleContextInfo} object
   * @param userId
   * @return a built {@link LocaleContextInfo} object
   */
  public static LocaleContextInfo buildLocaleContextInfo(String userId) {
    LocaleContextInfo localeCtx = new LocaleContextInfo();
    localeCtx.setSupportedLocales(getSupportedLocales());
    localeCtx.setUserProfileLocale(getUserLocale(userId));
    localeCtx.setRemoteUser(userId);
    localeCtx.setPortalLocale(getPortalLocale());
    return localeCtx;
  }
  
  /**
   *
   * @param request
   * @return
   */
  public static List<Locale> getCookieLocales(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (LOCALE_COOKIE.equals(cookie.getName())) {
          List<Locale> locales = new ArrayList<Locale>();
          locales.add(LocaleContextInfo.getLocale(cookie.getValue()));
          return locales;
        }
      }
    }
    return Collections.emptyList();
  }
  
  /**
   * Get current session locale
   * @param request
   * @return
   */
  private static Locale getSessionLocale(HttpServletRequest request) {
    return getLocaleFromSession(request, LOCALE_SESSION_ATTR);
  }
  
  /**
   * Get previous session locale
   * @param request
   * @return
   */
  private static Locale getPreviousLocale(HttpServletRequest request) {
    return getLocaleFromSession(request, PREV_LOCALE_SESSION_ATTR);
  }
  
  /**
   * Helper method to retrieve supportedLocales from LocaleConfigService
   * @return supportedLocales
   */
  public static  Set<Locale> getSupportedLocales() {
    LocaleConfigService localeConfigService = ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(LocaleConfigService.class);
    Set<Locale> supportedLocales = new HashSet();
    for (LocaleConfig lc : localeConfigService.getLocalConfigs()) {
      supportedLocales.add(lc.getLocale());
    }
    return supportedLocales;
  }
  
  /**
   * Get session locale
   * @param request
   * @param attrName
   * @return
   */
  private static Locale getLocaleFromSession(HttpServletRequest request, String attrName) {
    String lang = null;
    HttpSession session = request.getSession(false);
    if (session != null)
      lang = (String) session.getAttribute(attrName);
    return (lang != null) ? LocaleContextInfo.getLocale(lang) : null;
  }
  
  /**
   * Helper method to retrieve user locale from UserProfile
   * @param userId
   * @return user locale
   */
  private static Locale getUserLocale(String userId) {
    String lang = "";
    UserProfile profile = null;
    //
    if(userId != null) {
      OrganizationService organizationService = ExoContainerContext.getCurrentContainer()
                .getComponentInstanceOfType(OrganizationService.class);
      // get user profile
      beginContext(organizationService);
      try {
        profile = organizationService.getUserProfileHandler().findUserProfileByName(userId);
      } catch (Exception e) {
        LOG.debug(userId + " profile not found ", e);
      } finally {
        endContext(organizationService);
      }
      // fetch profile lang
      if(profile != null) {
        lang = profile.getAttribute(Constants.USER_LANGUAGE);
      }
      if (lang != null && lang.trim().length() > 0) {
        return LocaleFactory.DEFAULT_FACTORY.createLocale(lang);
      }
    }
    return null;
  }
  
  /**
   *  Helper method to get portal locale from portal config
   * @return return portalLocale, if not set then return the JVM Locale
   */
  private static Locale getPortalLocale() {
    String lang = "";
    //
    UserPortalConfigService userPortalConfigService = ExoContainerContext.getCurrentContainer()
            .getComponentInstanceOfType(UserPortalConfigService.class);
    POMSessionManager pomSessionManager = ExoContainerContext.getCurrentContainer().
            getComponentInstanceOfType(POMSessionManager.class);
    if(userPortalConfigService != null && pomSessionManager != null) {
      // initialise pomsession
      if (pomSessionManager.getSession() == null) {
        pomSessionManager.openSession();
      }
      //
      try {
        PortalConfig config = userPortalConfigService.getDataStorage().getPortalConfig(userPortalConfigService.getDefaultPortal());
        if (config != null) {
          lang = config.getLocale();
        }
      } catch (Exception e) {
        LOG.debug("Can't load default portal config ", e);
      } finally {
        if (pomSessionManager.getSession() != null) {
          pomSessionManager.getSession().close();
        }
      }
    }
    //  return portal Locale, if not set then return the JVM Locale
    return (lang != null && lang.trim().length() > 0) ? LocaleFactory.DEFAULT_FACTORY.createLocale(lang) : Locale.getDefault();
  }
  
  /**
   * Begin request life cycle for OrganizationService
   * @param orgService
   */
  private static void beginContext(OrganizationService orgService) {
    if (orgService instanceof ComponentRequestLifecycle) {
      RequestLifeCycle.begin((ComponentRequestLifecycle) orgService);
    }
  }
  
  /**
   * End RequestLifeCycle for OrganizationService
   * @param orgService
   */
  private static void endContext(OrganizationService orgService) {
    if (orgService instanceof ComponentRequestLifecycle) {
      RequestLifeCycle.end();
    }
  }
  
}