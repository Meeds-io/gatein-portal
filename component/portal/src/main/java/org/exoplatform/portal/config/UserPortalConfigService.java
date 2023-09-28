/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.portal.config;


import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.importer.ImportMode;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.portal.mop.storage.DescriptionStorage;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.exoplatform.portal.mop.user.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by The eXo Platform SAS Apr 19, 2007 This service is used to load the PortalConfig, Page config and Navigation config
 * for a given user.
 */
public class UserPortalConfigService implements Startable {

  private static final Scope      HOME_PAGE_URI_PREFERENCE_SCOPE = Scope.PORTAL.id("HOME");

  private static final String     HOME_PAGE_URI_PREFERENCE_KEY   = "HOME_PAGE_URI";

  public static final String      DEFAULT_GLOBAL_PORTAL       = "global";

  public static final String      DEFAULT_GROUP_SITE_TEMPLATE = "group";

  public static final String      DEFAULT_USER_SITE_TEMPLATE  = "user";

    LayoutService layoutService;

    UserACL userACL_;

    OrganizationService orgService_;

    private SettingService          settingService;

    private NewPortalConfigListener newPortalConfigListener_;

    /** . */
    final NavigationService navigationService;

    /** . */
    final DescriptionStorage descriptionStorage;

    final PageStorage        pageStorage;

    /** . */
    boolean createUserPortal;

    /** . */
    boolean destroyUserPortal;
    
    /** . */
    String globalPortal_;

    /** . */
    String defaultGroupSiteTemplate;

    /** . */
    String defaultUserSiteTemplate;

    /** . */
    private final ImportMode defaultImportMode;

    private PortalConfig defaultPortalConfig;

    private Log log = ExoLogger.getLogger("Portal:UserPortalConfigService");

    public UserPortalConfigService(UserACL userACL, // NOSONAR
                                   LayoutService storage,
                                   OrganizationService orgService,
                                   NavigationService navService,
                                   DescriptionStorage descriptionService,
                                   PageStorage pageStorage,
                                   SettingService settingService,
                                   InitParams params) {

        //
        ValueParam createUserPortalParam = params == null ? null : params.getValueParam("create.user.portal");
        boolean createUserPortal = createUserPortalParam == null
                || createUserPortalParam.getValue().toLowerCase().trim().equals("true");

        //
        ValueParam destroyUserPortalParam = params == null ? null : params.getValueParam("destroy.user.portal");
        boolean destroyUserPortal = destroyUserPortalParam == null
                || destroyUserPortalParam.getValue().toLowerCase().trim().equals("true");

        //
        ValueParam defaultImportModeParam = params == null ? null : params.getValueParam("default.import.mode");
        ImportMode defaultImportMode = defaultImportModeParam == null ? ImportMode.CONSERVE : ImportMode
                .valueOf(defaultImportModeParam.getValue().toUpperCase().trim());

        //
        ValueParam globalPortalParam = params == null ? null : params.getValueParam("global.portal");
        this.globalPortal_ = globalPortalParam == null ? DEFAULT_GLOBAL_PORTAL : globalPortalParam.getValue();

        ValueParam defaultGroupSiteTemplateParam = params == null ? null : params.getValueParam("default.groupSite.template");
        String defaultGroupSiteTemplate = defaultGroupSiteTemplateParam == null ? DEFAULT_GROUP_SITE_TEMPLATE : defaultGroupSiteTemplateParam.getValue();

        //
        ValueParam defaultUserSiteTemplateParam = params == null ? null : params.getValueParam("default.userSite.template");
        String defaultUserSiteTemplate = defaultUserSiteTemplateParam == null ? DEFAULT_USER_SITE_TEMPLATE : defaultUserSiteTemplateParam.getValue();

        //
        this.layoutService = storage;
        this.orgService_ = orgService;
        this.settingService = settingService;
        this.userACL_ = userACL;
        this.navigationService = navService;
        this.descriptionStorage = descriptionService;
        this.pageStorage = pageStorage;
        this.createUserPortal = createUserPortal;
        this.destroyUserPortal = destroyUserPortal;
        this.defaultImportMode = defaultImportMode;
        this.defaultGroupSiteTemplate = defaultGroupSiteTemplate;
        this.defaultUserSiteTemplate = defaultUserSiteTemplate;
    }

