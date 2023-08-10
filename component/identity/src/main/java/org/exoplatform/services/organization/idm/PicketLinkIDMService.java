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

package org.exoplatform.services.organization.idm;

import org.gatein.portal.idm.impl.store.attribute.ExtendedAttributeManager;
import org.picketlink.idm.api.IdentitySession;
import org.picketlink.idm.api.IdentitySessionFactory;
import org.picketlink.idm.api.cfg.IdentityConfiguration;
import org.picketlink.idm.spi.configuration.metadata.IdentityConfigurationMetaData;

/**
 * @author <a href="mailto:boleslaw.dawidowicz@redhat.com">Boleslaw Dawidowicz</a>
 */
public interface PicketLinkIDMService {

    IdentitySessionFactory getIdentitySessionFactory();

    IdentitySession getIdentitySession() throws Exception;

    IdentitySession getIdentitySession(String realm) throws Exception;

    IdentityConfigurationMetaData getConfigMD();

    IdentityConfiguration getIdentityConfiguration();

    ExtendedAttributeManager getExtendedAttributeManager() throws Exception;

}
