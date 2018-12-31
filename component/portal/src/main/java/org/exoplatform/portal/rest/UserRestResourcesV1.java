package org.exoplatform.portal.rest;

import java.util.Collection;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("v1/users")
public class UserRestResourcesV1 implements ResourceContainer {

  private OrganizationService organizationService;

  private UserACL             userACL;

  public UserRestResourcesV1(OrganizationService organizationService, UserACL userACL) {
    this.organizationService = organizationService;
    this.userACL = userACL;
  }

  /**
   * Get all groups, filter by name if exists.
   *
   * @return List of groups in json format.
   * @throws Exception
   */
  @GET
  @RolesAllowed("users")
  @Path("{id}")
  @ApiOperation(value = "Gets user", httpMethod = "GET", response = Response.class, notes = "This returns the list of groups containing the given search text, only if the authenticated user is a spaces administrator")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
      @ApiResponse(code = 404, message = "User not found"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getUser(@Context UriInfo uriInfo, @PathParam("id") String id) throws Exception {

    if (!userACL.isSuperUser() && !userACL.isUserInGroup(userACL.getAdminGroups())
        && !ConversationState.getCurrent().getIdentity().getUserId().equals(id)) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    User user = organizationService.getUserHandler().findUserByName(id);

    if (user == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    UserRestEntity userRestEntity = new UserRestEntity(user.getUserName(),
                                                       user.getFirstName(),
                                                       user.getLastName(),
                                                       user.getDisplayName(),
                                                       user.getEmail(),
                                                       false);

    Collection<Membership> memberships = organizationService.getMembershipHandler().findMembershipsByUser(id);
    if(memberships != null) {
      userRestEntity.setPlatformAdministrator(memberships.stream().anyMatch(membership ->
              membership.getGroupId().equals(userACL.getAdminGroups())));
    }

    return Response.ok(userRestEntity, MediaType.APPLICATION_JSON).build();
  }
}