    public PageStorage getPageService() {
        return pageStorage;
    }

    public LayoutService getDataStorage() {
        return layoutService;
    }

    public ImportMode getDefaultImportMode() {
        return defaultImportMode;
    }

    public boolean getCreateUserPortal() {
        return createUserPortal;
    }

    public void setCreateUserPortal(boolean createUserPortal) {
        this.createUserPortal = createUserPortal;
    }

    public boolean getDestroyUserPortal() {
        return destroyUserPortal;
    }

    public void setDestroyUserPortal(boolean destroyUserPortal) {
        this.destroyUserPortal = destroyUserPortal;
    }

    public String getDefaultGroupSiteTemplate() {
      return defaultGroupSiteTemplate;
    }

    public void setDefaultGroupSiteTemplate(String defaultGroupSiteTemplate) {
      this.defaultGroupSiteTemplate = defaultGroupSiteTemplate;
    }

    public String getDefaultUserSiteTemplate() {
      return defaultUserSiteTemplate;
    }

    public void setDefaultUserSiteTemplate(String defaultUserSiteTemplate) {
      this.defaultUserSiteTemplate = defaultUserSiteTemplate;
    }

    /**
     * Returns the navigation service associated with this service.
     *
     * @return the navigation service;
     */
    public NavigationService getNavigationService() {
        return navigationService;
    }

    public DescriptionStorage getDescriptionService() {
        return descriptionStorage;
    }

    public UserACL getUserACL() {
        return userACL_;
    }

    public OrganizationService getOrganizationService() {
        return orgService_;
    }

    /** Temporary until the {@link #getUserPortalConfig(String, String)} is removed. */
    private static final UserPortalContext NULL_CONTEXT = new UserPortalContext() {
        public ResourceBundle getBundle(UserNavigation navigation) {
            return null;
        }

        public Locale getUserLocale() {
            return Locale.ENGLISH;
        }
    };

    /**
     * <p>
     * Build and returns an instance of <code>UserPortalConfig</code>.
     * </p>
     * <br>
     * <p>
     * To return a valid config, the current thread must be associated with an identity that will grant him access to the portal
     * as returned by the {@link UserACL#hasPermission(org.exoplatform.portal.config.model.PortalConfig)} method.
     * </p>
     * <br>
     * The navigation loaded on the <code>UserPortalConfig</code> object are obtained according to the specified user
     * argument. The portal navigation is always loaded. If the specified user is null then the navigation of the guest
     * group as configured by {@link org.exoplatform.portal.config.UserACL#getGuestsGroup()} is also loaded, otherwise
     * the navigations are loaded according to the following rules:
     * <br>
     * <ul> <li>The navigation corresponding to the user is loaded.</li> <li>When the user is root according to the value
     * returned by {@link org.exoplatform.portal.config.UserACL#getSuperUser()} then the navigation of all groups are
     * loaded.</li> <li>When the user is not root, then all its groups are added except the guest group as configued per
     * {@link org.exoplatform.portal.config.UserACL#getGuestsGroup()}.</li> </ul>
     * <br>
     * All the navigations are sorted using the value returned by {@link org.exoplatform.portal.config.model.PageNavigation#getPriority()}.
     *
     * @param portalName the portal name
     * @param accessUser the user name
     * @return the config
     * @throws Exception any exception
     * @deprecated the method {@link #getUserPortalConfig(String, String, org.exoplatform.portal.mop.user.UserPortalContext)}
     *             should be used instead
     */
    @Deprecated
    public UserPortalConfig getUserPortalConfig(String portalName, String accessUser) throws Exception {
        return getUserPortalConfig(portalName, accessUser, NULL_CONTEXT);
    }

