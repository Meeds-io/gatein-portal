package org.exoplatform.portal.rest;

import java.util.Arrays;
import java.util.LinkedList;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.search.GroupSearchService;
import org.exoplatform.services.rest.resource.ResourceContainer;

@Path("v1/groups")
public class GroupRestResourcesV1 implements ResourceContainer {

  public static final int DEFAULT_LIMIT  = 20;

  public static final int DEFAULT_OFFSET = 0;

  private GroupSearchService groupSearchService;

  public GroupRestResourcesV1(GroupSearchService groupSearchService) {
    this.groupSearchService = groupSearchService;
  }

  /**
   * Get all groups, filter by name if exists.
   *
   * @param q value that an group's name match
   * @return List of groups in json format.
   * @throws Exception
   */
  @GET
  @RolesAllowed("administrators")
  @ApiOperation(value = "Gets groups",
          httpMethod = "GET",
          response = Response.class,
          notes = "This returns the list of groups containing the given search text, only if the authenticated user is a spaces administrator")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse (code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse (code = 500, message = "Internal server error") })
  public Response getGroups(@Context UriInfo uriInfo,
                            @ApiParam(value = "Search text to filter groups") @QueryParam("q") String q,
                            @ApiParam(value = "Offset") @QueryParam("offset") int offset,
                            @ApiParam(value = "Limit") @QueryParam("limit") int limit,
                            @QueryParam("returnSize") boolean returnSize,
                            @QueryParam("expand") String expand) throws Exception {
    offset = offset > 0 ? offset : DEFAULT_OFFSET;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    ListAccess<Group> groups = groupSearchService.searchGroups(q);
    limit = limit < groups.getSize() ? limit : groups.getSize();
    Group [] allGroups = groups.load(offset, limit);
    LinkedList<String> listAllGroups = new LinkedList(Arrays.asList(allGroups));
    
    return Response.ok(listAllGroups, MediaType.APPLICATION_JSON).build();
  }
}
