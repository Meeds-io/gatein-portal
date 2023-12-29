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
package org.exoplatform.application.registry.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gatein.common.i18n.LocalizedString;
import org.gatein.mop.api.content.ContentType;
import org.gatein.pc.api.PortletInvoker;
import org.gatein.pc.api.PortletInvokerException;
import org.gatein.pc.api.info.MetaInfo;
import org.gatein.pc.api.info.PortletInfo;
import org.picocontainer.Startable;

import org.exoplatform.application.registry.Application;
import org.exoplatform.application.registry.ApplicationCategoriesPlugins;
import org.exoplatform.application.registry.ApplicationCategory;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.application.registry.dao.ApplicationDAO;
import org.exoplatform.application.registry.dao.CategoryDAO;
import org.exoplatform.application.registry.entity.ApplicationEntity;
import org.exoplatform.application.registry.entity.CategoryEntity;
import org.exoplatform.commons.utils.Safe;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE;
import org.exoplatform.portal.mop.storage.LayoutStorage;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.MembershipEntry;

public class JDBCApplicationRegistryService implements ApplicationRegistryService, Startable {

  private static final Log                   log                           =
                                                 ExoLogger.getExoLogger(JDBCApplicationRegistryService.class);

  private LayoutStorage                      layoutStorage;

  private CategoryDAO                        catDAO;

  private ApplicationDAO                     appDAO;

  private String                             anyOfAdminGroup;

  private List<ApplicationCategoriesPlugins> plugins                       = new ArrayList<>();

  private static final String                INTERNAL_PORTLET_TAG          = "gatein_internal";

  private static final String                REMOTE_CATEGORY_NAME          = "remote";

  private static final String                PRODUCER_NAME_META_INFO_KEY   = "producer-name";

  public static final String                 PRODUCER_CATEGORY_NAME_SUFFIX = " Producer";

  public JDBCApplicationRegistryService(LayoutStorage layoutStorage,
                                        CategoryDAO catDAO,
                                        ApplicationDAO appDAO,
                                        UserACL userACL) {
    this.layoutStorage = layoutStorage;
    this.catDAO = catDAO;
    this.appDAO = appDAO;
    this.anyOfAdminGroup = new MembershipEntry(userACL.getAdminGroups()).toString();
  }

  public List<ApplicationCategory> getApplicationCategories(Comparator<ApplicationCategory> sortComparator,
                                                            String accessUser,
                                                            ApplicationType<?>... appTypes) {
    List<ApplicationCategory> categories = new ArrayList<>();

    //
    for (CategoryEntity catEntity : catDAO.findAll()) {
      ApplicationCategory category = buildCategory(catEntity, appTypes);
      categories.add(category);
    }

    //
    if (sortComparator != null) {
      Collections.sort(categories, sortComparator);
    }

    //
    return categories;
  }

  public ApplicationCategory getApplicationCategory(String name) {
    CategoryEntity catEntity = catDAO.findByName(name);
    if (catEntity != null) {
      return buildCategory(catEntity);
    } else {
      return null;
    }
  }

  public void save(final ApplicationCategory category) {
    CategoryEntity catEntity = catDAO.findByName(category.getName());
    catEntity = buildCatEntity(catEntity, category);

    if (catEntity.getId() == null) {
      catDAO.create(catEntity);
    } else {
      catDAO.update(catEntity);
    }
    layoutStorage.savePermissions(CategoryEntity.class.getName(), catEntity.getId(), TYPE.ACCESS, category.getAccessPermissions());
  }

  public void remove(ApplicationCategory category) {
    List<Long> ids = new LinkedList<>();

    CategoryEntity catEntity = catDAO.findByName(category.getName());
    if (catEntity != null) {
      for (ApplicationEntity app : catEntity.getApplications()) {
        ids.add(app.getId());
      }
      ids.add(catEntity.getId());
      catDAO.delete(catEntity);
    }

    for (Long id : ids) {
      layoutStorage.deletePermissions(ApplicationEntity.class.getName(), id);
    }
  }

