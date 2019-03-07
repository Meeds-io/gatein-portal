/*
 * Copyright (C) 2019 eXo Platform SAS.
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
package org.exoplatform.application.gadget.impl;

import java.util.*;
import java.util.concurrent.Callable;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.chromattic.api.Chromattic;
import org.chromattic.api.ChromatticSession;
import org.chromattic.ext.ntdef.Resource;
import org.exoplatform.application.gadget.Gadget;
import org.exoplatform.application.gadget.GadgetImporter;
import org.exoplatform.application.gadget.GadgetRegistryService;
import org.exoplatform.application.registry.impl.ApplicationRegistryChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.security.IdentityConstants;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class GadgetRegistryServiceImpl implements GadgetRegistryService {

    /** . */
    private final Logger log = LoggerFactory.getLogger(GadgetRegistryServiceImpl.class);

    /** . */
    private static final String DEFAULT_DEVELOPER_GROUP = "/platform/administrators";

    /** . */
    private ChromatticLifeCycle chromatticLifeCycle;

    /** . */
    private String country;

    /** . */
    private String language;

    /** . */
    private String moduleId;

    /** . */
    private String hostName;

    private RepositoryService repoService;

    public GadgetRegistryServiceImpl(ChromatticManager chromatticManager, RepositoryService repoService, InitParams params) {
        ApplicationRegistryChromatticLifeCycle lifeCycle = (ApplicationRegistryChromatticLifeCycle) chromatticManager
                .getLifeCycle("app");

        //
        String gadgetDeveloperGroup = null;
        String country = null;
        String language = null;
        String moduleId = null;
        String hostName = null;
        if (params != null) {
            PropertiesParam properties = params.getPropertiesParam("developerInfo");
            gadgetDeveloperGroup = properties != null ? properties.getProperty("developer.group") : null;
            ValueParam gadgetCountry = params.getValueParam("gadgets.country");
            country = gadgetCountry != null ? gadgetCountry.getValue() : null;
            ValueParam gadgetLanguage = params.getValueParam("gadgets.language");
            language = gadgetLanguage != null ? gadgetLanguage.getValue() : null;
            ValueParam gadgetModuleId = params.getValueParam("gadgets.moduleId");
            moduleId = gadgetModuleId != null ? gadgetModuleId.getValue() : null;
            ValueParam gadgetHostName = params.getValueParam("gadgets.hostName");
            hostName = gadgetHostName != null ? gadgetHostName.getValue() : null;
        }

        //
        if (gadgetDeveloperGroup == null) {
            gadgetDeveloperGroup = DEFAULT_DEVELOPER_GROUP;
        }

        //
        this.country = country;
        this.language = language;
        this.moduleId = moduleId;
        this.hostName = hostName;
        this.chromatticLifeCycle = lifeCycle;
        this.repoService = repoService;
    }

    public GadgetRegistry getRegistry() {
        Chromattic chromattic = chromatticLifeCycle.getChromattic();
        ChromatticSession session = chromattic.openSession();
        GadgetRegistry registry = session.findByPath(GadgetRegistry.class, "app:gadgets");
        if (registry == null) {
            registry = session.insert(GadgetRegistry.class, "app:gadgets");
        }
        return registry;
    }

    // ***************

    public void deploy(Iterable<GadgetImporter> gadgets) {
        for (GadgetImporter importer : gadgets) {
            try {
                new DeployTask(importer).call();
            } catch (Exception e) {
                log.error("Could not process gadget file " + importer, e);
            }
        }
    }

    public Gadget getGadget(String name) {
        GadgetRegistry registry = getRegistry();

        //
        GadgetDefinition def = registry.getGadget(name);

        //
        return def == null ? null : loadGadget(def);
    }

    public List<Gadget> getAllGadgets() throws Exception {
        return getAllGadgets(null);
    }

    public List<Gadget> getAllGadgets(Comparator<Gadget> sortComparator) {
        GadgetRegistry registry = getRegistry();
        List<Gadget> gadgets = new ArrayList<Gadget>();
        for (GadgetDefinition def : registry.getGadgets()) {
            Gadget gadget = loadGadget(def);
            gadgets.add(gadget);
        }
        if (sortComparator != null) {
            Collections.sort(gadgets, sortComparator);
        }
        return gadgets;
    }

    public void saveGadget(Gadget gadget) throws Exception {
        if (gadget == null) {
            throw new NullPointerException();
        }

        //
        GadgetRegistry registry = getRegistry();
        GadgetDefinition def = registry.getGadget(gadget.getName());

        //
        if (def == null) {
            def = registry.addGadget(gadget.getName());

            if (gadget.isLocal()) {
                def.setLocal(true);
                LocalGadgetData data = (LocalGadgetData) def.getData();
                String fileName = gadget.getName() + ".xml";
                data.setFileName(fileName);
                data.getResources().createFile(
                        fileName,
                        new Resource("application.xml", "UTF-8", ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Module><ModulePrefs title=\"\" />" + "<Content type=\"html\"> <![CDATA[]]>" + "</Content>"
                                + "</Module>").getBytes("UTF-8")));
            } else {
                def.setLocal(false);
                RemoteGadgetData data = (RemoteGadgetData) def.getData();
                data.setURL(gadget.getUrl());
            }
        }

        if (!gadget.isLocal()) {
            def.setDescription(gadget.getDescription());
            def.setReferenceURL(gadget.getReferenceUrl());
            def.setTitle(gadget.getTitle());
            def.setThumbnail(gadget.getThumbnail());
        }

        updatePermissions(gadget.getName());
    }

    public void removeGadget(String name) {
        if (name == null) {
            throw new NullPointerException();
        }

        //
        GadgetRegistry registry = getRegistry();
        GadgetDefinition def = registry.getGadget(name);

        //
        if (def == null) {
            throw new IllegalArgumentException("No such gadget " + name);
        }

        //
        registry.removeGadget(name);
    }

    public String getGadgetURL(String gadgetName) {
        String url;
        GadgetData data = this.getRegistry().getGadget(gadgetName).getData();
        if (data instanceof LocalGadgetData) {
            LocalGadgetData localData = (LocalGadgetData) data;
            url = "/" + PortalContainer.getCurrentRestContextName() + "/" + getJCRGadgetURL(localData);
        } else if (data instanceof RemoteGadgetData) {
            RemoteGadgetData remoteData = (RemoteGadgetData) data;
            url = remoteData.getURL();
        } else {
            throw new IllegalStateException("Gadget has to be instance of LocalGadgetData or RemoteGadgetData");
        }

        return url;
    }

    private String getJCRGadgetURL(LocalGadgetData data) {
        return "jcr/" + chromatticLifeCycle.getRepositoryName() + "/" + chromatticLifeCycle.getWorkspaceName() + data.getPath()
                + "/app:resources/" + data.getFileName();
    }

    private Gadget loadGadget(GadgetDefinition def) {
        GadgetData data = def.getData();
        Gadget gadget = new Gadget();

        //
        if (data instanceof LocalGadgetData) {
            try {
                String gadgetName = def.getName();
                LocalGadgetData localData = (LocalGadgetData) data;
                Resource resource = localData.getResources().getFile(localData.getFileName()).getContentResource();
                String content = new String(resource.getData(), resource.getEncoding());
                GadgetSpec gadgetSpec = new GadgetSpec(Uri.parse(getGadgetURL(gadgetName)), content);
                ModulePrefs prefs = gadgetSpec.getModulePrefs();

                String title = prefs.getDirectoryTitle();
                if (title == null || title.trim().length() < 1) {
                    title = prefs.getTitle();
                }
                if (title == null || title.trim().length() < 1) {
                    title = gadgetName;
                }
                gadget.setName(def.getName());
                gadget.setDescription(prefs.getDescription());
                gadget.setLocal(true);
                gadget.setTitle(title);
                gadget.setReferenceUrl(prefs.getTitleUrl().toString());
                gadget.setThumbnail(prefs.getThumbnail().toString());
                gadget.setUrl(getJCRGadgetURL(localData));
            } catch (Exception ex) {
                log.error("Error while loading the content of local gadget " + def.getName(), ex);
            }
        } else {
            RemoteGadgetData remoteData = (RemoteGadgetData) data;
            gadget.setName(def.getName());
            gadget.setDescription(def.getDescription());
            gadget.setLocal(false);
            gadget.setTitle(def.getTitle());
            gadget.setReferenceUrl(def.getReferenceURL());
            gadget.setThumbnail(def.getThumbnail());
            gadget.setUrl(remoteData.getURL());
        }
        //
        return gadget;
    }

    public boolean isGadgetDeveloper(String username) {
        return PropertyManager.isDevelopping();
    }

    public String getCountry() {
        return country;
    }

    public String getLanguage() {
        return language;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getHostName() {
        return hostName;
    }

    private class DeployTask implements Callable<Boolean> {

        /** . */
        private final GadgetImporter importer;

        private DeployTask(GadgetImporter importer) {
            this.importer = importer;
        }

        public Boolean call() throws Exception {
            chromatticLifeCycle.openContext();
            boolean done = true;
            try {
                if (getRegistry().getGadget(importer.getGadgetName()) == null) {
                    GadgetDefinition def = getRegistry().addGadget(importer.getGadgetName());
                    importer.doImport(def);

                    updatePermissions(importer.getGadgetName());
                } else {
                    log.debug("Will not import existing gagdet " + importer.getGadgetName());
                }
            } catch (Exception e) {
                done = false;
            } finally {
                chromatticLifeCycle.closeContext(done);
            }

            return done;
        }
    }

    private void updatePermissions(String gadgetName) throws RepositoryException {
        ChromatticSession chromatticSession = chromatticLifeCycle.getContext().getSession();
        Session session = chromatticSession.getJCRSession();
        session.save();

        ExtendedNode extendedNode = (ExtendedNode)session.getItem("/production/app:gadgets/app:" + gadgetName);
        if (extendedNode.canAddMixin("exo:privilegeable")) {
            extendedNode.addMixin("exo:privilegeable");
        }
        extendedNode.setPermission(IdentityConstants.ANY, PermissionType.DEFAULT_AC);
        extendedNode.save();
    }
}
