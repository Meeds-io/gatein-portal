package org.exoplatform.portal.rest;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.rest.model.MembershipRestEntity;
import org.exoplatform.portal.rest.model.UserRestEntity;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.search.UserSearchService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import io.swagger.annotations.*;
import io.swagger.jaxrs.PATCH;
import org.exoplatform.web.login.recovery.ChangePasswordConnector;
import org.exoplatform.web.login.recovery.PasswordRecoveryService;

@Path("v1/users")
public class UserRestResourcesV1 implements ResourceContainer {

  public static final int                DEFAULT_LIMIT                  = 10;

  public static final String             PASSWORD_UNKNOWN_ERROR_CODE    = "PASSWORD_UNKNOWN_ERROR";

  public static final String             USER_NOT_FOUND_ERROR_CODE      = "USER_NOT_FOUND";

  public static final String             WRONG_USER_PASSWORD_ERROR_CODE = "WRONG_USER_PASSWORD";
  
  public static final String             CHANGE_PASSWORD_NOT_ALLOWED = "CHANGE_PASSWORD_NOT_ALLOWED";

  private static final String            ADMINISTRATOR_GROUP            = "/platform/administrators";

  private static final String            DELEGATED_GROUP                = "/platform/delegated";

    public static final String           UNCHANGED_NEW_PASSWORD_ERROR_CODE = "UNCHANGED_NEW_PASSWORD";

  public static final UserFieldValidator USERNAME_VALIDATOR             = new UserFieldValidator("userName", true, false);

  public static final UserFieldValidator EMAIL_VALIDATOR                = new UserFieldValidator("emailAddress", false, false);

  public static final UserFieldValidator LASTNAME_VALIDATOR             = new UserFieldValidator("lastName", false, true);

  public static final UserFieldValidator FIRSTNAME_VALIDATOR            = new UserFieldValidator("firstName", false, true);

  public static final UserFieldValidator PASSWORD_VALIDATOR             = new UserFieldValidator("password", false, false, 8, 255);

  private OrganizationService            organizationService;

  private UserSearchService              userSearchService;

  private UserACL                        userACL;
  
  private PasswordRecoveryService        passwordRecoveryService;

