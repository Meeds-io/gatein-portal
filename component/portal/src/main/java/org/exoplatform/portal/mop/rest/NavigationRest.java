package org.exoplatform.portal.mop.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.storage.PageStorage;
import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.NavigationCategoryService;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.user.HttpUserPortalContext;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/navigations")
@Tag(name = "v1/navigations", description = "Retrieve sites navigations")
public class NavigationRest implements ResourceContainer {

  private static final Log                  LOG                  = ExoLogger.getLogger(NavigationRest.class);

  private static final Visibility[]         DEFAULT_VISIBILITIES = Visibility.values();

  private static final UserNodeFilterConfig USER_FILTER_CONFIG   = getUserFilterConfig(DEFAULT_VISIBILITIES);

  private UserPortalConfigService           portalConfigService;

  private NavigationCategoryService         navigationCategoryService;

  private PageStorage                       pageStorage;

  public NavigationRest(UserPortalConfigService portalConfigService, NavigationCategoryService navigationCategoryService, PageStorage pageStorage) {
    this.portalConfigService = portalConfigService;
    this.navigationCategoryService = navigationCategoryService;
    this.pageStorage = pageStorage;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Gets navigations",
          description = "Gets navigations",
          method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "404", description = "Navigation does not exist"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getSiteNavigation(
                                    @Context
                                    HttpServletRequest request,
                                    @Parameter(description = "Offset", required = false)
                                    @Schema(defaultValue = "0")
                                    @QueryParam("offset")
                                    int offset,
                                    @Parameter(description = "Limit, if equals to 0, it will use default limit.", required = false)
                                    @Schema(defaultValue = "20")
                                    @QueryParam("limit")
                                    int limit) {
    HttpUserPortalContext userPortalContext = new HttpUserPortalContext(request);
    String portalName = portalConfigService.getDefaultPortal();

    ConversationState state = ConversationState.getCurrent();
    Identity userIdentity = state == null ? null : state.getIdentity();
    String username = userIdentity == null ? null : userIdentity.getUserId();
    if (StringUtils.equals(username, IdentityConstants.ANONIM)) {
      username = null;
    }

    try {
      UserPortalConfig userPortalConfig = portalConfigService.getUserPortalConfig(portalName, username, userPortalContext);
      List<ResultUserNavigation> allNavs = userPortalConfig.getUserPortal()
                                                           .getNavigations()
                                                           .stream()
                                                           .filter(this::isValidNavigation)
                                                           .map(ResultUserNavigation::new)
                                                           .toList();
      return Response.ok(allNavs).build();

    } catch (Exception e) {
      LOG.error("Error retrieving navigations", e);
      return Response.status(500).build();
    }

  }

  @Path("/{siteType}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Gets navigations of one or multiple site navigations",
          description = "Gets navigations of one or multiple site navigations",
          method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "404", description = "Navigation does not exist"),
          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getSiteTypeNavigations(@Context HttpServletRequest request,
                                         @Parameter(description = "Portal site type, possible values: PORTAL, GROUP or USER", required = true) @PathParam("siteType") String siteTypeName,
                                         @Parameter(description = "Portal site name", required = true) @QueryParam("siteName") String siteName,
                                         @Parameter(description = "Scope of navigations tree to retrieve, possible values: ALL, CHILDREN, GRANDCHILDREN, SINGLE", required = false)
                                         @Schema(defaultValue = "ALL") @QueryParam("scope") String scopeName,
                                         @Parameter(description = "parent navigation node id") @QueryParam("nodeId") String nodeId,
                                         @Parameter(description = "Multivalued visibilities of navigation nodes to retrieve, possible values: DISPLAYED, HIDDEN, SYSTEM or TEMPORAL. If empty, all visibilities will be used.", required = false)
                                         @Schema(defaultValue = "All possible values combined") @QueryParam("visibility") List<String> visibilityNames,
                                         @Parameter(description = "if to include Global site in results in portal type case", required = false)
                                         @DefaultValue("true")  @QueryParam("includeGlobal") boolean includeGlobal,
                                         @Parameter(description = "to include extra node page attribute in results")
                                         @QueryParam("expandPageDetails") boolean expandPageDetails) {
    // this function return nodes and not navigations
    if (StringUtils.isBlank(siteTypeName)) {
      return Response.status(400).build();
    }

    return getNavigations(request, siteTypeName, siteName, scopeName, nodeId, visibilityNames, includeGlobal, expandPageDetails);
  }

  @Path("/categories")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
      summary = "Gets navigations categories for UI",
      description = "Gets navigations categories for UI",
      method = "GET")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "500", description = "Internal server error"),
      }
  )
  public Response getNavigationCategories() {
    try {
      JSONObject object = new JSONObject();
      object.put("navs", navigationCategoryService.getNavigationCategories());
      object.put("categoriesOrder", navigationCategoryService.getNavigationCategoriesOrder());
      object.put("urisOrder", navigationCategoryService.getNavigationUriOrder());
      return Response.ok(object.toString()).build();
    } catch (JSONException e) {
      LOG.warn("Error parsing navigation categories result", e);
      return Response.serverError().build();
    }
  }

  private Response getNavigations(HttpServletRequest request, // NOSONAR
                                  String siteTypeName,
                                  String siteName,
                                  String scopeName,
                                  String nodeId,
                                  List<String> visibilityNames,
                                  boolean includeGlobal,
                                  boolean expandPageDetails) {
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
    if (siteType != SiteType.PORTAL || StringUtils.isBlank(siteName)) {
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
      Collection<UserNode> nodes = new ArrayList<>();

      if (nodeId != null && siteName != null) {
        SiteKey siteKey = new SiteKey(siteTypeName, siteName);
        UserNode userNode = userPortal.getNodeById(nodeId, siteKey, scope, userFilterConfig, null);
        nodes.add(userNode);
      } else if (siteType == SiteType.PORTAL || StringUtils.isBlank(siteName)) {
        nodes = userPortal.getNodes(siteType, scope, userFilterConfig, includeGlobal);
      } else {
        UserNavigation navigation = userPortal.getNavigation(new SiteKey(siteType, siteName));
        if (navigation == null) {
          return Response.status(404).build();
        }
        UserNode rootNode = userPortal.getNode(navigation, scope, userFilterConfig, null);
        nodes = rootNode.getChildren();
      }
      List<ResultUserNode> resultNodes = convertNodes(nodes, expandPageDetails);
      return Response.ok(resultNodes).build();
    } catch (Exception e) {
      LOG.error("Error retrieving ");
      return Response.status(500).build();
    }
  }

  private List<ResultUserNode> convertNodes(Collection<UserNode> nodes, boolean expandPageDetails) {
    if (nodes == null) {
      return Collections.emptyList();
    }
    List<ResultUserNode> result = new ArrayList<>();
    for (UserNode userNode : nodes) {
      if (userNode == null) {
        continue;
      }
      ResultUserNode resultNode = new ResultUserNode(userNode);
      if (expandPageDetails && userNode.getPageRef() != null) {
        Page userNodePage = pageStorage.getPage(userNode.getPageRef());
        boolean canEdit = ConversationState.getCurrent()
                                           .getIdentity()
                                           .isMemberOf(userNodePage.getEditPermission().split(":")[1],
                                                       userNodePage.getEditPermission().split(":")[0]);
        resultNode.setCanEdit(canEdit);
        resultNode.setEditPermission(userNodePage.getEditPermission());
        resultNode.setAccessPermissions(userNodePage.getAccessPermissions());
      }
      resultNode.setChildren(convertNodes(userNode.getChildren(), expandPageDetails));
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
                          .toList()
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
      case "ALL":
        scope = Scope.ALL;
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
    private UserNode             userNode;

    private List<ResultUserNode> subNodes;

    private boolean              canEdit;

    private String               editPermission;

    private String[]             accessPermissions;

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

    public SiteKey getSiteKey() {
      return userNode.getNavigation().getKey();
    }

    public PageKey getPageKey() {
      return userNode.getPageRef();
    }

    public boolean isCanEdit() {
      return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
      this.canEdit = canEdit;
    }

    public String getEditPermission() {
      return editPermission;
    }

    public void setEditPermission(String editPermission) {
      this.editPermission = editPermission;
    }

    public String[] getAccessPermissions() {
      return accessPermissions;
    }

    public void setAccessPermissions(String[] accessPermissions) {
      this.accessPermissions = accessPermissions;
    }
  }

  /**
   * A class to retrieve {@link UserNavigation} attributes by avoiding cyclic JSON
   * parsing of instance when retrieving {@link UserNavigation#getBundle()}
   * attributes using {@link JsonParserImpl}
   */
  public static final class ResultUserNavigation {
    public SiteKey getKey() {
      return key;
    }

    public String getLabel() {
      return label;
    }

    private SiteKey key;

    private String  label;

    public ResultUserNavigation(UserNavigation userNavigation) {
      key = userNavigation.getKey();
      if (key.getType().equals(SiteType.GROUP)) {
        try {
          OrganizationService orgService = ExoContainerContext.getCurrentContainer()
                                                              .getComponentInstanceOfType(OrganizationService.class);
          Group group = orgService.getGroupHandler().findGroupById(key.getName());
          if (group != null) {
            label = group.getLabel();
          }
        } catch (Exception e) {
          LOG.error("Error when getting group label {}", key.getName(), e);
        }
      } else {
        label = key.getName();
      }
    }

  }

  private boolean isValidNavigation(UserNavigation userNavigation) {
    return !userNavigation.getKey()
                          .getName()
                          .equals(portalConfigService.getGlobalPortal());
  }

}
