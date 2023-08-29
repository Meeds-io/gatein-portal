package org.exoplatform.portal.mop.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exoplatform.portal.mop.SiteType;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SiteRestEntity {

  private SiteType                  siteType;

  private String                    name;

  private String                    displayName;

  private String                    description;

  private List<Map<String, Object>> accessPermissions;

  private Map<String, Object>       editPermission;

  private boolean                   displayed;

  private int                       displayOrder;

  private boolean                   isDefaultSite;

  List<UserNodeRestEntity>          siteNavigations;

}
