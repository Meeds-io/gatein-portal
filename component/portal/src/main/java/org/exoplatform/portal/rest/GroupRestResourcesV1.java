package org.exoplatform.portal.rest;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

  @GET
  @RolesAllowed("administrators")
  @ApiOperation(value = "Gets groups",
          httpMethod = "GET",
          response = Response.class,
          notes = "This returns the list of groups containing the given search text")
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

    int size = groupSearchService.searchGroups(q).getSize();
    limit = limit < size ? limit : size;
    Group[] groups = groupSearchService.searchGroups(q).load(offset, limit);
    List<Group> listAllGroups = Arrays.asList(groups);
    return Response.ok(listAllGroups, MediaType.APPLICATION_JSON).build();
  }
}
