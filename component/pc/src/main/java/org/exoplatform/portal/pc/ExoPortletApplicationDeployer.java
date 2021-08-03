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

package org.exoplatform.portal.pc;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.commons.utils.Safe;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.pc.portlet.container.ContainerPortletInvoker;
import org.gatein.pc.portlet.container.managed.ManagedObjectRegistryEventListener;
import org.gatein.pc.portlet.impl.deployment.DeploymentException;
import org.gatein.pc.portlet.impl.deployment.PortletApplicationDeployer;
import org.gatein.pc.portlet.impl.deployment.PortletApplicationDeployment;
import org.gatein.pc.portlet.impl.deployment.staxnav.PortletApplicationMetaDataBuilder;
import org.gatein.pc.portlet.impl.metadata.PortletApplication10MetaData;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Extends the {@link org.gatein.pc.portlet.impl.deployment.PortletApplicationDeployer} to inject configuration metadata from
 * global portlet.xml and to configure the resource bundle factory of deployed portlet applications. The resource bundle factory
 * used is {@link org.exoplatform.portal.pc.ExoResourceBundleFactory}.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class ExoPortletApplicationDeployer extends PortletApplicationDeployer {

    /** . */
    private final Logger log = LoggerFactory.getLogger(ExoPortletApplicationDeployer.class);

    private ManagedObjectRegistryEventListener cdiListener;

    public ExoPortletApplicationDeployer() {
        this(null);
    }

    public ExoPortletApplicationDeployer(ContainerPortletInvoker containerPortletInvoker) {
        super(containerPortletInvoker);

        try {
            Class clazz = Class.forName("org.gatein.cdi.CDIInjectionListener");
            Object listenerInstance = clazz.newInstance();
            if (listenerInstance instanceof ManagedObjectRegistryEventListener) {
                cdiListener = (ManagedObjectRegistryEventListener) listenerInstance;

                if (null != cdiListener) {
                    // Add listener to perform CDI Injection
                    this.addListener(cdiListener);
                }
            }
        } catch (Exception e) {
            log.warn("Unable to create org.gatein.cdiCDIInjectionListener", e);
        }
    }

    @Override
    protected PortletApplication10MetaData buildPortletApplicationMetaData(ServletContext webApp) throws DeploymentException {
        PortletApplication10MetaData md = super.buildPortletApplicationMetaData(webApp);
        if (md != null) {
            md.setResourceBundleFactoryName(ExoResourceBundleFactory.class.getName());

            String globalPortletLocation = PropertyManager.getProperty("gatein.portlet.config");
            if (globalPortletLocation != null) {
                try {
                    GlobalPortletMetaData globalPortletMetaData = loadGlobalMetadata(globalPortletLocation);
                    if (globalPortletMetaData != null) {
                      globalPortletMetaData.mergeTo(md);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Complete merging global portlet metadata to portlet application "
                                + webApp.getServletContextName());
                    }
                } catch (Exception ex) {
                    if (log.isErrorEnabled()) {
                        log.error(
                                "Error during merge global portlet metadata to portlet application "
                                        + webApp.getServletContextName(), ex);
                    }
                }
            } else {
                log.warn("The global portlet metadata is not configured");
            }

        }
        return md;
    }

    @Override
    protected PortletApplicationDeployment createPortletApplicationDeployment(ServletContext webApp, PortletApplication10MetaData metaData) {
        return super.createPortletApplicationDeployment(webApp, metaData);
    }

    /**
     * This method is invoked for each portlet application deployment. That is necessary for the moment to ensure independence
     * between portlet applications
     *
     * @return the global meta data
     * @throws Exception any exception
     */
    private GlobalPortletMetaData loadGlobalMetadata(String globalPortletLocation) throws Exception {
        File globalPortletFile = new File(globalPortletLocation);
        if (globalPortletFile.exists()) {
          InputStream in = new FileInputStream(globalPortletFile);
          try {
              PortletApplicationMetaDataBuilder builder = new PortletApplicationMetaDataBuilder();
              return GlobalPortletMetaData.unmarshalling(in);
          } finally {
              Safe.close(in);
          }
        } else {
          return null;
        }
    }
}
