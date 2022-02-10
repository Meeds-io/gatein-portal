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
import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.swagger.annotations.*;

@Path("/v1/platform/branding")
@Api(tags = "/v1/platform/branding", value = "/v1/platform/branding", description = "Managing branding information")
public class BrandingRestResourcesV1 implements ResourceContainer {
  private static final Log LOG = ExoLogger.getLogger(BrandingRestResourcesV1.class);

  // 1 year
  private static final int CACHE_IN_MILLI_SECONDS = 365 * 86400 * 1000;

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
  @Produces("image/png")
  @ApiOperation(value = "Get Branding logo", httpMethod = "GET", response = Response.class)
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Branding logo retrieved"),
          @ApiResponse(code = 404, message = "Branding logo not found"),
          @ApiResponse(code = 500, message = "Server error when retrieving branding logo") })
  public Response getBrandingLogo(@Context Request request,
                                  @ApiParam(value = "The value of lastModified parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'") @QueryParam("lastModified") String lastModified) {

    Logo logo = brandingService.getLogo();
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
    builder.type("image/png");
    builder.tag(eTag);
    builder.lastModified(new Date(lastUpdated));
    // If the query has a lastModified parameter, it means that the client
    // will change the lastModified entry when it really changes
    // Which means that we can cache the image in browser side
    // for a long time
    if (StringUtils.isNotBlank(lastModified)) {
      builder.expires(new Date(System.currentTimeMillis() + CACHE_IN_MILLI_SECONDS));
    }
    builder.cacheControl(cc);
    return builder.cacheControl(cc).build();
  }

  @GET
  @Path("/css")
  @ApiOperation(value = "Get Branding CSS content", httpMethod = "GET", response = Response.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Branding css retrieved"),
      @ApiResponse(code = 304, message = "Branding css not modified"),
      @ApiResponse(code = 500, message = "Server error when retrieving branding css")
  })
  public Response getBrandingCSS(@Context Request request,
                                 @ApiParam(value = "The value of lastModified parameter will determine whether the query should be cached by browser or not. If not set, no 'expires HTTP Header will be sent'") @QueryParam("v") String lastModified) {
    //
    long lastUpdated = brandingService.getLastUpdatedTime();
    EntityTag eTag = new EntityTag(String.valueOf(lastUpdated));

    //
    Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
    if (builder == null) {
      String themeCSS = brandingService.getThemeCSSContent();
      builder = Response.ok(themeCSS, "text/css");
      builder.tag(eTag);
    }
    CacheControl cc = new CacheControl();
    cc.setMaxAge(86400);
    builder.type("text/css");
    builder.tag(eTag);
    builder.lastModified(new Date(lastUpdated));
    if (StringUtils.isNotBlank(lastModified)) {
      builder.expires(new Date(System.currentTimeMillis() + CACHE_IN_MILLI_SECONDS));
    }
    // If the query has a lastModified parameter, it means that the client
    // will change the lastModified entry when it really changes
    // Which means that we can cache the image in browser side
    // for a long time
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
