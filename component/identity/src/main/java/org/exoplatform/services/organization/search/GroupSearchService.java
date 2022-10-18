package org.exoplatform.services.organization.search;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.Group;

/**
 * Service to search in groups
 */
public interface GroupSearchService {

  ListAccess<Group> searchGroups(String term) throws Exception;

}
