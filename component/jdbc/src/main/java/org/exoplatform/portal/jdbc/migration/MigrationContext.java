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

public final class MigrationContext {
  public static final String PORTAL_RDBMS_MIGRATION_STATUS_KEY = "PORTAL_RDBMS_MIGRATION_DONE";
  public static final String PORTAL_RDBMS_SITE_MIGRATION_KEY = "PORTAL_RDBMS_SITE_MIGRATION_DONE";  
  public static final String PORTAL_RDBMS_SITE_CLEANUP_KEY = "PORTAL_RDBMS_SITE_CLEANUP_DONE";
  public static final String PORTAL_RDBMS_PAGE_MIGRATION_KEY = "PORTAL_RDBMS_PAGE_MIGRATION_DONE";;
  public static final String PORTAL_RDBMS_PAGE_CLEANUP_KEY = "PORTAL_RDBMS_PAGE_CLEANUP_DONE";
  public static final String PORTAL_RDBMS_NAV_MIGRATION_KEY = "PORTAL_RDBMS_NAV_MIGRATION_DONE";;
  public static final String PORTAL_RDBMS_NAV_CLEANUP_KEY = "PORTAL_RDBMS_NAV_CLEANUP_DONE";
  public static final String PORTAL_RDBMS_APP_MIGRATION_KEY = "PORTAL_RDBMS_APP_MIGRATION_DONE";;
  public static final String PORTAL_RDBMS_APP_CLEANUP_KEY = "PORTAL_RDBMS_APP_CLEANUP_DONE";
    
  //
  private static boolean isDone = false;
  private static boolean isSiteDone = false;
  private static boolean isSiteCleanupDone = false;
  
  private static boolean isPageDone = false;
  private static boolean isPageCleanupDone = false;
  
  private static boolean isNavDone = false;
  private static boolean isNavCleanupDone = false;
  
  private static boolean isAppDone = false;
  private static boolean isAppCleanupDone = false;

  public static boolean isDone() {
    return isDone;
  }

  public static void setDone(boolean isDoneArg) {
    isDone = isDoneArg;
  }

  public static boolean isSiteDone() {
    return isSiteDone;
  }

  public static void setSiteDone(boolean isSiteDone) {
    MigrationContext.isSiteDone = isSiteDone;
  }

  public static boolean isSiteCleanupDone() {
    return isSiteCleanupDone;
  }

  public static void setSiteCleanupDone(boolean isSiteCleanupDone) {
    MigrationContext.isSiteCleanupDone = isSiteCleanupDone;
  }

  public static void setPageDone(boolean isPageDone) {
    MigrationContext.isPageDone = isPageDone;
  }

  public static void setNavigationDone(boolean isNavDone) {
    MigrationContext.isNavDone = isNavDone;
  }

  public static void setAppDone(boolean isAppDone) {
    MigrationContext.isAppDone = isAppDone;
  }

  public static boolean isPageCleanupDone() {
    return isPageCleanupDone;
  }

  public static void setPageCleanupDone(boolean isPageCleanupDone) {
    MigrationContext.isPageCleanupDone = isPageCleanupDone;
  }

  public static boolean isNavDone() {
    return isNavDone;
  }

  public static void setNavDone(boolean isNavDone) {
    MigrationContext.isNavDone = isNavDone;
  }

  public static boolean isNavCleanupDone() {
    return isNavCleanupDone;
  }

  public static void setNavCleanupDone(boolean isNavCleanupDone) {
    MigrationContext.isNavCleanupDone = isNavCleanupDone;
  }

  public static boolean isAppCleanupDone() {
    return isAppCleanupDone;
  }

  public static void setAppCleanupDone(boolean isAppCleanupDone) {
    MigrationContext.isAppCleanupDone = isAppCleanupDone;
  }

  public static boolean isPageDone() {
    return isPageDone;
  }

  public static boolean isAppDone() {
    return isAppDone;
  }
}
