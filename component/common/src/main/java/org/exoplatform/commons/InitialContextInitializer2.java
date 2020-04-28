/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.commons;

import java.io.FileNotFoundException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.naming.BindReferencePlugin;
import org.exoplatform.services.naming.InitialContextInitializer;

/**
 * This code should be moved in the core, for now it is here as it is needed here. It extends the
 * {@link org.exoplatform.services.naming.InitialContextInitializer} to override the
 * {@link #addPlugin(org.exoplatform.container.component.ComponentPlugin)} method and perform no binding if there is an existing
 * binding before.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class InitialContextInitializer2 extends InitialContextInitializer {

    public InitialContextInitializer2(InitParams params) throws NamingException, ConfigurationException, FileNotFoundException,
            XMLStreamException {
        super(params);
    }

    @Override
    public void addPlugin(ComponentPlugin plugin) {
        if (plugin instanceof BindReferencePlugin) {
            BindReferencePlugin brplugin = (BindReferencePlugin) plugin;
            InitialContext initialContext = getInitialContext();
            try {
                initialContext.lookup(brplugin.getBindName());
                // If we reach this step it means that something is already bound
            } catch (NamingException e) {
                super.addPlugin(plugin);
            }
        }
    }
}
