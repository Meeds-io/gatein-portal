/**
 * Copyright (C) 2016 eXo Platform SAS.
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
package org.exoplatform.portal.mop.storage;

import static org.exoplatform.portal.mop.storage.utils.MOPUtils.generateStorageName;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.parseJsonArray;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.parseJsonObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.impl.UnmarshallingContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.NoSuchDataException;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.dao.SiteDAO;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.portal.pom.data.RedirectData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class SiteStorageImpl implements SiteStorage {

  private static final String  DEFAULT_SHAREDLAYOUT_PATH = "war:/conf/portal/portal/sharedlayout.xml";   // NOSONAR

  private static final String  IMPORTED_STATUS           = Status.class.getName();

  private static final Log     LOG                       = ExoLogger.getExoLogger(SiteStorageImpl.class);

  private SettingService       settingService;

  private ConfigurationManager configurationManager;

  private NavigationStorage    navigationStorage;

  private PageStorage          pageStorage;

  private LayoutStorage        layoutStorage;

  private SiteDAO              siteDAO;

  public SiteStorageImpl(SettingService settingService,
                         ConfigurationManager configurationManager,
                         NavigationStorage navigationStorage,
                         PageStorage pageStorage,
                         LayoutStorage layoutStorage,
                         SiteDAO siteDAO) {
    this.navigationStorage = navigationStorage;
    this.pageStorage = pageStorage;
    this.layoutStorage = layoutStorage;
    this.settingService = settingService;
    this.configurationManager = configurationManager;
    this.siteDAO = siteDAO;
  }

  @Override
  public void create(PortalConfig config) {
    create(config.build());
  }

  @Override
  public void create(PortalData config) {
    SiteEntity entity = new SiteEntity();
    buildSiteEntity(entity, config);
    entity = siteDAO.create(entity);
    savePermissions(entity.getId(), config);
  }

  @Override
  public void save(PortalConfig config) {
    save(config.build());
  }

  @Override
  public void save(PortalData config) {
    SiteKey siteKey = new SiteKey(config.getKey().getType(), config.getKey().getId());
    SiteEntity entity = siteDAO.findByKey(siteKey);
    if (entity == null) {
      throw new IllegalStateException("Cannot update portal " + config.getName() + " that does not exist");
    }
    buildSiteEntity(entity, config);
    entity = siteDAO.update(entity);
    savePermissions(entity.getId(), config);
  }

  @Override
  public void remove(PortalConfig config) {
    remove(config.build());
  }

  @Override
  public void remove(PortalData config) {
    SiteKey siteKey = new SiteKey(config.getKey().getType(), config.getKey().getId());
    remove(siteKey);
  }

  @Override
  @ExoTransactional
  public void remove(SiteKey siteKey) {
    PortalData config = getPortalConfig(siteKey);
    if (config != null) {
      SiteEntity entity = siteDAO.findByKey(siteKey);
      String siteBody = entity.getSiteBody();
      JSONArray children = parseJsonArray(siteBody);
      layoutStorage.deleteChildren(children);
      layoutStorage.deletePermissions(SiteEntity.class.getName(), entity.getId());
      navigationStorage.destroyNavigation(siteKey);
      pageStorage.destroyPages(siteKey);
      siteDAO.delete(entity);
    } else {
      throw new NoSuchDataException("Could not remove non existing portal " + siteKey);
    }
  }

  @Override
  public PortalConfig getPortalConfig(String ownerType, String portalName) {
    SiteKey siteKey = new SiteKey(ownerType, portalName);
    PortalData data = getPortalConfig(siteKey);
    return data != null ? new PortalConfig(data) : null;
  }

  @Override
  public PortalData getPortalConfig(PortalKey key) {
    SiteKey siteKey = new SiteKey(key.getType(), key.getId());
    return getPortalConfig(siteKey);
  }

  @Override
  public PortalData getPortalConfig(SiteKey siteKey) {
    SiteEntity entity = siteDAO.findByKey(siteKey);
    if (entity != null) {
      return buildPortalData(entity);
    } else {
      return null;
    }
  }

  @Override
  public List<String> getSiteNames(SiteType siteType, int offset, int limit) {
    return switch (siteType) {
    case PORTAL -> siteDAO.findPortalSites(offset, limit);
    case GROUP -> siteDAO.findGroupSites(offset, limit);
    case SPACE -> siteDAO.findSpaceSites(offset, limit);
    case USER -> siteDAO.findUserSites(offset, limit);
    default -> throw new IllegalArgumentException("Unexpected value: " + siteType);
    };
  }

  @Override
  public Status getImportStatus() {
    @SuppressWarnings("unchecked")
    SettingValue<String> setting = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                             Scope.GLOBAL.id(null),
                                                                             IMPORTED_STATUS);
    if (setting != null) {
      String value = setting.getValue();
      try {
        return Status.getStatus(Integer.parseInt(value));
      } catch (Exception ex) {
        LOG.error("Can't parse setting value of import status", ex);
      }
    }
    return null;
  }

  @Override
  public void saveImportStatus(Status status) {
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL.id(null),
                       IMPORTED_STATUS,
                       SettingValue.create(String.valueOf(status.status())));
  }

  @Override
  public Container getSharedLayout(String siteName) {
    String path = null;
    if (StringUtils.isBlank(siteName)) {
      path = DEFAULT_SHAREDLAYOUT_PATH;
    } else {
      path = "war:/conf/portal/portal/sharedlayout-" + siteName + ".xml";
    }
    try {
      InputStream inputStream = configurationManager.getInputStream(path);
      String out = IOUtil.getStreamContentAsString(inputStream);
      ByteArrayInputStream is = new ByteArrayInputStream(out.getBytes(StandardCharsets.UTF_8));
      IBindingFactory bfact = BindingDirectory.getFactory(Container.class);
      UnmarshallingContext uctx = (UnmarshallingContext) bfact.createUnmarshallingContext();
      uctx.setDocument(is, null, "UTF-8", false);
      Container container = (Container) uctx.unmarshalElement();
      generateStorageName(container);
      return container;
    } catch (IOException e) {
      if (StringUtils.isNotBlank(siteName)) {
        return getSharedLayout(null);
      } else {
        throw new IllegalStateException("Unable to read file with path: " + path, e);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to parse shared layout foud in path: " + path, e);
    }
  }

  private void buildSiteEntity(SiteEntity entity, PortalData config) {
    entity.setDescription(config.getDescription());
    entity.setLabel(config.getLabel());
    entity.setLocale(config.getLocale());
    entity.setName(config.getName());
    entity.setSiteType(SiteType.valueOf(config.getKey().getType().toUpperCase()));
    entity.setDefaultSiteBody(config.isDefaultLayout());
    entity.setSkin(config.getSkin());
    String propertiesString = "{}";
    Map<String, String> properties = config.getProperties();
    if (properties != null) {
      propertiesString = new JSONObject(properties).toJSONString();
    }
    entity.setProperties(propertiesString);

    List<ComponentData> children = new ArrayList<>();
    children.add(config.getPortalLayout());

    JSONArray siteBody = parseJsonArray(entity.getSiteBody());
    List<ComponentEntity> newSiteBody = layoutStorage.saveChildren(siteBody, children);
    entity.setChildren(newSiteBody);
    entity.setSiteBody(((JSONArray) entity.toJSON().get("children")).toJSONString());
    entity.setDisplayed(config.isDisplayed());
    entity.setDisplayOrder(config.getDisplayOrder());
  }

  @SuppressWarnings("unchecked")
  private PortalData buildPortalData(SiteEntity entity) {
    if (entity == null) {
      return null;
    }
    JSONArray siteBody = parseJsonArray(entity.getSiteBody());
    List<ComponentData> children = layoutStorage.buildChildren(siteBody);
    ContainerData rootContainer = null;
    if (!children.isEmpty()) {
      rootContainer = (ContainerData) children.get(0);
    } else {
      throw new IllegalStateException("site doens't has root container layout");
    }

    List<String> access = layoutStorage.getPermissions(SiteEntity.class.getName(),
                                                       entity.getId(),
                                                       PermissionEntity.TYPE.ACCESS);
    List<String> editPermissionsList = layoutStorage.getPermissions(SiteEntity.class.getName(),
                                                                    entity.getId(),
                                                                    PermissionEntity.TYPE.EDIT);
    String editPermission = CollectionUtils.isEmpty(editPermissionsList) ? null : editPermissionsList.get(0);
    List<RedirectData> redirects = Collections.emptyList();

    Map<String, String> properties = new HashMap<>();
    if (StringUtils.isNotBlank(entity.getProperties())) {
      JSONObject jProp = parseJsonObject(entity.getProperties());
      jProp.forEach((key, value) -> {
        if (key != null && value != null) {
          properties.put(key.toString(), value.toString());
        }
      });
    }

    return new PortalData("site_" + entity.getId(),
                          entity.getName(),
                          entity.getSiteType().getName(),
                          entity.getLocale(),
                          entity.getLabel(),
                          entity.getDescription(),
                          access,
                          editPermission,
                          Collections.unmodifiableMap(properties),
                          entity.getSkin(),
                          rootContainer,
                          entity.isDefaultSiteBody(),
                          redirects,
                          entity.isDisplayed(),
                          entity.getDisplayOrder());
  }

  private void savePermissions(Long id, PortalData config) {
    if (id == null) {
      throw new IllegalArgumentException("id is null");
    }
    layoutStorage.savePermissions(SiteEntity.class.getName(),
                                  id,
                                  PermissionEntity.TYPE.ACCESS,
                                  config.getAccessPermissions());
    layoutStorage.savePermissions(SiteEntity.class.getName(),
                                  id,
                                  PermissionEntity.TYPE.EDIT,
                                  Arrays.asList(config.getEditPermission()));
  }

}
