package org.exoplatform.services.organization.search;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.ExtGroup;
import org.exoplatform.services.security.Identity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GroupSearchServiceImpl implements GroupSearchService {
  
  private OrganizationService organizationService;

  private static final String            ADMINISTRATOR_GROUP            = "/platform/administrators";


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
  public List<Group> findAllGroupsByKeyword(String keyword, List<String> excludedGroupsTypes, Identity identity) throws Exception {

    ListAccess<Group> allGroups;
    boolean isManager = identity.isMemberOf(ADMINISTRATOR_GROUP);
    if (StringUtils.isBlank(keyword)) {
      allGroups = organizationService.getGroupHandler().findGroupsByKeyword("");
    } else {
      String lowerCaseKeyword = keyword.toLowerCase();
      allGroups = organizationService.getGroupHandler().findGroupsByKeyword(lowerCaseKeyword);
    }
    List<Group> listAllGroups= null;
    Group[] groups = allGroups.load(0, allGroups.getSize());
    if (isManager){
      listAllGroups = Arrays.stream(groups)
              .filter(group -> !excludedGroupsTypes.contains(((ExtGroup) group).getGroupType()))
              .collect(Collectors.toList());
    } else {
      listAllGroups = Arrays.stream(groups)
              .filter(group -> identity.isMemberOf(group.getId()) && !excludedGroupsTypes.contains(((ExtGroup) group).getGroupType()))
              .collect(Collectors.toList());
    }

    return listAllGroups;
  }
}
