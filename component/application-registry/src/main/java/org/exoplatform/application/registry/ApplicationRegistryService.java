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

package org.exoplatform.application.registry;

import java.util.Comparator;
import java.util.List;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.portal.config.model.ApplicationType;

/** Created y the eXo platform team User: Tuan Nguyen Date: 20 april 2007 */
public interface ApplicationRegistryService {
    String REMOTE_DISPLAY_NAME_SUFFIX = " (remote)";

    /**
     * Return list of ApplicationCatgory (and applications in each category)
     *
     * @param accessUser
     * @param appTypes - array of ApplicationType, used to filter applications in each application category
     */
    List<ApplicationCategory> getApplicationCategories(String accessUser, ApplicationType<?>... appTypes) throws Exception;

    void initListener(ComponentPlugin com);

    /**
     * Return list of all current application categories (unsorted, all Application in all ApplicationType)
     */
    List<ApplicationCategory> getApplicationCategories() throws Exception;

    /**
     * Return list of all current application categories (sorted, all applications in all types)
     *
     * @param sortComparator - Comparator used to sort the returned list
     */
    List<ApplicationCategory> getApplicationCategories(Comparator<ApplicationCategory> sortComparator) throws Exception;

    /**
     * Return list of all app categories of app determined by name and type
     *
     * @param appName - application name
     * @param appType - type of app: local, remote, gadget
     * @param sortComparator - Comparator used to sort the returned list
     */
    List<ApplicationCategory> getApplicationCategories(String appName, ApplicationType appType,
                                                       Comparator<ApplicationCategory> sortComparator);

    /**
     * Return ApplicationCategory with name provided <br>
     * if not found, return null
     *
     * @param name - ApplicationCategory's name
     */
    ApplicationCategory getApplicationCategory(String name);

    /**
     * Save an ApplicationCategory to database <br>
     * If it doesn't exist, a new one will be created, if not, it will be updated
     *
     * @param category - ApplicationCategory object that will be saved
     */
    void save(ApplicationCategory category);

    /**
     * Remove application category (and all application in it) from database <br>
     * If it doesn't exist, it will be ignored
     *
     * @param category - ApplicationCategory object that will be removed
     */
    void remove(ApplicationCategory category);

    /**
     * Return list of applications (unsorted) in specific category and have specific type
     *
     * @param category - ApplicationCategory that you want to list applications
     * @param appTypes - array of application type
     */
    List<Application> getApplications(ApplicationCategory category, ApplicationType<?>... appTypes) throws Exception;

    /**
     * Return list of applications (sorted) in specific category and have specific type
     *
     * @param category - ApplicationCategory that you want to list applications
     * @param sortComparator - comparator used to sort application list
     * @param appTypes - array of application type
     */
    List<Application> getApplications(ApplicationCategory category, Comparator<Application> sortComparator,
            ApplicationType<?>... appTypes);

    /**
     * Return list of all Application in database (unsorted) <br>
     * If there are not any Application in database, return an empty list
     */
    List<Application> getAllApplications() throws Exception;

    /**
     * Return Application with id provided
     *
     * @param id - must be valid applicationId (catgoryname/applicationName), if not, this will throw exception
     */
    Application getApplication(String id) throws Exception;

    /**
     * Return Application in specific category and have name provided in param <br>
     * If it can't be found, return null
     *
     * @param category - name of application category
     * @param name - name of application
     */
    Application getApplication(String category, String name);

    /**
     * Save Application in an ApplicationCategory <br>
     * If ApplicationCategory or Application don't exist, they'll be created <br>
     * If Application has been already existed, it will be updated <br>
     *
     * @param category - ApplicationCategory that your application'll be saved to
     * @param application - Application that will be saved
     */
    void save(ApplicationCategory category, Application application);

    /**
     * Update an Application <br>
     * It must be existed in database, if not, this will throw an IllegalStateException
     *
     * @param application - Application that you want to update
     */
    void update(Application application);

    /**
     * Remove an Application from database <br>
     * If it can't be found, it will be ignored (no exception)
     *
     * @param app - Application that you want to remove, must not be null
     */
    void remove(Application app);

    /**
     * Get all deployed portlet, add to portlet's ApplicationCategory <br>
     * If ApplicationCategory currently doesn't exist, it'll be created <br>
     * If Application've already existed, it'll be ignored
     */
    void importAllPortlets() throws Exception;

    // TODO: dang.tung
    /**
     * Get all Gadget, add to eXoGadgets application category <br>
     * When first added, it's access permission will be Everyone <br>
     * If ApplicationCategory currently doesn't exist, it'll be created <br>
     * Gadget that has been imported will be ignored
     */
    void importExoGadgets() throws Exception;
}
