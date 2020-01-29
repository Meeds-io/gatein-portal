/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.portal.pom.config.tasks;

import java.util.*;
import java.util.regex.Pattern;

import org.gatein.mop.api.Attributes;
import org.gatein.mop.api.content.ContentType;
import org.gatein.mop.api.content.Customization;
import org.gatein.mop.api.workspace.*;
import org.gatein.mop.api.workspace.ui.*;

import org.exoplatform.portal.config.NoSuchDataException;
import org.exoplatform.portal.config.StaleModelException;
import org.exoplatform.portal.config.model.*;
import org.exoplatform.portal.mop.*;
import org.exoplatform.portal.mop.redirects.*;
import org.exoplatform.portal.mop.redirects.NodeMap;
import org.exoplatform.portal.pom.config.POMSession;
import org.exoplatform.portal.pom.config.Utils;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.mop.core.util.Tools;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class Mapper {
    /** . */
    private final static Log LOG = ExoLogger.getLogger(Mapper.class);

    /** . */
    private static final Set<String> propertiesBlackList = Tools.set("jcr:uuid", "jcr:primaryType");

    /** . */
    private static final Set<String> portalPropertiesBlackList = Tools.set(MappedAttributes.LOCALE.getName(),
            MappedAttributes.SKIN.getName());

    /** . */
    private static final Set<String> windowPropertiesBlackList = Tools.set(MappedAttributes.THEME.getName(),
            MappedAttributes.TYPE.getName(), MappedAttributes.ICON.getName(), MappedAttributes.WIDTH.getName(),
            MappedAttributes.HEIGHT.getName());

    /** . */
    private final POMSession session;

    public Mapper(POMSession session) {
        this.session = session;
    }

    public PortalData load(Site src) {
        String type = Mapper.getOwnerType(src.getObjectType());
        Attributes attrs = src.getAttributes();

        //
        Templatized templarized = src.getRootNavigation().getTemplatized();
        org.gatein.mop.api.workspace.Page template = templarized.getTemplate();
        UIContainer srcLayout = template.getRootComponent();

        //
        Map<String, String> properties = new HashMap<String, String>();
        load(attrs, properties, portalPropertiesBlackList);

        //
        List<ComponentData> layoutChildren = loadChildren(srcLayout);
        ContainerData layout = load(srcLayout, layoutChildren);

        //
        List<String> accessPermissions = Collections.emptyList();
        String editPermission = null;
        if (src.isAdapted(ProtectedResource.class)) {
            ProtectedResource pr = src.adapt(ProtectedResource.class);
            accessPermissions = pr.getAccessPermissions();
            editPermission = pr.getEditPermission();
        }

        //
        List<RedirectData> redirects = null;
        if (src.isAdapted(Redirectable.class)) {
            Redirectable redirectAble = src.adapt(Redirectable.class);
            redirects = loadRedirects(src, redirectAble.getRedirects());
        }

        Described described = src.adapt(Described.class);

        //
        return new PortalData(src.getObjectId(), src.getName(), type, attrs.getValue(MappedAttributes.LOCALE),
                described.getName(), described.getDescription(), accessPermissions, editPermission,
                Collections.unmodifiableMap(properties), attrs.getValue(MappedAttributes.SKIN), layout, redirects);
    }

    private List<RedirectData> loadRedirects(Site src, Map<String, Redirect> redirects) {
        if (redirects == null || redirects.isEmpty()) {
            return null;
        } else {
            List<RedirectData> redirectsData = new ArrayList<RedirectData>();
            for (Redirect redirect : redirects.values()) {
                RedirectData redirectDate = new RedirectData(src.getObjectId(), redirect.getSite(), redirect.getName(),
                        redirect.getEnabled(), buildConditionData(src, redirect.getConditions()), buildMappingsData(
                                src.getObjectId(), redirect.getMapping()));
                redirectsData.add(redirectDate);
            }
            return redirectsData;
        }
    }

    private List<RedirectConditionData> buildConditionData(Site src, Map<String, Condition> conditions) {
        List<RedirectConditionData> conditionDatas = new ArrayList<RedirectConditionData>();

        for (Condition condition : conditions.values()) {
            RedirectConditionData conditionData = new RedirectConditionData(src.getObjectId(), null, condition.getName());

            RedirectUserAgentConditionData userAgentConditionData = new RedirectUserAgentConditionData(src.getObjectId(), null);
            userAgentConditionData.getUserAgentContains().addAll(condition.getUserAgentContains());
            userAgentConditionData.getUserAgentDoesNotContain().addAll(condition.getUserAgentDoesNotContain());

            conditionData.setUserAgentConditionData(userAgentConditionData);

            for (DeviceProperty deviceProperty : condition.getDeviceProperties().values()) {
                RedirectDevicePropertyConditionData propertyConditionData = new RedirectDevicePropertyConditionData(
                        src.getObjectId(), null, deviceProperty.getName());
                if (deviceProperty.getEquals() != null) {
                    propertyConditionData.setEquals(deviceProperty.getEquals());
                }

                if (deviceProperty.getGreaterThan() != null) {
                    propertyConditionData.setGreaterThan(deviceProperty.getGreaterThan());
                }

                if (deviceProperty.getLessThan() != null) {
                    propertyConditionData.setLessThan(deviceProperty.getLessThan());
                }

                if (deviceProperty.getPattern() != null) {
                    propertyConditionData.setMatches(Pattern.compile(deviceProperty.getPattern()));
                }

                conditionData.getDevicePropertyConditionData().add(propertyConditionData);
            }

            conditionDatas.add(conditionData);
        }

        return conditionDatas;
    }

    private RedirectMappingsData buildMappingsData(String storageId, Mappings mappings) {
        if (mappings != null) {
            RedirectMappingsData redirectMappingsData = new RedirectMappingsData(storageId);

            if (mappings.getUnresolvedNodeMatching() != null) {
                redirectMappingsData.setUnresolvedNode(mappings.getUnresolvedNodeMatching());
            }

            if (mappings.getNodeNameMatching() != null) {
                redirectMappingsData.setUseNodeNameMatching(mappings.getNodeNameMatching());
            }

            if (mappings.getNodeMap() != null) {
                HashMap<String, String> map = new HashMap<String, String>();
                for (NodeMap nodeMap : mappings.getNodeMap().values()) {
                    map.put(nodeMap.getOriginNode(), nodeMap.getRedirectNode());
                }
                redirectMappingsData.getMappings().putAll(map);
            }

            return redirectMappingsData;
        } else {
            return null;
        }
    }

    public void save(PortalData src, Site dst) {
        try {
            if (src.getStorageId() != null && !src.getStorageId().equals(dst.getObjectId())) {
                String msg = "Attempt to save a site " + src.getType() + "/" + src.getName() + " on the wrong target site "
                        + dst.getObjectType() + "/" + dst.getName();
                throw new IllegalArgumentException(msg);
            }

            //
            Attributes attrs = dst.getAttributes();
            attrs.setValue(MappedAttributes.LOCALE, src.getLocale());
            attrs.setValue(MappedAttributes.SKIN, src.getSkin());
            if (src.getProperties() != null) {
                save(src.getProperties(), attrs, portalPropertiesBlackList);
            }

            ProtectedResource pr = dst.adapt(ProtectedResource.class);
            pr.setAccessPermissions(src.getAccessPermissions());
            pr.setEditPermission(src.getEditPermission());

            Described described = dst.adapt(Described.class);
            described.setName(src.getLabel());
            described.setDescription(src.getDescription());

            Redirectable redirectable = dst.adapt(Redirectable.class);
            Map<String, Redirect> redirects = redirectable.getRedirects();

            List<RedirectData> redirectsData = src.getRedirects();

            redirects.clear(); // clear the redirects map since we need to rebuild it based on the new redirects
            if (src.getRedirects() != null) {
                for (RedirectData redirectData : redirectsData) {
                    Redirect redirect = redirectable.createRedirect();
                    redirects.put(redirectData.getRedirectName(), redirect);

                    redirect.setName(redirectData.getRedirectName());
                    redirect.setSite(redirectData.getRedirectSiteName());
                    redirect.setEnabled(redirectData.isEnabled());

                    if (redirectData.getConditions() != null) {
                        redirect.getConditions().clear(); // clear the map so that we can rebuild it
                        for (RedirectConditionData conditionData : redirectData.getConditions()) {
                            Condition condition = redirect.createCondition();
                            redirect.getConditions().put(conditionData.getRedirectName(), condition);
                            buildCondition(conditionData, condition);
                        }
                    }

                    if (redirectData.getMappings() != null) {
                        Mappings mappings = redirect.getMapping();
                        if (redirect.getMapping() == null) {
                            mappings = redirect.createMapping();
                            redirect.setMapping(mappings);
                        }
                        buildMappings(redirectData.getMappings(), mappings);
                    }
                }
            }

            //
            org.gatein.mop.api.workspace.Page templates = dst.getRootPage().getChild("templates");
            org.gatein.mop.api.workspace.Page template = templates.getChild("default");
            if (template == null) {
                template = templates.addChild("default");
            }

            //
            ContainerData srcContainer = src.getPortalLayout();
            UIContainer dstContainer = template.getRootComponent();

            // Workaround to have the real source container used as the model / UI layer lose this
            // ID which lead to bugs
            ContainerData realSrcContainer = new ContainerData(dstContainer.getObjectId(), srcContainer.getId(),
                    srcContainer.getName(), srcContainer.getIcon(), srcContainer.getTemplate(), srcContainer.getFactoryId(),
                    srcContainer.getTitle(), srcContainer.getDescription(), srcContainer.getWidth(), srcContainer.getHeight(),
                    srcContainer.getAccessPermissions(), srcContainer.getMoveAppsPermissions(),
                    srcContainer.getMoveContainersPermissions(), srcContainer.getChildren());

            //
            save(realSrcContainer, dstContainer);
            saveChildren(realSrcContainer, dstContainer);

            //
            Templatized templatized = dst.getRootNavigation().getTemplatized();
            if (templatized != null) {
                templatized.setTemplate(template);
            } else {
                template.templatize(dst.getRootNavigation());
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void buildCondition(RedirectConditionData redirectConditionData, Condition condition) {
        condition.setName(redirectConditionData.getRedirectName());

        if (redirectConditionData.getUserAgentConditionData() != null) {
            condition.setUserAgentContains(redirectConditionData.getUserAgentConditionData().getUserAgentContains());
        }
        if (redirectConditionData.getUserAgentConditionData() != null) {
            condition
                    .setUserAgentDoesNotContain(redirectConditionData.getUserAgentConditionData().getUserAgentDoesNotContain());
        }

        // if the device property conditions is empty and there are currently no stored device properties, then skip
        // otherwise we need to either remove or add device properties
        if (redirectConditionData.getDevicePropertyConditionData() != null || !condition.getDeviceProperties().isEmpty()) {
            // A list of stored redirect names which have not been found in the current RedirectConditionData list.
            //
            // This is because during a save we receive a list of RedirectData, and we need to keep track if an existing
            // stored redirect has been deleted or not.
            Set<String> notFoundProperties = new HashSet<String>(condition.getDeviceProperties().keySet());

            for (RedirectDevicePropertyConditionData propertyConditionData : redirectConditionData
                    .getDevicePropertyConditionData()) {

                DeviceProperty deviceProperty = condition.getDeviceProperties().get(propertyConditionData.getPropertyName());
                // if not found, then create it
                if (deviceProperty == null) {
                    deviceProperty = condition.createDeviceProperty();
                    condition.getDeviceProperties().put(propertyConditionData.getPropertyName(), deviceProperty);
                } else {
                    // if found, remove from the nonFound list
                    notFoundProperties.remove(propertyConditionData.getPropertyName());
                }

                deviceProperty.setName(propertyConditionData.getPropertyName());

                if (propertyConditionData.getGreaterThan() != null) {
                    deviceProperty.setGreaterThan(propertyConditionData.getGreaterThan());
                }

                if (propertyConditionData.getLessThan() != null) {
                    deviceProperty.setLessThan(propertyConditionData.getLessThan());
                }

                if (propertyConditionData.getEquals() != null) {
                    deviceProperty.setEquals(propertyConditionData.getEquals());
                }

                if (propertyConditionData.getMatches() != null) {
                    deviceProperty.setPattern(propertyConditionData.getMatches().toString());
                }
            }

            // remove all the device conditions which were not in the RedirectConditionData list
            for (String devicePropertyName : notFoundProperties) {
                condition.getDeviceProperties().remove(devicePropertyName);
            }
        }
    }

    private void buildMappings(RedirectMappingsData mappingsData, Mappings mappings) {
        mappings.setNodeNameMatching(mappingsData.isUseNodeNameMatching());
        mappings.setUnresolvedNodeMatching(mappingsData.getUnresolvedNode());

        // if the mappings conditions is empty and there are currently no stored mappings, then skip
        // otherwise we need to either remove or add mappings
        if (!mappingsData.getMappings().isEmpty() || !mappings.getNodeMap().isEmpty()) {

            // A list of stored node mappings which have not been found in the current RedirectMappingsData list.
            //
            // This is because during a save we receive a list of nodes from RedirectMappingsData, and we need to keep track
            // if an existing stored redirect node has been deleted or not.
            Set<String> notFoundNodeMappings = new HashSet<String>(mappings.getNodeMap().keySet());

            for (String key : mappingsData.getMappings().keySet()) {
                NodeMap nodeMap = mappings.getNodeMap().get(Text.escapeIllegalJcrChars(key));
                if (nodeMap == null) {
                    nodeMap = mappings.createNode();
                    mappings.getNodeMap().put(Text.escapeIllegalJcrChars(key), nodeMap);
                } else {
                    notFoundNodeMappings.remove(Text.escapeIllegalJcrChars(key));
                }
                nodeMap.setOriginNode(key);
                nodeMap.setRedirectNode(mappingsData.getMappings().get(key));
            }

            // remove all the device conditions which were not in the RedirectConditionData list
            for (String nodeName : notFoundNodeMappings) {
                mappings.getNodeMap().remove(nodeName);
            }
        }
    }

    public PageData load(org.gatein.mop.api.workspace.Page src) {
        Site site = src.getSite();
        String ownerType = getOwnerType(site.getObjectType());
        String ownerId = site.getName();
        String name = src.getName();
        List<ComponentData> children = loadChildren(src.getRootComponent());

        List<String> moveAppsPermissions = null;
        List<String> moveContainersPermissions = null;
        if (src.isAdapted(ProtectedContainer.class)) {
            ProtectedContainer pc = src.adapt(ProtectedContainer.class);
            moveAppsPermissions = pc.getMoveAppsPermissions();
            moveContainersPermissions = pc.getMoveContainersPermissions();
        } else {
            moveAppsPermissions = Collections.emptyList();
            moveContainersPermissions = Collections.emptyList();
        }
        //
        return new PageData(src.getObjectId(), null, name, null, null, null, null, null, null, null,
                Collections.<String> emptyList(), children, ownerType, ownerId, null, false, moveAppsPermissions,
                moveContainersPermissions);
    }

    private ContainerData load(UIContainer src, List<ComponentData> children) {
        //
        List<String> accessPermissions = Collections.emptyList();
        if (src.isAdapted(ProtectedResource.class)) {
            ProtectedResource pr = src.adapt(ProtectedResource.class);
            accessPermissions = pr.getAccessPermissions();
        }
        List<String> moveAppsPermissions = null;
        List<String> moveContainersPermissions = null;
        if (src.isAdapted(ProtectedContainer.class)) {
            ProtectedContainer pc = src.adapt(ProtectedContainer.class);
            moveAppsPermissions = pc.getMoveAppsPermissions();
            moveContainersPermissions = pc.getMoveContainersPermissions();
        } else {
            /* legacy mode */
            moveAppsPermissions = Container.DEFAULT_MOVE_APPLICATIONS_PERMISSIONS;
            moveContainersPermissions = Container.DEFAULT_MOVE_CONTAINERS_PERMISSIONS;
        }

        //
        Described described = src.adapt(Described.class);

        Attributes attrs = src.getAttributes();
        return new ContainerData(src.getObjectId(), attrs.getValue(MappedAttributes.ID), attrs.getValue(MappedAttributes.NAME),
                attrs.getValue(MappedAttributes.ICON), attrs.getValue(MappedAttributes.TEMPLATE),
                attrs.getValue(MappedAttributes.FACTORY_ID), described.getName(), described.getDescription(),
                attrs.getValue(MappedAttributes.WIDTH), attrs.getValue(MappedAttributes.HEIGHT),
                Utils.safeImmutableList(accessPermissions), Utils.safeImmutableList(moveAppsPermissions),
                Utils.safeImmutableList(moveContainersPermissions), children);
    }

    private List<ComponentData> loadChildren(UIContainer src) {
        if (src == null)
            throw new NoSuchDataException("Can not load children");
        ArrayList<ComponentData> children = new ArrayList<ComponentData>();
        for (UIComponent component : src.getComponents()) {

            // Obtain a model object from the ui component
            ComponentData mo;
            if (component instanceof UIContainer) {
                UIContainer srcContainer = (UIContainer) component;
                Attributes attrs = srcContainer.getAttributes();
                String type = attrs.getValue(MappedAttributes.TYPE);
                if ("dashboard".equals(type)) {
                    // Gadget is dropped so we just ignore the dashboard
                    continue;
                }
                List<ComponentData> dstChildren = loadChildren(srcContainer);
                mo = load(srcContainer, dstChildren);
            } else if (component instanceof UIWindow) {
                UIWindow window = (UIWindow) component;
                ApplicationData<?> application = load(window);
                if (application == null) {
                  continue;
                }
                mo = application;
            } else if (component instanceof UIBody) {
                mo = new BodyData(component.getObjectId(), BodyType.PAGE);
            } else {
                throw new AssertionError();
            }

            // Add among children
            children.add(mo);
        }
        return children;
    }

    public List<ModelChange> save(PageData src, Site site, String name) throws IllegalStateException {
        org.gatein.mop.api.workspace.Page root = site.getRootPage();
        org.gatein.mop.api.workspace.Page pages = root.getChild("pages");
        org.gatein.mop.api.workspace.Page dst = pages.getChild(name);

        //
        LinkedList<ModelChange> changes = new LinkedList<ModelChange>();

        //
        if (dst == null) {
            throw new NoSuchDataException("The page " + name + " not found");
        } else {
            changes.add(new ModelChange.Update(src));
        }

        //
        UIContainer rootContainer = dst.getRootComponent();

        // We are creating a new Page with the root container id as this one is lost
        // in the model / ui layer. Not doing this cause a class cast exception later
        // so it's likely the best fix we can do at the moment
        PageData src2 = new PageData(rootContainer.getObjectId(), src.getId(), src.getName(), src.getIcon(), src.getTemplate(),
                null, null, null, src.getWidth(), src.getHeight(), Collections.<String> emptyList(), src.getChildren(),
                src.getOwnerType(), src.getOwnerId(), null, false, src.getMoveAppsPermissions(),
                src.getMoveContainersPermissions());

        //
        LinkedList<ModelChange> childrenChanges = saveChildren(src2, rootContainer);

        //
        changes.addAll(childrenChanges);

        //
        return changes;
    }

    private void save(ContainerData src, UIContainer dst) {

        ProtectedResource pr = dst.adapt(ProtectedResource.class);
        pr.setAccessPermissions(src.getAccessPermissions());

        ProtectedContainer pc = dst.adapt(ProtectedContainer.class);
        pc.setMoveAppsPermissions(src.getMoveAppsPermissions());
        pc.setMoveContainersPermissions(src.getMoveContainersPermissions());

        Described described = dst.adapt(Described.class);
        described.setName(src.getTitle());
        described.setDescription(src.getDescription());

        Attributes dstAttrs = dst.getAttributes();
        dstAttrs.setValue(MappedAttributes.ID, src.getId());
        dstAttrs.setValue(MappedAttributes.TYPE, null);
        dstAttrs.setValue(MappedAttributes.ICON, src.getIcon());
        dstAttrs.setValue(MappedAttributes.TEMPLATE, src.getTemplate());
        dstAttrs.setValue(MappedAttributes.FACTORY_ID, src.getFactoryId());
        dstAttrs.setValue(MappedAttributes.WIDTH, src.getWidth());
        dstAttrs.setValue(MappedAttributes.HEIGHT, src.getHeight());
        dstAttrs.setValue(MappedAttributes.NAME, src.getName());
    }

    /*
     * Performs routing of the corresponding save method
     */
    private void save(ModelData src, WorkspaceObject dst, LinkedList<ModelChange> changes,
            Map<String, String> hierarchyRelationships) {
        if (src instanceof ContainerData) {
            save((ContainerData) src, (UIContainer) dst);
            saveChildren((ContainerData) src, (UIContainer) dst, changes, hierarchyRelationships);
        } else if (src instanceof ApplicationData) {
            save((ApplicationData<?>) src, (UIWindow) dst);
        } else if (src instanceof BodyData) {
            // Stateless
        } else {
            throw new AssertionError("Was not expecting child " + src);
        }
    }

    private LinkedList<ModelChange> saveChildren(final ContainerData src, UIContainer dst) {
        LinkedList<ModelChange> changes = new LinkedList<ModelChange>();

        // The relationship in the hierarchy
        // basically it's a map of the relationships between parent/child nodes
        // that is helpful to detect move operations
        // that we make immutable to avoid any bug
        Map<String, String> hierarchyRelationships = new HashMap<String, String>();
        build(src, hierarchyRelationships);
        hierarchyRelationships = Collections.unmodifiableMap(hierarchyRelationships);

        //
        saveChildren(src, dst, changes, hierarchyRelationships);

        //
        return changes;
    }

    private void build(ContainerData parent, Map<String, String> hierarchyRelationships) {
        String parentId = parent.getStorageId();
        for (ModelData child : parent.getChildren()) {
            String childId = child.getStorageId();
            if (childId != null) {
                if (hierarchyRelationships.containsKey(childId)) {
                    throw new AssertionError("The same object is present two times in the object hierarchy");
                }

                // Note that we are aware that parent id may be null
                hierarchyRelationships.put(childId, parentId);
            }
            if (child instanceof ContainerData) {
                build((ContainerData) child, hierarchyRelationships);
            }
        }
    }

    private void saveChildren(final ContainerData src, UIContainer dst, LinkedList<ModelChange> changes,
            Map<String, String> hierarchyRelationships) {
        final List<String> orders = new ArrayList<String>();
        final Map<String, ModelData> modelObjectMap = new HashMap<String, ModelData>();

        //
        for (ModelData srcChild : src.getChildren()) {
            String srcChildId = srcChild.getStorageId();
            // Flag variable, become non null if and only if we are saving a transient dashboard
            ApplicationData<?> transientDashboardData = null;

            //
            UIComponent dstChild;
            if (srcChildId != null) {
                dstChild = session.findObjectById(ObjectType.COMPONENT, srcChildId);
                if (dstChild == null) {
                    throw new StaleModelException("Could not find supposed present child with id " + srcChildId);
                }

                // julien : this can fail due to a bug in chromattic not implementing equals method properly
                // and is replaced with the foreach below
                /*
                 * if (!dst.contains(dstChild)) { throw new IllegalArgumentException("Attempt for updating a ui component " +
                 * session.pathOf(dstChild) + "that is not present in the target ui container " + session.pathOf(dst)); }
                 */
                boolean found = false;
                for (UIComponent child : dst.getComponents()) {
                    if (child.getObjectId().equals(srcChildId)) {
                        found = true;
                        break;
                    }
                }

                //
                if (!found) {
                    if (hierarchyRelationships.containsKey(srcChildId)) {
                        String srcId = hierarchyRelationships.get(srcChildId);

                        // It's a move operation, so we move the node first
                        dst.getComponents().add(dstChild);

                        //
                        changes.add(new ModelChange.Move(srcId, dst.getObjectId(), srcChildId));
                    } else {
                        throw new IllegalArgumentException("Attempt for updating a ui component " + session.pathOf(dstChild)
                                + " that is not present in the target ui container " + session.pathOf(dst));
                    }
                }

                //
                changes.add(new ModelChange.Update(srcChild));
            } else {
                String name = srcChild.getStorageName();
                if (name == null) {
                    // We manufacture one name
                    name = UUID.randomUUID().toString();
                }
                if (srcChild instanceof ContainerData) {
                    dstChild = dst.add(ObjectType.CONTAINER, name);
                } else if (srcChild instanceof ApplicationData) {
                    dstChild = dst.add(ObjectType.WINDOW, name);
                } else if (srcChild instanceof BodyData) {
                    dstChild = dst.add(ObjectType.BODY, name);
                } else {
                    throw new StaleModelException("Was not expecting child " + srcChild);
                }
                changes.add(new ModelChange.Create(dst.getObjectId(), srcChild));
            }

            //
            if (transientDashboardData != null) {
                Attributes attrs = dstChild.getAttributes();
                attrs.setValue(MappedAttributes.SHOW_INFO_BAR, transientDashboardData.isShowInfoBar());
                attrs.setValue(MappedAttributes.SHOW_MODE, transientDashboardData.isShowApplicationMode());
                attrs.setValue(MappedAttributes.SHOW_WINDOW_STATE, transientDashboardData.isShowApplicationState());
                attrs.setValue(MappedAttributes.THEME, transientDashboardData.getTheme());
            }
            save(srcChild, dstChild, changes, hierarchyRelationships);

            //
            String dstChildId = dstChild.getObjectId();
            modelObjectMap.put(dstChildId, srcChild);
            orders.add(dstChildId);
        }

        // Take care of move operation that could be seen as a remove otherwise
        for (UIComponent dstChild : dst.getComponents()) {
            String dstChildId = dstChild.getObjectId();
            if (!modelObjectMap.containsKey(dstChildId)) {
                String parentId = hierarchyRelationships.get(dstChildId);
                if (parentId != null) {
                    // Get the new parent
                    UIContainer parent = session.findObjectById(ObjectType.CONTAINER, parentId);

                    // Perform the move
                    parent.getComponents().add(dstChild);

                    //
                    changes.add(new ModelChange.Move(dst.getObjectId(), parentId, dstChildId));

                    // julien : we do not need to create an update operation
                    // as later the update operation will be created when the
                    // object
                    // will be processed
                } else if (hierarchyRelationships.containsKey(dstChildId)) {
                    // The dstChild is placed under transient Chromattic entity whose storageId == null. However,
                    // the hierachyRelationships contains dstChildId in key set, so we have to mark dstChild as
                    // moved object
                    modelObjectMap.put(dstChildId, null);
                }
            }
        }

        // Delete removed children
        for (Iterator<UIComponent> i = dst.getComponents().iterator(); i.hasNext();) {
            UIComponent dstChild = i.next();
            String dstChildId = dstChild.getObjectId();
            if (!modelObjectMap.containsKey(dstChildId)) {
                i.remove();
                changes.add(new ModelChange.Destroy(dstChildId));
            }
        }

        // Now sort children according to the order provided by the container
        // need to replace that with Collections.sort once the set(int index, E element) is implemented in Chromattic lists
        UIComponent[] a = dst.getComponents().toArray(new UIComponent[dst.getComponents().size()]);
        Arrays.sort(a, new Comparator<UIComponent>() {
            public int compare(UIComponent o1, UIComponent o2) {
                int i1 = orders.indexOf(o1.getObjectId());
                int i2 = orders.indexOf(o2.getObjectId());
                return i1 - i2;
            }
        });
        for (int j = 0; j < a.length; j++) {
            dst.getComponents().add(j, a[j]);
        }
    }

    public <S> ApplicationData<S> load(UIWindow src) {
        Attributes attrs = src.getAttributes();

        //
        Customization<?> customization = src.getCustomization();
        ApplicationType<S> type = null;
        PersistentApplicationState<S> instanceState = null;
        if (customization == null) {
          return null;
        } else {
          ContentType<?> contentType = customization.getType();
          String customizationid = customization.getId();

          type = (ApplicationType<S>) ApplicationType.getType(contentType);
          instanceState = new PersistentApplicationState<S>(customizationid);
        }

        //
        HashMap<String, String> properties = new HashMap<String, String>();
        load(attrs, properties, windowPropertiesBlackList);

        //
        List<String> accessPermissions = Collections.emptyList();
        if (src.isAdapted(ProtectedResource.class)) {
            ProtectedResource pr = src.adapt(ProtectedResource.class);
            accessPermissions = pr.getAccessPermissions();
        }

        //
        Described described = src.adapt(Described.class);

        //
        boolean showInfoBar = attrs.getValue(MappedAttributes.SHOW_INFO_BAR, false);
        boolean showWindowState = attrs.getValue(MappedAttributes.SHOW_WINDOW_STATE, false);
        boolean showMode = attrs.getValue(MappedAttributes.SHOW_MODE, false);
        String theme = attrs.getValue(MappedAttributes.THEME, null);

        //
        return new ApplicationData<S>(src.getObjectId(), src.getName(), type, instanceState, null, described.getName(),
                attrs.getValue(MappedAttributes.ICON), described.getDescription(), showInfoBar, showWindowState, showMode,
                theme, attrs.getValue(MappedAttributes.WIDTH), attrs.getValue(MappedAttributes.HEIGHT),
                Utils.safeImmutableMap(properties), Utils.safeImmutableList(accessPermissions));
    }

    public <S> void save(ApplicationData<S> src, UIWindow dst) {

        ProtectedResource pr = dst.adapt(ProtectedResource.class);
        pr.setAccessPermissions(src.getAccessPermissions());

        Described described = dst.adapt(Described.class);
        described.setName(src.getTitle());
        described.setDescription(src.getDescription());

        //
        Attributes attrs = dst.getAttributes();
        attrs.setValue(MappedAttributes.SHOW_INFO_BAR, src.isShowInfoBar());
        attrs.setValue(MappedAttributes.SHOW_WINDOW_STATE, src.isShowApplicationState());
        attrs.setValue(MappedAttributes.SHOW_MODE, src.isShowApplicationMode());
        attrs.setValue(MappedAttributes.THEME, src.getTheme());
        attrs.setValue(MappedAttributes.ICON, src.getIcon());
        attrs.setValue(MappedAttributes.WIDTH, src.getWidth());
        attrs.setValue(MappedAttributes.HEIGHT, src.getHeight());
        save(src.getProperties(), attrs, windowPropertiesBlackList);

        //
        ApplicationState<S> instanceState = src.getState();

        // We modify only transient portlet state
        // and we ignore any persistent portlet state
        if (instanceState instanceof TransientApplicationState) {

            //
            TransientApplicationState<S> transientState = (TransientApplicationState<S>) instanceState;

            // Attempt to get a site from the instance state
            Site site = null;
            if (transientState.getOwnerType() != null && transientState.getOwnerId() != null) {
                ObjectType<Site> siteType = parseSiteType(transientState.getOwnerType());
                site = session.getWorkspace().getSite(siteType, transientState.getOwnerId());
            }

            // The current site
            Site currentSite = dst.getPage().getSite();

            // If it is the same site than the current page
            // set null
            if (site == dst.getPage().getSite()) {
                site = null;
            }

            // The content id
            String contentId = transientState.getContentId();
            ContentType<S> contentType = src.getType().getContentType();

            // The customization that we will inherit from if not null
            Customization<?> customization = null;

            // Destroy existing window previous customization
            if (dst.getCustomization() != null) {
                dst.getCustomization().destroy();
            }

            // If the existing customization is not null and matches the content id
            Customization<S> dstCustomization;
            if (customization != null && customization.getType().equals(contentType)
                    && customization.getContentId().equals(contentId)) {

                // Cast is ok as content type matches
                @SuppressWarnings("unchecked")
                Customization<S> bilto = (Customization<S>) customization;

                // If it's a customization of the current site we extend it
                if (bilto.getContext() == currentSite) {
                    dstCustomization = dst.customize(bilto);
                } else {
                    // Otherwise we clone it propertly
                    S state = bilto.getVirtualState();
                    dstCustomization = dst.customize(contentType, contentId, state);
                }
            } else {
                // Otherwise we create an empty customization
                dstCustomization = dst.customize(contentType, contentId, null);
            }

            // At this point we have customized the window
            // now if we have any additional state payload we must merge it
            // with the current state
            S state = ((TransientApplicationState<S>) instanceState).getContentState();
            if (state != null) {
                dstCustomization.setState(state);
            }
        } else if (instanceState instanceof CloneApplicationState) {
            CloneApplicationState cloneState = (CloneApplicationState) instanceState;

            //
            Customization<?> customization = session.findCustomizationById(cloneState.getStorageId());

            //
            dst.customize(customization);
        } else if (instanceState instanceof PersistentApplicationState) {
            // Do nothing
        } else {
            throw new IllegalArgumentException("Cannot save application with state " + instanceState);
        }
    }

    private static void load(Attributes src, Map<String, String> dst, Set<String> blackList) {
        for (String name : src.getKeys()) {
            if (!blackList.contains(name) && !propertiesBlackList.contains(name)) {
                Object value = src.getObject(name);
                if (value instanceof String) {
                    dst.put(name, (String) value);
                }
            }
        }
    }

    public static void save(Map<String, String> src, Attributes dst, Set<String> blackList) {
        for (Map.Entry<String, String> property : src.entrySet()) {
            String name = property.getKey();
            if (!blackList.contains(name) && !propertiesBlackList.contains(name)) {
                dst.setString(name, property.getValue());
            }
        }
    }

    public static String getOwnerType(ObjectType<? extends Site> siteType) {
        if (siteType == ObjectType.PORTAL_SITE) {
            return PortalConfig.PORTAL_TYPE;
        } else if (siteType == ObjectType.GROUP_SITE) {
            return PortalConfig.GROUP_TYPE;
        } else if (siteType == ObjectType.USER_SITE) {
            return PortalConfig.USER_TYPE;
        } else {
            throw new IllegalArgumentException("Invalid site type " + siteType);
        }
    }

    public static ObjectType<Site> parseSiteType(String ownerType) {
        if (ownerType.equals(PortalConfig.PORTAL_TYPE)) {
            return ObjectType.PORTAL_SITE;
        } else if (ownerType.equals(PortalConfig.GROUP_TYPE)) {
            return ObjectType.GROUP_SITE;
        } else if (ownerType.equals(PortalConfig.USER_TYPE)) {
            return ObjectType.USER_SITE;
        } else {
            throw new IllegalArgumentException("Invalid owner type " + ownerType);
        }
    }
}
