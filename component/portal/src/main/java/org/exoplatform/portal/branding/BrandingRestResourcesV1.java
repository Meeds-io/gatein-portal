/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.portal.branding;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.swagger.annotations.*;

@Path("/v1/platform/branding")
@Api(tags = "/v1/platform/branding", value = "/v1/platform/branding", description = "Managing branding information")
public class BrandingRestResourcesV1 implements ResourceContainer {
  private static final Log LOG = ExoLogger.getLogger(BrandingRestResourcesV1.class);
  
  private BrandingService brandingService;

  public BrandingRestResourcesV1(BrandingService brandingService) {
    this.brandingService = brandingService;
  }
  
  
  /**
   * @return global settings of Branding Company Name
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get Branding information",
  httpMethod = "GET",
  response = Response.class)
  @ApiResponses(value = {
      @ApiResponse (code = 200, message = "Request fulfilled"),
      @ApiResponse (code = 500, message = "Server error when retrieving branding information") })
  public Response getBrandingInformation() {
    try {
      return Response.ok(brandingService.getBrandingInformation()).build();
    } catch (Exception e) {
      LOG.error("Error when retrieving branding information", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  @PUT
  @RolesAllowed("administrators")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update Branding information", httpMethod = "POST", response = Response.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Branding information updated"),
      @ApiResponse(code = 500, message = "Server error when updating branding information") })
  public Response updateBrandingInformation(Branding branding) {
    try {
      brandingService.updateBrandingInformation(branding);
    } catch (Exception e) {
      LOG.error("Error when updating branding information", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.status(Response.Status.OK).build();
  }

  @GET
  @Path("/logo")
  @RolesAllowed("users")
  @ApiOperation(value = "Get Branding logo", httpMethod = "GET", response = Response.class)
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Branding logo retrieved"),
          @ApiResponse(code = 404, message = "Branding logo not found"),
          @ApiResponse(code = 500, message = "Server error when retrieving branding logo") })
  public Response getBrandingLogo(@Context Request request,
                                  @ApiParam(value = "'404' to return a 404 http code when no logo has been uploaded, no value to return the logo define in the configuration or the default logo") @QueryParam("defaultLogo") String defaultLogo) {
    
    Logo logo = brandingService.getLogo();
    if (logo == null) {
      if("404".equals(defaultLogo)) {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
      } else {
        logo = brandingService.getDefaultLogo();
        if(logo == null) {
          throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
      }
    }
    //
    long lastUpdated = logo.getUpdatedDate();
    EntityTag eTag = new EntityTag(String.valueOf(lastUpdated));
    //
    Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
    if (builder == null) {
      InputStream stream = new ByteArrayInputStream(logo.getData());
      builder = Response.ok(stream, "image/png");
      builder.tag(eTag);
    }
    CacheControl cc = new CacheControl();
    cc.setMaxAge(86400);
    builder.cacheControl(cc);
    return builder.cacheControl(cc).build();
  }

  @GET
  @Path("/defaultLogo")
  @RolesAllowed("users")
  @ApiOperation(value = "Get Branding default logo", httpMethod = "GET", response = Response.class)
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Branding default logo retrieved"),
          @ApiResponse(code = 404, message = "Branding default logo not found"),
          @ApiResponse(code = 500, message = "Server error when retrieving branding default logo") })
  public Response getBrandingDefaultLogo(@Context Request request) {

    Logo logo = brandingService.getDefaultLogo();
    if (logo == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    //
    long lastUpdated = logo.getUpdatedDate();
    EntityTag eTag = new EntityTag(String.valueOf(lastUpdated));
    //
    Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
    if (builder == null) {
      InputStream stream = new ByteArrayInputStream(logo.getData());
      builder = Response.ok(stream, "image/png");
      builder.tag(eTag);
    }
    CacheControl cc = new CacheControl();
    cc.setMaxAge(86400);
    builder.cacheControl(cc);
    return builder.cacheControl(cc).build();
  }
}
