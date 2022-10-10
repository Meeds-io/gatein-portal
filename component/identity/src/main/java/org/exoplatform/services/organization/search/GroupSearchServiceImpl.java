package org.exoplatform.services.organization.search;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.ExtGroup;
import org.exoplatform.services.security.Identity;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

  @Override
  public Collection<Group> findAllGroupsByKeyword(String keyword, List<String> excludedGroupsTypes, Identity identity) throws Exception {
    if (StringUtils.isBlank(keyword)) {
      return organizationService.getGroupHandler().findAllGroupsByKeyword("", excludedGroupsTypes, identity);
    } else {
      String lowerCaseKeyword = keyword.toLowerCase();
      return organizationService.getGroupHandler().findAllGroupsByKeyword(lowerCaseKeyword, excludedGroupsTypes, identity);
    }

  }
}