    public UserPortalConfig getUserPortalConfig(String portalName, String accessUser, UserPortalContext userPortalContext)
            throws Exception {
        PortalConfig portal = layoutService.getPortalConfig(portalName);
        if (portal == null || !userACL_.hasPermission(portal)) {
            return null;
        }

        return new UserPortalConfig(portal, this, portalName, accessUser, userPortalContext);
    }

    /**
     * Compute and returns the list that the specified user can manage. If the user is root then all existing groups are
     * returned otherwise the list is computed from the groups in which the user has a configured membership. The membership is
     * configured from the value returned by {@link org.exoplatform.portal.config.UserACL#getMakableMT()}
     *
     * @param remoteUser the user to get the makable navigations
     * @param withSite true if a site must exist
     * @return the list of groups
     * @throws Exception any exception
     */
    public List<String> getMakableNavigations(String remoteUser, boolean withSite) throws Exception {
        Collection<Group> groups;
        if (remoteUser.equals(userACL_.getSuperUser())) {
            groups = orgService_.getGroupHandler().getAllGroups();
        } else {
            groups = orgService_.getGroupHandler().resolveGroupByMembership(remoteUser, userACL_.getMakableMT());
        }

        //
        List<String> list = new ArrayList<String>();
        if (groups != null) {
            Set<String> existingNames = null;
            if (withSite) {
                existingNames = new HashSet<String>();
                Query<PortalConfig> q = new Query<PortalConfig>("group", null, PortalConfig.class);
                LazyPageList<PortalConfig> lpl = layoutService.find(q, null);
                for (PortalConfig groupSite : lpl.getAll()) {
                    existingNames.add(groupSite.getName());
                }
            }

            //
            for (Group group : groups) {
                String groupId = group.getId().trim();
                if (existingNames == null || existingNames.contains(groupId)) {
                    list.add(groupId);
                }
            }
        }

        //
        return list;
    }

/**
     * Returns a boolean hasNav according to if the user has at least one makable navigation i.e if he
     * belongs to at least one group
     * @param remoteUser the user to get the makable navigations
     * @param withSite whether or not check if siteConfig exists.
     * @return true or false
     * @throws Exception any exception
     */
    public boolean hasMakableNavigations(String remoteUser, boolean withSite) throws Exception{
        Collection<Group> groups;
        boolean hasNav = false;
        if (remoteUser == null) {
            hasNav = false;
        } else if (remoteUser.equals(userACL_.getSuperUser())) {
            hasNav = true; // as the super user is member of all groups
        } else {
            groups = orgService_.getGroupHandler().resolveGroupByMembership(remoteUser, userACL_.getMakableMT());
            if (groups != null && groups.size() > 0) {
                  if (withSite) {
                      for (Group group : groups) {
                        PortalConfig cfg = layoutService.getPortalConfig(PortalConfig.GROUP_TYPE, group.getId());
                        if (cfg != null) {
                          hasNav = true;
                          break;
                        }
                      }
                  } else {
                      hasNav = true;
                  }
              }
        }
 
        return hasNav;
	}
    /**
     * Create a user site for the specified user. It will perform the following:
     * <ul>
     * <li>create the user site by calling {@link #createUserPortalConfig(String, String, String)} which may create a site or
     * not according to the default configuration</li>
     * <li>if not site exists then it creates a site then it creates an empty site</li>
     * <li>if not navigation exists for the user site then it creates an empty navigation</li>
     * </ul>
     *
     * @param userName the user name
     * @throws Exception a nasty exception
     */
    public void createUserSite(String userName) throws Exception {
        // Create the portal using default template
        createUserPortalConfig(PortalConfig.USER_TYPE, userName, getDefaultUserSiteTemplate());

        // Need to insert the corresponding user site if needed
        PortalConfig cfg = layoutService.getPortalConfig(PortalConfig.USER_TYPE, userName);
        if (cfg == null) {
            cfg = new PortalConfig(PortalConfig.USER_TYPE, userName);
            cfg.useDefaultPortalLayout();
            layoutService.create(cfg);
        }

        // Create a blank navigation if needed
        SiteKey key = SiteKey.user(userName);
        NavigationContext nav = navigationService.loadNavigation(key);
        if (nav == null) {
            nav = new NavigationContext(key, new NavigationState(5));
            navigationService.saveNavigation(nav);
        }
    }

