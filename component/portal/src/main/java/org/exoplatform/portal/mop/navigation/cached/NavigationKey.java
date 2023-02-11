package org.exoplatform.portal.mop.navigation.cached;

import java.io.Serializable;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;

import lombok.Data;

@Data
public class NavigationKey implements Serializable {

  private static final long serialVersionUID = 186446668859416892L;

  private SiteType          type;

  private SiteKey           key;

  private Long              nodeId;

  public NavigationKey(SiteType type) {
    this.type = type;
  }

  public NavigationKey(SiteKey key, Long nodeId) {
    this.key = key;
    this.nodeId = nodeId;
  }
}
