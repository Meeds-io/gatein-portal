/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.portal.mop.cache;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.navigation.NavigationStore;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.storage.LayoutStorage;
import org.exoplatform.portal.mop.storage.SiteStorageImpl;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.ListenerService;

public class CacheSiteStorage extends SiteStorageImpl {

  public static final String                                SITE_CACHE_NAME = "portal.SiteService";

  private final FutureExoCache<SiteKey, PortalData, Object> siteFutureCache;

  private final ExoCache<SiteKey, PortalData>               siteCache;

  public CacheSiteStorage(CacheService cacheService, // NOSONAR
                          SettingService settingService,
                          ListenerService listenerService,
                          ConfigurationManager confManager,
                          NavigationStore navigationStorage,
                          PageService pageStorage,
                          LayoutStorage layoutStorage,
                          SiteDAO siteDAO) {
    super(settingService, listenerService, confManager, navigationStorage, pageStorage, layoutStorage, siteDAO);
    this.siteCache = cacheService.getCacheInstance(SITE_CACHE_NAME);
    this.siteFutureCache = new FutureExoCache<>(new Loader<SiteKey, PortalData, Object>() {
      @Override
      public PortalData retrieve(Object context, SiteKey siteKey) throws Exception {
        return CacheSiteStorage.super.getPortalConfig(siteKey);
      }
    }, siteCache);
  }

  @Override
  public void create(PortalData config) {
    try {
      super.create(config);
    } finally {
      siteFutureCache.remove(getSiteKey(config.getKey()));
    }
  }

  @Override
  public void save(PortalData config) {
    try {
      super.save(config);
    } finally {
      siteFutureCache.remove(getSiteKey(config.getKey()));
    }
  }

  @Override
  public void remove(PortalData config) {
    try {
      super.remove(config);
    } finally {
      siteFutureCache.remove(getSiteKey(config.getKey()));
    }
  }

  @Override
  public PortalData getPortalConfig(SiteKey key) {
    return siteFutureCache.get(null, key);
  }

  private SiteKey getSiteKey(PortalKey portalKey) {
    return new SiteKey(portalKey.getType(), portalKey.getId());
  }
}
