package org.exoplatform.services.organization.search;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;

public class GroupSearchServiceImpl implements GroupSearchService {
  
  private OrganizationService organizationService;
  
  public GroupSearchServiceImpl(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @Override
  public ListAccess<Group> searchGroups(String keyword) throws Exception {
    List<Group> groups = new LinkedList<>();
    if (StringUtils.isBlank(keyword)) {
      groups.addAll(organizationService.getGroupHandler().getAllGroups());
    } else {
      String lowerCaseKeyword = keyword.toLowerCase();
      for (Group group : organizationService.getGroupHandler().getAllGroups()) {
        if (group.getGroupName().toLowerCase().contains(lowerCaseKeyword)) {
          groups.add(group);
        }
      }
    }
    return new ListAccessImpl<>(Group.class, groups);
  }
}