  public List<Application> getApplications(ApplicationCategory category,
                                           Comparator<Application> sortComparator,
                                           ApplicationType<?>... appTypes) {
    List<Application> apps = new ArrayList<>();

    CategoryEntity catEntity = catDAO.findByName(category.getName());
    if (catEntity != null) {
      category = buildCategory(catEntity, appTypes);
      apps.addAll(category.getApplications());
    }

    if (sortComparator != null) {
      Collections.sort(apps, sortComparator);
    }
    return apps;
  }

  public List<Application> getAllApplications() throws Exception {
    List<Application> apps = new ArrayList<>();

    for (ApplicationEntity appEntity : appDAO.findAll()) {
      apps.add(buildApplication(appEntity));
    }
    return apps;
  }

  public Application getApplication(String category, String name) {
    ApplicationEntity appEntity = appDAO.find(category, name);
    if (appEntity != null) {
      return buildApplication(appEntity);
    }
    return null;
  }

  public void save(ApplicationCategory category, Application application) {
    save(category);
    CategoryEntity catEntity = catDAO.findByName(category.getName());

    ApplicationEntity appEntity = appDAO.find(category.getName(), application.getApplicationName());
    if (appEntity == null && application.getStorageId() != null) {
      appEntity = appDAO.find(Safe.parseLong(application.getStorageId()));
    }
    appEntity = buildAppEntity(appEntity, application);

    appEntity.setCategory(catEntity);
    catEntity.getApplications().add(appEntity);

    if (appEntity.getId() == null) {
      appDAO.create(appEntity);
    } else {
      appDAO.update(appEntity);
    }
    layoutStorage.savePermissions(ApplicationEntity.class.getName(), appEntity.getId(), TYPE.ACCESS, application.getAccessPermissions());
  }

  public void update(Application application) {
    ApplicationEntity appEntity = appDAO.find(Safe.parseLong(application.getStorageId()));
    if (appEntity != null) {
      appEntity = buildAppEntity(appEntity, application);
      appDAO.update(appEntity);
      layoutStorage.savePermissions(ApplicationEntity.class.getName(), appEntity.getId(), TYPE.ACCESS, application.getAccessPermissions());
    } else {
      throw new IllegalStateException();
    }
  }

  public void remove(Application app) {
    ApplicationEntity appEntity = appDAO.find(Safe.parseLong(app.getStorageId()));
    if (appEntity != null) {
      layoutStorage.deletePermissions(ApplicationEntity.class.getName(), appEntity.getId());
      appDAO.delete(appEntity);
    }
  }

  @Override
  public Collection<ApplicationCategory> detectPortletsFromWars() throws PortletInvokerException {
    ExoContainer manager = ExoContainerContext.getCurrentContainer();
    PortletInvoker portletInvoker = (PortletInvoker) manager.getComponentInstance(PortletInvoker.class);
    Set<org.gatein.pc.api.Portlet> portlets = portletInvoker.getPortlets();
    Map<String, ApplicationCategory> categoriesMap = new HashMap<>();

    for (org.gatein.pc.api.Portlet portlet : portlets) {
      PortletInfo info = portlet.getInfo();
      String categoryName = info.getApplicationName().trim();
      String portletName = info.getName();
      String contentId = categoryName + "/" + portletName;

      ApplicationCategory category = categoriesMap.get(categoryName);
      if (category == null) {
        category = new ApplicationCategory();
        category.setName(categoryName);
        category.setDisplayName(categoryName);
        category.setApplications(new ArrayList<>());
        categoriesMap.put(categoryName, category);
      }

      Application application = new Application();
      application.setApplicationName(portletName);
      application.setCategoryName(categoryName);
      application.setType(ApplicationType.PORTLET);
      application.setContentId(contentId);
      application.setId(contentId);
      LocalizedString descriptionLS = info.getMeta().getMetaValue(MetaInfo.DESCRIPTION);
      if (descriptionLS != null) {
        application.setDescription(getLocalizedStringValue(descriptionLS, portletName));
      }
      LocalizedString displayNameLS = portlet.getInfo().getMeta().getMetaValue(MetaInfo.DISPLAY_NAME);
      if (displayNameLS != null) {
        application.setDisplayName(getLocalizedStringValue(displayNameLS, portletName));
      }
      category.getApplications().add(application);
    }

    return categoriesMap.values();
  }

