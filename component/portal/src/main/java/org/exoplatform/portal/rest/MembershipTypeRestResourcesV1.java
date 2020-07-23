package org.exoplatform.portal.rest;

import java.util.Collection;
import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.MembershipTypeImpl;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import io.swagger.annotations.*;

@Path("v1/membershipTypes")
public class MembershipTypeRestResourcesV1 implements ResourceContainer {

  private OrganizationService organizationService;

  private UserACL             userACL;

  public MembershipTypeRestResourcesV1(OrganizationService organizationService, UserACL userACL) {
    this.organizationService = organizationService;
    this.userACL = userACL;
  }

  @GET
  @RolesAllowed("administrators")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Gets membership types",
      httpMethod = "GET",
      response = Response.class,
      produces = MediaType.APPLICATION_JSON,
      notes = "This returns the list of membership types"
  )
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 401, message = "User not authorized to call this endpoint"),
          @ApiResponse(code = 500, message = "Internal server error")
      }
  )
  public Response getMembershipType() throws Exception {
    Collection<MembershipType> membershipTypes = organizationService.getMembershipTypeHandler().findMembershipTypes();
    return Response.ok(membershipTypes).build();
  }

  @POST
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Creates a new membership type",
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
  public Response createMembershipType(MembershipTypeImpl membershipType) throws Exception {
    if (membershipType == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership type object is required").build();
    }
    if (StringUtils.isBlank(membershipType.getName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:MANDATORY").build();
    } else if (membershipType.getName().length() > 30 || membershipType.getName().length()<3) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:LENGTH_INVALID").build();
    }
    if (StringUtils.isBlank(membershipType.getDescription())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("DESCRIPTION:MANDATORY").build();
    } else if (membershipType.getDescription().length() > 255 || membershipType.getDescription().length()<3) {
      return Response.status(Response.Status.BAD_REQUEST).entity("DESCRIPTION:LENGTH_INVALID").build();
    }
    if (organizationService.getMembershipTypeHandler().findMembershipType(membershipType.getName()) != null) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("NAME:ALREADY_EXISTS")
                     .build();
    }
    membershipType.setCreatedDate(new Date());
    membershipType.setModifiedDate(new Date());
    membershipType.setOwner(ConversationState.getCurrent().getIdentity().getUserId());

    organizationService.getMembershipTypeHandler().createMembershipType(membershipType, true);
    return Response.noContent().build();
  }

  @PUT
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Updates an existing membership type",
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
  public Response updateMembershipType(MembershipTypeImpl membershipType) throws Exception {
    if (membershipType == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Membership type object is required").build();
    }
    if (StringUtils.isBlank(membershipType.getName())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:MANDATORY").build();
    }
    if (organizationService.getMembershipTypeHandler().findMembershipType(membershipType.getName()) == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("NAME:NOT_FOUND")
                     .build();
    }
    membershipType.setModifiedDate(new Date());
    membershipType.setOwner(ConversationState.getCurrent().getIdentity().getUserId());
    organizationService.getMembershipTypeHandler().saveMembershipType(membershipType, true);
    return Response.noContent().build();
  }

  @DELETE
  @Path("{membershipType}")
  @RolesAllowed("administrators")
  @ApiOperation(
      value = "Deletes an existing membership type",
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
  public Response deleteMembershipType(@ApiParam(value = "Membership type name", required = true) @PathParam(
    "membershipType"
  ) String membershipType) throws Exception {
    if (StringUtils.isBlank(membershipType)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("NAME:MANDATORY").build();
    }
    if ((userACL.getMandatoryMSTypes() != null && userACL.getMandatoryMSTypes().contains(membershipType))
        || StringUtils.equals(userACL.getMakableMT(), membershipType)) {
      return Response.status(Response.Status.BAD_REQUEST)
                     .entity("MandatoryMembershipType")
                     .build();
    }
    if (organizationService.getMembershipTypeHandler().findMembershipType(membershipType) == null) {
      return Response.status(Response.Status.NOT_FOUND)
                     .entity("NAME:NOT_FOUND")
                     .build();
    }
    organizationService.getMembershipTypeHandler().removeMembershipType(membershipType, true);
    return Response.noContent().build();
  }

}
