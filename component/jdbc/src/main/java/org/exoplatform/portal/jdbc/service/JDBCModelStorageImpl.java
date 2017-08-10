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
package org.exoplatform.portal.jdbc.service;

import java.io.Serializable;
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

import org.apache.commons.lang.StringUtils;
import org.gatein.common.io.IOTools;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.config.NoSuchDataException;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.CloneApplicationState;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.jdbc.dao.ContainerDAO;
import org.exoplatform.portal.jdbc.dao.PageDAO;
import org.exoplatform.portal.jdbc.dao.PermissionDAO;
import org.exoplatform.portal.jdbc.dao.SettingDAO;
import org.exoplatform.portal.jdbc.dao.SiteDAO;
import org.exoplatform.portal.jdbc.dao.WindowDAO;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.ComponentEntity.TYPE;
import org.exoplatform.portal.jdbc.entity.ContainerEntity;
import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.SettingEntity;
import org.exoplatform.portal.jdbc.entity.SiteEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity.AppType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Imported.Status;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.config.tasks.SearchTask;
import org.exoplatform.portal.pom.config.tasks.SearchTask.FindPortletPreferences;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.BodyData;
import org.exoplatform.portal.pom.data.BodyType;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.DashboardData;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.exoplatform.portal.pom.data.ModelChange;
import org.exoplatform.portal.pom.data.ModelData;
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

  private static final String IMPORTED_STATUS = Status.class.getName();

  private SiteDAO             siteDAO;

  private PageDAO             pageDAO;

  private WindowDAO           windowDAO;

  private ContainerDAO        containerDAO;

  private PermissionDAO       permissionDAO;

  private SettingDAO          settingDAO;

  private POMDataStorage      delegate;

  private static Log          log             = ExoLogger.getExoLogger(JDBCModelStorageImpl.class);

  public JDBCModelStorageImpl(SiteDAO siteDAO,
                              PageDAO pageDAO,
                              WindowDAO windowDAO,
                              ContainerDAO containerDAO,
                              PermissionDAO permissionDAO,
                              SettingDAO settingDAO,
                              POMDataStorage delegate) {
    this.siteDAO = siteDAO;
    this.pageDAO = pageDAO;
    this.windowDAO = windowDAO;
    this.containerDAO = containerDAO;
    this.permissionDAO = permissionDAO;
    this.settingDAO = settingDAO;
    this.delegate = delegate;
  }

  @Override
  public void create(PortalData config) throws Exception {
    if (getPortalConfig(config.getKey()) != null) {
      throw new IllegalArgumentException("Cannot create portal " + config.getName() + " that already exist");
    }

    SiteEntity entity = buildSiteEntity(null, config);
    siteDAO.create(entity);
    savePermissions(entity.getId(), config);
  }

  @Override
  public void save(PortalData config) throws Exception {
    SiteKey siteKey = new SiteKey(config.getKey().getType(), config.getKey().getId());
    SiteEntity entity = siteDAO.findByKey(siteKey);
    if (entity == null) {
      throw new IllegalArgumentException("Cannot update portal " + config.getName() + " that does not exist");
    }

    entity = buildSiteEntity(entity, config);
    siteDAO.update(entity);
    savePermissions(entity.getId(), config);
  }

  @Override
  public PortalData getPortalConfig(PortalKey key) throws Exception {
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
  public void remove(PortalData config) throws Exception {
    SiteKey siteKey = new SiteKey(config.getKey().getType(), config.getKey().getId());
    SiteEntity entity = siteDAO.findByKey(siteKey);

    if (entity != null) {
      String siteBody = entity.getSiteBody();
      JSONArray children = parse(siteBody);
      deleteChildren(children);

      permissionDAO.deletePermissions(entity.getId());
      siteDAO.delete(entity);
    } else {
      throw new NoSuchDataException("Could not remove non existing portal " + siteKey);
    }
  }

  @Override
  public PageData getPage(PageKey key) throws Exception {
    SiteKey siteKey = new SiteKey(key.getType(), key.getId());
    org.exoplatform.portal.mop.page.PageKey pageKey = new org.exoplatform.portal.mop.page.PageKey(siteKey, key.getName());
    PageEntity entity = pageDAO.findByKey(pageKey);
    return buildPageData(entity);
  }

  @Override
  public List<ModelChange> save(PageData page) throws Exception {
    PageKey key = page.getKey();
    SiteKey siteKey = new SiteKey(key.getType(), key.getId());
    org.exoplatform.portal.mop.page.PageKey mopKey = new org.exoplatform.portal.mop.page.PageKey(siteKey, key.getName());

    PageEntity dst = pageDAO.findByKey(mopKey);
    if (dst == null) {
      throw new NoSuchDataException("The page " + key + " not found");
    }

    JSONParser parser = new JSONParser();
    JSONArray pageBody = (JSONArray) parser.parse(dst.getPageBody());

    List<ComponentData> children = page.getChildren();
    cleanDeletedComponents(pageBody, children);

    List<ComponentEntity> newBody = saveChildren(children);
    dst.setChildren(newBody);
    dst.setPageBody(((JSONArray) dst.toJSON().get("children")).toJSONString());

    pageDAO.update(dst);
    return Collections.<ModelChange> emptyList();
  }

  @Override
  public <S> String getId(ApplicationState<S> state) throws Exception {
    if (state instanceof TransientApplicationState) {
      TransientApplicationState tstate = (TransientApplicationState) state;
      return tstate.getContentId();
    }

    String id;
    if (state instanceof PersistentApplicationState) {
      PersistentApplicationState pstate = (PersistentApplicationState) state;
      id = pstate.getStorageId();
    } else if (state instanceof CloneApplicationState) {
      CloneApplicationState cstate = (CloneApplicationState) state;
      id = cstate.getStorageId();
    } else {
      throw new AssertionError();
    }

    WindowEntity window = windowDAO.find(id);
    if (window != null) {
      return window.getContentId();
    } else {
      return delegate.getId(state);
    }

  }

  @Override
  public <S> S load(ApplicationState<S> state, ApplicationType<S> type) throws Exception {
    if (state instanceof TransientApplicationState) {
      TransientApplicationState<S> transientState = (TransientApplicationState<S>) state;
      S prefs = transientState.getContentState();
      return prefs != null ? prefs : null;
    }

    String id;
    if (state instanceof CloneApplicationState) {
      id = ((CloneApplicationState<S>) state).getStorageId();
    } else {
      id = ((PersistentApplicationState<S>) state).getStorageId();
    }
    WindowEntity window = windowDAO.find(id);
    if (window != null) {
      byte[] customization = window.getCustomization();
      if (customization != null) {
        return (S) IOTools.unserialize(window.getCustomization());
      } else {
        return null;
      }
    } else {
      return delegate.load(state, type);
    }
  }

  @Override
  public <S> ApplicationState<S> save(ApplicationState<S> state, S preferences) throws Exception {
    if (state instanceof TransientApplicationState) {
      throw new AssertionError("Does not make sense");
    }

    String id;
    if (state instanceof CloneApplicationState) {
      id = ((CloneApplicationState<S>) state).getStorageId();
    } else {
      id = ((PersistentApplicationState<S>) state).getStorageId();
    }
    WindowEntity window = windowDAO.find(id);
    if (window != null) {
      if (preferences != null) {
        window.setCustomization(IOTools.serialize((Serializable) preferences));
      } else {
        window.setCustomization(null);
      }
      windowDAO.update(window);
    } else {
      delegate.save(state, preferences);
    }
    return state;
  }

  @Override
  public <T> LazyPageList<T> find(Query<T> q) throws Exception {
    return find(q, null);
  }

  @Override
  public <T> LazyPageList<T> find(Query<T> q, Comparator<T> sortComparator) throws Exception {
    Class<T> type = q.getClassType();
    if (PageData.class.equals(type)) {
      throw new UnsupportedOperationException("Use PageService.findPages to instead of");
    } else if (PortletPreferences.class.equals(type)) {
      // this task actually return empty portlet preferences
      FindPortletPreferences task = new SearchTask.FindPortletPreferences((Query<PortletPreferences>) q);
      return (LazyPageList<T>) task.run(null);
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
      SiteType siteType = SiteType.valueOf(ownerType.toUpperCase());
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
  public Container getSharedLayout() throws Exception {
    return delegate.getSharedLayout();
  }

  @Override
  public void saveDashboard(DashboardData dashboard) throws Exception {
    String id = dashboard.getStorageId();
    ContainerEntity dst = containerDAO.find(id);
    if (dst == null) {
      throw new IllegalStateException("Not found dashboard to update: " + id);
    }

    JSONParser parser = new JSONParser();
    JSONArray containerBody = (JSONArray) parser.parse(dst.getContainerBody());
    cleanDeletedComponents(containerBody, dashboard.getChildren());

    dst.setChildren(saveChildren(dashboard.getChildren()));
    buildContainerEntity(dst, dashboard);
    dst.setContainerBody(((JSONArray) dst.toJSON().get("children")).toJSONString());
    containerDAO.update(dst);
    savePermissions(dst.getId(), dashboard);
  }

  @Override
  public DashboardData loadDashboard(String dashboardId) throws Exception {
    ContainerEntity dashboard = containerDAO.find(dashboardId);

    List<PermissionEntity> access = permissionDAO.getPermissions(dashboardId,
                                                                 org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.ACCESS);

    List<PermissionEntity> moveApps =
                                    permissionDAO.getPermissions(dashboardId,
                                                                 org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.MOVE_APP);
    List<PermissionEntity> moveContainers =
                                          permissionDAO.getPermissions(dashboardId,
                                                                       org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.MOVE_CONTAINER);

    //
    JSONParser parser = new JSONParser();
    JSONArray body = (JSONArray) parser.parse(dashboard.getContainerBody());
    List<ComponentData> children = buildChildren(body);

    return new DashboardData(dashboard.getId(),
                             dashboard.getId(),
                             dashboard.getName(),
                             dashboard.getId(),
                             dashboard.getTemplate(),
                             dashboard.getFactoryId(),
                             dashboard.getTitle(),
                             dashboard.getDescription(),
                             dashboard.getWidth(),
                             dashboard.getHeight(),
                             buildPermission(access),
                             buildPermission(moveApps),
                             buildPermission(moveContainers),
                             children);
  }

  @Override
  public void save() throws Exception {
    delegate.save();
  }

  @Override
  public String[] getSiteInfo(String workspaceObjectId) throws Exception {
    return delegate.getSiteInfo(workspaceObjectId);
  }

  @Override
  public <S> ApplicationData<S> getApplicationData(String applicationStorageId) {
    WindowEntity window = windowDAO.find(applicationStorageId);
    if (window != null) {
      return buildWindow(window);
    } else {
      return null;
    }
  }

  @Override
  public <A> A adapt(ModelData modelData, Class<A> type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <A> A adapt(ModelData modelData, Class<A> type, boolean create) {
    throw new UnsupportedOperationException();
  }

//  @Override
  public Status getImportStatus() {
    SettingEntity setting = settingDAO.findByName(IMPORTED_STATUS);
    if (setting != null) {
      String value = setting.getValue();
      try {
        return Status.getStatus(Integer.parseInt(value));
      } catch (Exception ex) {
        log.error("Can't parse setting value of import status", ex);
      }
    }
    return null;
  }

//  @Override
  public void saveImportStatus(Status status) {
    SettingEntity setting = settingDAO.findByName(IMPORTED_STATUS);
    if (setting == null) {
      setting = new SettingEntity();
      setting.setName(IMPORTED_STATUS);
    }
    setting.setModifiedDate(System.currentTimeMillis());
    setting.setValue(String.valueOf(status.status()));

    if (setting.getId() == null) {
      settingDAO.create(setting);
    } else {
      settingDAO.update(setting);
    }
  }

  private PageData buildPageData(PageEntity entity) throws Exception {
    if (entity == null) {
      return null;
    }
    //
    JSONParser parser = new JSONParser();
    JSONArray pageBody = (JSONArray) parser.parse(entity.getPageBody());

    List<PermissionEntity> moveApps =
                                    permissionDAO.getPermissions(entity.getId(),
                                                                 org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.MOVE_APP);
    List<PermissionEntity> moveContainers =
                                          permissionDAO.getPermissions(entity.getId(),
                                                                       org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.MOVE_CONTAINER);

    PageData pageData =
                      new PageData(entity.getId(),
                                   entity.getId(),
                                   entity.getName(),
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
    return pageData;
  }

  private Map<String, WindowEntity> queryWindow(JSONArray jsonBody) {
    Set<String> ids = new HashSet<String>();
    filterId(jsonBody, TYPE.WINDOW, ids);
    List<WindowEntity> entities = windowDAO.findByIds(new LinkedList<String>(ids));

    Map<String, WindowEntity> results = new HashMap<String, WindowEntity>();
    for (WindowEntity entity : entities) {
      results.put(entity.getId(), entity);
    }

    ids.removeAll(results.keySet());
    if (ids.size() > 0) {
      log.error("Can't find Window with ids: {}", StringUtils.join(ids, ","));
    }

    return results;
  }

  private Set<String> filterId(JSONArray jsonBody, TYPE type, Set<String> ids) {
    if (jsonBody != null) {
      for (Object obj : jsonBody) {
        JSONObject component = (JSONObject) obj;
        TYPE t = TYPE.valueOf(component.get("type").toString());

        if (t.equals(type)) {
          ids.add(component.get("id").toString());
        }
        if (TYPE.CONTAINER.equals(t)) {
          filterId((JSONArray) component.get("children"), type, ids);
        }
      }
    }
    return ids;
  }

  private Map<String, ContainerEntity> queryContainer(JSONArray jsonBody) {
    Set<String> ids = new HashSet<String>();
    filterId(jsonBody, TYPE.CONTAINER, ids);
    List<ContainerEntity> entities = containerDAO.findByIds(new LinkedList<String>(ids));

    Map<String, ContainerEntity> results = new HashMap<String, ContainerEntity>();
    for (ContainerEntity entity : entities) {
      results.put(entity.getId(), entity);
    }

    ids.removeAll(results.keySet());
    if (ids.size() > 0) {
      log.error("Can't find Container with ids: {}", StringUtils.join(ids, ","));
    }

    return results;
  }

  private List<ComponentEntity> saveChildren(List<ComponentData> children) throws Exception {
    List<ComponentEntity> results = new LinkedList<ComponentEntity>();

    for (ComponentData srcChild : children) {
      String srcChildId = srcChild.getStorageId();

      // Replace dashboard application by container if needed
      // this should be removed once we make the dashboard as first class
      // citizen of the portal
      List<ComponentEntity> dashboardChilds = null;
      if (srcChild instanceof ApplicationData) {
        ApplicationData<?> app = (ApplicationData<?>) srcChild;
        if (app.getType() == ApplicationType.PORTLET && app.getState() instanceof TransientApplicationState) {
          TransientApplicationState<?> state = (TransientApplicationState<?>) app.getState();
          String contentId = state.getContentId();
          if ("dashboard/DashboardPortlet".equals(contentId)) {
            JDBCDashboardData data;
            if (app.getStorageId() != null) {
              ContainerEntity dstDashboard = containerDAO.find(app.getStorageId());
              data = buildDashBoard(dstDashboard);
            } else {
              data = JDBCDashboardData.INITIAL_DASHBOARD;
              dashboardChilds = saveChildren(data.getChildren());
            }

            //
            String icon = app.getIcon();
            String title = app.getTitle();
            String description = app.getDescription();
            String width = app.getWidth();
            String height = app.getHeight();
            // Update those attributes as we have to do it now, they don't
            // exist in a container
            // but do exist in a dashboard container
            JSONObject properties = new JSONObject();
            properties.put(MappedAttributes.SHOW_INFO_BAR.getName(), app.isShowInfoBar());
            properties.put(MappedAttributes.SHOW_MODE.getName(), app.isShowApplicationMode());
            properties.put(MappedAttributes.SHOW_WINDOW_STATE.getName(), app.isShowApplicationState());
            properties.put(MappedAttributes.THEME.getName(), app.getTheme());

            data = new JDBCDashboardData(data.getStorageId(),
                                         data.getId(),
                                         data.getName(),
                                         icon,
                                         data.getTemplate(),
                                         data.getFactoryId(),
                                         title,
                                         description,
                                         width,
                                         height,
                                         app.getAccessPermissions(),
                                         data.getMoveAppsPermissions(),
                                         data.getMoveContainersPermissions(),
                                         properties,
                                         data.getChildren());

            //
            srcChild = data;
          }
        }
      }

      ComponentEntity dstChild = null;
      if (srcChildId != null) { // update
        if (srcChild instanceof ContainerData) {
          dstChild = containerDAO.find(srcChildId);
          buildContainerEntity((ContainerEntity) dstChild, (ContainerData) srcChild);
          containerDAO.update((ContainerEntity) dstChild);
        } else if (srcChild instanceof ApplicationData) {
          dstChild = windowDAO.find(srcChildId);
          buildWindowEntity((WindowEntity) dstChild, (ApplicationData) srcChild);
          windowDAO.update((WindowEntity) dstChild);
        } else if (srcChild instanceof BodyData) {
          // nothing to update on body data
          dstChild = containerDAO.find(srcChildId);
        } else {
          log.warn("this layout component type is not supported: {}", srcChild);
        }
      } else { // create new
        if (srcChild instanceof ContainerData) {
          dstChild = buildContainerEntity(null, (ContainerData) srcChild);
          containerDAO.create((ContainerEntity) dstChild);
        } else if (srcChild instanceof ApplicationData) {
          dstChild = buildWindowEntity(null, (ApplicationData) srcChild);
          windowDAO.create((WindowEntity) dstChild);
        } else if (srcChild instanceof BodyData) {
          dstChild = buildContainerEntity((BodyData) srcChild);
          containerDAO.create((ContainerEntity) dstChild);
        } else {
          log.warn("this layout component type is not supported: {}", srcChild);
          // throw new StaleModelException("Was not expecting child " +
          // srcChild);
        }
      }

      savePermissions(dstChild.getId(), srcChild);

      if (srcChild instanceof ContainerData) {
        if (srcChild instanceof JDBCDashboardData) {
          ContainerEntity dashboard = (ContainerEntity) dstChild;
          // create dashboard children first time
          if (dashboardChilds != null) {
            dashboard.setChildren(dashboardChilds);
            dashboard.setContainerBody(((JSONArray) dashboard.toJSON().get("children")).toJSONString());
            dashboard.setChildren(Collections.<ComponentEntity> emptyList());
            containerDAO.update(dashboard);
          }
        } else {
          List<ComponentEntity> descendants = saveChildren(((ContainerData) srcChild).getChildren());
          ((ContainerEntity) dstChild).setChildren(descendants);
        }
      }

      results.add(dstChild);
    }
    return results;
  }

  private ComponentEntity buildContainerEntity(BodyData bodyData) {
    ContainerEntity dst = new ContainerEntity();
    JSONObject properties = new JSONObject();
    properties.put(MappedAttributes.TYPE.getName(), bodyData.getType().name());
    dst.setProperties(properties.toJSONString());
    return dst;
  }

  private JDBCDashboardData buildDashBoard(ContainerEntity dstDashboard) {
    ContainerData container = buildContainer(dstDashboard,
                                             new JSONObject(),
                                             Collections.<String, ContainerEntity> emptyMap(),
                                             Collections.<String, WindowEntity> emptyMap());
    JSONObject properties = new JSONObject();
    if (dstDashboard.getProperties() != null) {
      JSONParser parser = new JSONParser();
      try {
        properties = (JSONObject) parser.parse(dstDashboard.getProperties());
      } catch (ParseException e) {
        log.error(e);
      }
    }
    return new JDBCDashboardData(container.getId(),
                                 container.getId(),
                                 container.getName(),
                                 container.getIcon(),
                                 container.getTemplate(),
                                 container.getFactoryId(),
                                 container.getTitle(),
                                 container.getDescription(),
                                 container.getWidth(),
                                 container.getHeight(),
                                 container.getAccessPermissions(),
                                 container.getMoveAppsPermissions(),
                                 container.getMoveContainersPermissions(),
                                 properties,
                                 container.getChildren());
  }

  private List<ComponentData> buildChildren(JSONArray jsonBody) {
    Map<String, ContainerEntity> containers = queryContainer(jsonBody);
    Map<String, WindowEntity> windows = queryWindow(jsonBody);

    return buildChildren(jsonBody, containers, windows);
  }

  private List<ComponentData> buildChildren(JSONArray jsonBody,
                                            Map<String, ContainerEntity> containers,
                                            Map<String, WindowEntity> windows) {
    List<ComponentData> results = new LinkedList<ComponentData>();

    if (jsonBody != null) {
      for (Object component : jsonBody) {
        JSONObject jsonComponent = (JSONObject) component;
        String id = jsonComponent.get("id").toString();
        TYPE type = TYPE.valueOf(jsonComponent.get("type").toString());

        switch (type) {
        case CONTAINER:
          ContainerEntity srcContainer = containerDAO.find(id);
          JSONParser parser = new JSONParser();
          JSONObject attrs;
          try {
            attrs = (JSONObject) parser.parse(srcContainer.getProperties());
          } catch (ParseException e) {
            throw new IllegalStateException(e);
          }
          String ctype = (String) attrs.get(MappedAttributes.TYPE.getName());
          if ("dashboard".equals(ctype)) {
            TransientApplicationState<Portlet> state = new TransientApplicationState<Portlet>("dashboard/DashboardPortlet",
                                                                                              null,
                                                                                              null,
                                                                                              null);

            //
            boolean showInfoBar = Boolean.parseBoolean(String.valueOf(attrs.get(MappedAttributes.SHOW_INFO_BAR.getName())));
            boolean showMode = Boolean.parseBoolean(String.valueOf(attrs.get(MappedAttributes.SHOW_MODE.getName())));
            boolean showWindowState =
                                    Boolean.parseBoolean(String.valueOf(attrs.get(MappedAttributes.SHOW_WINDOW_STATE.getName())));
            String theme = (String) attrs.get(MappedAttributes.THEME.getName());

            //
            List<String> accessPermissions =
                                           buildPermission(permissionDAO.getPermissions(srcContainer.getId(),
                                                                                        org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.ACCESS));
            if (accessPermissions.isEmpty()) {
              accessPermissions = Collections.singletonList(UserACL.EVERYONE);
            }

            //
            results.add(new ApplicationData<Portlet>(srcContainer.getId(),
                                                     srcContainer.getId(),
                                                     ApplicationType.PORTLET,
                                                     state,
                                                     srcContainer.getId(),
                                                     srcContainer.getTitle(),
                                                     srcContainer.getIcon(),
                                                     srcContainer.getDescription(),
                                                     showInfoBar,
                                                     showWindowState,
                                                     showMode,
                                                     theme,
                                                     srcContainer.getWidth(),
                                                     srcContainer.getHeight(),
                                                     Collections.<String, String> emptyMap(),
                                                     accessPermissions));
          } else if (BodyType.PAGE.name().equals(ctype)) {
            BodyData body = new BodyData(id, BodyType.PAGE);
            results.add(body);
          } else {
            results.add(buildContainer(containers.get(id), jsonComponent, containers, windows));
          }
          break;
        case WINDOW:
          results.add(buildWindow(windows.get(id)));
        }
      }
    }

    return results;
  }

  @SuppressWarnings("unchecked")
  private ApplicationData buildWindow(WindowEntity windowEntity) {
    ApplicationType<?> appType = convert(windowEntity.getAppType());
    PersistentApplicationState<?> state = new PersistentApplicationState(windowEntity.getId());

    Map<String, String> properties = new HashMap<String, String>();
    try {
      JSONParser parser = new JSONParser();
      JSONObject jProp = (JSONObject) parser.parse(windowEntity.getProperties());
      for (Object key : jProp.keySet()) {
        properties.put(key.toString(), jProp.get(key).toString());
      }
    } catch (Exception ex) {
      log.error(ex);
    }

    List<PermissionEntity> access = permissionDAO.getPermissions(windowEntity.getId(),
                                                                 org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.ACCESS);

    return new ApplicationData(windowEntity.getId(),
                               null,
                               appType,
                               state,
                               windowEntity.getId(),
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

  private ApplicationType convert(AppType appType) {
    switch (appType) {
    case PORTLET:
      return ApplicationType.PORTLET;
    case GADGET:
      return ApplicationType.GADGET;
    case WSRP:
      return ApplicationType.WSRP_PORTLET;
    }
    return null;
  }

  private ContainerData buildContainer(ContainerEntity entity,
                                       JSONObject jsonComponent,
                                       Map<String, ContainerEntity> containers,
                                       Map<String, WindowEntity> windows) {
    List<ComponentData> children = buildChildren((JSONArray) jsonComponent.get("children"), containers, windows);

    List<PermissionEntity> access = permissionDAO.getPermissions(entity.getId(),
                                                                 org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.ACCESS);
    List<PermissionEntity> moveApps =
                                    permissionDAO.getPermissions(entity.getId(),
                                                                 org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.MOVE_APP);
    List<PermissionEntity> moveConts =
                                     permissionDAO.getPermissions(entity.getId(),
                                                                  org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.MOVE_CONTAINER);

    return new ContainerData(entity.getId(),
                             entity.getWebuiId(),
                             entity.getName(),
                             entity.getIcon(),
                             entity.getTemplate(),
                             entity.getFactoryId(),
                             entity.getTitle(),
                             entity.getDescription(),
                             entity.getWidth(),
                             entity.getHeight(),
                             buildPermission(access),
                             buildPermission(moveApps),
                             buildPermission(moveConts),
                             children);
  }

  private void cleanDeletedComponents(JSONArray body, List<ComponentData> children) {
    Set<String> windowIds = new HashSet<String>();
    filterId(body, TYPE.WINDOW, windowIds);
    for (String id : windowIds) {
      if (findById(id, children) == null) {
        windowDAO.deleteById(id);
      }
    }

    Set<String> containerIds = new HashSet<String>();
    filterId(body, TYPE.CONTAINER, containerIds);
    for (String id : containerIds) {
      if (findById(id, children) == null) {
        containerDAO.deleteById(id);
      }
    }
  }

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
    if (src instanceof DashboardData) {
      JSONObject properties = new JSONObject();
      if (src instanceof JDBCDashboardData) {
        properties = ((JDBCDashboardData) src).getProperties();
      }
      properties.put(MappedAttributes.TYPE.getName(), "dashboard");
      dst.setProperties(properties.toJSONString());
    }
    dst.setTemplate(src.getTemplate());
    dst.setTitle(src.getTitle());
    dst.setWidth(src.getWidth());
    return dst;
  }

  private WindowEntity buildWindowEntity(WindowEntity dst, ApplicationData srcChild) throws Exception {
    if (dst == null) {
      dst = new WindowEntity();

      ApplicationType type = srcChild.getType();
      if (ApplicationType.PORTLET.getName().equals(type.getName())) {
        dst.setAppType(AppType.PORTLET);
      } else if (ApplicationType.GADGET.getName().equals(type.getName())) {
        dst.setAppType(AppType.GADGET);
      } else if (ApplicationType.WSRP_PORTLET.getName().equals(type.getName())) {
        dst.setAppType(AppType.WSRP);
      }

      ApplicationState state = srcChild.getState();
      if (state instanceof TransientApplicationState) {
        TransientApplicationState s = (TransientApplicationState) state;
        dst.setContentId(s.getContentId());
        if (s.getContentState() != null) {
          dst.setCustomization(IOTools.serialize((Serializable) s.getContentState()));
        }
      } else {
        throw new IllegalStateException("Can't create new window");
      }
    }
    dst.setDescription(srcChild.getDescription());
    dst.setHeight(srcChild.getHeight());
    dst.setIcon(srcChild.getIcon());
    dst.setProperties(new JSONObject(srcChild.getProperties()).toJSONString());
    dst.setShowApplicationMode(srcChild.isShowApplicationMode());
    dst.setShowApplicationState(srcChild.isShowApplicationState());
    dst.setShowInfoBar(srcChild.isShowInfoBar());
    dst.setTheme(srcChild.getTheme());
    dst.setTitle(srcChild.getTitle());
    dst.setWidth(srcChild.getWidth());

    return dst;
  }

  private ComponentData findById(String id, List<ComponentData> children) {
    if (children != null) {
      for (ComponentData child : children) {
        if (id.equals(child.getStorageId())) {
          return child;
        } else if (child instanceof ContainerData) {
          ComponentData result = findById(id, ((ContainerData) child).getChildren());
          if (result != null) {
            return result;
          }
        }
      }
    }
    return null;
  }

  private List<String> buildPermission(List<PermissionEntity> permissions) {
    List<String> results = new ArrayList<String>();

    if (permissions != null) {
      for (PermissionEntity per : permissions) {
        results.add(per.getPermission());
      }
    }

    return results;
  }

  private void savePermissions(String id, PortalData config) {
    permissionDAO.savePermissions(id,
                                  org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.ACCESS,
                                  config.getAccessPermissions());
    permissionDAO.savePermissions(id,
                                  org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.EDIT,
                                  Arrays.asList(config.getEditPermission()));
  }

  private void savePermissions(String id, ComponentData srcChild) {
    List<String> access = null;
    if (srcChild instanceof ContainerData) {
      ContainerData srcData = (ContainerData) srcChild;
      access = srcData.getAccessPermissions();
      permissionDAO.savePermissions(id,
                                    org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.MOVE_APP,
                                    srcData.getMoveAppsPermissions());
      permissionDAO.savePermissions(id,
                                    org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.MOVE_CONTAINER,
                                    srcData.getMoveContainersPermissions());
    } else if (srcChild instanceof ApplicationData) {
      ApplicationData srcData = (ApplicationData) srcChild;
      access = srcData.getAccessPermissions();
    }

    permissionDAO.savePermissions(id, org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.ACCESS, access);
  }

  private SiteEntity buildSiteEntity(SiteEntity entity, PortalData config) throws Exception {
    if (entity == null) {
      entity = new SiteEntity();
    }
    entity.setDescription(config.getDescription());
    entity.setLabel(config.getLabel());
    entity.setLocale(config.getLocale());
    entity.setName(config.getName());
    entity.setSiteType(SiteType.valueOf(config.getKey().getType().toUpperCase()));
    entity.setSkin(config.getSkin());

    List<ComponentData> children = new ArrayList<ComponentData>();
    children.add(config.getPortalLayout());

    JSONParser parser = new JSONParser();
    JSONArray pageBody = (JSONArray) parser.parse(entity.getSiteBody());
    cleanDeletedComponents(pageBody, children);

    List<ComponentEntity> newBody = saveChildren(children);

    entity.setChildren(newBody);
    entity.setSiteBody(((JSONArray) entity.toJSON().get("children")).toJSONString());

    return entity;
  }

  private PortalData buildPortalData(SiteEntity entity) throws Exception {
    if (entity == null) {
      return null;
    }
    //
    JSONParser parser = new JSONParser();
    JSONArray siteBody = (JSONArray) parser.parse(entity.getSiteBody());
    List<ComponentData> children = buildChildren(siteBody);
    ContainerData rootContainer = null;
    if (children.size() > 0) {
      rootContainer = (ContainerData) children.get(0);
    } else {
      throw new IllegalStateException("site doens't has root container layout");
    }

    List<PermissionEntity> access = permissionDAO.getPermissions(entity.getId(),
                                                                 org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.ACCESS);
    List<PermissionEntity> edit = permissionDAO.getPermissions(entity.getId(),
                                                               org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE.EDIT);
    String editPer = edit.size() > 0 ? edit.get(0).getPermission() : null;

    List<RedirectData> redirects = Collections.emptyList();

    Map<String, String> properties = new HashMap<String, String>();
    try {
      if (entity.getProperties() != null) {
        JSONObject jProp = (JSONObject) parser.parse(entity.getProperties());
        for (Object key : jProp.keySet()) {
          properties.put(key.toString(), jProp.get(key).toString());
        }
      }
    } catch (Exception ex) {
      log.error(ex);
    }

    return new PortalData(entity.getId(),
                          entity.getName(),
                          entity.getSiteType().getName(),
                          entity.getLocale(),
                          entity.getLabel(),
                          entity.getDescription(),
                          buildPermission(access),
                          editPer,
                          Collections.unmodifiableMap(properties),
                          entity.getSkin(),
                          rootContainer,
                          redirects);
  }

  private JSONArray parse(String body) {
    JSONParser parser = new JSONParser();
    JSONArray children;
    try {
      children = (JSONArray) parser.parse(body);
      return children;
    } catch (ParseException e) {
      log.error(e);
      throw new IllegalStateException("Can't parse body: " + body);
    }
  }

  private void deleteChildren(JSONArray children) {
    for (Object child : children) {
      JSONObject c = (JSONObject) child;
      String id = c.get("id").toString();
      TYPE t = TYPE.valueOf(c.get("type").toString());

      if (TYPE.CONTAINER.equals(t)) {
        JSONArray descendants = (JSONArray) c.get("children");
        if (descendants != null) {
          deleteChildren(descendants);
        }

        ContainerEntity container = containerDAO.find(id);
        if (container != null) {
          JSONArray dashboardChilds = parse(container.getContainerBody());
          deleteChildren(dashboardChilds);

          permissionDAO.deletePermissions(container.getId());
          containerDAO.delete(container);
        }
      } else if (TYPE.WINDOW.equals(t)) {
        WindowEntity window = windowDAO.find(id);
        if (window != null) {
          permissionDAO.deletePermissions(window.getId());
          windowDAO.delete(window);
        }
      } else {
        throw new IllegalArgumentException("Can't delete child with type: " + t);
      }
    }
  }

}
