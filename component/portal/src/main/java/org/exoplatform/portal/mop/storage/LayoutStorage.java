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

import static org.exoplatform.portal.mop.storage.utils.MOPUtils.convertAppType;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.parseJsonArray;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.parseJsonObject;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.serialize;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.toJSONString;
import static org.exoplatform.portal.mop.storage.utils.MOPUtils.unserialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.utils.Safe;
import org.exoplatform.portal.config.StaleModelException;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.CloneApplicationState;
import org.exoplatform.portal.config.model.PersistentApplicationState;
import org.exoplatform.portal.config.model.TransientApplicationState;
import org.exoplatform.portal.jdbc.entity.ComponentEntity;
import org.exoplatform.portal.jdbc.entity.ComponentEntity.TYPE;
import org.exoplatform.portal.jdbc.entity.ContainerEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.jdbc.entity.WindowEntity.AppType;
import org.exoplatform.portal.mop.dao.ContainerDAO;
import org.exoplatform.portal.mop.dao.PermissionDAO;
import org.exoplatform.portal.mop.dao.WindowDAO;
import org.exoplatform.portal.pom.data.ApplicationData;
import org.exoplatform.portal.pom.data.BodyData;
import org.exoplatform.portal.pom.data.BodyType;
import org.exoplatform.portal.pom.data.ComponentData;
import org.exoplatform.portal.pom.data.ContainerData;
import org.exoplatform.portal.pom.data.MappedAttributes;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class LayoutStorage {

  private static final String TYPE_PROP     = MappedAttributes.TYPE.getName();

  private static final String ID_PROP       = MappedAttributes.ID.getName();

  private static final String CHILDREN_PROP = "children";

  private static final Log    LOG           = ExoLogger.getExoLogger(LayoutStorage.class);

  private WindowDAO           windowDAO;

  private ContainerDAO        containerDAO;

  private PermissionDAO       permissionDAO;

  public LayoutStorage(WindowDAO windowDAO,
                       ContainerDAO containerDAO,
                       PermissionDAO permissionDAO) {
    this.windowDAO = windowDAO;
    this.containerDAO = containerDAO;
    this.permissionDAO = permissionDAO;
  }

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
    WindowEntity window = findWindow(id);
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
    WindowEntity window = findWindow(id);
    if (window != null) {
      if (preferences != null) {
        window.setCustomization(serialize((Serializable) preferences));
      } else {
        window.setCustomization(null);
      }
      updateWindow(window);
    }

    return state;
  }

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
      throw new IllegalStateException("Unrecognized Application state class type : " + state);
    }

    WindowEntity window = findWindow(id);
    if (window != null) {
      return window.getContentId();
    } else {
      return null;
    }

  }

  @SuppressWarnings("unchecked")
  public <S> ApplicationData<S> getApplicationData(String applicationStorageId) {
    WindowEntity window = windowDAO.find(Safe.parseLong(applicationStorageId));
    if (window != null) {
      return buildWindow(window);
    } else {
      return null;
    }
  }

  public <S> Application<S> getApplicationModel(String applicationStorageId) {
    ApplicationData<S> applicationData = getApplicationData(applicationStorageId);
    return new Application<>(applicationData);
  }

  public List<ComponentEntity> saveChildren(JSONArray pageBody, List<ComponentData> children) {
    cleanDeletedComponents(pageBody, children);
    return saveChildren(children);
  }

  public List<ComponentData> buildChildren(JSONArray jsonBody) {
    Map<Long, ContainerEntity> containers = getContainerEntities(jsonBody);
    Map<Long, WindowEntity> windows = getWindowEntities(jsonBody);
    return buildChildren(jsonBody, containers, windows);
  }

  public void deleteChildren(JSONArray children) {
    for (Object child : children) {
      JSONObject c = (JSONObject) child;
      Long id = Safe.parseLong(c.get(ID_PROP).toString());
      TYPE t = TYPE.valueOf(c.get(TYPE_PROP).toString());

      if (TYPE.CONTAINER.equals(t)) {
        JSONArray descendants = (JSONArray) c.get(CHILDREN_PROP);
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
        WindowEntity window = findWindow(id);
        if (window != null) {
          permissionDAO.deletePermissions(WindowEntity.class.getName(), window.getId());
          deleteWindow(window);
        }
      } else {
        throw new IllegalArgumentException("Can't delete child with type: " + t);
      }
    }
  }

  public List<String> getPermissions(String objectType,
                                     Long objectId,
                                     org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE permisssionType) {
    return buildPermission(permissionDAO.getPermissions(objectType,
                                                        objectId,
                                                        permisssionType));
  }

  public List<PermissionEntity> savePermissions(String objectType,
                                                long objectId,
                                                PermissionEntity.TYPE type,
                                                List<String> permissions) {
    return permissionDAO.savePermissions(objectType,
                                         objectId,
                                         type,
                                         permissions);
  }

  public List<ComponentEntity> clone(String objectType, String pageBody) {
    List<ComponentEntity> results = new LinkedList<>();

    JSONArray children = parse(pageBody);

    for (Object child : children) {
      JSONObject c = (JSONObject) child;
      Long id = Safe.parseLong(c.get(ID_PROP).toString());
      TYPE type = TYPE.valueOf(c.get(TYPE_PROP).toString());

      switch (type) {
      case CONTAINER:
        ContainerEntity srcC = containerDAO.find(id);
        ContainerEntity dstC = clone(srcC);

        JSONArray descendants = parse(srcC.getContainerBody());
        if (CollectionUtils.isNotEmpty(descendants)) {
          dstC.setChildren(clone(objectType, srcC.getContainerBody()));
        } else {
          dstC.setChildren(clone(objectType, ((JSONArray) c.get(CHILDREN_PROP)).toJSONString()));
        }
        dstC.setContainerBody(((JSONArray) dstC.toJSON().get(CHILDREN_PROP)).toJSONString());

        containerDAO.create(dstC);
        clonePermissions(objectType, dstC.getId(), srcC.getId());
        results.add(dstC);
        break;
      case WINDOW:
        WindowEntity srcW = findWindow(id);
        WindowEntity dstW = clone(srcW);

        dstW = createWindow(dstW);
        clonePermissions(objectType, dstW.getId(), srcW.getId());
        results.add(dstW);
        break;
      default:
        throw new IllegalStateException("Can't handle type: " + type);
      }

    }
    return results;
  }

  public void clonePermissions(String objectType, long dstId, long srcId) {
    clonePermissions(objectType, dstId, srcId, PermissionEntity.TYPE.ACCESS);
    clonePermissions(objectType, dstId, srcId, PermissionEntity.TYPE.EDIT);
    clonePermissions(objectType, dstId, srcId, PermissionEntity.TYPE.MOVE_APP);
    clonePermissions(objectType, dstId, srcId, PermissionEntity.TYPE.MOVE_CONTAINER);
  }

  public void clonePermissions(String objectType, long dstId, long srcId, PermissionEntity.TYPE type) {
    List<String> permissions = getPermissions(objectType, srcId, type);
    if (!permissions.isEmpty()) {
      savePermissions(objectType, dstId, type, permissions);
    }
  }

  public void deletePermissions(String objectType, Long objectId) {
    permissionDAO.deletePermissions(objectType, objectId);
  }

  protected WindowEntity createWindow(WindowEntity dstW) {
    return windowDAO.create(dstW);
  }

  protected WindowEntity updateWindow(WindowEntity window) {
    return windowDAO.update(window);
  }

  protected WindowEntity findWindow(Long id) {
    return windowDAO.find(id);
  }

  protected void deleteWindow(WindowEntity window) {
    windowDAO.delete(window);
  }

  protected void deleteWindowById(Long id) {
    windowDAO.deleteById(id);
  }

  private WindowEntity clone(WindowEntity src) {
    WindowEntity dst = new WindowEntity();
    dst.setAppType(src.getAppType());
    dst.setContentId(src.getContentId());
    dst.setCustomization(src.getCustomization());
    dst.setDescription(src.getDescription());
    dst.setHeight(src.getHeight());
    dst.setIcon(src.getIcon());
    dst.setProperties(src.getProperties());
    dst.setShowApplicationMode(src.isShowApplicationMode());
    dst.setShowApplicationState(src.isShowApplicationState());
    dst.setShowInfoBar(src.isShowInfoBar());
    dst.setTheme(src.getTheme());
    dst.setTitle(src.getTitle());
    dst.setWidth(src.getWidth());

    return dst;
  }

  private JSONArray parse(String body) {
    JSONParser parser = new JSONParser();
    JSONArray children;
    try {
      children = (JSONArray) parser.parse(body);
      return children;
    } catch (ParseException e) {
      throw new IllegalStateException("Can't parse body: " + body);
    }
  }

  private ContainerEntity clone(ContainerEntity src) {
    ContainerEntity dst = new ContainerEntity();

    dst.setDescription(src.getDescription());
    dst.setFactoryId(src.getFactoryId());
    dst.setHeight(src.getHeight());
    dst.setIcon(src.getIcon());
    dst.setName(src.getName());
    dst.setProperties(src.getProperties());
    dst.setTemplate(src.getTemplate());
    dst.setTitle(src.getTitle());
    dst.setWidth(src.getWidth());
    dst.setContainerBody(src.getContainerBody());

    return dst;
  }

  private void cleanDeletedComponents(JSONArray body,
                                      List<ComponentData> children) {
    Set<Long> windowIds = new HashSet<>();
    filterBodyContainerIds(body, TYPE.WINDOW, windowIds);
    for (Long id : windowIds) {
      if (findById(id, TYPE.WINDOW, children) == null) {
        deleteWindowById(id);
      }
    }

    Set<Long> containerIds = new HashSet<>();
    filterBodyContainerIds(body, TYPE.CONTAINER, containerIds);
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

  @SuppressWarnings({
      "unchecked", "rawtypes"
  })
  private ApplicationData buildWindow(WindowEntity windowEntity) {
    ApplicationType<?> appType = convertAppType(windowEntity.getAppType());
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
    List<ComponentData> children = buildChildren((JSONArray) jsonComponent.get(CHILDREN_PROP),
                                                 containers,
                                                 windows);

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

  private List<ComponentData> buildChildren(JSONArray jsonBody, // NOSONAR
                                            Map<Long, ContainerEntity> containers,
                                            Map<Long, WindowEntity> windows) {
    List<ComponentData> results = new LinkedList<>();

    if (jsonBody != null) {
      for (Object component : jsonBody) {
        JSONObject jsonComponent = (JSONObject) component;
        Long id = Safe.parseLong(jsonComponent.get(ID_PROP).toString());
        TYPE type = TYPE.valueOf(jsonComponent.get(TYPE_PROP).toString());

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
            results.add(buildContainer(containers.get(id),
                                       jsonComponent,
                                       attrs,
                                       containers,
                                       windows));
          }
        }
      }
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
          dstChild = findWindow(srcChildId);
          if (dstChild != null) {
            dstChild = buildWindowEntity((WindowEntity) dstChild, appData);
            if (dstChild == null) {
              continue;
            }
            dstChild = updateWindow((WindowEntity) dstChild);
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
          dstChild = createWindow((WindowEntity) dstChild);
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

  private Map<Long, ContainerEntity> getContainerEntities(JSONArray jsonBody) {
    Set<Long> ids = new HashSet<>();
    filterBodyContainerIds(jsonBody, TYPE.CONTAINER, ids);
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

  @SuppressWarnings("unchecked")
  private ComponentEntity buildContainerEntity(BodyData bodyData) {
    ContainerEntity dst = new ContainerEntity();
    JSONObject properties = new JSONObject();
    properties.put(MappedAttributes.TYPE.getName(), bodyData.getType().name());
    dst.setProperties(properties.toJSONString());
    return dst;
  }

  private Map<Long, WindowEntity> getWindowEntities(JSONArray jsonBody) {
    Set<Long> ids = new HashSet<>();
    filterBodyContainerIds(jsonBody, TYPE.WINDOW, ids);
    List<WindowEntity> entities = ids.stream()
                                     .map(this::findWindow)
                                     .filter(Objects::nonNull)
                                     .toList();

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

  private Set<Long> filterBodyContainerIds(JSONArray jsonBody, TYPE type, Set<Long> ids) {
    if (jsonBody != null) {
      for (Object obj : jsonBody) {
        JSONObject component = (JSONObject) obj;
        TYPE componentType = TYPE.valueOf(component.get(TYPE_PROP).toString());
        if (componentType.equals(type)) {
          ids.add(Safe.parseLong(component.get(ID_PROP).toString()));
        }
        if (TYPE.CONTAINER.equals(componentType)) {
          filterBodyContainerIds((JSONArray) component.get(CHILDREN_PROP), type, ids);
        }
      }
    }
    return ids;
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
      savePermissions(typeName,
                      id,
                      PermissionEntity.TYPE.MOVE_APP,
                      srcChildContainer.getMoveAppsPermissions());
      savePermissions(typeName,
                      id,
                      PermissionEntity.TYPE.MOVE_CONTAINER,
                      srcChildContainer.getMoveContainersPermissions());
    } else if (srcChild instanceof ApplicationData srcChildApplication) {
      typeName = WindowEntity.class.getName();
      access = srcChildApplication.getAccessPermissions();
    }

    savePermissions(typeName, id, PermissionEntity.TYPE.ACCESS, access);
  }

}
