package org.exoplatform.portal.webui.util;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.mop.user.UserPortalImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SAS Author : Phan Le Thanh Chuong
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com Nov 21, 2008
 */
public class NavigationUtils {

  public static final Scope                       ECMS_NAVIGATION_SCOPE = Scope.CHILDREN;

  private static ThreadLocal<Map<String, String>> gotNavigationKeeper   = new ThreadLocal<Map<String, String>>();

  private static Constructor<UserNavigation>      userNavigationCtor    = null;

  private static final Log                        LOG                   = ExoLogger.getLogger(NavigationUtils.class.getName());
  static {
    try {
      // reflection here to get UserNavigation to avoid for using such as:
      // spaceNav = userPortal.getNavigation(SiteKey.group(groupId));
      userNavigationCtor = UserNavigation.class.getDeclaredConstructor(
                                                                       new Class[] { UserPortalImpl.class,
                                                                                     NavigationContext.class, boolean.class });
      userNavigationCtor.setAccessible(true);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  } // of static reflection

  public static boolean gotNavigation(String portal, String user) {
    return gotNavigation(portal, user, "");
  }

  public static boolean gotNavigation(String portal, String user, String scope) {
    Map<String, String> navigations = gotNavigationKeeper.get();
    if (navigations == null)
      return false;
    String navigation = navigations.get(portal + " " + user + " " + scope);
    return (navigation != null);
  }

  public static UserNavigation getUserNavigationOfPortal(UserPortal userPortal, String portalName) throws Exception {
    UserACL userACL = ExoContainerContext.getService(UserACL.class);
    UserPortalConfigService userPortalConfigService = ExoContainerContext.getService(UserPortalConfigService.class);
    NavigationContext portalNav = userPortalConfigService.getNavigationService()
                                                         .loadNavigation(new SiteKey(SiteType.PORTAL, portalName));
    if (portalNav == null) {
      return null;
    }
    UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig(portalName,
                                                                                 ConversationState.getCurrent()
                                                                                                  .getIdentity()
                                                                                                  .getUserId());
    return userNavigationCtor.newInstance(
                                          userPortal,
                                          portalNav,
                                          userACL.hasEditPermission(userPortalCfg.getPortalConfig()));
  }

  /**
   * Get UserNavigation of a specified element
   * 
   * @param userPortal
   * @param siteKey Key
   * @return UserNavigation of group
   */
  public static UserNavigation getUserNavigation(UserPortal userPortal, SiteKey siteKey) throws Exception {
    if (siteKey.getTypeName().equalsIgnoreCase(SiteType.PORTAL.getName())) {
      return getUserNavigationOfPortal(userPortal, siteKey.getName());
    }
    UserACL userACL = ExoContainerContext.getService(UserACL.class);
    UserPortalConfigService userPortalConfigService = ExoContainerContext.getService(UserPortalConfigService.class);
    // userPortalConfigService.get
    NavigationContext portalNav = userPortalConfigService.getNavigationService().loadNavigation(siteKey);
    if (portalNav == null) {
      return null;
    } else {
      return userNavigationCtor.newInstance(userPortal, portalNav, userACL.hasEditPermissionOnNavigation(siteKey));
    }
  }
}