  public void importAllPortlets() throws Exception {
    log.info("About to import portlets in application registry");

    //
    ExoContainer manager = ExoContainerContext.getCurrentContainer();
    PortletInvoker portletInvoker = (PortletInvoker) manager.getComponentInstance(PortletInvoker.class);
    Set<org.gatein.pc.api.Portlet> portlets = portletInvoker.getPortlets();

    //
    portlet: for (org.gatein.pc.api.Portlet portlet : portlets) {
      if (portlet.isRemote()) {
        continue;
      }
      PortletInfo info = portlet.getInfo();
      String portletApplicationName = info.getApplicationName();
      String portletName = portlet.getContext().getId();

      // Need to sanitize portlet and application names in case they contain
      // characters that would
      // cause an improper Application name
      portletApplicationName = portletApplicationName.replace('/', '_');
      portletName = portletName.replace('/', '_');

      MetaInfo metaInfo = portlet.getInfo().getMeta();
      LocalizedString keywordsLS = metaInfo.getMetaValue(MetaInfo.KEYWORDS);

      //
      Set<String> categoryNames = new HashSet<String>();

      // Process keywords
      if (keywordsLS != null) {
        String keywords = keywordsLS.getDefaultString();
        if (keywords != null && keywords.length() != 0) {
          for (String categoryName : keywords.split(",")) {
            // Trim name
            categoryName = categoryName.trim();
            if (INTERNAL_PORTLET_TAG.equalsIgnoreCase(categoryName)) {
              log.debug("Skipping portlet (" + portletApplicationName + "," + portletName + ") + tagged as internal");
              continue portlet;
            } else {
              categoryNames.add(categoryName);
            }
          }
        }
      }

      ArrayList<String> permissions = new ArrayList<String>();
      permissions.add(anyOfAdminGroup);
      // If no keywords, use the portlet application name
      if (categoryNames.isEmpty()) {
        categoryNames.add(portletApplicationName.trim());
      }

      // Additionally categorise the portlet as remote
      boolean remote = portlet.isRemote();
      if (remote) {
        categoryNames.add(REMOTE_CATEGORY_NAME);

        // add producer name to categories for easier finding of portlets for
        // GTNPORTAL-823
        LocalizedString producerNameLS = metaInfo.getMetaValue(PRODUCER_NAME_META_INFO_KEY);
        if (producerNameLS != null) {
          categoryNames.add(producerNameLS.getDefaultString() + PRODUCER_CATEGORY_NAME_SUFFIX);
        }
      }

      //
      log.info("Importing portlet (" + portletApplicationName + "," + portletName + ") in categories " + categoryNames);

      // Process category names
      for (String categoryName : categoryNames) {
        CategoryEntity category = catDAO.findByName(categoryName);

        //
        if (category == null) {
          category = new CategoryEntity();
          category.setName(categoryName);
          category.setDisplayName(categoryName);
          catDAO.create(category);
          layoutStorage.savePermissions(CategoryEntity.class.getName(), category.getId(), TYPE.ACCESS, permissions);
        }

        //
        ApplicationEntity app = appDAO.find(categoryName, portletName);
        if (app == null) {
          LocalizedString descriptionLS = metaInfo.getMetaValue(MetaInfo.DESCRIPTION);
          LocalizedString displayNameLS = metaInfo.getMetaValue(MetaInfo.DISPLAY_NAME);
          String displayName = getLocalizedStringValue(displayNameLS, portletName);

          ApplicationType<?> appType;
          String contentId;
          if (remote) {
            appType = ApplicationType.PORTLET; // WSRP Portlet, but WSRP is deleted
            contentId = portlet.getContext().getId();
            displayName += REMOTE_DISPLAY_NAME_SUFFIX; // add remote to display
                                                       // name to make it more
                                                       // obvious that
                                                       // the portlet is remote
          } else {
            appType = ApplicationType.PORTLET;
            contentId = info.getApplicationName() + "/" + info.getName();
          }

          // Check if the portlet has already existed in this category
          List<Application> applications = buildCategory(category).getApplications();
          boolean isExist = false;
          for (Application application : applications) {
            if (application.getContentId().equals(contentId)) {
              isExist = true;
              break;
            }
          }

          if (!isExist) {
            app = new ApplicationEntity();
            app.setApplicationName(portletName);
            app.setCategory(category);
            app.setContentId(contentId);
            app.setType(appType.getName());
            app.setDisplayName(displayName);
            app.setDescription(getLocalizedStringValue(descriptionLS, portletName));
            appDAO.create(app);
            layoutStorage.savePermissions(ApplicationEntity.class.getName(), app.getId(), TYPE.ACCESS, permissions);
          }
        }
      }
    }
  }

