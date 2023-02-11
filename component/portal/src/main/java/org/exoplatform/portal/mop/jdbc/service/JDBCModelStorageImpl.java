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
package org.exoplatform.portal.mop.jdbc.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.gatein.common.io.IOTools;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.impl.UnmarshallingContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.Safe;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.config.NoSuchDataException;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.StaleModelException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.CloneApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.ComponentEntity.TYPE;
import org.exoplatform.portal.jdbc.entity.ContainerEntity;
import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity.AppType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.jdbc.dao.ContainerDAO;
import org.exoplatform.portal.mop.jdbc.dao.PageDAO;
import org.exoplatform.portal.mop.jdbc.dao.PermissionDAO;
import org.exoplatform.portal.mop.jdbc.dao.SiteDAO;
import org.exoplatform.portal.mop.jdbc.dao.WindowDAO;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.BodyData;
import org.exoplatform.portal.pom.data.BodyType;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.exoplatform.portal.pom.data.ModelChange;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PageData;
import org.exoplatform.portal.pom.data.PageKey;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.portal.pom.data.RedirectData;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class JDBCModelStorageImpl implements ModelDataStorage {

  private static final String  DEFAULT_SHAREDLAYOUT_PATH = "war:/conf/portal/portal/sharedlayout.xml";

  private static final String  IMPORTED_STATUS           = Status.class.getName();

  private static final Log     LOG                       = ExoLogger.getExoLogger(JDBCModelStorageImpl.class);

  private SiteDAO              siteDAO;

  private PageDAO              pageDAO;

  private WindowDAO            windowDAO;

  private ContainerDAO         containerDAO;

  private PermissionDAO        permissionDAO;

  private SettingService       settingService;

  private ConfigurationManager confManager;

  public JDBCModelStorageImpl(SiteDAO siteDAO,
                              PageDAO pageDAO,
                              WindowDAO windowDAO,
                              ContainerDAO containerDAO,
                              PermissionDAO permissionDAO,
                              SettingService settingService,
                              ConfigurationManager confManager) {
    this.siteDAO = siteDAO;
    this.pageDAO = pageDAO;
    this.windowDAO = windowDAO;
    this.containerDAO = containerDAO;
    this.permissionDAO = permissionDAO;
    this.settingService = settingService;
    this.confManager = confManager;
  }

  @Override
  public void create(PortalData config) {
    if (getPortalConfig(config.getKey()) != null) {
      throw new IllegalArgumentException("Cannot create portal " + config.getName() + " that already exist");
    }

    SiteEntity entity = new SiteEntity();
    buildSiteEntity(entity, config);
    entity = siteDAO.create(entity);
    savePermissions(entity.getId(), config);
  }

  @Override
  public void save(PortalData config) {
    SiteKey siteKey = new SiteKey(config.getKey().getType(), config.getKey().getId());
    SiteEntity entity = siteDAO.findByKey(siteKey);
    if (entity == null) {
      throw new IllegalArgumentException("Cannot update portal " + config.getName() + " that does not exist");
    }
    buildSiteEntity(entity, config);
    entity = siteDAO.update(entity);
    savePermissions(entity.getId(), config);
  }

  @Override
  public PortalData getPortalConfig(PortalKey key) {
    SiteKey siteKey = new SiteKey(key.getType(), key.getId());
    SiteEntity entity = siteDAO.findByKey(siteKey);
    if (entity != null) {
      return buildPortalData(entity);
    } else {
      return null;
    }
  }

  @ExoTransactional
  @Override
  public void remove(PortalData config) {
    SiteKey siteKey = new SiteKey(config.getKey().getType(), config.getKey().getId());
    SiteEntity entity = siteDAO.findByKey(siteKey);

    if (entity != null) {
      String siteBody = entity.getSiteBody();
      JSONArray children = parseJsonArray(siteBody);
      deleteChildren(children);

      permissionDAO.deletePermissions(SiteEntity.class.getName(), entity.getId());
      siteDAO.delete(entity);
    } else {
      throw new NoSuchDataException("Could not remove non existing portal " + siteKey);
    }
  }

  @Override
  public List<String> getSiteNames(SiteType siteType, int offset, int limit) {
    return switch (siteType) {
    case PORTAL -> siteDAO.findPortalSites(offset, limit);
    case GROUP -> siteDAO.findGroupSites(offset, limit);
    case SPACE -> siteDAO.findSpaceSites(offset, limit);
    default -> throw new IllegalArgumentException("Unexpected value: " + siteType);
    };
  }

  @Override
  public PageData getPage(PageKey key) {
    SiteKey siteKey = new SiteKey(key.getType(), key.getId());
    org.exoplatform.portal.mop.page.PageKey pageKey = new org.exoplatform.portal.mop.page.PageKey(siteKey, key.getName());
    PageEntity entity = pageDAO.findByKey(pageKey);
    return buildPageData(entity);
  }

  @Override
  public List<ModelChange> save(PageData page) {
    PageKey key = page.getKey();
    SiteKey siteKey = new SiteKey(key.getType(), key.getId());
    org.exoplatform.portal.mop.page.PageKey mopKey = new org.exoplatform.portal.mop.page.PageKey(siteKey, key.getName());

    PageEntity dst = pageDAO.findByKey(mopKey);
    if (dst == null) {
      throw new NoSuchDataException("The page " + key + " not found");
    }
    List<ComponentData> children = page.getChildren();

    JSONArray pageBodyJson = parseJsonArray(dst.getPageBody());
    cleanDeletedComponents(pageBodyJson, children);

    List<ComponentEntity> newBody = saveChildren(children);
    dst.setChildren(newBody);
    dst.setPageBody(((JSONArray) dst.toJSON().get("children")).toJSONString());

    pageDAO.update(dst);
    return Collections.<ModelChange> emptyList();
  }

  @Override
  @SuppressWarnings("rawtypes")
  public <S> String getId(ApplicationState<S> state) {
    if (state instanceof TransientApplicationState) {
      TransientApplicationState<S> tstate = (TransientApplicationState<S>) state;
      return tstate.getContentId();
    }

    Long id;
    if (state instanceof PersistentApplicationState pstate) {
      id = Safe.parseLong(pstate.getStorageId());
    } else if (state instanceof CloneApplicationState cstate) {
      id = Safe.parseLong(cstate.getStorageId());
    } else {
      throw new AssertionError();
    }

    WindowEntity window = windowDAO.find(id);
    if (window != null) {
      return window.getContentId();
    } else {
      return null;
    }

  }

  @Override
  @SuppressWarnings("unchecked")
  public <S> S load(ApplicationState<S> state, ApplicationType<S> type) {
    if (state instanceof TransientApplicationState) {
      TransientApplicationState<S> transientState = (TransientApplicationState<S>) state;
      S prefs = transientState.getContentState();
      if (prefs == null && type.getContentType().getStateClass().equals(Portlet.class)) {
        return (S) new Portlet();
      }
      return prefs;
    }

    Long id;
    if (state instanceof CloneApplicationState) {
      id = Safe.parseLong(((CloneApplicationState<S>) state).getStorageId());
    } else {
      id = Safe.parseLong(((PersistentApplicationState<S>) state).getStorageId());
    }
    WindowEntity window = windowDAO.find(id);
    if (window != null) {
      byte[] customization = window.getCustomization();
      if (customization != null) {
        return (S) unserialize(customization);
      } else if (type.getContentType().getStateClass().equals(Portlet.class)) {
        return (S) new Portlet();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public <S> ApplicationState<S> save(ApplicationState<S> state, S preferences) {
    if (state instanceof TransientApplicationState) {
      throw new AssertionError("Does not make sense");
    }

    Long id;
    if (state instanceof CloneApplicationState) {
      id = Safe.parseLong(((CloneApplicationState<S>) state).getStorageId());
    } else {
      id = Safe.parseLong(((PersistentApplicationState<S>) state).getStorageId());
    }
    WindowEntity window = windowDAO.find(id);
    if (window != null) {
      if (preferences != null) {
        window.setCustomization(serialize((Serializable) preferences));
      } else {
        window.setCustomization(null);
      }
      windowDAO.update(window);
    }

    return state;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <S> ApplicationData<S> getApplicationData(String applicationStorageId) {
    WindowEntity window = windowDAO.find(Safe.parseLong(applicationStorageId));
    if (window != null) {
      return buildWindow(window);
    } else {
      return null;
    }
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
  public <T> LazyPageList<T> find(Query<T> q) {
    return find(q, null);
  }

  @Override
  public <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator) {
    Class<T> type = q.getClassType();
    if (PageData.class.equals(type)) {
      throw new UnsupportedOperationException("Use PageService.findPages to instead of");
    } else if (PortletPreferences.class.equals(type)) {
      // this task actually return empty portlet preferences
      return new LazyPageList<T>(new ListAccess<T>() {
        public T[] load(int index, int length) throws Exception {
          throw new AssertionError();
        }

        public int getSize() throws Exception {
          return 0;
        }
      }, 10);
    } else if (PortalData.class.equals(type)) {
      SiteType siteType = SiteType.PORTAL;
      String ownerType = q.getOwnerType();
      if (ownerType != null) {
        siteType = SiteType.valueOf(ownerType.toUpperCase());
      }
      final List<SiteEntity> results = siteDAO.findByType(siteType);

      ListAccess<PortalData> la = new ListAccess<PortalData>() {
        public PortalData[] load(int index, int length) throws Exception {
          List<SiteEntity> entities = results.subList(index, index + length);
          List<PortalData> data = new ArrayList<PortalData>();
          for (SiteEntity entity : entities) {
            data.add(buildPortalData(entity));
          }
          return data.toArray(new PortalData[data.size()]);
        }

        public int getSize() throws Exception {
          return results.size();
        }
      };
      return new LazyPageList(la, 10);
    } else if (PortalKey.class.equals(type) && ("portal".equals(q.getOwnerType()) || "group".equals(q.getOwnerType()))) {
      String ownerType = q.getOwnerType();
      SiteType siteType = SiteType.valueOf(StringUtils.upperCase(ownerType));
      final List<SiteKey> keys = siteDAO.findSiteKey(siteType);

      ListAccess<PortalKey> la = new ListAccess<PortalKey>() {
        public PortalKey[] load(int index, int length) throws Exception {
          List<PortalKey> results = new ArrayList<PortalKey>();
          for (SiteKey key : keys.subList(index, index + length)) {
            results.add(new PortalKey(key.getType().getName(), key.getName()));
          }
          return results.toArray(new PortalKey[results.size()]);
        }

        public int getSize() throws Exception {
          return keys.size();
        }
      };
      return new LazyPageList(la, 10);
    } else {
      throw new UnsupportedOperationException("Could not perform search on query " + q);
    }
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
      InputStream inputStream = confManager.getInputStream(path);
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

  private PageData buildPageData(PageEntity entity) {
    if (entity == null) {
      return null;
    }
    //
    JSONArray pageBody = parseJsonArray(entity.getPageBody());

    List<PermissionEntity> moveApps =
                                    permissionDAO.getPermissions(PageEntity.class.getName(),
                                                                 entity.getId(),
                                                                 PermissionEntity.TYPE.MOVE_APP);
    List<PermissionEntity> moveContainers =
                                          permissionDAO.getPermissions(PageEntity.class.getName(),
                                                                       entity.getId(),
                                                                       PermissionEntity.TYPE.MOVE_CONTAINER);

    return new PageData("page_" + entity.getId(),
                        null,
                        entity.getName(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Collections.<String> emptyList(),
                        buildChildren(pageBody),
                        entity.getOwnerType().getName(),
                        entity.getOwnerId(),
                        null,
                        false,
                        buildPermission(moveApps),
                        buildPermission(moveContainers));
  }

  private Map<Long, WindowEntity> queryWindow(JSONArray jsonBody) {
    Set<Long> ids = new HashSet<>();
    filterId(jsonBody, TYPE.WINDOW, ids);
    List<WindowEntity> entities = windowDAO.findByIds(new LinkedList<>(ids));

    Map<Long, WindowEntity> results = new HashMap<>();
    for (WindowEntity entity : entities) {
      results.put(entity.getId(), entity);
    }

    ids.removeAll(results.keySet());
    if (!ids.isEmpty()) {
      LOG.error("Can't find Window with ids: {}", StringUtils.join(ids, ","));
    }

    return results;
  }

  private Set<Long> filterId(JSONArray jsonBody, TYPE type, Set<Long> ids) {
    if (jsonBody != null) {
      for (Object obj : jsonBody) {
        JSONObject component = (JSONObject) obj;
        TYPE t = TYPE.valueOf(component.get("type").toString());

        if (t.equals(type)) {
          ids.add(Safe.parseLong(component.get("id").toString()));
        }
        if (TYPE.CONTAINER.equals(t)) {
          filterId((JSONArray) component.get("children"), type, ids);
        }
      }
    }
    return ids;
  }

  private Map<Long, ContainerEntity> queryContainer(JSONArray jsonBody) {
    Set<Long> ids = new HashSet<>();
    filterId(jsonBody, TYPE.CONTAINER, ids);
    List<ContainerEntity> entities = containerDAO.findByIds(new LinkedList<>(ids));

    Map<Long, ContainerEntity> results = new HashMap<>();
    for (ContainerEntity entity : entities) {
      results.put(entity.getId(), entity);
    }

    ids.removeAll(results.keySet());
    if (!ids.isEmpty()) {
      LOG.warn("Can't find Container with ids: {}", StringUtils.join(ids, ","));
    }

    return results;
  }

  @SuppressWarnings("rawtypes")
  private List<ComponentEntity> saveChildren(List<ComponentData> children) { // NOSONAR
    List<ComponentEntity> results = new LinkedList<>();

    for (ComponentData srcChild : children) { // NOSONAR
      Long srcChildId = Safe.parseLong(srcChild.getStorageId());

      ComponentEntity dstChild = null;
      if (srcChildId != null && srcChildId > 0) { // update
        if (srcChild instanceof ContainerData containerData) {
          dstChild = containerDAO.find(srcChildId);
          if (dstChild != null) {
            buildContainerEntity((ContainerEntity) dstChild, containerData);
            containerDAO.update((ContainerEntity) dstChild);
          }
        } else if (srcChild instanceof ApplicationData appData) {
          dstChild = windowDAO.find(srcChildId);
          if (dstChild != null) {
            dstChild = buildWindowEntity((WindowEntity) dstChild, appData);
            if (dstChild == null) {
              continue;
            }
            windowDAO.update((WindowEntity) dstChild);
          }
        } else if (srcChild instanceof BodyData) {
          // nothing to update on body data
          dstChild = containerDAO.find(srcChildId);
        } else {
          throw new StaleModelException("this layout component type is not supported: " + srcChild);
        }
      }

      if (dstChild == null || dstChild.getId() == null) { // create new
        if (srcChild instanceof ContainerData srcChildContainer) {
          dstChild = buildContainerEntity(null, srcChildContainer);
          dstChild = containerDAO.create((ContainerEntity) dstChild);
        } else if (srcChild instanceof ApplicationData srcChildApplication) {
          dstChild = buildWindowEntity(null, srcChildApplication);
          if (dstChild == null) {
            continue;
          }
          dstChild = windowDAO.create((WindowEntity) dstChild);
        } else if (srcChild instanceof BodyData srcChildBody) {
          dstChild = buildContainerEntity(srcChildBody);
          dstChild = containerDAO.create((ContainerEntity) dstChild);
        } else {
          throw new StaleModelException("Was not expecting child " + srcChild);
        }
        if (dstChild.getId() == null) {
          throw new IllegalStateException("Id of saved child wasn't found: " + dstChild.getType() + " / " + dstChild);
        }
      }

      savePermissions(dstChild.getId(), srcChild);

      if (srcChild instanceof ContainerData srcChildContainer) {
        List<ComponentEntity> descendants = saveChildren(srcChildContainer.getChildren());
        ((ContainerEntity) dstChild).setChildren(descendants);
      }

      results.add(dstChild);
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  private ComponentEntity buildContainerEntity(BodyData bodyData) {
    ContainerEntity dst = new ContainerEntity();
    JSONObject properties = new JSONObject();
    properties.put(MappedAttributes.TYPE.getName(), bodyData.getType().name());
    dst.setProperties(properties.toJSONString());
    return dst;
  }

  private List<ComponentData> buildChildren(JSONArray jsonBody) {
    Map<Long, ContainerEntity> containers = queryContainer(jsonBody);
    Map<Long, WindowEntity> windows = queryWindow(jsonBody);

    return buildChildren(jsonBody, containers, windows);
  }

  private List<ComponentData> buildChildren(JSONArray jsonBody, // NOSONAR
                                            Map<Long, ContainerEntity> containers,
                                            Map<Long, WindowEntity> windows) {
    List<ComponentData> results = new LinkedList<>();

    if (jsonBody != null) {
      for (Object component : jsonBody) {
        JSONObject jsonComponent = (JSONObject) component;
        Long id = Safe.parseLong(jsonComponent.get("id").toString());
        TYPE type = TYPE.valueOf(jsonComponent.get("type").toString());

        if (type == TYPE.WINDOW) {
          results.add(buildWindow(windows.get(id)));
        } else if (type == TYPE.CONTAINER) {
          ContainerEntity srcContainer = containerDAO.find(id);
          JSONObject attrs = parseJsonObject(srcContainer.getProperties());
          String ctype = (String) attrs.get(MappedAttributes.TYPE.getName());
          if (BodyType.PAGE.name().equals(ctype)) {
            BodyData body = new BodyData(String.valueOf(id), BodyType.PAGE);
            results.add(body);
          } else {
            results.add(buildContainer(containers.get(id), jsonComponent, attrs, containers, windows));
          }
        }
      }
    }

    return results;
  }

  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  private ApplicationData buildWindow(WindowEntity windowEntity) {
    ApplicationType<?> appType = convert(windowEntity.getAppType());
    if (appType == null) {
      return null;
    }
    PersistentApplicationState<Portlet> state = new PersistentApplicationState<>(String.valueOf(windowEntity.getId()));

    Map<String, String> properties = new HashMap<>();
    JSONObject jProp = parseJsonObject(windowEntity.getProperties());
    jProp.forEach((key, value) -> {
      if (key != null && value != null) {
        properties.put(key.toString(), value.toString());
      }
    });

    List<PermissionEntity> access = permissionDAO.getPermissions(WindowEntity.class.getName(),
                                                                 windowEntity.getId(),
                                                                 PermissionEntity.TYPE.ACCESS);

    return new ApplicationData(String.valueOf(windowEntity.getId()),
                               null,
                               appType,
                               state,
                               String.valueOf(windowEntity.getId()),
                               windowEntity.getTitle(),
                               windowEntity.getIcon(),
                               windowEntity.getDescription(),
                               windowEntity.isShowInfoBar(),
                               windowEntity.isShowApplicationState(),
                               windowEntity.isShowApplicationMode(),
                               windowEntity.getTheme(),
                               windowEntity.getWidth(),
                               windowEntity.getHeight(),
                               properties,
                               buildPermission(access));
  }

  private ContainerData buildContainer(ContainerEntity entity,
                                       JSONObject jsonComponent,
                                       JSONObject attrs,
                                       Map<Long, ContainerEntity> containers,
                                       Map<Long, WindowEntity> windows) {
    List<ComponentData> children = buildChildren((JSONArray) jsonComponent.get("children"), containers, windows);

    List<PermissionEntity> access = permissionDAO.getPermissions(ContainerEntity.class.getName(),
                                                                 entity.getId(),
                                                                 PermissionEntity.TYPE.ACCESS);
    List<PermissionEntity> moveApps =
                                    permissionDAO.getPermissions(ContainerEntity.class.getName(),
                                                                 entity.getId(),
                                                                 PermissionEntity.TYPE.MOVE_APP);
    List<PermissionEntity> moveConts =
                                     permissionDAO.getPermissions(ContainerEntity.class.getName(),
                                                                  entity.getId(),
                                                                  PermissionEntity.TYPE.MOVE_CONTAINER);

    String cssClass = null;
    String profiles = null;
    if (attrs != null) {
      if (attrs.containsKey(MappedAttributes.CSS_CLASS.getName())) {
        cssClass = (String) attrs.get(MappedAttributes.CSS_CLASS.getName());
      }
      if (attrs.containsKey(MappedAttributes.PROFILES.getName())) {
        profiles = (String) attrs.get(MappedAttributes.PROFILES.getName());
      }
    }

    return new ContainerData(String.valueOf(entity.getId()),
                             entity.getWebuiId(),
                             entity.getName(),
                             entity.getIcon(),
                             entity.getTemplate(),
                             entity.getFactoryId(),
                             entity.getTitle(),
                             entity.getDescription(),
                             entity.getWidth(),
                             entity.getHeight(),
                             cssClass,
                             profiles,
                             buildPermission(access),
                             buildPermission(moveApps),
                             buildPermission(moveConts),
                             children);
  }

  private void cleanDeletedComponents(JSONArray body, List<ComponentData> children) {
    Set<Long> windowIds = new HashSet<>();
    filterId(body, TYPE.WINDOW, windowIds);
    for (Long id : windowIds) {
      if (findById(id, TYPE.WINDOW, children) == null) {
        windowDAO.deleteById(id);
      }
    }

    Set<Long> containerIds = new HashSet<>();
    filterId(body, TYPE.CONTAINER, containerIds);
    for (Long id : containerIds) {
      if (findById(id, TYPE.CONTAINER, children) == null) {
        containerDAO.deleteById(id);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private ContainerEntity buildContainerEntity(ContainerEntity dst, ContainerData src) {
    if (dst == null) {
      dst = new ContainerEntity();
    }
    dst.setWebuiId(src.getId());
    dst.setDescription(src.getDescription());
    dst.setFactoryId(src.getFactoryId());
    dst.setHeight(src.getHeight());
    dst.setIcon(src.getIcon());
    dst.setName(src.getName());
    dst.setTemplate(src.getTemplate());
    dst.setTitle(src.getTitle());
    dst.setWidth(src.getWidth());

    boolean hasProfiles = StringUtils.isNotBlank(src.getProfiles());
    boolean hasCssClass = StringUtils.isNotBlank(src.getCssClass());
    if (hasProfiles || hasCssClass) {
      JSONObject properties = new JSONObject();
      if (hasProfiles) {
        properties.put(MappedAttributes.PROFILES.getName(), src.getProfiles());
      }
      if (hasCssClass) {
        properties.put(MappedAttributes.CSS_CLASS.getName(), src.getCssClass());
      }
      dst.setProperties(properties.toJSONString());
    }

    return dst;
  }

  @SuppressWarnings("rawtypes")
  private WindowEntity buildWindowEntity(WindowEntity dst, ApplicationData srcChild) {
    if (dst == null) {
      dst = new WindowEntity();

      ApplicationType type = srcChild.getType();
      if (type == null) {
        LOG.warn("Application type of instance {} is not recognized, ignore it", dst.getContentId());
        return null;
      }
      if (ApplicationType.PORTLET.getName().equals(type.getName())) {
        dst.setAppType(AppType.PORTLET);
      }

      ApplicationState state = srcChild.getState();
      if (state instanceof TransientApplicationState s) {
        dst.setContentId(s.getContentId());
        if (s.getContentState() != null) {
          dst.setCustomization(serialize((Serializable) s.getContentState()));
        }
      } else {
        throw new IllegalStateException("Can't create new window");
      }
    }
    dst.setDescription(srcChild.getDescription());
    dst.setHeight(srcChild.getHeight());
    dst.setIcon(srcChild.getIcon());
    dst.setProperties(toJSONString(srcChild.getProperties()));
    dst.setShowApplicationMode(srcChild.isShowApplicationMode());
    dst.setShowApplicationState(srcChild.isShowApplicationState());
    dst.setShowInfoBar(srcChild.isShowInfoBar());
    dst.setTheme(srcChild.getTheme());
    dst.setTitle(srcChild.getTitle());
    dst.setWidth(srcChild.getWidth());

    return dst;
  }

  private ComponentData findById(Long id, ComponentEntity.TYPE type, List<ComponentData> children) {
    if (children != null) {
      for (ComponentData child : children) {
        if (id.equals(Safe.parseLong(child.getStorageId()))
            && ((type == TYPE.WINDOW && child instanceof ApplicationData)
                || (type == TYPE.CONTAINER && child instanceof ContainerData))) {

          return child;
        } else if (child instanceof ContainerData childContainer) {
          ComponentData result = findById(id, type, childContainer.getChildren());
          if (result != null) {
            return result;
          }
        }
      }
    }
    return null;
  }

  private List<String> buildPermission(List<PermissionEntity> permissions) {
    List<String> results = new ArrayList<>();
    if (permissions != null) {
      for (PermissionEntity per : permissions) {
        results.add(per.getPermission());
      }
    }
    return results;
  }

  private void savePermissions(Long id, PortalData config) {
    if (id == null) {
      throw new IllegalArgumentException("id is null");
    }
    permissionDAO.savePermissions(SiteEntity.class.getName(),
                                  id,
                                  PermissionEntity.TYPE.ACCESS,
                                  config.getAccessPermissions());
    permissionDAO.savePermissions(SiteEntity.class.getName(),
                                  id,
                                  PermissionEntity.TYPE.EDIT,
                                  Arrays.asList(config.getEditPermission()));
  }

  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  private void savePermissions(Long id, ComponentData srcChild) {
    if (id == null) {
      throw new IllegalArgumentException("id is null");
    }
    List<String> access = null;
    String typeName = srcChild.getClass().getName();
    if (srcChild instanceof ContainerData srcChildContainer) {
      typeName = ContainerEntity.class.getName();
      access = srcChildContainer.getAccessPermissions();
      permissionDAO.savePermissions(typeName,
                                    id,
                                    PermissionEntity.TYPE.MOVE_APP,
                                    srcChildContainer.getMoveAppsPermissions());
      permissionDAO.savePermissions(typeName,
                                    id,
                                    PermissionEntity.TYPE.MOVE_CONTAINER,
                                    srcChildContainer.getMoveContainersPermissions());
    } else if (srcChild instanceof ApplicationData srcChildApplication) {
      typeName = WindowEntity.class.getName();
      access = srcChildApplication.getAccessPermissions();
    }

    permissionDAO.savePermissions(typeName, id, PermissionEntity.TYPE.ACCESS, access);
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

    JSONArray pageBody = parseJsonArray(entity.getSiteBody());
    cleanDeletedComponents(pageBody, children);

    List<ComponentEntity> newBody = saveChildren(children);

    entity.setChildren(newBody);
    entity.setSiteBody(((JSONArray) entity.toJSON().get("children")).toJSONString());
  }

  @SuppressWarnings("unchecked")
  private PortalData buildPortalData(SiteEntity entity) {
    if (entity == null) {
      return null;
    }
    JSONArray siteBody = parseJsonArray(entity.getSiteBody());
    List<ComponentData> children = buildChildren(siteBody);
    ContainerData rootContainer = null;
    if (!children.isEmpty()) {
      rootContainer = (ContainerData) children.get(0);
    } else {
      throw new IllegalStateException("site doens't has root container layout");
    }

    List<PermissionEntity> access = permissionDAO.getPermissions(SiteEntity.class.getName(),
                                                                 entity.getId(),
                                                                 PermissionEntity.TYPE.ACCESS);
    List<PermissionEntity> editPermissionsList = permissionDAO.getPermissions(SiteEntity.class.getName(),
                                                                              entity.getId(),
                                                                              PermissionEntity.TYPE.EDIT);
    String editPermission = CollectionUtils.isEmpty(editPermissionsList) ? null : editPermissionsList.get(0).getPermission();
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
                          buildPermission(access),
                          editPermission,
                          Collections.unmodifiableMap(properties),
                          entity.getSkin(),
                          rootContainer,
                          entity.isDefaultSiteBody(),
                          redirects);
  }

  private void deleteChildren(JSONArray children) {
    for (Object child : children) {
      JSONObject c = (JSONObject) child;
      Long id = Safe.parseLong(c.get("id").toString());
      TYPE t = TYPE.valueOf(c.get("type").toString());

      if (TYPE.CONTAINER.equals(t)) {
        JSONArray descendants = (JSONArray) c.get("children");
        if (descendants != null) {
          deleteChildren(descendants);
        }

        ContainerEntity container = containerDAO.find(id);
        if (container != null) {
          JSONArray dashboardChilds = parseJsonArray(container.getContainerBody());
          deleteChildren(dashboardChilds);

          permissionDAO.deletePermissions(ContainerEntity.class.getName(), container.getId());
          containerDAO.delete(container);
        }
      } else if (TYPE.WINDOW.equals(t)) {
        WindowEntity window = windowDAO.find(id);
        if (window != null) {
          permissionDAO.deletePermissions(WindowEntity.class.getName(), window.getId());
          windowDAO.delete(window);
        }
      } else {
        throw new IllegalArgumentException("Can't delete child with type: " + t);
      }
    }
  }

  /**
   * This is a hack and should be removed, it is only used temporarily. This is
   * because the objects are loaded from files and don't have name. (this is
   * clone from POMDataStorage)
   */
  private void generateStorageName(ModelObject obj) {
    if (obj instanceof Container container) {
      for (ModelObject child : container.getChildren()) {
        generateStorageName(child);
      }
    } else if (obj instanceof Application) {
      obj.setStorageName(UUID.randomUUID().toString());
    }
  }

  private JSONArray parseJsonArray(String content) {
    try {
      JSONParser parser = new JSONParser();
      return (JSONArray) parser.parse(content);
    } catch (ParseException e) {
      throw new IllegalStateException("Error parsing JSON content: " + content, e);
    }
  }

  private JSONObject parseJsonObject(String content) {
    try {
      JSONParser parser = new JSONParser();
      return (JSONObject) parser.parse(content);
    } catch (ParseException e) {
      throw new IllegalStateException("Error parsing JSON content: " + content, e);
    }
  }

  private String toJSONString(Map<?, ?> properties) {
    JSONObject json = new JSONObject(properties);
    return json.toJSONString();
  }

  private Serializable unserialize(byte[] bytes) {
    try {
      return IOTools.unserialize(bytes);
    } catch (Exception e) {
      throw new IllegalStateException("Error unserializing bytes", e);
    }
  }

  private byte[] serialize(Serializable obj) {
    try {
      return IOTools.serialize(obj);
    } catch (Exception e) {
      throw new IllegalStateException("Error serializing object: " + obj, e);
    }
  }

  private ApplicationType<Portlet> convert(AppType appType) {
    if (appType == AppType.PORTLET) {
      return ApplicationType.PORTLET;
    } else {
      return null;
    }
  }

}
