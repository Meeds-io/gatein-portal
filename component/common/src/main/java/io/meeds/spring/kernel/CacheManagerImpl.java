/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.spring.kernel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractValueAdaptingCache;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import lombok.Setter;

public class CacheManagerImpl implements CacheManager {

  @Setter
  private CacheService       cacheService;

  private Map<String, Cache> cacheInstances = new ConcurrentHashMap<>();

  public CacheManagerImpl(CacheService cacheService) {
    this.cacheService = cacheService;
  }

  @Override
  public Collection<String> getCacheNames() {
    return cacheService.getAllCacheInstances().stream().map(ExoCache::getName).toList();
  }

  @Override
  public Cache getCache(String name) {
    return cacheInstances.computeIfAbsent(name, k -> {
      ExoCache<Serializable, Object> cacheInstance = cacheService.getCacheInstance(name);
      return new AbstractValueAdaptingCache(false) {

        @Override
        public String getName() {
          return cacheInstance.getName();
        }

        @Override
        public Object getNativeCache() {
          return cacheInstance;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
          T value = (T) cacheInstance.get((Serializable) key);
          if (value == null) {
            try {
              value = valueLoader.call();
              cacheInstance.put((Serializable) key, value);
            } catch (Exception e) {
              throw new ValueRetrievalException(key, valueLoader, e);
            }
          }
          return value;
        }

        @Override
        public void put(Object key, Object value) {
          cacheInstance.put((Serializable) key, value);
        }

        @Override
        public void evict(Object key) {
          cacheInstance.remove((Serializable) key);
        }

        @Override
        public void clear() {
          cacheInstance.clearCache();
        }

        @Override
        protected Object lookup(Object key) {
          return cacheInstance.get((Serializable) key);
        }

      };
    });
  }

}
