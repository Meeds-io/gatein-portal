/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.mop.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.GenericScope;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationServiceException;
import org.exoplatform.portal.mop.navigation.NodeChangeListener;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.navigation.VisitMode;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;

/**
 * @author  <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class UserPortalImpl implements UserPortal {

  public static final Comparator<UserNavigation> USER_NAVIGATION_COMPARATOR = new Comparator<UserNavigation>() {
                                                                              public int compare(UserNavigation nav1,
                                                                                                 UserNavigation nav2) {
                                                                                                                                                     /*
                                                                                                                                                      * Because
                                                                                                                                                      * we
                                                                                                                                                      * cannot
                                                                                                                                                      * tell
                                                                                                                                                      * how
                                                                                                                                                      * expensive
                                                                                                                                                      * getPriority
                                                                                                                                                      * (
                                                                                                                                                      * )
                                                                                                                                                      * is
                                                                                                                                                      * we
                                                                                                                                                      * better
                                                                                                                                                      * store
                                                                                                                                                      * the
                                                                                                                                                      * priorities
                                                                                                                                                      * into
                                                                                                                                                      * local
                                                                                                                                                      * variables
                                                                                                                                                      */
                                                                                int priority1 = nav1.getPriority();
                                                                                int priority2 = nav2.getPriority();
                                                                                if (priority1 == priority2) {
                                                                                                                                                     /*
                                                                                                                                                      * A
                                                                                                                                                      * fix
                                                                                                                                                      * for
                                                                                                                                                      * GTNPORTAL
                                                                                                                                                      * -
                                                                                                                                                      * 3322.
                                                                                                                                                      * The
                                                                                                                                                      * original
                                                                                                                                                      * comparator
                                                                                                                                                      * has
                                                                                                                                                      * not
                                                                                                                                                      * imposed
                                                                                                                                                      * a
                                                                                                                                                      * total
                                                                                                                                                      * ordering
                                                                                                                                                      * as
                                                                                                                                                      * required
                                                                                                                                                      * by
                                                                                                                                                      * the
                                                                                                                                                      * Comparator
                                                                                                                                                      * interface:
                                                                                                                                                      * "The
                                                                                                                                                      * implementor
                                                                                                                                                      * must
                                                                                                                                                      * ensure
                                                                                                                                                      * that
                                                                                                                                                      * sgn
                                                                                                                                                      * (
                                                                                                                                                      * compare
                                                                                                                                                      * (
                                                                                                                                                      * x,
                                                                                                                                                      * y
                                                                                                                                                      * )
                                                                                                                                                      * )
                                                                                                                                                      * ==
                                                                                                                                                      * -sgn
                                                                                                                                                      * (
                                                                                                                                                      * compare
                                                                                                                                                      * (
                                                                                                                                                      * y,
                                                                                                                                                      * x
                                                                                                                                                      * )
                                                                                                                                                      * )
                                                                                                                                                      * for
                                                                                                                                                      * all
                                                                                                                                                      * x
                                                                                                                                                      * and
                                                                                                                                                      * y
                                                                                                                                                      * .
                                                                                                                                                      * "
                                                                                                                                                      * This
                                                                                                                                                      * was
                                                                                                                                                      * not
                                                                                                                                                      * the
                                                                                                                                                      * case
                                                                                                                                                      * when
                                                                                                                                                      * comparing
                                                                                                                                                      * two
                                                                                                                                                      * nodes
                                                                                                                                                      * both
                                                                                                                                                      * having
                                                                                                                                                      * UNDEFINED_PRIORITY.
                                                                                                                                                      */
                                                                                  return 0;
                                                                                } else if (priority1 == PageNavigation.UNDEFINED_PRIORITY) {
                                                                                  return 1;
                                                                                } else if (priority2 == PageNavigation.UNDEFINED_PRIORITY) {
                                                                                  return -1;
                                                                                } else {
                                                                                  return priority1 - priority2;
                                                                                }
                                                                              }
                                                                            };

  /** . */
  final UserPortalConfigService                  service;

  /** . */
  private final PortalConfig                     portalConfig;

  /** . */
  final UserPortalContext                        context;

  /** . */
  final String                                   userName;

  /** . */
  private List<UserNavigation>                   navigations;

  /** . */
  private final String                           portalName;

  /** . */
  private final Locale                           portalLocale;

  public UserPortalImpl(UserPortalConfigService service,
                        String portalName,
                        PortalConfig portal,
                        String userName,
                        UserPortalContext context) {
    if (context == null) {
      throw new NullPointerException("No null context argument allowed");
    }

    //
    String locale = portal.getLocale();

    //
    this.portalLocale = locale != null ? new Locale(locale) : null;
    this.service = service;
    this.portalName = portalName;
    this.portalConfig = portal;
    this.userName = userName;
    this.context = context;
    this.navigations = null;
  }

  public Locale getLocale() {
    return portalLocale;
  }

  public PortalConfig getPortalConfig() {
    return portalConfig;
  }

  /**
   * Returns an immutable sorted list of the valid navigations related to the
   * user.
   *
   * @return                     the navigations
   * @throws UserPortalException any user portal exception
   */
  public List<UserNavigation> getNavigations() throws UserPortalException, NavigationServiceException {
    if (navigations == null) {
      List<UserNavigation> userNavigations = new ArrayList<>(userName == null ? 1 : 10);
      NavigationContext portalNav = service.getNavigationService()
                                           .loadNavigation(new SiteKey(SiteType.PORTAL, portalName));
      if (portalNav != null && portalNav.getState() != null) {
        userNavigations.add(new UserNavigation(this, portalNav, service.getUserACL().hasEditPermission(portalConfig)));
      }
      if (userName != null) {
        // Add group navigations
        List<String> userGroupIds = getUserGroupIds(ConversationState.getCurrent());
        if (CollectionUtils.isNotEmpty(userGroupIds)) {
          userGroupIds.forEach(groupId -> {
            NavigationContext groupNavigation = service.getNavigationService()
                                                       .loadNavigation(SiteKey.group(groupId));
            if (groupNavigation != null && groupNavigation.getState() != null) {
              UserNavigation userNavigation = new UserNavigation(this,
                                                                 groupNavigation,
                                                                 service.getUserACL()
                                                                        .hasEditPermissionOnNavigation(groupNavigation.getKey()));
              userNavigations.add(userNavigation);
            }
          });
        }

        // Sort the list finally
        Collections.sort(userNavigations, USER_NAVIGATION_COMPARATOR);
      }

      // Add global navigation at the end
      NavigationContext globalPortalNav = service.getNavigationService()
                                                 .loadNavigation(new SiteKey(SiteType.PORTAL, service.getGlobalPortal()));
      if (globalPortalNav != null && globalPortalNav.getState() != null) {
        userNavigations.add(new UserNavigation(this, globalPortalNav, false));
      }

      //
      this.navigations = Collections.unmodifiableList(userNavigations);
    }
    return navigations;
  }

  private List<String> getUserGroupIds(ConversationState conversationState) {
    Collection<?> groups = null;
    if (conversationState != null && conversationState.getIdentity() != null
        && !IdentityConstants.ANONIM.equals(conversationState.getIdentity().getUserId())
        && !IdentityConstants.SYSTEM.equals(conversationState.getIdentity().getUserId())) {
      groups = conversationState.getIdentity().getGroups();
    } else {
      try {
        groups = service.getOrganizationService().getGroupHandler().findGroupsOfUser(userName);
      } catch (Exception e) {
        throw new IllegalStateException("Could not retrieve groups", e);
      }
    }
    return getUserGroupIds(groups);
  }

  private List<String> getUserGroupIds(Collection<?> groups) {
    String guestsGroupId = service.getUserACL().getGuestsGroup();
    return groups.stream()
                 .map(groupObj -> {
                   if (groupObj instanceof Group group) {
                     return group.getId().trim();
                   } else {
                     return groupObj.toString().trim();
                   }
                 })
                 .filter(groupId -> !StringUtils.equals(groupId, guestsGroupId))
                 .toList();
  }

  @Override
  public Collection<UserNode> getNodes(SiteType siteType,
                                       String excludedSiteName,
                                       Scope scope,
                                       UserNodeFilterConfig filterConfig) {
    Collection<UserNode> resultUserNodes = new ArrayList<>();
    Set<String> addedUserNodesURI = new HashSet<>();
    List<UserNavigation> userNavigations = getNavigations();

    for (UserNavigation userNavigation : userNavigations) {
      SiteKey siteKey = userNavigation.getKey();
      if (siteKey.getType() != siteType) {
        continue;
      }
      if (StringUtils.isNotBlank(excludedSiteName) && Pattern.matches(excludedSiteName, siteKey.getName())) {
        continue;
      }

      UserNode rootNode = getNode(userNavigation, scope, filterConfig, null);
      Collection<UserNode> userNodes = rootNode.getChildren();
      for (UserNode userNode : userNodes) {
        if (addedUserNodesURI.contains(userNode.getURI())) {
          continue;
        }
        addedUserNodesURI.add(userNode.getURI());
        resultUserNodes.add(userNode);
      }
    }
    return resultUserNodes;
  }

  @Override
  public Collection<UserNode> getNodes(SiteType siteType, Scope scope, UserNodeFilterConfig filterConfig) {
    return getNodes(siteType, null, scope, filterConfig);
  }

  public UserNavigation getNavigation(SiteKey key) throws NullPointerException,
                                                   UserPortalException,
                                                   NavigationServiceException {
    if (key == null) {
      throw new NullPointerException("No null key accepted");
    }
    for (UserNavigation navigation : getNavigations()) {
      if (navigation.getKey().equals(key)) {
        return navigation;
      }
    }
    return null;
  }

  public void refresh() {
    navigations = null;
  }

  public UserNode getNode(UserNavigation userNavigation,
                          Scope scope,
                          UserNodeFilterConfig filterConfig,
                          NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                 UserPortalException,
                                                                 NavigationServiceException {
    UserNodeContext context = new UserNodeContext(userNavigation, filterConfig);
    NodeContext<UserNode> nodeContext = service.getNavigationService()
                                               .loadNode(context,
                                                         userNavigation.navigation,
                                                         scope,
                                                         new UserNodeListener(listener));
    if (nodeContext != null) {
      return nodeContext.getNode().filter();
    } else {
      return null;
    }
  }

  @Override
  public UserNode getNodeById(String userNodeId, SiteKey siteKey,
                              Scope scope,
                              UserNodeFilterConfig filterConfig,
                              NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                     UserPortalException,
                                                                     NavigationServiceException {
    UserNavigation userNavigation = getNavigation(siteKey);
    UserNodeContext userNodeContext = new UserNodeContext(userNavigation, filterConfig);
    NodeContext<UserNode> nodeContext = service.getNavigationService()
                                               .loadNodeById(userNodeContext,
                                                             userNodeId,
                                                             scope,
                                                             new UserNodeListener(listener));
    if (nodeContext != null) {
      return nodeContext.getNode().filter();
    } else {
      return null;
    }
  }

  public void updateNode(UserNode node, Scope scope, NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                                            IllegalArgumentException,
                                                                                            UserPortalException,
                                                                                            NavigationServiceException {
    if (node == null) {
      throw new NullPointerException("No null node accepted");
    }
    service.getNavigationService().updateNode(node.context, scope, new UserNodeListener(listener));
    node.filter();
  }

  public void rebaseNode(UserNode node, Scope scope, NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                                            IllegalArgumentException,
                                                                                            UserPortalException,
                                                                                            NavigationServiceException {
    if (node == null) {
      throw new NullPointerException("No null node accepted");
    }
    service.getNavigationService().rebaseNode(node.context, scope, new UserNodeListener(listener));
    node.filter();
  }

  public void saveNode(UserNode node, NodeChangeListener<UserNode> listener) throws NullPointerException,
                                                                             UserPortalException,
                                                                             NavigationServiceException {
    if (node == null) {
      throw new NullPointerException("No null node accepted");
    }
    service.getNavigationService().saveNode(node.context, new UserNodeListener(listener));
    navigations = null;
    node.filter();
  }

  /**
   * Note : the scope implementation is not stateless but we don't care in this
   * case.
   */
  private class MatchingScope extends GenericScope.Branch.Visitor implements Scope {
    final UserNavigation       userNavigation;

    final UserNodeFilterConfig filterConfig;

    final String[]             match;

    int                        score;

    String                     id;

    UserNode                   userNode;

    MatchingScope(UserNavigation userNavigation, UserNodeFilterConfig filterConfig, String[] match) {
      this.userNavigation = userNavigation;
      this.filterConfig = filterConfig;
      this.match = match;
    }

    public Visitor get() {
      return this;
    }

    @Override
    protected int getSize() {
      return match.length;
    }

    @Override
    protected String getName(int index) {
      return match[index];
    }

    @Override
    protected Visitor getFederated() {
      return Scope.CHILDREN.get();
    }

    void resolve() throws NavigationServiceException {
      UserNodeContext context = new UserNodeContext(userNavigation, filterConfig);
      NodeContext<UserNode> nodeContext = service.getNavigationService()
                                                 .loadNode(context,
                                                           userNavigation.navigation,
                                                           this,
                                                           null);
      if (score > 0) {
        userNode = nodeContext.getNode().filter().find(id);
      }
    }

    public VisitMode enter(int depth, String id, String name, NodeState state) {
      VisitMode vm = super.enter(depth, id, name, state);
      if (depth == 0) {
        score = 0;
        MatchingScope.this.id = null;
      } else {
        if (vm == VisitMode.ALL_CHILDREN) {
          MatchingScope.this.id = id;
          score++;
        }
      }
      return vm;
    }
  }

  public UserNode getDefaultPath(UserNodeFilterConfig filterConfig) throws UserPortalException, NavigationServiceException {
    for (UserNavigation userNavigation : getNavigations()) {
      UserNode node = getDefaultPath(userNavigation, filterConfig);
      if (node != null) {
        return node;
      }
    }

    //
    return null;
  }

  public UserNode getDefaultPath(UserNavigation userNavigation, UserNodeFilterConfig filterConfig)
                                                                                                   throws UserPortalException,
                                                                                                   NavigationServiceException {
    NavigationContext navigation = userNavigation.navigation;
    if (navigation.getState() != null) {
      UserNodeContext context = new UserNodeContext(userNavigation, null);
      NodeContext<UserNode> nodeContext = service.getNavigationService()
                                                 .loadNode(context,
                                                           navigation,
                                                           Scope.CHILDREN,
                                                           null);
      if (nodeContext != null) {
        UserNode root = nodeContext.getNode();

        //
        if (filterConfig == null) {
          filterConfig = UserNodeFilterConfig.builder().build();
        }
        UserNodeFilter filter = new UserNodeFilter(userNavigation.portal, filterConfig);

        // Filter node by node
        for (UserNode node : root.getChildren()) {
          if (node.context.accept(filter)) {
            return node;
          }
        }
      }
    }
    return null;
  }

  public UserNode resolvePath(UserNodeFilterConfig filterConfig, String path) throws NullPointerException,
                                                                              UserPortalException,
                                                                              NavigationServiceException {
    if (path == null) {
      throw new NullPointerException("No null path accepted");
    }

    // Parse path
    String[] segments = Utils.parsePath(path);

    // Find the first navigation available or return null
    if (segments == null) {
      return getDefaultPath(filterConfig);
    }

    // Create a filter as we need one for the path
    if (filterConfig == null) {
      filterConfig = UserNodeFilterConfig.builder().build();
    } else {
      filterConfig = UserNodeFilterConfig.builder(filterConfig).build();
    }

    // Restrict the filter with path
    filterConfig.path = segments;

    // Get navigations
    List<UserNavigation> navigations = getNavigations();

    //
    MatchingScope best = null;
    for (UserNavigation navigation : navigations) {
      MatchingScope scope = new MatchingScope(navigation, filterConfig, segments);
      scope.resolve();
      if (scope.score == segments.length) {
        best = scope;
        break;
      } else {
        if (best == null) {
          best = scope;
        } else {
          if (scope.score > best.score) {
            best = scope;
          }
        }
      }
    }

    //
    if (best != null && best.score > 0) {
      UserNode ret = best.userNode;
      if (ret != null) {
        ret.owner.filterConfig.path = null;
      }
      return ret;
    } else {
      return getDefaultPath(null);
    }
  }

  public UserNode resolvePath(UserNavigation navigation, UserNodeFilterConfig filterConfig, String path)
                                                                                                         throws NullPointerException,
                                                                                                         UserPortalException,
                                                                                                         NavigationServiceException {
    if (navigation == null) {
      throw new NullPointerException("No null navigation accepted");
    }
    if (path == null) {
      throw new NullPointerException("No null path accepted");
    }

    //
    String[] segments = Utils.parsePath(path);

    //
    if (segments == null) {
      return null;
    }

    // Create a filter as we need one for the path
    if (filterConfig == null) {
      filterConfig = UserNodeFilterConfig.builder().build();
    } else {
      filterConfig = UserNodeFilterConfig.builder(filterConfig).build();
    }

    // Restrict the filter with the path
    filterConfig.path = segments;

    //
    MatchingScope scope = new MatchingScope(navigation, filterConfig, segments);
    scope.resolve();

    //
    if (scope.score > 0) {
      UserNode ret = scope.userNode;
      if (ret != null && !StringUtils.equals(scope.userNode.getURI(), ret.getURI())) {
        UserNode globalNode = getGlobalUserNode(filterConfig, navigation.getKey(), segments);
        if (globalNode != null) {
          return globalNode;
        }
      }
      if (ret != null) {
        ret.owner.filterConfig.path = null;
      }
      return ret;
    }
    return getGlobalUserNode(filterConfig, navigation.getKey(), segments);
  }

  private UserNode getGlobalUserNode(UserNodeFilterConfig filterConfig, SiteKey siteKey, String[] segments) {
    if (siteKey.getType() != SiteType.PORTAL) {
      return null;
    }
    UserNavigation globalNavigation = getNavigation(SiteKey.portal(this.service.getGlobalPortal()));
    if (globalNavigation != null) {
      MatchingScope globalScope = new MatchingScope(globalNavigation, filterConfig, segments);
      globalScope.resolve();
      if (globalScope.score > 0) {
        UserNode globalNode = globalScope.userNode;
        if (globalNode != null) {
          globalNode.owner.filterConfig.path = null;
        }
        if (globalNode != null && StringUtils.equals(globalScope.userNode.getURI(), globalNode.getURI())) {
          return globalNode;
        }
      }
    }
    return null;
  }
}
