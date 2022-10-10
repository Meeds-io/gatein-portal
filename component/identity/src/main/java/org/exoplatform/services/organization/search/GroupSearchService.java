package org.exoplatform.services.organization.search;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.security.Identity;

import java.util.List;

/**
 * Service to search in groups
 */
public interface GroupSearchService {

  ListAccess<Group> searchGroups(String term) throws Exception;

  List<Group> findAllGroupsByKeyword(String keyword, List<String> excludedGroupsTypes, Identity identity) throws Exception;

}
