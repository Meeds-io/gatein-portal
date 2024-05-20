package org.exoplatform.portal.localization;

import java.util.*;

import jakarta.servlet.http.*;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.*;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.Constants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.resources.*;

/**
 * This class is used to ease {@link LocaleContextInfo} object build
 */
public class LocaleContextInfoUtils {

  private static final String      LOCALE_COOKIE            = "LOCALE";

  private static final String      PREV_LOCALE_SESSION_ATTR = "org.gatein.LAST_LOCALE";

  private static final String      LOCALE_SESSION_ATTR      = "org.gatein.LOCALE";

  private static final Log         LOG                      = ExoLogger.getLogger(LocaleContextInfoUtils.class);

  private static final Set<Locale> SUPPORTED_LOCALES        = new HashSet<>();

  /**
   * Computes locale of currently authenticated user based on multiple
   * conditions switch implemented {@link LocalePolicy}: - User Profile Locale
   * (coming from User preferences) - Session Locale - Browser Locale - Cookie
   * Locale
   * 
   * @param request {@link HttpServletRequestWrapper}
   * @return {@link Locale} retrieved using {@link LocalePolicy}, else return
   *         default configured locale in {@link LocaleConfigService}
   */
  public static Locale computeLocale(HttpServletRequest request) {
    LocalePolicy localePolicy = ExoContainerContext.getService(LocalePolicy.class);
    LocaleContextInfo localeCtx = buildLocaleContextInfo(request);
    Set<Locale> supportedLocales = getSupportedLocales();
    Locale locale = localePolicy.determineLocale(localeCtx);
    boolean supported = supportedLocales.contains(locale);
    if (!supported && StringUtils.isNotBlank(locale.getCountry())) {
      locale = new Locale(locale.getLanguage());
      supported = supportedLocales.contains(locale);
    }
    if (!supported) {
      LocaleConfigService localeConfigService = ExoContainerContext.getService(LocaleConfigService.class);
      Locale defaultLocale = localeConfigService.getDefaultLocaleConfig().getLocale();
      LOG.warn("Unsupported locale returned by LocalePolicy: {}. Falling back to default configured local '{}'.",
               locale,
               defaultLocale);
      locale = defaultLocale;
    }
    return locale;
  }

  /**
   * Helper method for setters invocation on {@link LocaleContextInfo} object
   * 
   * @param request
   * @return a built {@link LocaleContextInfo} object
   */
  public static LocaleContextInfo buildLocaleContextInfo(HttpServletRequest request) {
    LocaleContextInfo localeCtx = new LocaleContextInfo();
    // start with setting supported locales and and portal locale
    localeCtx.setSupportedLocales(getSupportedLocales());
    // check request nullability before proceeding
    if (request == null) {
      return localeCtx;
    }
    //
    String username = request.getRemoteUser();
    // get session locale
    Locale previousLocale = getPreviousLocale(request);
    Locale sessionLocale = previousLocale == null ? getSessionLocale(request) : previousLocale;
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
   * 
   * @param userId
   * @return a built {@link LocaleContextInfo} object
   */
  public static LocaleContextInfo buildLocaleContextInfo(String userId) {
    LocaleContextInfo localeCtx = new LocaleContextInfo();
    localeCtx.setSupportedLocales(getSupportedLocales());
    localeCtx.setUserProfileLocale(getUserLocale(userId));
    localeCtx.setRemoteUser(userId);
    return localeCtx;
  }

  /**
   * @param request
   * @return
   */
  public static List<Locale> getCookieLocales(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (LOCALE_COOKIE.equals(cookie.getName())) {
          List<Locale> locales = new ArrayList<>();
          locales.add(LocaleContextInfo.getLocale(cookie.getValue()));
          return locales;
        }
      }
    }
    return Collections.emptyList();
  }

  /**
   * Get current session locale
   * 
   * @param request
   * @return
   */
  private static Locale getSessionLocale(HttpServletRequest request) {
    return getLocaleFromSession(request, LOCALE_SESSION_ATTR);
  }

  /**
   * Get previous session locale
   * 
   * @param request
   * @return
   */
  private static Locale getPreviousLocale(HttpServletRequest request) {
    return getLocaleFromSession(request, PREV_LOCALE_SESSION_ATTR);
  }

  /**
   * Helper method to retrieve supportedLocales from LocaleConfigService
   * 
   * @return supportedLocales
   */
  public static Set<Locale> getSupportedLocales() {
    if (SUPPORTED_LOCALES.isEmpty()) {
      LocaleConfigService localeConfigService = ExoContainerContext.getService(LocaleConfigService.class);
      if (localeConfigService != null) {
        for (LocaleConfig lc : localeConfigService.getLocalConfigs()) {
          SUPPORTED_LOCALES.add(lc.getLocale());
        }
      }
    }
    return SUPPORTED_LOCALES;
  }

  /**
   * Get session locale
   * 
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
   * 
   * @param userId
   * @return user locale
   */
  private static Locale getUserLocale(String userId) {
    String lang = "";
    UserProfile profile = null;
    //
    if (userId != null) {
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
      if (profile != null) {
        lang = profile.getAttribute(Constants.USER_LANGUAGE);
      }
      if (lang != null && lang.trim().length() > 0) {
        return LocaleUtils.toLocale(lang);
      }
    }
    return null;
  }

  /**
   * Begin request life cycle for OrganizationService
   * 
   * @param orgService
   */
  private static void beginContext(OrganizationService orgService) {
    if (orgService instanceof ComponentRequestLifecycle) {
      RequestLifeCycle.begin((ComponentRequestLifecycle) orgService);
    }
  }

  /**
   * End RequestLifeCycle for OrganizationService
   * 
   * @param orgService
   */
  private static void endContext(OrganizationService orgService) {
    if (orgService instanceof ComponentRequestLifecycle) {
      RequestLifeCycle.end();
    }
  }

}
