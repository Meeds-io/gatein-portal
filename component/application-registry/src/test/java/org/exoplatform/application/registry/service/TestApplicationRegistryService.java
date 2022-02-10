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

package org.exoplatform.application.registry.service;

import java.util.ArrayList;
import java.util.List;

import org.picocontainer.Startable;

import org.exoplatform.application.registry.*;
import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.*;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.services.organization.*;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.application-registry-configuration.xml"),
})
public class TestApplicationRegistryService extends AbstractKernelTest {

  protected static String              demo      = "demo";

  protected static String              Group1    = "Group1";

  protected static String              Group2    = "Group2";

  protected static String              username1 = "userName_1";

  protected static String              username2 = "userName_2";

  protected static String              memtype1  = "MembershipType_1";

  protected static String              memtype2  = "MembershipType_2";

  protected Group                      group1, group2, groupDefault;

  protected MembershipType             mType1, mType2, mTypeDefault;

  protected User                       user1, user2, userDefault;

  protected ApplicationRegistryService service_;

  protected ConfigurationManager       configurationManager;

  protected OrganizationService        orgService;

  @Override
  protected void setUp() throws Exception {
    PortalContainer portalContainer = PortalContainer.getInstance();
    service_ = portalContainer.getComponentInstanceOfType(ApplicationRegistryService.class);
    configurationManager = portalContainer.getComponentInstanceOfType(ConfigurationManager.class);
    begin();
  }

  @Override
  protected void tearDown() throws Exception {
    end();
  }

  public void testApplicationCategory() throws Exception {
    // Add new ApplicationRegistry
    String categoryName = "Category1";
    String categoryDes = "Description for category 1";
    ApplicationCategory category1 = createAppCategory(categoryName, categoryDes);
    service_.save(category1);

    int numberOfCats = service_.getApplicationCategories().size();
    assertEquals(1, numberOfCats);

    ApplicationCategory returnedCategory1 = service_.getApplicationCategory(categoryName);
    assertNotNull(returnedCategory1);
    assertEquals(category1.getName(), returnedCategory1.getName());
    assertEquals(categoryName, returnedCategory1.getName());

    // Update the ApplicationRegistry
    String newDescription = "New description for category 1";
    category1.setDescription(newDescription);
    service_.save(category1);

    numberOfCats = service_.getApplicationCategories().size();
    assertEquals(1, numberOfCats);
    returnedCategory1 = service_.getApplicationCategory(categoryName);
    assertEquals(newDescription, returnedCategory1.getDescription());

    // Remove the ApplicationRegistry
    service_.remove(category1);
    numberOfCats = service_.getApplicationCategories().size();
    assertEquals(0, numberOfCats);

    returnedCategory1 = service_.getApplicationCategory(categoryName);
    assertNull(returnedCategory1);
  }

  public void testAppCategoryGetByAccessUser() throws Exception {
    String officeCategoryName = "Office";
    ApplicationCategory officeCategory = createAppCategory(officeCategoryName, "None");
    service_.save(officeCategory);
    String[] officeApps = { "MSOffice", "OpenOffice" };
    Application msApp = createApplication(officeApps[0], officeCategoryName);
    ArrayList<String> pers = new ArrayList<String>();
    pers.add("member:/users");
    msApp.setAccessPermissions(pers);
    service_.save(officeCategory, msApp);
    Application openApp = createApplication(officeApps[1], officeCategoryName);
    service_.save(officeCategory, openApp);

    String gameCategoryName = "Game";
    ApplicationCategory gameCategory = createAppCategory(gameCategoryName, "None");
    service_.save(gameCategory);
    String[] gameApps = { "HaftLife", "Chess" };
    Application haftlifeApp = createApplication(gameApps[0], gameCategoryName);
    pers = new ArrayList<String>();
    pers.add("member:/portal/admin");
    haftlifeApp.setAccessPermissions(pers);
    service_.save(gameCategory, haftlifeApp);
    Application chessApp = createApplication(gameApps[1], gameCategoryName);
    chessApp.setAccessPermissions(pers);
    service_.save(gameCategory, chessApp);

    List<ApplicationCategory> returnCategories = service_.getApplicationCategories(username1);
    assertEquals(2, returnCategories.size());
    assertEquals(2, returnCategories.get(0).getApplications().size());
    assertEquals(2, returnCategories.get(1).getApplications().size());
  }