  private ApplicationCategory buildCategory(CategoryEntity catEntity, ApplicationType<?>... appTypes) {
    ApplicationCategory category = new ApplicationCategory();

    //
    category.setName(catEntity.getName());
    category.setDisplayName(catEntity.getDisplayName());
    category.setDescription(catEntity.getDescription());

    List<String> access = layoutStorage.getPermissions(CategoryEntity.class.getName(), catEntity.getId(), TYPE.ACCESS);
    category.setAccessPermissions(access);

    category.setCreatedDate(new Date(catEntity.getCreatedDate()));
    category.setModifiedDate(new Date(catEntity.getModifiedDate()));

    //
    for (ApplicationEntity appEntity : catEntity.getApplications()) {
      Application application = buildApplication(appEntity);
      if (isApplicationType(application, appTypes)) {
        category.getApplications().add(application);
      }
    }

    //
    return category;
  }

  private boolean isApplicationType(Application app, ApplicationType<?>... appTypes) {
    if (appTypes == null || appTypes.length == 0) {
      return true;
    }
    for (ApplicationType<?> appType : appTypes) {
      if (appType.equals(app.getType())) {
        return true;
      }
    }
    return false;
  }

  private Application buildApplication(ApplicationEntity appEntity) {
    ApplicationType<?> applicationType = ApplicationType.getType(appEntity.getType());

    //
    Application application = new Application();
    application.setId(appEntity.getCategory().getName() + "/" + appEntity.getApplicationName());
    application.setCategoryName(appEntity.getCategory().getName());
    application.setType(applicationType);
    application.setApplicationName(appEntity.getApplicationName());
    application.setIconURL(getApplicationIconURL(appEntity));
    application.setDisplayName(appEntity.getDisplayName());
    application.setDescription(appEntity.getDescription());

    List<String> access = layoutStorage.getPermissions(ApplicationEntity.class.getName(), appEntity.getId(), TYPE.ACCESS);
    application.setAccessPermissions(new ArrayList<String>(access));

    application.setCreatedDate(new Date(appEntity.getCreatedDate()));
    application.setModifiedDate(new Date(appEntity.getModifiedDate()));
    application.setStorageId(String.valueOf(appEntity.getId()));
    application.setContentId(appEntity.getContentId());
    return application;
  }

  private static String getApplicationIconURL(ApplicationEntity appEntity) {
    ApplicationType<?> applicationType = ApplicationType.getType(appEntity.getType());
    ContentType type = applicationType.getContentType();
    String contentId = appEntity.getContentId();

    if (type == Portlet.CONTENT_TYPE) {
      String[] chunks = contentId.split("/");
      if (chunks.length == 2) {
        return "/" + chunks[0] + "/skin/DefaultSkin/portletIcons/" + chunks[1] + ".png";
      }
    }
    return null;
  }

  private CategoryEntity buildCatEntity(CategoryEntity catEntity, ApplicationCategory category) {
    if (catEntity == null) {
      catEntity = new CategoryEntity();
    }
    if (category.getCreatedDate() != null) {
      catEntity.setCreatedDate(category.getCreatedDate().getTime());
    }
    catEntity.setDescription(category.getDescription());
    catEntity.setDisplayName(category.getDisplayName());
    if (category.getModifiedDate() != null) {
      catEntity.setModifiedDate(category.getModifiedDate().getTime());
    }
    catEntity.setName(category.getName());
    return catEntity;
  }

