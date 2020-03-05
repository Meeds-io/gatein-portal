package org.exoplatform.portal.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.exoplatform.portal.mop.SiteKey;

public class DynamicPortalLayoutMatcherTest {

  private static final String PORTAL_SITE_NAME     = "testPortal";

  private static final String GROUP_SITE_NAME      = "/group/testGroup";

  private static final String CURRENT_SITE_NAME_OK = "testPortal.*";

  private static final String CURRENT_SITE_NAME_KO = "testPortal.+";

  private static final String SITE_NAME_MATCHER_OK = "/group/test.*Group";

  private static final String SITE_NAME_MATCHER_KO = "/group/test.+Group";

  private static final String SITE_TYPE_MATCHER_OK = "group|GROUP|user|USER";

  private static final String SITE_TYPE_MATCHER_KO = "portal";

  @Test
  public void testNullSiteKeyMatch() {
    DynamicPortalLayoutMatcher dynamicPortalLayoutMatcher = mockMatcher(CURRENT_SITE_NAME_OK,
                                                                        SITE_NAME_MATCHER_OK,
                                                                        SITE_TYPE_MATCHER_OK);

    assertFalse("Portal site shouldn't match", dynamicPortalLayoutMatcher.matches(null, PORTAL_SITE_NAME));
  }

  @Test
  public void testSiteKeyMatch() {
    DynamicPortalLayoutMatcher dynamicPortalLayoutMatcher = mockMatcher(CURRENT_SITE_NAME_OK,
                                                                        SITE_NAME_MATCHER_OK,
                                                                        SITE_TYPE_MATCHER_OK);

    assertTrue("Shouldn't match group site name",
               dynamicPortalLayoutMatcher.matches(SiteKey.group(GROUP_SITE_NAME), PORTAL_SITE_NAME));
  }

  @Test
  public void testNotMatchSiteName() {
    DynamicPortalLayoutMatcher dynamicPortalLayoutMatcher = mockMatcher(CURRENT_SITE_NAME_OK,
                                                                        SITE_NAME_MATCHER_KO,
                                                                        SITE_TYPE_MATCHER_OK);

    assertFalse("Shouldn't match group site name",
                dynamicPortalLayoutMatcher.matches(SiteKey.group(GROUP_SITE_NAME), PORTAL_SITE_NAME));
  }

  @Test
  public void testNotMatchSiteType() {
    DynamicPortalLayoutMatcher dynamicPortalLayoutMatcher = mockMatcher(CURRENT_SITE_NAME_OK,
                                                                        SITE_NAME_MATCHER_OK,
                                                                        SITE_TYPE_MATCHER_KO);

    assertFalse("Shouldn't match group site name",
                dynamicPortalLayoutMatcher.matches(SiteKey.group(GROUP_SITE_NAME), PORTAL_SITE_NAME));
  }

  @Test
  public void testNotMatchCurrentSiteName() {
    DynamicPortalLayoutMatcher dynamicPortalLayoutMatcher = mockMatcher(CURRENT_SITE_NAME_KO,
                                                                        SITE_NAME_MATCHER_OK,
                                                                        SITE_TYPE_MATCHER_OK);

    assertFalse("Shouldn't match group site name",
                dynamicPortalLayoutMatcher.matches(SiteKey.group(GROUP_SITE_NAME), PORTAL_SITE_NAME));
  }

  private DynamicPortalLayoutMatcher mockMatcher(String currentSiteNameRegex, String siteNameRegex, String siteTypeRegex) {
    DynamicPortalLayoutMatcher dynamicPortalLayoutMatcher = new DynamicPortalLayoutMatcher();
    dynamicPortalLayoutMatcher.setCurrentSiteNameRegex(currentSiteNameRegex);
    dynamicPortalLayoutMatcher.setSiteNameRegex(siteNameRegex);
    dynamicPortalLayoutMatcher.setSiteTypeRegex(siteTypeRegex);
    return dynamicPortalLayoutMatcher;
  }

}
