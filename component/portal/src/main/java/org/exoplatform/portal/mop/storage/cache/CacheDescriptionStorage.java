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
package org.exoplatform.portal.mop.storage.cache;

import java.util.Locale;
import java.util.Map;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.portal.mop.State;
import org.exoplatform.portal.mop.dao.DescriptionDAO;
import org.exoplatform.portal.mop.storage.DescriptionStorageImpl;
import org.exoplatform.portal.mop.storage.cache.model.DescriptionCacheKey;
import org.exoplatform.portal.mop.storage.cache.model.DescriptionCacheSelector;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class CacheDescriptionStorage extends DescriptionStorageImpl {

  private static final Log                                         LOG                    =
                                                                       ExoLogger.getExoLogger(CacheDescriptionStorage.class);

  public static final String                                       DESCRIPTION_CACHE_NAME = "portal.DescriptionService";

  private final FutureExoCache<DescriptionCacheKey, State, Object> descriptionFutureCache;

  private final ExoCache<DescriptionCacheKey, State>               descriptionCache;

  public CacheDescriptionStorage(CacheService cacheService,
                                 DescriptionDAO descriptionDAO) {
    super(descriptionDAO);
    this.descriptionCache = cacheService.getCacheInstance(DESCRIPTION_CACHE_NAME);
    this.descriptionFutureCache = new FutureExoCache<>(new Loader<DescriptionCacheKey, State, Object>() {
      @Override
      public State retrieve(Object context, DescriptionCacheKey cacheKey) throws Exception {
        return CacheDescriptionStorage.super.getDescription(cacheKey.getId(), cacheKey.getLocale(), cacheKey.isIncludeParent());
      }
    }, descriptionCache);
  }

  @Override
  public void setDescription(String id, Locale locale, State description) {
    try {
      super.setDescription(id, locale, description);
    } finally {
      clearCacheEntries(id);
    }
  }

  @Override
  public void setDescription(String id, State description) {
    try {
      super.setDescription(id, description);
    } finally {
      clearCacheEntries(id);
    }
  }

  @Override
  public void setDescriptions(String id, Map<Locale, State> descriptions) {
    try {
      super.setDescriptions(id, descriptions);
    } finally {
      clearCacheEntries(id);
    }
  }

  @Override
  public State getDescription(String id, Locale locale, boolean checkParent) {
    return descriptionFutureCache.get(null, new DescriptionCacheKey(id, locale, checkParent));
  }

  private void clearCacheEntries(String id) {
    try {
      descriptionCache.select(new DescriptionCacheSelector(id));
    } catch (Exception e) {
      LOG.warn("Error selecting cache entries to clear, clear all entries", e);
      descriptionCache.clearCache();
    }
  }

}
