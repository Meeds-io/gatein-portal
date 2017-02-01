package org.exoplatform.services.organization.search;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;

public class UserSearchServiceImpl implements UserSearchService {

  private OrganizationService organizationService;

  public UserSearchServiceImpl(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @Override
  public ListAccess<User> searchUsers(String keyword) throws Exception {
    if (StringUtils.isEmpty(keyword)) {
      return organizationService.getUserHandler().findAllUsers();
    } else {
      Query query = new Query();
      if (keyword.indexOf("*") < 0) {
        if (keyword.charAt(0) != '*')
          keyword = "*" + keyword;
        if (keyword.charAt(keyword.length() - 1) != '*')
          keyword += "*";
      }
      keyword = keyword.replace('?', '_');
      query.setUserName(keyword);
      return organizationService.getUserHandler().findUsersByQuery(query);
    }
  }

}
