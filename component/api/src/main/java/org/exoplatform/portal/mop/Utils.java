/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.mop;

import java.util.*;

import org.gatein.mop.api.workspace.ObjectType;
import org.gatein.mop.api.workspace.Site;

import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.pom.data.PageData;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class Utils {

  /** . */
  public static final String[]                             EMPTY_STRING_ARRAY = new String[0];

  /** . */
  private static final ComparableComparator                INSTANCE           = new ComparableComparator();

  /** . */
  private static final EnumMap<SiteType, ObjectType<Site>> a                  =
                                                             new EnumMap<SiteType, ObjectType<Site>>(SiteType.class);

  /** . */
  private static final Map<ObjectType<Site>, SiteType>     b                  = new HashMap<ObjectType<Site>, SiteType>(3);

  static {
    a.put(SiteType.PORTAL, ObjectType.PORTAL_SITE);
    a.put(SiteType.GROUP, ObjectType.GROUP_SITE);
    a.put(SiteType.USER, ObjectType.USER_SITE);
    b.put(ObjectType.PORTAL_SITE, SiteType.PORTAL);
    b.put(ObjectType.GROUP_SITE, SiteType.GROUP);
    b.put(ObjectType.USER_SITE, SiteType.USER);
  }

  public static ObjectType<Site> objectType(SiteType siteType) {
    return a.get(siteType);
  }

  public static SiteType siteType(ObjectType objectType) {
    return b.get(objectType);
  }

  public static <T extends Comparable<T>> Comparator<T> comparator() {
    // Not totally good but well... should we pass the class to the caller ?
    @SuppressWarnings("unchecked")
    ComparableComparator instance = INSTANCE;
    return instance;
  }

  public static PageState toPageState(Page page) {
    PageState pageState = new PageState(page.getTitle(),
                                        page.getDescription(),
                                        page.isShowMaxWindow(),
                                        page.isHideSharedLayout(),
                                        page.getFactoryId(),
                                        page.getProfiles(),
                                        page.getAccessPermissions() != null ? Arrays.asList(page.getAccessPermissions()) : null,
                                        page.getEditPermission(),
                                        page.getType(),
                                        page.getLink());
    pageState.setStorageId(page.getStorageId());
    return pageState;
  }

  public static PageState toPageState(PageData page) {
    PageState pageState = new PageState(page.getTitle(),
                                        page.getDescription(),
                                        page.isShowMaxWindow(),
                                        page.isHideSharedLayout(),
                                        page.getFactoryId(),
                                        page.getProfiles(),
                                        page.getAccessPermissions(),
                                        page.getEditPermission(),
                                        page.getType(),
                                        page.getLink());
    pageState.setStorageId(page.getStorageId());
    return pageState;
  }

  private static class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {
    public int compare(T o1, T o2) {
      return o1.compareTo(o2);
    }
  }
}
