package org.exoplatform.portal.rest;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.rest.model.GroupRestEntity;
import org.exoplatform.portal.rest.model.MembershipRestEntity;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.idm.MembershipImpl;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.organization.search.GroupSearchService;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.swagger.annotations.*;

@Path("v1/groups")
public class GroupRestResourcesV1 implements ResourceContainer {

  public static final int     DEFAULT_LIMIT  = 20;

  public static final int     DEFAULT_OFFSET = 0;

  private GroupSearchService  groupSearchService;

  private OrganizationService organizationService;

  public GroupRestResourcesV1(OrganizationService organizationService, GroupSearchService groupSearchService) {
    this.organizationService = organizationService;
    this.groupSearchService = groupSearchService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(
      value = "Gets groups",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns the list of groups containing the given search text"
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 500, message = "Internal server error") }
  )
  public Response getGroups(@Context UriInfo uriInfo,
                            @ApiParam(value = "Search text to filter groups") @QueryParam(
                              "q"
                            ) String q,
                            @ApiParam(value = "Offset") @QueryParam(
                              "offset"
                            ) int offset,
                            @ApiParam(value = "Limit") @QueryParam(
                              "limit"
                            ) int limit,
                            @ApiParam(value = "Whether build tree until results or not", defaultValue = "false") @QueryParam(
                              "tree"
                            ) boolean buildTree,
                            @QueryParam(
                              "returnSize"
                            ) boolean returnSize,
                            @QueryParam("expand") String expand) throws Exception {
    offset = offset > 0 ? offset : DEFAULT_OFFSET;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    int size = groupSearchService.searchGroups(q).getSize();
    limit = limit < size ? limit : size;
    Group[] groups = groupSearchService.searchGroups(q).load(offset, limit);
    List<GroupRestEntity> listAllGroups = Arrays.stream(groups)
                                                .map(group -> new GroupRestEntity(group))
                                                .collect(Collectors.toList());
    CollectionEntity<GroupRestEntity> result = null;
    if (buildTree) {
      List<GroupRestEntity> rootGroups = new ArrayList<>();
      Map<String, GroupRestEntity> groupsById = new HashMap<>();
      for (GroupRestEntity groupRestEntity : listAllGroups) {
        buildTree(rootGroups, groupsById, groupRestEntity);
      }
      result = new CollectionEntity<>(rootGroups,
                                      offset,
                                      limit,
                                      rootGroups.size());
    } else {
      result = new CollectionEntity<>(listAllGroups,
                                      offset,
                                      limit,
                                      size);
    }
    return Response.ok(result).build();
  }

  @GET
  @Path("tree")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(
      value = "Gets groups tree",
      httpMethod = "GET",
      response = Response.class,
      notes = "This returns the list of groups children containing the given search text"
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 500, message = "Internal server error") }
  )
  public Response getGroupsTree(@Context UriInfo uriInfo,
                                @ApiParam(value = "Parent groupId to search") @QueryParam(
                                  "parentId"
                                ) String parentId,
                                @ApiParam(value = "Search text to filter groups") @QueryParam(
                                  "q"
                                ) String q,
                                @ApiParam(value = "Offset") @QueryParam(
                                  "offset"
                                ) int offset,
                                @ApiParam(value = "Limit") @QueryParam(
                                  "limit"
                                ) int limit,
                                @QueryParam(
                                  "returnSize"
                                ) boolean returnSize) throws Exception {
    offset = offset > 0 ? offset : DEFAULT_OFFSET;
    limit = limit > 0 ? limit : DEFAULT_LIMIT;

    Group parentGroup = null;
    if (StringUtils.isNotBlank(parentId)) {
      parentGroup = organizationService.getGroupHandler().findGroupById(parentId);
      if (parentGroup == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    }

    ListAccess<Group> childrenGroupsListAccess = organizationService.getGroupHandler().findGroupChildren(parentGroup, q);

    int totalSize = childrenGroupsListAccess.getSize();
    int limitToFetch = limit;
    if (totalSize < (offset + limitToFetch)) {
      limitToFetch = totalSize - offset;
    }
    Group[] groups = null;
    if (limitToFetch <= 0) {
      groups = new Group[0];
    } else {
      groups = childrenGroupsListAccess.load(offset, limitToFetch);
      if (!returnSize) {
        totalSize = 0;
      }
    }
    List<Group> groupsList = Arrays.asList(groups);
    CollectionEntity<Group> result = new CollectionEntity<>(groupsList,
                                                            offset,
                                                            limit,
                                                            totalSize);
    return Response.ok(result).build();
  }

  @POST
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Creates a new group",
      httpMethod = "POST",
      response = Response.class,
      consumes = MediaType.APPLICATION_JSON
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 204, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Bad request"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 500, message = "Internal server error")
      }
  )
  public Response createGroup(GroupImpl group) throws Exception {
    if (group == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Group object is required").build();
    }
    if (StringUtils.isBlank(group.getGroupName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:MANDATORY").build();
    }
    if (StringUtils.isBlank(group.getLabel())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("LABEL:MANDATORY").build();
    }
    String groupId = (group.getParentId() == null ? "" : (group.getParentId() + "/")) + group.getGroupName();
    if (organizationService.getGroupHandler().findGroupById(groupId) != null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("NAME:ALREADY_EXISTS")
                     .build();
    }
    Group parent = null;
    if (group.getParentId() != null) {
      parent = organizationService.getGroupHandler().findGroupById(group.getParentId());
      if (parent == null) {
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity("PARENT:NOT_FOUND")
                       .build();
      }
    }

    organizationService.getGroupHandler().addChild(parent, group, true);
    return Response.noContent().build();
  }

  @PUT
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Updates an existing Group",
      httpMethod = "PUT",
      response = Response.class,
      consumes = MediaType.APPLICATION_JSON
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 204, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Bad request"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 500, message = "Internal server error")
      }
  )
  public Response updateGroup(GroupImpl group) throws Exception {
    if (group == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Group object is required").build();
    }
    if (StringUtils.isBlank(group.getId()) || organizationService.getGroupHandler().findGroupById(group.getId()) == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("ID:NOT_FOUND")
                     .build();
    }
    organizationService.getGroupHandler().saveGroup(group, true);
    return Response.noContent().build();
  }

  @DELETE
  @RolesAllowed("administrators")
  @ApiOperation(
      value = "Deletes an existing Group",
      httpMethod = "DELETE",
      response = Response.class
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 204, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Bad request"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 500, message = "Internal server error")
      }
  )
  public Response deleteGroup(@ApiParam(value = "Group id", required = true) @QueryParam(
    "groupId"
  ) String groupId) throws Exception {
    if (StringUtils.isBlank(groupId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("ID:MANDATORY").build();
    }
    Group group = organizationService.getGroupHandler().findGroupById(groupId);
    if (group == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("ID:NOT_FOUND")
                     .build();
    }
    organizationService.getGroupHandler().removeGroup(group, true);
    return Response.noContent().build();
  }

  @GET
  @Path("memberships")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(
      value = "Gets Group memberships list",
      httpMethod = "GET",
      response = Response.class
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 404, message = "Group not found"),
          @ApiResponse(code = 500, message = "Internal server error due to data encoding"),
      }
  )
  public Response getGroupMemberships(
                                      @ApiParam(value = "Group identifier", required = true) @QueryParam(
                                        "groupId"
                                      ) String groupId,
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

    Group group = organizationService.getGroupHandler().findGroupById(groupId);
    if (group == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    ListAccess<Membership> membershipsByGroup = organizationService.getMembershipHandler().findAllMembershipsByGroup(group);
    int totalSize = membershipsByGroup.getSize();
    Membership[] memberships;
    int limitToFetch = limit;
    if (totalSize < (offset + limitToFetch)) {
      limitToFetch = totalSize - offset;
    }
    List<MembershipRestEntity> membershipEntities = new ArrayList<>();
    if (limitToFetch > 0) {
      memberships = membershipsByGroup.load(offset, limitToFetch);
      for (Membership membership : memberships) {
        User user = organizationService.getUserHandler().findUserByName(membership.getUserName(), UserStatus.ANY);
        membershipEntities.add(new MembershipRestEntity(membership, group, user));
      }
      if (!returnSize) {
        totalSize = 0;
      }
    }

    return Response.ok(new CollectionEntity<>(membershipEntities, offset, limit, totalSize)).build();
  }

  @POST
  @Path("memberships")
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Creates a new membership",
      httpMethod = "POST",
      response = Response.class,
      consumes = MediaType.APPLICATION_JSON
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 204, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Bad request"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 500, message = "Internal server error")
      }
  )
  public Response createMembership(MembershipImpl membership) throws Exception {
    if (membership == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership object is required").build();
    }
    if (StringUtils.isBlank(membership.getGroupId())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("GROUP_ID:MANDATORY").build();
    }
    if (StringUtils.isBlank(membership.getUserName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("USER:MANDATORY").build();
    }
    if (StringUtils.isBlank(membership.getMembershipType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("MEMBERSHIP_TYPE:MANDATORY").build();
    }
    if (organizationService.getMembershipHandler().findMembership(membership.getId()) != null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MEMBERSHIP:ALREADY_EXISTS")
                     .build();
    }
    User user = organizationService.getUserHandler().findUserByName(membership.getUserName(), UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("USER:NOT_FOUND")
                     .build();
    }
    Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
    if (group == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("GROUP:NOT_FOUND")
                     .build();
    }
    MembershipType membershipType = organizationService.getMembershipTypeHandler()
                                                       .findMembershipType(membership.getMembershipType());
    if (membershipType == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MEMBERSHIP_TYPE:NOT_FOUND")
                     .build();
    }

    organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
    return Response.noContent().build();
  }

  @PUT
  @Path("memberships")
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Updates an existing membership",
      httpMethod = "PUT",
      response = Response.class,
      consumes = MediaType.APPLICATION_JSON
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 204, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Bad request"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 500, message = "Internal server error")
      }
  )
  public Response updateMembership(@ApiParam(
      value = "Membership identifier with format: MEMBERSHIP_TYPE:GROUP_ID:USER_NAME", required = true
  ) @QueryParam(
    "membershipId"
  ) String membershipId, MembershipImpl membership) throws Exception {
    if (membership == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership object is required").build();
    }
    if (StringUtils.isBlank(membershipId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership identifier object is required").build();
    }
    if (StringUtils.isBlank(membership.getGroupId())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("GROUP_ID:MANDATORY").build();
    }
    if (StringUtils.isBlank(membership.getUserName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("USER:MANDATORY").build();
    }
    if (StringUtils.isBlank(membership.getMembershipType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("MEMBERSHIP_TYPE:MANDATORY").build();
    }
    Membership oldMembership = organizationService.getMembershipHandler().findMembership(membershipId);
    if (oldMembership == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("MEMBERSHIP:NOT_FOUND")
                     .build();
    }
    if (membershipId.equals(membership.getId())) {
      return Response.noContent().build();
    }
    if (organizationService.getMembershipHandler().findMembership(membership.getId()) != null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MEMBERSHIP:ALREADY_EXISTS")
                     .build();
    }
    User user = organizationService.getUserHandler().findUserByName(membership.getUserName(), UserStatus.ANY);
    if (user == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("USER:NOT_FOUND")
                     .build();
    }
    Group group = organizationService.getGroupHandler().findGroupById(membership.getGroupId());
    if (group == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("GROUP:NOT_FOUND")
                     .build();
    }
    MembershipType membershipType = organizationService.getMembershipTypeHandler()
                                                       .findMembershipType(membership.getMembershipType());
    if (membershipType == null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MEMBERSHIP_TYPE:NOT_FOUND")
                     .build();
    }

    organizationService.getMembershipHandler().removeMembership(membershipId, true);
    organizationService.getMembershipHandler().linkMembership(user, group, membershipType, true);
    return Response.noContent().build();
  }

  @DELETE
  @Path("memberships")
  @RolesAllowed("administrators")
  @ApiOperation(
      value = "Deletes an existing membership",
      httpMethod = "DELETE",
      response = Response.class
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 204, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Bad request"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 500, message = "Internal server error")
      }
  )
  public Response deleteMembership(@ApiParam(
      value = "Membership identifier with format: MEMBERSHIP_TYPE:GROUP_ID:USER_NAME", required = true
  ) @QueryParam(
    "membershipId"
  ) String membershipId) throws Exception {
    if (StringUtils.isBlank(membershipId)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("MEMBERSHIP:MANDATORY").build();
    }
    if (organizationService.getMembershipHandler().findMembership(membershipId) == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("NAME:NOT_FOUND")
                     .build();
    }
    organizationService.getMembershipHandler().removeMembership(membershipId, true);
    return Response.noContent().build();
  }

  private void buildTree(List<GroupRestEntity> rootGroups,
                         Map<String, GroupRestEntity> groupsById,
                         GroupRestEntity groupRestEntity) throws Exception {
    groupsById.put(groupRestEntity.getId(), groupRestEntity);

    String parentId = groupRestEntity.getParentId();
    if (parentId == null) {
      if (!rootGroups.contains(groupRestEntity)) {
        rootGroups.add(groupRestEntity);
      }
      return;
    }
    GroupRestEntity parentGroupEntity = groupsById.get(parentId);
    if (parentGroupEntity == null) {
      Group parentGroup = organizationService.getGroupHandler().findGroupById(parentId);
      parentGroupEntity = new GroupRestEntity(parentGroup);
      groupsById.put(parentId, parentGroupEntity);
    }
    parentGroupEntity.addChild(groupRestEntity);
    buildTree(rootGroups, groupsById, parentGroupEntity);
  }

}
