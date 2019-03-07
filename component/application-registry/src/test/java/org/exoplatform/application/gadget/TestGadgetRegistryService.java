/**
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

package org.exoplatform.application.gadget;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;

import org.apache.shindig.gadgets.spec.ModulePrefs;
import org.chromattic.ext.ntdef.NTFolder;
import org.chromattic.ext.ntdef.Resource;
import org.exoplatform.application.AbstractApplicationRegistryTest;
import org.exoplatform.application.gadget.impl.GadgetDefinition;
import org.exoplatform.application.gadget.impl.GadgetRegistryServiceImpl;
import org.exoplatform.application.gadget.impl.LocalGadgetData;
import org.exoplatform.application.gadget.impl.RemoteGadgetData;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.security.IdentityConstants;
import org.gatein.common.io.IOTools;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Pham Thanh Tung thanhtungty@gmail.com Jul 11, 2008
 */
public class TestGadgetRegistryService extends AbstractApplicationRegistryTest {

    private GadgetRegistryServiceImpl service_;

    private SourceStorage sourceStorage;

    private ChromatticManager chromatticManager;

    private ConfigurationManager configurationManager;

    public void setUp() throws Exception {
        PortalContainer container = PortalContainer.getInstance();
        service_ = (GadgetRegistryServiceImpl) container.getComponentInstanceOfType(GadgetRegistryService.class);
        sourceStorage = container.getComponentInstanceOfType(SourceStorage.class);
        chromatticManager = container.getComponentInstanceOfType(ChromatticManager.class);
        configurationManager = container.getComponentInstanceOfType(ConfigurationManager.class);
        begin();
    }

    @Override
    protected void tearDown() throws Exception {
        end();
    }

    public void testShouldLocalGadgetSavedWithRightPermissionsWhenCreatingLocalGadget() throws Exception {
        // Given
        String gadgetName = "local_test_save";
        Gadget gadget = new Gadget();
        gadget.setName(gadgetName);
        gadget.setLocal(true);

        Source source = new Source(gadgetName, "application/xml");
        URI gadgetURI = ClassLoader.getSystemResource("org/exoplatform/application/gadgets/weather.xml").toURI();
        source.setTextContent(new String(Files.readAllBytes(Paths.get(gadgetURI))));
        source.setLastModified(Calendar.getInstance());

        // When
        service_.saveGadget(gadget);
        sourceStorage.saveSource(gadget, source);

        // Then
        GadgetDefinition def = service_.getRegistry().getGadget(gadgetName);
        assertNotNull(def);
        assertEquals("__MSG_description__", def.getDescription());
        assertEquals("__MSG_gTitle__", def.getTitle());
        assertEquals("http://www.labpixies.com/campaigns/weather/images/thumbnail.jpg", def.getThumbnail());
        assertEquals("http://www.labpixies.com", def.getReferenceURL());

        Session session = chromatticManager.getLifeCycle("app").getContext().getSession().getJCRSession();
        ExtendedNode extendedNode = (ExtendedNode)session.getItem("/production/app:gadgets/app:" + gadget.getName());
        assertNotNull(extendedNode);
        List<AccessControlEntry> permissions = extendedNode.getACL().getPermissionEntries();
        assertNotNull(permissions);
        assertEquals(5, permissions.size());
        assertTrue(permissions.contains(new AccessControlEntry("*:/platform/administrators", "read")));
        assertTrue(permissions.contains(new AccessControlEntry("*:/platform/administrators", "add_node")));
        assertTrue(permissions.contains(new AccessControlEntry("*:/platform/administrators", "set_property")));
        assertTrue(permissions.contains(new AccessControlEntry("*:/platform/administrators", "remove")));
        assertTrue(permissions.contains(new AccessControlEntry(IdentityConstants.ANY, "read")));

        service_.removeGadget(gadgetName);
        assertNull(service_.getGadget(gadgetName));
    }