    /**
     * Create a group site for the specified group. It will perform the following:
     * <ul>
     * <li>create the group site by calling {@link #createUserPortalConfig(String, String, String)} which may create a site or
     * not according to the default configuration</li>
     * <li>if not site exists then it creates a site then it creates an empty site</li>
     * </ul>
     *
     * @param groupId the group id
     * @throws Exception a nasty exception
     */
    public void createGroupSite(String groupId) throws Exception {
        // Create the portal using default template
        createUserPortalConfig(PortalConfig.GROUP_TYPE, groupId, getDefaultGroupSiteTemplate());

        // Need to insert the corresponding group site
        PortalConfig cfg = layoutService.getPortalConfig(PortalConfig.GROUP_TYPE, groupId);
        if (cfg == null) {
            cfg = new PortalConfig(PortalConfig.GROUP_TYPE, groupId);
            cfg.useDefaultPortalLayout();
            layoutService.create(cfg);
        }
    }

    /**
     * This method should create a the portal config, pages and navigation according to the template name.
     *
     * @param siteType the site type
     * @param siteName the Site name
     * @param template the template to use
     * @throws Exception any exception
     */
    public void createUserPortalConfig(String siteType, String siteName, String template) throws Exception {
        NewPortalConfig portalConfig = null;
        if (StringUtils.isBlank(template)) {
          portalConfig = new NewPortalConfig();
          portalConfig.setUseDefaultPortalLayout(true);
        } else {
          String templatePath = newPortalConfigListener_.getTemplateConfig(siteType, template);
          portalConfig = new NewPortalConfig(templatePath);
          portalConfig.setTemplateName(template);
        }

        portalConfig.setOwnerType(siteType);
        newPortalConfigListener_.createPortalConfig(portalConfig, siteName);
        newPortalConfigListener_.createPage(portalConfig, siteName);
        newPortalConfigListener_.createPageNavigation(portalConfig, siteName);
    }

    /**
     * This method removes the PortalConfig, Page and PageNavigation that belong to the portal in the database.
     *
     * @param portalName the portal name
     * @throws Exception any exception
     */
    public void removeUserPortalConfig(String portalName) throws Exception {
        removeUserPortalConfig(PortalConfig.PORTAL_TYPE, portalName);
    }

    /**
     * This method removes the PortalConfig, Page and PageNavigation that belong to the portal in the database.
     *
     * @param ownerType the owner type
     * @param ownerId the portal name
     * @throws Exception any exception
     */
    public void removeUserPortalConfig(String ownerType, String ownerId) throws Exception {
        PortalConfig config = layoutService.getPortalConfig(ownerType, ownerId);
        if (config != null) {
            layoutService.remove(config);
        }
    }

    /**
     * Load metadata of specify page
     *
     * @param pageRef the PageKey
     * @return the PageContext
     */
    public PageContext getPage(PageKey pageRef) {
        if (pageRef == null) {
            return null;
        }

        PageContext page = layoutService.getPageContext(pageRef);
        if (page == null || !userACL_.hasPermission(page)) {
            return null;
        }
        return page;
    }

    /**
     * Creates a page from an existing template.
     *
     * @param temp the template name
     * @param ownerType the new owner type
     * @param ownerId the new owner id
     * @return the page
     * @throws Exception any exception
     */
    public Page createPageTemplate(String temp, String ownerType, String ownerId) throws Exception {
        Page page = newPortalConfigListener_.createPageFromTemplate(ownerType, ownerId, temp);
        updateOwnership(page, ownerType, ownerId);
        return page;
    }

