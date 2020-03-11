package org.exoplatform.portal.mop.rest;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.*;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.*;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;

import io.swagger.annotations.*;

@Path("/v1/navigations")
@Api(tags = "/v1/navigations", value = "/v1/navigations", description = "Retrieve sites navigations") // NOSONAR
public class NavigationRest implements ResourceContainer {

  private static final Log                  LOG                  = ExoLogger.getLogger(NavigationRest.class);

  private static final Visibility[]         DEFAULT_VISIBILITIES = Visibility.values();

  private static final UserNodeFilterConfig USER_FILTER_CONFIG   = getUserFilterConfig(DEFAULT_VISIBILITIES);

  private UserPortalConfigService           portalConfigService;

  public NavigationRest(UserPortalConfigService portalConfigService) {
    this.portalConfigService = portalConfigService;
  }

  @Path("/{siteType}/{siteName}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Gets a specific setting value", httpMethod = "GET", response = Response.class, notes = "This returns the requested site navigation")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"),
      @ApiResponse(code = 404, message = "Setting does not exist"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getSiteNavigations(@Context HttpServletRequest request,
                                     @ApiParam(value = "Portal site type, possible values: PORTAL, GROUP or USER", required = true) @PathParam("siteType") String siteTypeName,
                                     @ApiParam(value = "Portal site name", required = true) @PathParam("siteName") String siteName,
                                     @ApiParam(value = "Scope of navigations tree to retrieve, possible values: ALL, CHILDREN, GRANDCHILDREN, SINGLE", defaultValue = "ALL", required = false) @QueryParam("scope") String scopeName,
                                     @ApiParam(value = "Multivalued visibilities of navigation nodes to retrieve, possible values: DISPLAYED, HIDDEN, SYSTEM or TEMPORAL. If empty, all visibilities will be used.", defaultValue = "All possible values combined", required = false) @QueryParam("visibility") List<String> visibilityNames) {
    if (StringUtils.isBlank(siteTypeName) || StringUtils.isBlank(siteName)) {
      return Response.status(400).build();
    }

    ConversationState state = ConversationState.getCurrent();
    Identity userIdentity = state == null ? null : state.getIdentity();
    String username = userIdentity == null ? null : userIdentity.getUserId();
    if (StringUtils.equals(username, IdentityConstants.ANONIM)) {
      username = null;
    }

    SiteType siteType = null;
    try {
      siteType = SiteType.valueOf(StringUtils.upperCase(siteTypeName));
    } catch (Exception e) {
      return Response.status(400).entity("Bad site type: " + siteTypeName).build();
    }

    Scope scope = null;
    try {
      scope = getScope(scopeName);
    } catch (Exception e) {
      return Response.status(400).entity(e.getMessage()).build();
    }

    Visibility[] visibilities = null;
    try {
      visibilities = convertVisibilities(visibilityNames);
    } catch (Exception e) {
      return Response.status(400)
                     .entity("Bad visibility names: " + visibilityNames + ". should be one of " + DEFAULT_VISIBILITIES)
                     .build();
    }

    UserNodeFilterConfig userFilterConfig = USER_FILTER_CONFIG;
    if (visibilities.length > 0) {
      userFilterConfig = getUserFilterConfig(visibilities);
    }

    String portalName = siteName;
    if (siteType != SiteType.PORTAL) {
      portalName = portalConfigService.getDefaultPortal();
    }

    try {
      HttpUserPortalContext userPortalContext = new HttpUserPortalContext(request);
      UserPortalConfig userPortalConfig = portalConfigService.getUserPortalConfig(portalName,
                                                                                  username,
                                                                                  userPortalContext);
      if (userPortalConfig == null) {
        return Response.status(404).build();
      }

      UserPortal userPortal = userPortalConfig.getUserPortal();
      Collection<UserNode> nodes = null;
      if (siteType == SiteType.PORTAL) {
        nodes = userPortal.getNodes(siteType, scope, userFilterConfig);
      } else {
        UserNavigation navigation = userPortal.getNavigation(new SiteKey(siteType, siteName));
        if (navigation == null) {
          return Response.status(404).build();
        }
        UserNode rootNode = userPortal.getNode(navigation, scope, userFilterConfig, null);
        nodes = rootNode.getChildren();
      }
      List<ResultUserNode> resultNodes = convertNodes(nodes);
      return Response.ok(resultNodes).build();
    } catch (Exception e) {
      LOG.error("Error retrieving ");
      return Response.status(500).build();
    }

  }

  private List<ResultUserNode> convertNodes(Collection<UserNode> nodes) {
    if (nodes == null) {
      return Collections.emptyList();
    }
    List<ResultUserNode> result = new ArrayList<>();
    for (UserNode userNode : nodes) {
      if (userNode == null) {
        continue;
      }
      ResultUserNode resultNode = new ResultUserNode(userNode);
      resultNode.setChildren(convertNodes(userNode.getChildren()));
      result.add(resultNode);
    }
    return result;
  }

  private static UserNodeFilterConfig getUserFilterConfig(Visibility[] visibilities) {
    UserNodeFilterConfig.Builder builder = UserNodeFilterConfig.builder();
    builder.withReadWriteCheck()
           .withVisibility(visibilities)
           .withTemporalCheck()
           .withReadCheck();
    return builder.build();
  }

  private Visibility[] convertVisibilities(List<String> visibilityNames) {
    if (visibilityNames == null) {
      return new Visibility[0];
    }
    return visibilityNames.stream()
                          .map(visibilityName -> Visibility.valueOf(StringUtils.upperCase(visibilityName)))
                          .collect(Collectors.toList())
                          .toArray(new Visibility[0]);
  }

  private Scope getScope(String scopeName) {
    Scope scope = null;
    if (StringUtils.isBlank(scopeName)) {
      scope = Scope.ALL;
    } else {
      switch (StringUtils.upperCase(scopeName)) {
      case "SINGLE":
        scope = Scope.SINGLE;
        break;
      case "CHILDREN":
        scope = Scope.CHILDREN;
        break;
      case "GRANDCHILDREN":
        scope = Scope.GRANDCHILDREN;
        break;
      default:
        throw new IllegalStateException("Bad scope name: " + scopeName);
      }
    }
    return scope;
  }

  /**
   * A class to retrieve {@link UserNode} attributes by avoiding cyclic JSON
   * parsing of instance when retrieving {@link UserNode#getChildren()} and
   * {@link UserNode#getParent()} attributes using {@link JsonParserImpl}
   */
  public static final class ResultUserNode {
    private UserNode     userNode;

    List<ResultUserNode> subNodes;

    public ResultUserNode(UserNode userNode) {
      this.userNode = userNode;
    }

    public void setChildren(List<ResultUserNode> subNodes) {
      this.subNodes = subNodes;
    }

    public List<ResultUserNode> getChildren() {
      return subNodes;
    }

    public String getLabel() {
      return userNode.getResolvedLabel();
    }

    public String getLabelKey() {
      return userNode.getLabel();
    }

    public String getIcon() {
      return userNode.getIcon();
    }

    public String getId() {
      return userNode.getId();
    }

    public String getUri() {
      return userNode.getURI();
    }

    public Visibility getVisibility() {
      return userNode.getVisibility();
    }

    public String getName() {
      return userNode.getName();
    }

    public long getStartPublicationTime() {
      return userNode.getStartPublicationTime();
    }

    public long getEndPublicationTime() {
      return userNode.getEndPublicationTime();
    }

  }
}