    public void testLocalGadget() throws Exception {
        String gadgetName = "local_test";
        TestGadgetImporter importer = new TestGadgetImporter(configurationManager, gadgetName,
                "org/exoplatform/application/gadgets/weather.xml", true);
        importer.doImport();

        GadgetDefinition def = service_.getRegistry().getGadget(gadgetName);
        assertNotNull(def);
        // No metadata is persisted in JCR
        assertNull(def.getDescription());
        assertNull(def.getTitle());
        assertNull(def.getThumbnail());
        assertNull(def.getReferenceURL());

        assertEquals(1, service_.getAllGadgets().size());
        Gadget gadget = service_.getGadget(gadgetName);
        assertNotNull(gadget);
        assertEquals(gadgetName, gadget.getName());
        assertEquals("__MSG_description__", gadget.getDescription());
        assertEquals("__MSG_gTitle__", gadget.getTitle());
        assertEquals("http://www.labpixies.com/campaigns/weather/images/thumbnail.jpg", gadget.getThumbnail());
        assertEquals("http://www.labpixies.com", gadget.getReferenceUrl());

        service_.removeGadget(gadgetName);
        assertNull(service_.getGadget(gadgetName));
    }

    public void testRemoteGadget() throws Exception {
        String gadgetName = "remote_test";
        //load xml from local file instead of other server which may cause error
        //This doens't change the test purpose: we're using mock importer, and this is for testing the service
        TestGadgetImporter importer = new TestGadgetImporter(configurationManager, gadgetName,
                "org/exoplatform/application/gadgets/weather.xml", false);
        importer.doImport();
        assertEquals(1, service_.getAllGadgets().size());
        assertEquals(gadgetName, service_.getGadget(gadgetName).getName());
        service_.removeGadget(gadgetName);
        assertNull(service_.getGadget(gadgetName));
    }

    public void testUrl() {

    }

    class TestGadgetImporter extends GadgetImporter {
        /** . */
        private final Logger log = LoggerFactory.getLogger(TestGadgetImporter.class);

        private boolean local_;

        private ConfigurationManager configurationManager;

        protected TestGadgetImporter(ConfigurationManager configurationManager, String gadgetName, String gadgetURI,
                boolean local) {
            super(gadgetName, gadgetURI);
            this.local_ = local;
            this.configurationManager = configurationManager;
        }

        @Override
        protected byte[] getGadgetBytes(String gadgetURI) throws IOException {
            String filePath = "classpath:/" + gadgetURI;
            InputStream in = null;
            try {
                in = configurationManager.getInputStream(filePath);
                return IOTools.getBytes(in);
            } catch (Exception e) {
                throw new IOException("not found " + filePath, e);
            }
        }

        @Override
        protected String getGadgetURL() {
            return getGadgetURI();
        }

        @Override
        protected void process(String gadgetURI, GadgetDefinition def) throws Exception {
            def.setLocal(local_);
            if (local_) {
                byte[] content = getGadgetBytes(gadgetURI);
                if (content != null) {
                    LocalGadgetData data = (LocalGadgetData) def.getData();
                    String fileName = getName(gadgetURI);
                    data.setFileName(fileName);
                    NTFolder folder = data.getResources();
                    String encoding = EncodingDetector.detect(new ByteArrayInputStream(content));
                    folder.createFile(fileName, new Resource(LocalGadgetData.GADGET_MIME_TYPE, encoding, content));
                }
            } else {
                RemoteGadgetData data = (RemoteGadgetData) def.getData();
                data.setURL(gadgetURI);
            }
        }

        @Override
        protected void processMetadata(ModulePrefs prefs, GadgetDefinition def) {
            if (!def.isLocal()) {
                String gadgetName = def.getName();
                String description = prefs.getDescription();
                String thumbnail = prefs.getThumbnail().toString();
                String title = getGadgetTitle(prefs, gadgetName);
                String referenceURL = prefs.getTitleUrl().toString();

                def.setDescription(description);
                def.setThumbnail(thumbnail);
                def.setTitle(title);
                def.setReferenceURL(referenceURL);
            }
        }

        private String getGadgetTitle(ModulePrefs prefs, String defaultValue) {
            String title = prefs.getDirectoryTitle();
            if (title == null || title.trim().length() < 1) {
                title = prefs.getTitle();
            }
            if (title == null || title.trim().length() < 1) {
                return defaultValue;
            }
            return title;
        }

        private String getName(String resourcePath) {
            // Get index of last '/'
            int index = resourcePath.lastIndexOf('/');

            // Return name
            return resourcePath.substring(index + 1);
        }

        public void doImport() throws Exception {
            GadgetDefinition def = service_.getRegistry().addGadget(getGadgetName());
            doImport(def);
        }
    }
}
