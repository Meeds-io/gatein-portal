/*
 * Copyright (C) 2016 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.portal.jdbc.migration;

import java.util.*;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.pom.data.PortalKey;

public final class MigrationContext {
  protected static final String  CONTEXT_KEY                       = "PORTAL_MIGRATION_ENTITIES";

  protected static final Context CONTEXT                           = Context.GLOBAL.id("PORTAL_MIGRATION_ENTITIES");

  protected static final String  NAVIGATION_SCOPE                  = "NAVIGATION";

  public static final String     PORTAL_RDBMS_MIGRATION_STATUS_KEY = "PORTAL_RDBMS_MIGRATION_DONE";

  public static final String     PORTAL_RDBMS_APP_MIGRATION_KEY    = "PORTAL_RDBMS_APP_MIGRATION_DONE";

  public static final String     PORTAL_RDBMS_APP_CLEANUP_KEY      = "PORTAL_RDBMS_APP_CLEANUP_DONE";

  private static SettingService  settingService                    = null;

  private static boolean         forceStop                         = false;

  private static int             siteToMigrateIndex                = 0;

  private static List<PortalKey> sitesToMigrate                    = null;

  private static List<PortalKey> priorizedSitesToMigrate           = Collections.synchronizedList(new LinkedList<>());

  private MigrationContext() {
  }

  public static boolean isDone() {
    return getSettingValue(PORTAL_RDBMS_MIGRATION_STATUS_KEY);
  }

  public static boolean isMigrated(PortalKey siteToMigrateKey) {
    return !getSitesToMigrate().contains(siteToMigrateKey)
        || isDone()
        || (isMigrated(siteToMigrateKey, PortalEntityType.NAVIGATION) && isMigrated(siteToMigrateKey, PortalEntityType.PAGE)
            && isMigrated(siteToMigrateKey, PortalEntityType.SITE));
  }

  protected static void setDone() {
    updateSettingValue(PORTAL_RDBMS_MIGRATION_STATUS_KEY, true);
  }

  protected static boolean isAppDone() {
    return getSettingValue(PORTAL_RDBMS_APP_MIGRATION_KEY);
  }

  protected static void setAppDone() {
    updateSettingValue(PORTAL_RDBMS_APP_MIGRATION_KEY, true);
  }

  protected static boolean isAppCleanupDone() {
    return getSettingValue(PORTAL_RDBMS_APP_CLEANUP_KEY);
  }

  protected static void setAppCleanupDone() {
    updateSettingValue(PORTAL_RDBMS_APP_CLEANUP_KEY, true);
  }

  protected static void setMigrated(PortalKey siteToMigrateKey, PortalEntityType entityType) {
    getSettingService().set(entityType.getContext(),
                            new Scope(entityType.getScopeType(), siteToMigrateKey.getType()),
                            siteToMigrateKey.getId(),
                            SettingValue.create(true));
  }

  protected static boolean isMigrated(PortalKey siteToMigrateKey, PortalEntityType entityType) {
    SettingValue<?> settingValue = getSettingService().get(entityType.getContext(),
                                                           new Scope(entityType.getScopeType(), siteToMigrateKey.getType()),
                                                           siteToMigrateKey.getId());
    return settingValue != null && Boolean.parseBoolean(settingValue.getValue().toString());
  }

  protected static void setPageMigrated(PageKey key) {
    getSettingService().set(CONTEXT,
                            Scope.PAGE.id(key.getSite().getTypeName() + "::" + key.getSite().getName()),
                            key.getName(),
                            SettingValue.create(true));
  }

  protected static boolean isPageMigrated(PageKey key) {
    SettingValue<?> settingValue = getSettingService().get(CONTEXT,
                                                           Scope.PAGE.id(key.getSite().getTypeName() + "::"
                                                               + key.getSite().getName()),
                                                           key.getName());
    return settingValue != null && Boolean.parseBoolean(settingValue.getValue().toString());
  }

  protected static void setForceStop() {
    forceStop = true;
  }

  protected static boolean isForceStop() {
    return forceStop;
  }

  protected static List<PortalKey> getSitesToMigrate() {
    return sitesToMigrate;
  }

  protected static int getSitesCountToMigrate() {
    return sitesToMigrate == null ? 0 : sitesToMigrate.size();
  }

  protected static void addPriorizedSitesToMigrate(PortalKey priorizedSiteToMigrate) {
    priorizedSitesToMigrate.add(priorizedSiteToMigrate);
  }

  protected static PortalKey getNextSiteKeyToMigrate() {
    try {
      while (!priorizedSitesToMigrate.isEmpty()) {
        PortalKey portalKey = priorizedSitesToMigrate.get(0);
        priorizedSitesToMigrate.remove(portalKey);

        int indexOfPriorizedSiteKey = sitesToMigrate.indexOf(portalKey);
        if (indexOfPriorizedSiteKey > siteToMigrateIndex) {
          Collections.swap(sitesToMigrate, indexOfPriorizedSiteKey, siteToMigrateIndex);
          return portalKey;
        }
      }
      return sitesToMigrate.get(siteToMigrateIndex);
    } finally {
      siteToMigrateIndex++;
    }
  }

  protected static void setSitesToMigrate(List<PortalKey> sitesToMigrate) {
    MigrationContext.sitesToMigrate = sitesToMigrate;
  }

  protected static SettingService getSettingService() {
    if (settingService == null) {
      settingService = PortalContainer.getInstance().getComponentInstanceOfType(SettingService.class);
    }
    return settingService;
  }

  protected static void restartTransaction() {
    if (forceStop) {
      return;
    }
    int i = 0;
    // Close transactions until no encapsulated transaction
    boolean success = true;
    do {
      try {
        RequestLifeCycle.end();
        i++;
      } catch (IllegalStateException e) {
        success = false;
      }
    } while (success);

    // Restart transactions with the same number of encapsulations
    for (int j = 0; j < i; j++) {
      RequestLifeCycle.begin(PortalContainer.getInstance());
    }
  }

  private static boolean getSettingValue(String key) {
    SettingValue<?> setting = getSettingService().get(MigrationContext.CONTEXT, Scope.GLOBAL.id(null), key);
    if (setting != null) {
      return Boolean.parseBoolean(setting.getValue().toString());
    }
    return false;
  }

  private static void updateSettingValue(String key, Boolean status) {
    getSettingService().set(MigrationContext.CONTEXT, Scope.GLOBAL.id(null), key, SettingValue.create(status));
  }

  protected enum PortalEntityType {
    SITE(CONTEXT, Scope.PORTAL.getName(), "SITE"),
    NAVIGATION(CONTEXT, NAVIGATION_SCOPE, "SITE NAVIGATION"),
    PAGE(CONTEXT, Scope.PAGE.getName(), "SITE PAGES");

    private Context context;

    private String  scopeType;

    private String  title;

    private PortalEntityType(Context context, String scopeType, String title) {
      this.context = context;
      this.scopeType = scopeType;
      this.title = title;
    }

    public Context getContext() {
      return context;
    }

    public String getScopeType() {
      return scopeType;
    }

    public String getTitle() {
      return title;
    }
  }

}
