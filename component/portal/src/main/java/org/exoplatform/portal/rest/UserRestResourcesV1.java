package org.exoplatform.portal.rest;

import java.util.Collection;
import java.util.regex.Pattern;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import io.swagger.annotations.*;
import io.swagger.jaxrs.PATCH;

@Path("v1/users")
public class UserRestResourcesV1 implements ResourceContainer {

  public static final String  PASSWORD_UNKNOWN_ERROR_CODE            = "PASSWORD_UNKNOWN_ERROR";

  public static final String  PASSWORD_REGEX_ERROR_CODE              = "PASSWORD_REGEX_ERROR";

  public static final String  PASSWORD_MAX_LENGTH_ERROR_CODE         = "PASSWORD_MAX_LENGTH";

  public static final String  PASSWORD_MIN_LENGTH_ERROR_CODE         = "PASSWORD_MIN_LENGTH";

  public static final String  PASSWORD_CUSTOM_VALIDATOR_MIN          = "gatein.validators.password.length.min";

  public static final String  PASSWORD_CUSTOM_VALIDATOR_MAX          = "gatein.validators.password.length.max";

  public static final String  PASSWORD_CUSTOM_VALIDATOR_REGEXP       = "gatein.validators.password.regexp";

  public static final String  PASSWORD_CUSTOM_VALIDATOR_REGEXP_ERROR = "gatein.validators.password.format.message";

  public static final String  USER_NOT_FOUND_ERROR_CODE              = "USER_NOT_FOUND";

  public static final String  WRONG_USER_PASSWORD_ERROR_CODE         = "WRONG_USER_PASSWORD";

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
    if (memberships != null) {
      userRestEntity.setPlatformAdministrator(memberships.stream()
                                                         .anyMatch(membership -> membership.getGroupId()
                                                                                           .equals(userACL.getAdminGroups())));
    }

    return Response.ok(userRestEntity, MediaType.APPLICATION_JSON).build();
  }

  @PATCH
  @RolesAllowed("users")
  @Path("{id}/changePassword")
  @ApiOperation(value = "Changes user password", httpMethod = "POST", response = Response.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
      @ApiResponse(code = 404, message = "User not found"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response changePassword(@ApiParam(value = "username to change his password", required = true) @PathParam("id") String username,
                                 @ApiParam(value = "Current user password", required = true) @FormParam("currentPassword") String currentPassword,
                                 @ApiParam(value = "New user password", required = true) @FormParam("newPassword") String newPassword) {
    boolean isAdmin = userACL.isSuperUser() || userACL.isUserInGroup(userACL.getAdminGroups());
    boolean isSameUser = ConversationState.getCurrent().getIdentity().getUserId().equals(username);
    if (!isAdmin && !isSameUser) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    try {
      UserHandler userHandler = organizationService.getUserHandler();
      User user = userHandler.findUserByName(username);
      if (user == null) {
        return Response.serverError().entity(USER_NOT_FOUND_ERROR_CODE).build();
      }

      if (isSameUser && !userHandler.authenticate(username, currentPassword)) {
        return Response.serverError().entity(WRONG_USER_PASSWORD_ERROR_CODE).build();
      }

      String errorMessage = checkCustomPasswordValidators(newPassword);
      if (StringUtils.isNotBlank(errorMessage)) {
        return Response.serverError().entity(errorMessage).build();
      }

      user.setPassword(newPassword);
      userHandler.saveUser(user, false);

      return Response.noContent().build();
    } catch (Exception e) {
      return Response.serverError().entity(PASSWORD_UNKNOWN_ERROR_CODE + ":" + e.getMessage()).build();
    }
  }

  private String checkCustomPasswordValidators(String newPassword) {
    String minLengthString = PropertyManager.getProperty(PASSWORD_CUSTOM_VALIDATOR_MIN);
    if (StringUtils.isNotBlank(minLengthString) && newPassword.length() < Integer.parseInt(minLengthString)) {
      return PASSWORD_MIN_LENGTH_ERROR_CODE + ":" + minLengthString;
    }
    String maxLengthString = PropertyManager.getProperty(PASSWORD_CUSTOM_VALIDATOR_MAX);
    if (StringUtils.isNotBlank(maxLengthString) && newPassword.length() > Integer.parseInt(maxLengthString)) {
      return PASSWORD_MAX_LENGTH_ERROR_CODE + ":" + maxLengthString;
    }
    String passwordRegex = PropertyManager.getProperty(PASSWORD_CUSTOM_VALIDATOR_REGEXP);
    if (StringUtils.isNotBlank(passwordRegex) && !Pattern.matches(passwordRegex, passwordRegex)) {
      String passwordRegexError = PropertyManager.getProperty(PASSWORD_CUSTOM_VALIDATOR_REGEXP_ERROR);
      return PASSWORD_REGEX_ERROR_CODE + ":" + passwordRegexError;
    }
    return null;
  }
}