    public String getGlobalPortal() {
      return globalPortal_;
    }

    public List<String> getSiteNames(SiteType siteType, int offset, int limit) {
      List<String> list = layoutService.getSiteNames(siteType, offset, limit);
      for (Iterator<String> i = list.iterator(); i.hasNext();) {
        String name = i.next();
        if (siteType == SiteType.PORTAL && StringUtils.equals(name, globalPortal_)) {
          i.remove();
          continue;
        }
        PortalConfig config = layoutService.getPortalConfig(siteType.getName(), name);
        if (config == null || !userACL_.hasPermission(config)) {
          i.remove();
        }
      }
      return list;
    }

    public List<PortalConfig> getSites(SiteFilter siteFilter) {
      List<PortalConfig> list = layoutService.getSites(siteFilter);
      return list.stream().filter(config -> config != null && userACL_.hasPermission(config)).toList();
    }
    public Collection<UserNode> getSiteNavigations(String siteName, String userName, HttpServletRequest context) throws Exception {
        HttpUserPortalContext userPortalContext = new HttpUserPortalContext(context);
        UserPortalConfig userPortalConfig = getUserPortalConfig(siteName, userName, userPortalContext);
        UserPortal userPortal = userPortalConfig.getUserPortal();
        UserNavigation navigation = userPortal.getNavigation(new SiteKey(SiteType.PORTAL, siteName));
        UserNodeFilterConfig builder = UserNodeFilterConfig.builder()
                .withReadWriteCheck()
                .withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL)
                .withTemporalCheck()
                .build();
        UserNode rootNode = userPortal.getNode(navigation, org.exoplatform.portal.mop.navigation.Scope.ALL, builder, null);
        return rootNode != null ? rootNode.getChildren() : Collections.emptyList();
    }
    public String getFirstAvailableNodeUri(Collection<UserNode> userNodes) {
      String nodeUri = "";
      for (UserNode node : userNodes) {
        if (node.getPageRef() != null) {
          nodeUri = node.getURI();
          break;
        } else if (node.getChildren() != null && !node.getChildren().isEmpty()) {
          nodeUri = getFirstAvailableNodeUri(node.getChildren());
        }
        if (StringUtils.isNotBlank(nodeUri)) {
          break;
        }
      }
      return nodeUri;
    }

    /**
     * Update the ownership recursively on the model graph.
     *
     * @param object the model object graph root
     * @param ownerType the new owner type
     * @param ownerId the new owner id
     */
    private void updateOwnership(ModelObject object, String ownerType, String ownerId) {
        if (object instanceof Container) {
            Container container = (Container) object;
            if (container instanceof Page) {
                Page page = (Page) container;
                page.setOwnerType(ownerType);
                page.setOwnerId(ownerId);
            }
            for (ModelObject child : container.getChildren()) {
                updateOwnership(child, ownerType, ownerId);
            }
        } else if (object instanceof Application) {
            Application application = (Application) object;
            TransientApplicationState applicationState = (TransientApplicationState) application.getState();
            if (applicationState != null && (applicationState.getOwnerType() == null || applicationState.getOwnerId() == null)) {
                applicationState.setOwnerType(ownerType);
                applicationState.setOwnerId(ownerId);
            }
        }
    }

    public void initListener(ComponentPlugin listener) {
        if (listener instanceof NewPortalConfigListener) {
            synchronized (this) {
                if (newPortalConfigListener_ == null) {
                    this.newPortalConfigListener_ = (NewPortalConfigListener) listener;
                } else {
                    newPortalConfigListener_.mergePlugin((NewPortalConfigListener) listener);
                }
            }
        }
    }

    public void deleteListenerElements(ComponentPlugin listener) {
        if (listener instanceof NewPortalConfigListener) {
            synchronized (this) {
                if (newPortalConfigListener_ != null) {
                    newPortalConfigListener_.deleteListenerElements((NewPortalConfigListener) listener);
                }
            }
        }
    }

    public void start() {
        try {
            if (newPortalConfigListener_ == null) {
                return;
            }

            //
            newPortalConfigListener_.run();
        } catch (Exception e) {
            log.error("Could not import initial data", e);
        }

        loadDefaultPortalConfig();
    }

    public void stop() {
    }

    public String getDefaultPortal() {
        return newPortalConfigListener_.getDefaultPortal();
    }

    /**
     * @param username user name
     * @return User home page uri preference
     */
    public String getUserHomePage(String username) {
      SettingValue<?> homePageSettingValue = settingService.get(Context.USER.id(username),
                                                                HOME_PAGE_URI_PREFERENCE_SCOPE,
                                                                HOME_PAGE_URI_PREFERENCE_KEY);
      if (homePageSettingValue != null && homePageSettingValue.getValue() != null) {
        return homePageSettingValue.getValue().toString();
      }
      return PropertyManager.getProperty("exo.portal.user.defaultHome");
    }

    /**
     * Stores user default home page
     * 
     * @param username user name
     * @param uri URI to consider as default page of user
     */
    public void saveUserHomePage(String username, String uri) {
      if (StringUtils.isBlank(uri)) {
        settingService.remove(Context.USER.id(username),
                              HOME_PAGE_URI_PREFERENCE_SCOPE,
                              HOME_PAGE_URI_PREFERENCE_KEY);
      } else {
        settingService.set(Context.USER.id(username),
                           HOME_PAGE_URI_PREFERENCE_SCOPE,
                           HOME_PAGE_URI_PREFERENCE_KEY,
                           SettingValue.create(uri));
      }
    }

    /**
     * Returns the default portal template to be used when creating a site
     *
     * @return the default portal template name
     */
    public String getDefaultPortalTemplate() {
        return newPortalConfigListener_.getDefaultPortalTemplate();
    }

    public Set<String> getPortalTemplates() {
        return newPortalConfigListener_.getTemplateConfigs(PortalConfig.PORTAL_TYPE);
    }

    public PortalConfig getPortalConfigFromTemplate(String templateName) {
        return newPortalConfigListener_.getPortalConfigFromTemplate(PortalConfig.PORTAL_TYPE, templateName);
    }

    /**
     * Get the skin name of the default portal
     * @return Skin name of the default portal
     */
    public String getDefaultPortalSkinName() {
        return defaultPortalConfig != null && StringUtils.isNotBlank(defaultPortalConfig.getSkin()) ?
                defaultPortalConfig.getSkin() : null;
    }

    /**
     * Get the PortalConfig object of the default portal
     * @return PortalConfig object of the default portal
     */
    public PortalConfig getDefaultPortalConfig() {
        return defaultPortalConfig;
    }

    /**
     * Set the PortalConfig object of the default portal
     * @param defaultPortalConfig PortalConfig object of the default portal
     */
    public void setDefaultPortalConfig(PortalConfig defaultPortalConfig) {
        this.defaultPortalConfig = defaultPortalConfig;
    }

    /**
     * Load the PortalConfig object of the default portal
     */
    protected void loadDefaultPortalConfig() {
        String defaultPortal = this.getDefaultPortal();

        if(defaultPortal != null) {
            try {
                RequestLifeCycle.begin(ExoContainerContext.getCurrentContainer());
                defaultPortalConfig = getDataStorage().getPortalConfig(defaultPortal);
            } catch (Exception e) {
                log.error("Cannot retrieve data of portal " + defaultPortal, e);
            } finally {
                RequestLifeCycle.end();
            }
        }
    }

    public void reloadConfig(String ownerType, String predefinedOwner, String location, String importMode, boolean overrideMode) {
      newPortalConfigListener_.reloadConfig(ownerType, predefinedOwner, location, importMode, overrideMode);
    }

}
