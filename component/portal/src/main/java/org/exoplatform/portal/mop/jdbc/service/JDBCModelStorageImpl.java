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

import java.io.*;
import java.util.*;

import org.apache.commons.codec.digest.DigestUtils;
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
import org.exoplatform.commons.utils.*;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.config.*;
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.jdbc.entity.*;
import org.exoplatform.portal.jdbc.entity.ComponentEntity.TYPE;
import org.exoplatform.portal.jdbc.entity.WindowEntity.AppType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.importer.Status;
import org.exoplatform.portal.mop.jdbc.dao.*;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class JDBCModelStorageImpl implements ModelDataStorage {

  private static final String  IMPORTED_STATUS = Status.class.getName();

  private static final String  CHECKSUM_KEY    = "checksum";

  private SiteDAO              siteDAO;

  private PageDAO              pageDAO;

  private WindowDAO            windowDAO;

  private ContainerDAO         containerDAO;

  private PermissionDAO        permissionDAO;

  private SettingService       settingService;

  /** . */
  private ConfigurationManager confManager;

  private static Log           log             = ExoLogger.getExoLogger(JDBCModelStorageImpl.class);

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

      permissionDAO.deletePermissions(SiteEntity.class.getName(), entity.getId());
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

    Long id;
    if (state instanceof PersistentApplicationState) {
      PersistentApplicationState pstate = (PersistentApplicationState) state;
      id = Safe.parseLong(pstate.getStorageId());
    } else if (state instanceof CloneApplicationState) {
      CloneApplicationState cstate = (CloneApplicationState) state;
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
  public <S> S load(ApplicationState<S> state, ApplicationType<S> type) throws Exception {
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
        return (S) IOTools.unserialize(window.getCustomization());
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
  public <S> ApplicationState<S> save(ApplicationState<S> state, S preferences) throws Exception {
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
        window.setCustomization(IOTools.serialize((Serializable) preferences));
      } else {
        window.setCustomization(null);
      }
      windowDAO.update(window);
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
  public Container getSharedLayout(String siteName) throws Exception {
    String path = null;
    if (StringUtils.isBlank(siteName)) {
      path = DEFAULT_SHAREDLAYOUT_PATH;
    } else {
      path = "war:/conf/portal/portal/sharedlayout-" + siteName + ".xml";
    }
    InputStream inputStream = null;
    try {
      inputStream = confManager.getInputStream(path);
    } catch (IOException e) {
      log.debug("Unable to find file '" + path + "'", e);
      return getSharedLayout(null);
    }
    String out = IOUtil.getStreamContentAsString(inputStream);
    ByteArrayInputStream is = new ByteArrayInputStream(out.getBytes("UTF-8"));
    IBindingFactory bfact = BindingDirectory.getFactory(Container.class);
    UnmarshallingContext uctx = (UnmarshallingContext) bfact.createUnmarshallingContext();
    uctx.setDocument(is, null, "UTF-8", false);
    Container container = (Container) uctx.unmarshalElement();
    generateStorageName(container);
    return container;
  }

  @Override
  public void save() throws Exception {
  }

  @Override
  public String[] getSiteInfo(String workspaceObjectId) throws Exception {
    return null;
  }

  @Override
  public <S> ApplicationData<S> getApplicationData(String applicationStorageId) {
    WindowEntity window = windowDAO.find(Safe.parseLong(applicationStorageId));
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

  // @Override
  public Status getImportStatus() {
    SettingValue<String> setting = (SettingValue<String>) settingService.get(Context.GLOBAL,
                                                                             Scope.GLOBAL.id(null),
                                                                             IMPORTED_STATUS);
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

  // @Override
  public void saveImportStatus(Status status) {
    settingService.set(Context.GLOBAL,
                       Scope.GLOBAL.id(null),
                       IMPORTED_STATUS,
                       SettingValue.create(String.valueOf(status.status())));
  }

  @Override
  public boolean isChecksumChanged(String path, String xml) {
    SettingValue<?> checksumValue = settingService.get(Context.GLOBAL,
                                                       Scope.APPLICATION.id("MOP"),
                                                       CHECKSUM_KEY + path);
    String checksum = checksumValue == null || checksumValue.getValue() == null ? null : checksumValue.getValue().toString();
    if (StringUtils.isNotBlank(checksum)) {
      String currentChecksum = DigestUtils.md5Hex(xml);
      return StringUtils.equalsIgnoreCase(checksum, currentChecksum);
    }
    return false;
  }

  @Override
  public void saveChecksum(String path, String xml) {
    String checksum = DigestUtils.md5Hex(xml);
    settingService.set(Context.GLOBAL,
                       Scope.APPLICATION.id("MOP"),
                       CHECKSUM_KEY + path,
                       SettingValue.create(checksum));
  }

  private PageData buildPageData(PageEntity entity) throws Exception {
    if (entity == null) {
      return null;
    }
    //
    JSONParser parser = new JSONParser();
    JSONArray pageBody = (JSONArray) parser.parse(entity.getPageBody());

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
    if (ids.size() > 0) {
      log.error("Can't find Window with ids: {}", StringUtils.join(ids, ","));
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
    if (ids.size() > 0) {
      log.error("Can't find Container with ids: {}", StringUtils.join(ids, ","));
    }

    return results;
  }

  private List<ComponentEntity> saveChildren(List<ComponentData> children) throws Exception {
    List<ComponentEntity> results = new LinkedList<ComponentEntity>();

    for (ComponentData srcChild : children) {
      Long srcChildId = Safe.parseLong(srcChild.getStorageId());

      ComponentEntity dstChild = null;
      if (srcChildId != null && srcChildId > 0) { // update
        if (srcChild instanceof ContainerData) {
          dstChild = containerDAO.find(srcChildId);
          if (dstChild != null) {
            buildContainerEntity((ContainerEntity) dstChild, (ContainerData) srcChild);
            containerDAO.update((ContainerEntity) dstChild);
          }
        } else if (srcChild instanceof ApplicationData) {
          dstChild = windowDAO.find(srcChildId);
          if (dstChild != null) {
            dstChild = buildWindowEntity((WindowEntity) dstChild, (ApplicationData) srcChild);
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
        if (srcChild instanceof ContainerData) {
          dstChild = buildContainerEntity(null, (ContainerData) srcChild);
          dstChild = containerDAO.create((ContainerEntity) dstChild);
        } else if (srcChild instanceof ApplicationData) {
          dstChild = buildWindowEntity(null, (ApplicationData) srcChild);
          if (dstChild == null) {
            continue;
          }
          dstChild = windowDAO.create((WindowEntity) dstChild);
        } else if (srcChild instanceof BodyData) {
          dstChild = buildContainerEntity((BodyData) srcChild);
          dstChild = containerDAO.create((ContainerEntity) dstChild);
        } else {
          throw new StaleModelException("Was not expecting child " + srcChild);
        }
        if (dstChild.getId() == null) {
          throw new IllegalStateException("Id of saved child wasn't found: " + dstChild.getType() + " / " + dstChild);
        }
      }

      savePermissions(dstChild.getId(), srcChild);

      if (srcChild instanceof ContainerData) {
        List<ComponentEntity> descendants = saveChildren(((ContainerData) srcChild).getChildren());
        ((ContainerEntity) dstChild).setChildren(descendants);
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

  private List<ComponentData> buildChildren(JSONArray jsonBody) {
    Map<Long, ContainerEntity> containers = queryContainer(jsonBody);
    Map<Long, WindowEntity> windows = queryWindow(jsonBody);

    return buildChildren(jsonBody, containers, windows);
  }

  private List<ComponentData> buildChildren(JSONArray jsonBody,
                                            Map<Long, ContainerEntity> containers,
                                            Map<Long, WindowEntity> windows) {
    List<ComponentData> results = new LinkedList<ComponentData>();

    if (jsonBody != null) {
      for (Object component : jsonBody) {
        JSONObject jsonComponent = (JSONObject) component;
        Long id = Safe.parseLong(jsonComponent.get("id").toString());
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
                                           buildPermission(permissionDAO.getPermissions(ContainerEntity.class.getName(),
                                                                                        srcContainer.getId(),
                                                                                        PermissionEntity.TYPE.ACCESS));
            if (accessPermissions.isEmpty()) {
              accessPermissions = Collections.singletonList(UserACL.EVERYONE);
            }

            //
            results.add(new ApplicationData<Portlet>(String.valueOf(srcContainer.getId()),
                                                     String.valueOf(srcContainer.getId()),
                                                     ApplicationType.PORTLET,
                                                     state,
                                                     String.valueOf(srcContainer.getId()),
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
            BodyData body = new BodyData(String.valueOf(id), BodyType.PAGE);
            results.add(body);
          } else {
            results.add(buildContainer(containers.get(id), jsonComponent, attrs, containers, windows));
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
    PersistentApplicationState<?> state = new PersistentApplicationState(String.valueOf(windowEntity.getId()));

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

  private ApplicationType convert(AppType appType) {
    switch (appType) {
    case PORTLET:
      return ApplicationType.PORTLET;
    }
    return null;
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

  private WindowEntity buildWindowEntity(WindowEntity dst, ApplicationData srcChild) throws Exception {
    if (dst == null) {
      dst = new WindowEntity();

      ApplicationType type = srcChild.getType();
      if (type == null) {
        log.warn("Application type of instance {} is not recognized, ignore it", dst.getContentId());
        return null;
      }
      if (ApplicationType.PORTLET.getName().equals(type.getName())) {
        dst.setAppType(AppType.PORTLET);
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
    dst.setProperties(this.toJSON(srcChild.getProperties()).toJSONString());
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
        } else if (child instanceof ContainerData) {
          ComponentData result = findById(id, type, ((ContainerData) child).getChildren());
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

  private void savePermissions(Long id, ComponentData srcChild) {
    if (id == null) {
      throw new IllegalArgumentException("id is null");
    }
    List<String> access = null;
    String typeName = srcChild.getClass().getName();
    if (srcChild instanceof ContainerData) {
      ContainerData srcData = (ContainerData) srcChild;
      typeName = ContainerEntity.class.getName();
      access = srcData.getAccessPermissions();
      permissionDAO.savePermissions(typeName,
                                    id,
                                    PermissionEntity.TYPE.MOVE_APP,
                                    srcData.getMoveAppsPermissions());
      permissionDAO.savePermissions(typeName,
                                    id,
                                    PermissionEntity.TYPE.MOVE_CONTAINER,
                                    srcData.getMoveContainersPermissions());
    } else if (srcChild instanceof ApplicationData) {
      ApplicationData srcData = (ApplicationData) srcChild;
      typeName = WindowEntity.class.getName();
      access = srcData.getAccessPermissions();
    }

    permissionDAO.savePermissions(typeName, id, PermissionEntity.TYPE.ACCESS, access);
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
    entity.setDefaultSiteBody(config.isDefaultLayout());
    entity.setSkin(config.getSkin());
    String propertiesString = "{}";
    Map<String, String> properties = config.getProperties();
    if (properties != null) {
      propertiesString = new JSONObject(properties).toJSONString();
    }
    entity.setProperties(propertiesString);

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
    if (!children.isEmpty()) {
      rootContainer = (ContainerData) children.get(0);
    } else {
      throw new IllegalStateException("site doens't has root container layout");
    }

    List<PermissionEntity> access = permissionDAO.getPermissions(SiteEntity.class.getName(),
                                                                 entity.getId(),
                                                                 PermissionEntity.TYPE.ACCESS);
    List<PermissionEntity> edit = permissionDAO.getPermissions(SiteEntity.class.getName(),
                                                               entity.getId(),
                                                               PermissionEntity.TYPE.EDIT);
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

    return new PortalData("site_" + entity.getId(),
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
                          entity.isDefaultSiteBody(),
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

  private JSONObject toJSON(Map map) {
    JSONObject json = new JSONObject();
    Set<Map.Entry> entries = map.entrySet();
    for (Map.Entry entry : entries) {
      json.put(entry.getKey(), entry.getValue());
    }
    return json;
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
          JSONArray dashboardChilds = parse(container.getContainerBody());
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
    if (obj instanceof Container) {
      for (ModelObject child : ((Container) obj).getChildren()) {
        generateStorageName(child);
      }
    } else if (obj instanceof Application) {
      obj.setStorageName(UUID.randomUUID().toString());
    }
  }

}
