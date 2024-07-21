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
package org.exoplatform.portal.test;

import org.gatein.portal.encoder.TestEncoderService;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.exoplatform.account.setup.web.AccountSetupServiceTest;
import org.exoplatform.portal.TestJIBXXmlMapping;
import org.exoplatform.portal.TestXSDCorruption;
import org.exoplatform.portal.branding.BrandingRestResourcesTest;
import org.exoplatform.portal.branding.BrandingServiceImplTest;
import org.exoplatform.portal.config.DefaultGroupVisibilityPluginTest;
import org.exoplatform.portal.config.DynamicPortalLayoutMatcherPluginTest;
import org.exoplatform.portal.config.DynamicPortalLayoutMatcherTest;
import org.exoplatform.portal.config.DynamicPortalLayoutServiceTest;
import org.exoplatform.portal.config.NewPortalConfigListenerTest;
import org.exoplatform.portal.config.TestEscape;
import org.exoplatform.portal.config.TestSerialization;
import org.exoplatform.portal.config.TestSiteDataImportConserve;
import org.exoplatform.portal.config.TestSiteDataImportMerge;
import org.exoplatform.portal.config.TestSiteDataImportOverwrite;
import org.exoplatform.portal.mop.dao.ContainerDAOTest;
import org.exoplatform.portal.mop.dao.DescriptionDAOTest;
import org.exoplatform.portal.mop.dao.NavigationDAOTest;
import org.exoplatform.portal.mop.dao.PageDAOTest;
import org.exoplatform.portal.mop.dao.PermissionDAOTest;
import org.exoplatform.portal.mop.dao.SiteDAOTest;
import org.exoplatform.portal.mop.dao.WindowDAOTest;
import org.exoplatform.portal.mop.rest.NavigationRestTest;
import org.exoplatform.portal.mop.storage.TestDescriptionService;
import org.exoplatform.portal.mop.storage.TestJCBCNavigationServiceRebase;
import org.exoplatform.portal.mop.storage.TestJDBCNavigationService;
import org.exoplatform.portal.mop.storage.TestJDBCNavigationServiceSave;
import org.exoplatform.portal.mop.storage.TestJDBCNavigationServiceUpdate;
import org.exoplatform.portal.mop.storage.TestJDBCNavigationServiceWrapper;
import org.exoplatform.portal.mop.storage.TestPageService;
import org.exoplatform.portal.rest.MembershipTypeRestResourcesTest;
import org.exoplatform.portal.rest.UserFieldValidatorTest;
import org.exoplatform.portal.rest.UserRestResourcesTest;
import org.exoplatform.portal.tree.list.TestListTree;
import org.exoplatform.settings.rest.SettingResourceTest;

import io.meeds.portal.permlink.service.PermanentLinkServiceTest;
import io.meeds.portal.security.listener.DefaultUserMembershipListenerTest;
import io.meeds.portal.security.listener.PortalRegistrationUpdateListenerTest;
import io.meeds.portal.security.service.SecuritySettingServiceTest;

@RunWith(Suite.class)
@SuiteClasses({
    SecuritySettingServiceTest.class,
    PortalRegistrationUpdateListenerTest.class,
    DefaultUserMembershipListenerTest.class,
    PermanentLinkServiceTest.class,
    AccountSetupServiceTest.class,
    BrandingServiceImplTest.class,
    DefaultGroupVisibilityPluginTest.class,
    DynamicPortalLayoutMatcherPluginTest.class,
    DynamicPortalLayoutMatcherTest.class,
    DynamicPortalLayoutServiceTest.class,
    TestEscape.class,
    TestSerialization.class,
    TestSiteDataImportConserve.class,
    TestSiteDataImportMerge.class,
    TestSiteDataImportOverwrite.class,
    ContainerDAOTest.class,
    DescriptionDAOTest.class,
    NavigationDAOTest.class,
    PageDAOTest.class,
    PermissionDAOTest.class,
    SiteDAOTest.class,
    WindowDAOTest.class,
    TestDescriptionService.class,
    TestJCBCNavigationServiceRebase.class,
    TestJDBCNavigationService.class,
    TestJDBCNavigationServiceSave.class,
    TestJDBCNavigationServiceUpdate.class,
    TestJDBCNavigationServiceWrapper.class,
    TestPageService.class,
    BrandingRestResourcesTest.class,
    NavigationRestTest.class,
    MembershipTypeRestResourcesTest.class,
    UserFieldValidatorTest.class,
    UserRestResourcesTest.class,
    TestJIBXXmlMapping.class,
    TestXSDCorruption.class,
    TestListTree.class,
    SettingResourceTest.class,
    TestEncoderService.class,
    NewPortalConfigListenerTest.class,
})
public class InitContainer1TestSuite {

}