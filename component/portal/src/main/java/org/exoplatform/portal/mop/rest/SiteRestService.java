package org.exoplatform.portal.mop.rest;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("v1/sitesManagement")
@Tag(name = "v1/sitesManagement", description = "Managing sites")
public class SiteRestService implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(SiteRestService.class);

  private LayoutService    layoutService;

  public SiteRestService(LayoutService layoutService) {
    this.layoutService = layoutService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Gets navigations categories for UI", description = "Gets navigations categories for UI", method = "GET")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error"), })
  public Response getSitesWithNavigationNodes(@Context
  HttpServletRequest request,
                                              @Parameter(description = "Portal site type, possible values: PORTAL, GROUP or USER", required = true)
                                              @QueryParam("siteType")
                                              String siteTypeName,
                                              @Parameter(description = "Portal site name to be excluded")
                                              @QueryParam("excludedSiteName")
                                              String excludedSiteName,
                                              @Parameter(description = "if to include empty site in results in portal type case")
                                              @DefaultValue("true")
                                              @QueryParam("includeEmpty")
                                              boolean includeEmpty,
                                              @Parameter(description = "to expand site navigations nodes")
                                              @DefaultValue("false")
                                              @QueryParam("expandNavigations")
                                              boolean expandNavigations,
                                              @Parameter(description = "to retrieve sites with  its displayed status")
                                              @DefaultValue("false")
                                              @QueryParam("displayed")
                                              boolean displayed,
                                              @Parameter(description = "to retrieve all sites")
                                              @DefaultValue("true")
                                              @QueryParam("allSites")
                                              boolean allSites,
                                              @Parameter(description = "to order sites by its display order")
                                              @DefaultValue("false")
                                              @QueryParam("orderByDisplayOrder")
                                              boolean orderByDisplayOrder,
                                              @Parameter(description = "to order sites by its display ")
                                              @DefaultValue("false")
                                              @QueryParam("orderByDisplayName")
                                              boolean orderByDisplayName,
                                              @Parameter(description = "to order sites by its display ")
                                              @DefaultValue("false")
                                              @QueryParam("filterByPermission")
                                              boolean filterByPermission,
                                              @Parameter(description = "Offset of results to retrieve")
                                              @QueryParam("offset")
                                              @DefaultValue("0")
                                              int offset,
                                              @Parameter(description = "Limit of results to retrieve")
                                              @QueryParam("limit")
                                              @DefaultValue("0")
                                              int limit) {
    try {
      SiteFilter siteFilter = new SiteFilter();
      if (siteTypeName != null) {
        siteFilter.setSiteType(SiteType.valueOf(siteTypeName.toUpperCase()));
      }
      siteFilter.setExcludedSiteName(excludedSiteName);
      siteFilter.setIncludeEmpty(includeEmpty);
      siteFilter.setOrderByDisplayOrder(orderByDisplayOrder);
      siteFilter.setDisplayed(displayed);
      siteFilter.setAllSites(allSites);
      siteFilter.setOrderByDisplayName(orderByDisplayName);
      siteFilter.setExpandNavigations(expandNavigations);
      siteFilter.setFilterByPermission(filterByPermission);
      siteFilter.setLimit(limit);
      siteFilter.setOffset(offset);
      List<PortalConfig> sites = layoutService.getSitesByFilter(siteFilter);
      return Response.ok(EntityBuilder.toSiteRestEntities(sites, request, siteFilter)).build();
    } catch (Exception e) {
      LOG.warn("Error while retrieving sites", e);
      return Response.serverError().build();
    }
  }
}
