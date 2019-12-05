package org.exoplatform.portal.resource;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This visitor is used in {@link SkinService#findSkins(SkinVisitor)} to filter
 * the returned results.
 *
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public interface SkinVisitor {

  /**
   * @param portalSkins
   * @param skinConfigs
   * @return List of {@link SkinConfig} after calling {@link SkinVisitor}
   */
  Collection<SkinConfig> getSkins(Set<Entry<SkinKey, SkinConfig>> portalSkins, Set<Entry<SkinKey, SkinConfig>> skinConfigs);
}