  public void testImportApplicationCategoryOnStartup() throws Exception {
    String categoryName = "categoryNameMerge";

    InitParams params = new InitParams();
    ObjectParameter categoryParameter = new ObjectParameter();
    categoryParameter.setName("category");
    ApplicationCategory category = createAppCategory(categoryName, "categoryDescription");
    categoryParameter.setObject(category);
    params.addParameter(categoryParameter);
    ValueParam mergeParam = new ValueParam();
    mergeParam.setName("merge");
    mergeParam.setValue("true");
    params.addParameter(mergeParam);

    ApplicationCategoriesPlugins plugins = new ApplicationCategoriesPlugins(service_, configurationManager, params);
    service_.initListener(plugins);
    startService();
    ApplicationCategory storedCategory = service_.getApplicationCategory(categoryName);
    assertNotNull(storedCategory);
  }
  
  public void testImportSystemApplicationOnStartup() throws Exception {
    
    String systemApplicationCategoryName = "systemApplicationCategory";
    
    InitParams systemApplicationParams = new InitParams();
    ObjectParameter systemApplicationCategoryParameter = new ObjectParameter();
    systemApplicationCategoryParameter.setName(systemApplicationCategoryName);
    ApplicationCategory systemApplicationCategory = createAppCategory(systemApplicationCategoryName, "systemApplicationCategoryDescription");
    
    Application systemApplication = createApplication("systemApplication", "systemApplicationCategory");
    List<Application> systemApplications = List.of(systemApplication);
    systemApplicationCategory.setApplications(systemApplications);
    systemApplicationCategoryParameter.setObject(systemApplicationCategory);
    systemApplicationParams.addParameter(systemApplicationCategoryParameter);
    ValueParam systemParam = new ValueParam();
    systemParam.setName("system");
    systemParam.setValue("true");
    systemApplicationParams.addParameter(systemParam);
    
    ApplicationCategoriesPlugins systemPlugins = new ApplicationCategoriesPlugins(service_, configurationManager, systemApplicationParams);
    service_.initListener(systemPlugins);
    startService();
    //not existing systemApplicationCategory is created
    ApplicationCategory storedSystemApplicationCategory = service_.getApplicationCategory(systemApplicationCategoryName);
    assertNotNull(storedSystemApplicationCategory);
    //systemApplication is created after systemApplicationCategory creation
    Application storedSystemApplication = service_.getApplication("systemApplicationCategory/systemApplication");
    assertNotNull(storedSystemApplication);
    assertEquals(storedSystemApplication.getDisplayName(), "systemApplication");
    
    Application secondSystemApplication = createApplication("secondSystemApplication", "systemApplicationCategory");
    systemApplications = List.of(systemApplication, secondSystemApplication);
    systemApplicationCategory.setApplications(systemApplications);
    systemApplicationCategoryParameter.setObject(systemApplicationCategory);
    systemApplicationParams.put(systemApplicationCategoryName, systemApplicationCategoryParameter);
    systemPlugins = new ApplicationCategoriesPlugins(service_, configurationManager, systemApplicationParams);
    storedSystemApplication.setDisplayName("updatedSystemApplication");
    service_.save(storedSystemApplicationCategory, storedSystemApplication);
    startService();
    //secondSystemApplication is created when systemApplicationCategory already exists
    Application storedSecondSystemApplication = service_.getApplication("systemApplicationCategory/secondSystemApplication");
    assertNotNull(storedSecondSystemApplication);
    assertEquals(storedSecondSystemApplication.getDisplayName(), "secondSystemApplication");
    //systemApplication is not overwritten and conserve its modified stored value 
    storedSystemApplication = service_.getApplication("systemApplicationCategory/systemApplication");
    assertEquals(storedSystemApplication.getDisplayName(), "updatedSystemApplication");
  }
  
  private void startService() {
    ((Startable) service_).start();
  }

  private ApplicationCategory createAppCategory(String categoryName, String categoryDes) {
    ApplicationCategory category = new ApplicationCategory();
    category.setName(categoryName);
    category.setDisplayName(categoryName);
    category.setDescription(categoryDes);
    return category;
  }

  private Application createApplication(String appName, String appGroup) {
    Application app = new Application();
    app.setContentId(appName);
    app.setApplicationName(appName);
    app.setDisplayName(appName);
    app.setType(ApplicationType.PORTLET);
    return app;
  }
}