  private ApplicationEntity buildAppEntity(ApplicationEntity appEntity, Application application) {
    if (appEntity == null) {
      appEntity = new ApplicationEntity();
    }
    appEntity.setApplicationName(application.getApplicationName());
    appEntity.setContentId(application.getContentId());
    if (application.getCreatedDate() != null) {
      appEntity.setCreatedDate(application.getCreatedDate().getTime());
    }
    appEntity.setDescription(application.getDescription());
    appEntity.setDisplayName(application.getDisplayName());
    if(application.getType() != null) {
      appEntity.setType(application.getType().getName());
    } else {
      appEntity.setType(ApplicationType.PORTLET.getName());
    }
    if (application.getModifiedDate() != null) {
      appEntity.setModifiedDate(application.getModifiedDate().getTime());
    }
    return appEntity;
  }

  private String getLocalizedStringValue(LocalizedString localizedString, String portletName) {
    if (localizedString == null || localizedString.getDefaultString() == null) {
      return portletName;
    } else {
      return localizedString.getDefaultString();
    }
  }

  @Override
  public void start() {
    if (plugins != null) {
      RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
      try {
        boolean firstStartup = this.getApplicationCategories().isEmpty();
        for (ApplicationCategoriesPlugins plugin : plugins) {
          plugin.run(firstStartup);
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      } finally {
        RequestLifeCycle.end();
      }
    }
  }

  @Override
  public void stop() {
  }

  @Override
  public Application getApplication(String id) throws Exception {
    String[] fragments = id.split("/");
    if (fragments.length < 2) {
      throw new Exception("Invalid Application Id: [" + id + "]");
    }

    String category = fragments[0];
    String applicationName = fragments[1];

    // If the application name contained a beginning slash (which can happen
    // with WSRP), we need to hack around the
    // hardcoding of portlet id expectations >_<
    if (fragments.length == 3 && applicationName.length() == 0) {
      applicationName = "/" + fragments[2];
    }
    return getApplication(category, applicationName);
  }

  @Override
  public List<ApplicationCategory> getApplicationCategories() throws Exception {
    return getApplicationCategories(null);
  }

  @Override
  public List<ApplicationCategory> getApplicationCategories(Comparator<ApplicationCategory> sortComparator) throws Exception {
    return getApplicationCategories(sortComparator, null);
  }

  @Override
  public List<ApplicationCategory> getApplicationCategories(String appName, ApplicationType appType, Comparator<ApplicationCategory> sortComparator) {
    List<ApplicationCategory> categories = new ArrayList<>();

    for (ApplicationCategory category : getApplicationCategories(sortComparator, null, appType)) {
      if (appName != null) {
        for (Application application : category.getApplications()) {
          if (appName.equals(application.getApplicationName())) {
            categories.add(category);
            break;
          }
        }
      } else {
        categories.add(category);
      }
    }

    //
    return categories;
  }

  @Override
  public List<ApplicationCategory> getApplicationCategories(String accessUser, ApplicationType<?>... appTypes) throws Exception {
    return getApplicationCategories(null, accessUser, appTypes);
  }

  @Override
  public List<Application> getApplications(ApplicationCategory category, ApplicationType<?>... appTypes) throws Exception {
    return getApplications(category, null, appTypes);
  }

  public void initListener(ComponentPlugin plugin) {
    if (plugin instanceof ApplicationCategoriesPlugins) {
      ApplicationCategoriesPlugins categoriesPlugin = (ApplicationCategoriesPlugins) plugin;
      if (categoriesPlugin.isMerge()) {
        List<ApplicationCategory> categories = categoriesPlugin.getCategories();
        if (categories != null && !categories.isEmpty()) {
          Iterator<ApplicationCategory> categoriesIterator = categories.iterator();
          while (categoriesIterator.hasNext()) {
            ApplicationCategory category = categoriesIterator.next();
            ApplicationCategory existingCategory = plugins.stream()
                                                          .map(ApplicationCategoriesPlugins::getCategories)
                                                          .flatMap(List::stream)
                                                          .filter(configuredCategory -> {
                                                            return configuredCategory.getName().equals(category.getName());
                                                          })
                                                          .findFirst()
                                                          .orElse(null);
            if (existingCategory != null) {
              existingCategory.getApplications().addAll(category.getApplications());
              categoriesIterator.remove();
            }
          }
        }
      }
      plugins.add(categoriesPlugin);
    }
  }
}
