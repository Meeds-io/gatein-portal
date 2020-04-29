package org.exoplatform.commons.utils;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

public class TimeZoneUtilsTest {

  @Test
  public void testGetTimezones() {
    List<TimeZone> timezones = TimeZoneUtils.getTimeZones();
    assertNotNull(timezones);
    assertTrue("List timezones must cover at least 24 timezones", timezones.size() > 24);
    int oldOffset = -12 * 3600 * 1000;
    for (TimeZone timeZone : timezones) {
      assertTrue(timeZone.getRawOffset() > oldOffset);
      oldOffset = timeZone.getRawOffset();
    }
  }

  @Test
  public void testGetTimezoneDisplayName() {
    List<TimeZone> timezones = TimeZoneUtils.getTimeZones();
    for (TimeZone timeZone : timezones) {
      String timeZoneDisplay = TimeZoneUtils.getTimeZoneDisplay(timeZone, Locale.ENGLISH);
      assertNotNull(timeZoneDisplay);
      assertTrue(timeZoneDisplay.startsWith("(GMT"));
    }
  }

}
