package org.exoplatform.services.organization.search;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;

/**
 * This Service is used to centralize the search of users
 */
public interface UserSearchService {

  /**
   * Search users by a term and having status = ENABLED
   * 
   * @param term query string that will be used to search in user name
   * @return {@link ListAccess} of {@link User}
   * @throws Exception
   */
  default ListAccess<User> searchUsers(String term) throws Exception {
    return this.searchUsers(term, UserStatus.ENABLED);
  }

  /**
   * Search users by a term and its status: ENABLED, DISABLED or ALL
   * 
   * @param term query string that will be used to search in user name
   * @param userStatus user status of type {@link UserStatus}
   * @return {@link ListAccess} of {@link User}
   * @throws Exception
   */
  ListAccess<User> searchUsers(String term, UserStatus userStatus) throws Exception;
}
