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
package org.exoplatform.portal.mop.storage;

import static org.exoplatform.portal.mop.storage.utils.MOPUtils.parseJsonArray;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.json.simple.JSONArray;

import org.exoplatform.portal.config.NoSuchDataException;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.dao.PageDAO;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PageKey;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public abstract class AbstractPageStorage implements PageStorage {

  private static final Log  LOG = ExoLogger.getLogger(AbstractPageStorage.class);

  protected ListenerService listenerService;

  protected LayoutStorage   layoutStorage;

  protected SiteDAO         siteDAO;

  protected PageDAO         pageDAO;

  protected AbstractPageStorage(ListenerService listenerService,
                                LayoutStorage layoutStorage,
                                SiteDAO siteDAO,
                                PageDAO pageDAO) {
    this.listenerService = listenerService;
    this.layoutStorage = layoutStorage;
    this.siteDAO = siteDAO;
    this.pageDAO = pageDAO;
  }

  @Override
  public void save(PageData page) {
    SiteKey siteKey = new SiteKey(page.getKey().getType(), page.getKey().getId());
    org.exoplatform.portal.mop.page.PageKey mopKey =
                                                   new org.exoplatform.portal.mop.page.PageKey(siteKey, page.getKey().getName());

    PageEntity dst = pageDAO.findByKey(mopKey);
    if (dst == null) {
      throw new NoSuchDataException("The page " + page.getKey() + " not found");
    }
    List<ComponentData> children = page.getChildren();

    JSONArray pageBodyJson = parseJsonArray(dst.getPageBody());
    List<ComponentEntity> newPageBody = layoutStorage.saveChildren(pageBodyJson, children);
    dst.setChildren(newPageBody);
    dst.setPageBody(((JSONArray) dst.toJSON().get("children")).toJSONString());

    pageDAO.update(dst);
    broadcastEvent(EventType.PAGE_UPDATED, mopKey);
  }

  @Override
  public PageData getPage(PageKey key) {
    SiteKey siteKey = new SiteKey(key.getType(), key.getId());
    org.exoplatform.portal.mop.page.PageKey pageKey = new org.exoplatform.portal.mop.page.PageKey(siteKey, key.getName());
    PageEntity entity = pageDAO.findByKey(pageKey);
    return buildPageData(entity);
  }

  protected PageData buildPageData(PageEntity entity) {
    if (entity == null) {
      return null;
    }

    List<String> moveAppsPermissions = layoutStorage.getPermissions(PageEntity.class.getName(),
                                                                    entity.getId(),
                                                                    PermissionEntity.TYPE.MOVE_APP);
    List<String> moveContainersPermissions = layoutStorage.getPermissions(PageEntity.class.getName(),
                                                                          entity.getId(),
                                                                          PermissionEntity.TYPE.MOVE_CONTAINER);
    List<String> accessPermissions = layoutStorage.getPermissions(PageEntity.class.getName(),
                                                                  entity.getId(),
                                                                  PermissionEntity.TYPE.ACCESS);
    List<String> editPermissions = layoutStorage.getPermissions(PageEntity.class.getName(),
                                                                entity.getId(),
                                                                PermissionEntity.TYPE.EDIT);
    String editPermission = CollectionUtils.isEmpty(editPermissions) ? null : editPermissions.get(0);

    List<ComponentData> children = layoutStorage.buildChildren(parseJsonArray(entity.getPageBody()));

    return new PageData("page_" + entity.getId(), // storageId
                        null, // id
                        entity.getName(), // name
                        null, // icon
                        null, // template
                        entity.getFactoryId(), // factoryId
                        entity.getDisplayName(), // title
                        entity.getDescription(), // description
                        null, // width
                        null, // height
                        null, // cssClass
                        entity.getProfiles(), // profiles
                        accessPermissions, // accessPermissions
                        children, // children
                        entity.getOwnerType().getName(), // ownerType
                        entity.getOwnerId(), // ownerId
                        editPermission, // editPermission
                        entity.isShowMaxWindow(), // showMaxWindow
                        entity.isHideSharedLayout(),
                        moveAppsPermissions,
                        moveContainersPermissions,
                        entity.getPageType() != null ? entity.getPageType().name() : null,
                        entity.getLink());
  }

  protected void broadcastEvent(String eventName, org.exoplatform.portal.mop.page.PageKey pageKey) {
    try {
      listenerService.broadcast(eventName, this, pageKey);
    } catch (Exception e) {
      LOG.warn("Error when broadcasting notification " + eventName + " for page " + pageKey, e);
    }
  }
}
