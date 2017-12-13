package org.exoplatform.services.organization.search;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.User;

/**
 * This Service is used to centralize the search of users
 */
public interface UserSearchService {

  ListAccess<User> searchUsers(String term) throws Exception;
}
