package org.exoplatform.commons.utils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TimeZoneUtils {

  private TimeZoneUtils() {
    // Class with static methods only
  }

  public static List<TimeZone> getTimeZones() {
    String[] ids = TimeZone.getAvailableIDs();
    Map<Integer, TimeZone> timeZoneByOffset = new HashMap<>();
    for (String id : ids) {
      TimeZone timeZone = TimeZone.getTimeZone(id);
      if (timeZone.getDisplayName().contains("GMT")) {
        continue;
      }
      timeZoneByOffset.put(timeZone.getRawOffset(), timeZone);
    }
    List<Integer> offsets = new ArrayList<>(timeZoneByOffset.keySet());
    Collections.sort(offsets);
    return offsets.stream().map(offset -> timeZoneByOffset.get(offset)).collect(Collectors.toList());
  }

  public static String getTimeZoneDisplay(TimeZone timeZone, Locale locale) {
    long hours = TimeUnit.MILLISECONDS.toHours(timeZone.getRawOffset());
    long minutes = TimeUnit.MILLISECONDS.toMinutes(timeZone.getRawOffset())
        - TimeUnit.HOURS.toMinutes(hours);
    minutes = Math.abs(minutes);
    String result = "";
    if (hours > 0) {
      result = String.format("(GMT +%02d:%02d) %s", hours, minutes, timeZone.getDisplayName(locale));
    } else {
      result = String.format("(GMT %02d:%02d) %s", hours, minutes, timeZone.getDisplayName(locale));
    }
    return result;
  }

}