  public UserRestResourcesV1(OrganizationService organizationService,
                             UserSearchService userSearchService,
                             UserACL userACL,
                             PasswordRecoveryService passwordRecoveryService) {
    this.organizationService = organizationService;
    this.userSearchService = userSearchService;
    this.userACL = userACL;
    this.passwordRecoveryService = passwordRecoveryService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(
      value = "Gets all users",
      httpMethod = "GET",
      response = Response.class
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 500, message = "Internal server error due to data encoding"),
      }
  )
  public Response getUsers(
                           @ApiParam(
                               value = "User name information to filter, ex: user name, last name, first name or full name",
                               required = false
                           ) @QueryParam("q") String q,
                           @ApiParam(value = "User status : ANY, ENABLED or DISABLED", required = false, defaultValue = "ANY") @QueryParam("status") String status,
                           @ApiParam(value = "Offset", required = false, defaultValue = "0") @QueryParam(
                             "offset"
                           ) int offset,
                           @ApiParam(value = "Limit", required = false, defaultValue = "10") @QueryParam(
                             "limit"
                           ) int limit,
                           @ApiParam(value = "Returning the number of users found or not", defaultValue = "false") @QueryParam(
                             "returnSize"
                           ) boolean returnSize) throws Exception {

    offset = offset > 0 ? offset : 0;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    UserStatus userStatus = StringUtils.isBlank(status) ||
        StringUtils.equalsIgnoreCase("ALL", status) ||
        StringUtils.equalsIgnoreCase("ANY", status) ? UserStatus.ANY : UserStatus.valueOf(status.toUpperCase());

    User[] users;
    int totalSize = 0;
    if (StringUtils.isBlank(q)) {
      ListAccess<User> allUsersListAccess = organizationService.getUserHandler().findAllUsers(userStatus);
      totalSize = allUsersListAccess.getSize();
      int limitToFetch = limit;
      if (totalSize < (offset + limitToFetch)) {
        limitToFetch = totalSize - offset;
      }
      if (limitToFetch <= 0) {
        users = new User[0];
      } else {
        users = allUsersListAccess.load(offset, limitToFetch);
        if (!returnSize) {
          totalSize = 0;
        }
      }
    } else {
      ListAccess<User> usersListAccess = userSearchService.searchUsers(q, userStatus);
      totalSize = usersListAccess.getSize();
      int limitToFetch = limit;
      if (totalSize < (offset + limitToFetch)) {
        limitToFetch = totalSize - offset;
      }
      if (limitToFetch <= 0) {
        users = new User[0];
      } else {
        users = usersListAccess.load(offset, limitToFetch);
        if (!returnSize) {
          totalSize = 0;
        }
      }
    }
    List<UserRestEntity> userEntities = Arrays.stream(users)
                                              .map(this::toEntity)
                                              .collect(Collectors.toList());
    CollectionEntity<UserRestEntity> result = new CollectionEntity<>(userEntities,
                                                                     offset,
                                                                     limit,
                                                                     totalSize);
    return Response.ok(result).build();
  }

  @POST
  @RolesAllowed("administrators")
  @ApiOperation(
      value = "Create new user",
      httpMethod = "GET",
      response = Response.class
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Invalid query input"),
          @ApiResponse(code = 500, message = "Internal server error due to data encoding"),
      }
  )
  public Response createUser(@Context HttpServletRequest request, UserRestEntity userEntity) throws Exception {
    if (userEntity == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("empty user object").build();
    }

    String userName = userEntity.getUserName();
    String email = userEntity.getEmail();
    String firstName = userEntity.getFirstName();
    String lastName = userEntity.getLastName();
    String password = userEntity.getPassword();

    Locale locale = request == null ? Locale.ENGLISH : request.getLocale();

    String errorMessage = USERNAME_VALIDATOR.validate(locale, userName);
    if (StringUtils.isNotBlank(errorMessage)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("USERNAME:" + errorMessage).build();
    }

    errorMessage = PASSWORD_VALIDATOR.validate(locale, password);
    if (StringUtils.isNotBlank(errorMessage)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("PASSWORD:" + errorMessage).build();
    }

    errorMessage = LASTNAME_VALIDATOR.validate(locale, lastName);
    if (StringUtils.isNotBlank(errorMessage)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("LASTNAME:" + errorMessage).build();
    }

    errorMessage = FIRSTNAME_VALIDATOR.validate(locale, firstName);
    if (StringUtils.isNotBlank(errorMessage)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("FIRSTNAME:" + errorMessage).build();
    }

    errorMessage = EMAIL_VALIDATOR.validate(locale, email);
    if (StringUtils.isNotBlank(errorMessage)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("EMAIL:" + errorMessage).build();
    }

    User user = organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    if (user != null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("USERNAME:ALREADY_EXISTS").build();
    }

    // Check if mail address is already used
    Query query = new Query();
    query.setEmail(email);
    if (organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY).getSize() > 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("EMAIL:ALREADY_EXISTS").build();
    }

    user = organizationService.getUserHandler().createUserInstance(userName);
    user.setEmail(email);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setPassword(password);
    try {
       organizationService.getUserHandler().createUser(user, true);
    } catch (ObjectAlreadyExistsException objectAlreadyExistsException) {
       return Response.status(Response.Status.BAD_REQUEST).entity("USERNAME:ALREADY_EXISTS_AS_DELETED").build();
    }

    if (!user.isEnabled()) {
      organizationService.getUserHandler().setEnabled(userName, false, true);
    }

    return Response.noContent().build();
  }

  @PUT
  @RolesAllowed("users")
  @ApiOperation(
      value = "Update an existing user",
      httpMethod = "GET",
      response = Response.class
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Invalid query input"),
          @ApiResponse(code = 500, message = "Internal server error due to data encoding"),
      }
  )
  public Response updateUser(@Context HttpServletRequest request, UserRestEntity userEntity) throws Exception {

    if (!userACL.isUserInGroup(DELEGATED_GROUP) && !userACL.isUserInGroup(ADMINISTRATOR_GROUP)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    if (userEntity == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("empty user object").build();
    }

    String userName = userEntity.getUserName();
    String email = userEntity.getEmail();
    String firstName = userEntity.getFirstName();
    String lastName = userEntity.getLastName();
    String password = userEntity.getPassword();
    boolean enabled = userEntity.isEnabled();

    Locale locale = request == null ? Locale.ENGLISH : request.getLocale();

    if (StringUtils.isNotBlank(password)) {
      String errorMessage = PASSWORD_VALIDATOR.validate(locale, password);
      if (StringUtils.isNotBlank(errorMessage)) {
        return Response.status(Response.Status.BAD_REQUEST).entity("PASSWORD:" + errorMessage).build();
      }
    }

    User user = organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    String errorMessage = LASTNAME_VALIDATOR.validate(locale, lastName);
    if (StringUtils.isNotBlank(errorMessage)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("LASTNAME:" + errorMessage).build();
    }

    errorMessage = FIRSTNAME_VALIDATOR.validate(locale, firstName);
    if (StringUtils.isNotBlank(errorMessage)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("FIRSTNAME:" + errorMessage).build();
    }

    errorMessage = EMAIL_VALIDATOR.validate(locale, email);
    if (StringUtils.isNotBlank(errorMessage)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("EMAIL:" + errorMessage).build();
    }

    // Check if mail address is already used
    Query query = new Query();
    query.setEmail(email);
    if (!StringUtils.equals(user.getEmail(), email)
        && organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY).getSize() > 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("EMAIL:ALREADY_EXISTS").build();
    }

    if (!StringUtils.equals(email, user.getEmail())
        || !StringUtils.equals(lastName, user.getLastName())
        || !StringUtils.equals(firstName, user.getFirstName())) {
      user.setEmail(email);
      user.setFirstName(firstName);
      user.setLastName(lastName);
      user.setPassword(password);
      user.setDisplayName(firstName+" "+lastName);
      organizationService.getUserHandler().saveUser(user, true);
    }

    if (StringUtils.isNotBlank(password)) {
      // we save the password separatly from saving the user, because when changing
      // password, we need to remove rememberme token
      // related to this password as they are no more valid
      passwordRecoveryService.getActiveChangePasswordConnector().changePassword(userName, password);
    }

    if (user.isEnabled() != enabled) {
      if (!enabled) {
        String currentUsername = getCurrentUsername();
        if (StringUtils.equals(currentUsername, user.getUserName())) {
          return Response.status(Response.Status.BAD_REQUEST).entity("SelfDisable").build();
        }
        if (StringUtils.equals(userACL.getSuperUser(), user.getUserName())) {
          return Response.status(Response.Status.BAD_REQUEST).entity("DisableSuperUser").build();
        }
      }
      organizationService.getUserHandler().setEnabled(userName, enabled, true);
    }

    return Response.noContent().build();
  }

  @DELETE
  @RolesAllowed("administrators")
  @Path("{id}")
  @ApiOperation(value = "Deletes a user identified by its id", httpMethod = "DELETE", response = Response.class)
  @ApiResponses(
      value = {
          @ApiResponse(code = 204, message = "Request fulfilled"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 404, message = "User not found"),
          @ApiResponse(code = 500, message = "Internal server error"),
      }
  )
  public Response deleteUser(@ApiParam(value = "User name identifier", required = true) @PathParam(
    "id"
  ) String userName) throws Exception {
    User user = organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    String currentUsername = getCurrentUsername();
    if (StringUtils.equals(currentUsername, user.getUserName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("SelfDelete").build();
    }
    if (StringUtils.equals(userACL.getSuperUser(), user.getUserName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("DeleteSuperUser").build();
    }
    organizationService.getUserHandler().removeUser(userName, true);
    return Response.noContent().build();
  }

  @GET
  @RolesAllowed("users")
  @Path("{id}")
  @ApiOperation(value = "Gets a user identified by its id", httpMethod = "GET", response = Response.class)
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 404, message = "User not found"),
          @ApiResponse(code = 500, message = "Internal server error"),
      }
  )
  public Response getUser(@Context UriInfo uriInfo,
                          @ApiParam(value = "User name identifier", required = true) @PathParam(
                            "id"
                          ) String id) throws Exception {

    if (!userACL.isSuperUser() && !userACL.isUserInGroup(userACL.getAdminGroups())
        && !ConversationState.getCurrent().getIdentity().getUserId().equals(id)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    User user = organizationService.getUserHandler().findUserByName(id, UserStatus.ANY);

    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    UserRestEntity userRestEntity = toEntity(user);

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
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 404, message = "User not found"),
          @ApiResponse(code = 500, message = "Internal server error")
      }
  )
  public Response changePassword(
                                 @Context HttpServletRequest request,
                                 @ApiParam(value = "username to change his password", required = true) @PathParam(
                                   "id"
                                 ) String username,
                                 @ApiParam(value = "Current user password", required = true) @FormParam(
                                   "currentPassword"
                                 ) String currentPassword,
                                 @ApiParam(value = "New user password", required = true) @FormParam(
                                   "newPassword"
                                 ) String newPassword) {
    boolean isAdmin = userACL.isSuperUser() || userACL.isUserInGroup(userACL.getAdminGroups());
    boolean isSameUser = ConversationState.getCurrent().getIdentity().getUserId().equals(username);
    if (!isAdmin && !isSameUser) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    try {
      UserHandler userHandler = organizationService.getUserHandler();
      User user = userHandler.findUserByName(username, UserStatus.ANY);
      if (user == null) {
        return Response.serverError().entity(USER_NOT_FOUND_ERROR_CODE).build();
      }
  
      if (!passwordRecoveryService.allowChangePassword(user.getUserName())) {
        return Response.serverError().entity(CHANGE_PASSWORD_NOT_ALLOWED).build();
      }
  
      if (isSameUser && !userHandler.authenticate(username, currentPassword)) {
        return Response.serverError().entity(WRONG_USER_PASSWORD_ERROR_CODE).build();
      }

      if (isSameUser && userHandler.authenticate(username, newPassword))  {
        return Response.serverError().entity(UNCHANGED_NEW_PASSWORD_ERROR_CODE).build();
      }
  
      Locale locale = request.getLocale();
  
      String errorMessage = PASSWORD_VALIDATOR.validate(locale, newPassword);
      if (StringUtils.isNotBlank(errorMessage)) {
        return Response.serverError().entity(errorMessage).build();
      }
  
      ChangePasswordConnector activeChangePasswordConnector = passwordRecoveryService.getActiveChangePasswordConnector();
      activeChangePasswordConnector.changePassword(user.getUserName(),newPassword);
      return Response.noContent().build();
    } catch (Exception e) {
      return Response.serverError().entity(PASSWORD_UNKNOWN_ERROR_CODE + ":" + e.getMessage()).build();
    }
  }

  @GET
  @Path("{id}/memberships")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(
      value = "Gets User memberships list",
      httpMethod = "GET",
      response = Response.class
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 404, message = "User not found"),
          @ApiResponse(code = 500, message = "Internal server error due to data encoding"),
      }
  )
  public Response getUserMemberships(
                                     @ApiParam(value = "User name identifier", required = true) @PathParam(
                                       "id"
                                     ) String userName,
                                     @ApiParam(value = "Offset", required = false, defaultValue = "0") @QueryParam(
                                       "offset"
                                     ) int offset,
                                     @ApiParam(value = "Limit", required = false, defaultValue = "20") @QueryParam(
                                       "limit"
                                     ) int limit,
                                     @ApiParam(value = "Returning the number of users found or not", defaultValue = "false") @QueryParam(
                                       "returnSize"
                                     ) boolean returnSize) throws Exception {

    offset = offset > 0 ? offset : 0;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    User user = organizationService.getUserHandler().findUserByName(userName, UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    ListAccess<Membership> membershipsByUser = organizationService.getMembershipHandler().findAllMembershipsByUser(user);
    int totalSize = membershipsByUser.getSize();
    Membership[] memberships;
    int limitToFetch = limit;
    if (totalSize < (offset + limitToFetch)) {
      limitToFetch = totalSize - offset;
    }
    List<MembershipRestEntity> membershipEntities = new ArrayList<>();
    if (limitToFetch > 0) {
      memberships = membershipsByUser.load(offset, limitToFetch);
      for (Membership membership : memberships) {
        Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
        membershipEntities.add(new MembershipRestEntity(membership, group, user));
      }
      if (!returnSize) {
        totalSize = 0;
      }
    }

    return Response.ok(new CollectionEntity<>(membershipEntities, offset, limit, totalSize)).build();
  }

  @GET
  @Path("isSuperUser")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(
      value = "Check if current user is a superUser",
      httpMethod = "GET",
      response = Response.class
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 500, message = "Internal server error due to data encoding"),
      }
  )
  public Response isSuperUser() {
    return Response.ok().entity("{\"isSuperUser\":\"" + userACL.isSuperUser() + "\"}").build();
  }

  @GET
  @Path("isDelegatedAdministrator")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(
          value = "Check if current user is a delegated administrator",
          httpMethod = "GET",
          response = Response.class
  )
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Request fulfilled"),
                  @ApiResponse(code = 500, message = "Internal server error due to data encoding"),
          }
  )
  public Response isDelegatedAdministrator() {
    boolean isDelegatedAdministrator = userACL.isUserInGroup(DELEGATED_GROUP) && !userACL.isUserInGroup(ADMINISTRATOR_GROUP);
    return Response.ok().entity("{\"result\":\"" + isDelegatedAdministrator + "\"}").build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("isSynchronizedUserAllowedToChangePassword")
  @RolesAllowed("users")
  @ApiOperation(value = "Check if synchronized user is allowed to change his password",
      httpMethod = "GET",
      response = Response.class,
      notes = "This can only be done by the logged in user.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error due to data encoding")})
  public Response isSynchronizedUserAllowedToChangePassword(@Context UriInfo uriInfo) throws Exception {

    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    User user = organizationService.getUserHandler().findUserByName(userId, UserStatus.ANY);
    if (user == null) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
    boolean allowChangePassword = user.isInternalStore() || (System.getProperty("exo.portal.allow.change.external.password") != null && Boolean.valueOf(System.getProperty("exo.portal.allow.change.external.password").toString()));
    return Response.ok().entity("{\"isSynchronizedUserAllowedToChangePassword\":\"" + allowChangePassword + "\"}").build();
  }

  private UserRestEntity toEntity(User user) {
    return new UserRestEntity(user.getUserName(),
                              user.getFirstName(),
                              user.getLastName(),
                              user.getDisplayName(),
                              user.getEmail(),
                              user.isEnabled(),
                              false);
  }

  public static String getCurrentUsername() {
    org.exoplatform.services.security.Identity currentIdentity =
                                                               ConversationState.getCurrent() == null ? null
                                                                                                      : ConversationState.getCurrent()
                                                                                                                         .getIdentity();
    if (currentIdentity == null) {
      return null;
    }
    return currentIdentity.getUserId();
  }

}
