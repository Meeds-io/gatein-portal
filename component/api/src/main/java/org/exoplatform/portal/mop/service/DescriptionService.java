package org.exoplatform.portal.mop.service;

import org.exoplatform.portal.mop.State;

import java.util.Locale;
import java.util.Map;

public interface DescriptionService {

  /**
   * Returns a map containing all the descriptions of an object or null if the
   * object is not internationalized.
   *
   * @param id the object id
   * @return the map the description map
   */
  Map<Locale, State> getDescriptions(String id);

  /**
   * Updates the description of the specified object or remove the
   * internationalized characteristic of the object if the description map is
   * null.
   *
   * @param id the object id
   * @param descriptions the new descriptions
   */
  void setDescriptions(String id, Map<Locale, org.exoplatform.portal.mop.State> descriptions);

}
