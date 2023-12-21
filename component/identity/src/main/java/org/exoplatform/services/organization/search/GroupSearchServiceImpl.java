package org.exoplatform.services.organization.search;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;

public class GroupSearchServiceImpl implements GroupSearchService {
  
  private OrganizationService organizationService;
  
  public GroupSearchServiceImpl(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @Override
  public ListAccess<Group> searchGroups(String keyword) throws Exception {
    if (StringUtils.isBlank(keyword)) {
      return organizationService.getGroupHandler().findGroupsByKeyword("");
    } else {
      String lowerCaseKeyword = keyword.toLowerCase();
      return organizationService.getGroupHandler().findGroupsByKeyword(lowerCaseKeyword);
    }
  }
}
