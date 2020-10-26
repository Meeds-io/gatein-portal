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
package org.exoplatform.portal.resource.compressor.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.ManagementAware;
import org.exoplatform.management.ManagementContext;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.portal.resource.compressor.ResourceCompressor;
import org.exoplatform.portal.resource.compressor.ResourceCompressorException;
import org.exoplatform.portal.resource.compressor.ResourceCompressorPlugin;
import org.exoplatform.portal.resource.compressor.ResourceType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="mailto:hoang281283@gmail.com">Minh Hoang TO</a> Aug 19, 2010
 */
@Managed
@ManagedDescription("The resource compressor service")
@NameTemplate({ @Property(key = "service", value = "resource") })
public class ResourceCompressorService implements ResourceCompressor, ManagementAware {
    /** . */
    private Log log = ExoLogger.getLogger(ResourceCompressorService.class);

    /** . */
    private Map<ResourceType, List<ResourceCompressorPlugin>> plugins;

    public ResourceCompressorService(InitParams params) throws Exception {

        plugins = new HashMap<ResourceType, List<ResourceCompressorPlugin>>();
    }

    public void registerCompressorPlugin(ComponentPlugin plugin) {
        if (plugin instanceof ResourceCompressorPlugin) {
            ResourceCompressorPlugin compressorPlugin = (ResourceCompressorPlugin) plugin;
            ResourceType type = compressorPlugin.getResourceType();
            List<ResourceCompressorPlugin> sameResourceTypePlugins = plugins.get(type);

            if (sameResourceTypePlugins != null) {
                sameResourceTypePlugins.add(compressorPlugin);
                log.debug("Loaded compressor plugin: " + compressorPlugin.getName() + " for resource type " + type.toString());
            } else {
                List<ResourceCompressorPlugin> newListOfPlugins = new ArrayList<ResourceCompressorPlugin>();
                newListOfPlugins.add(compressorPlugin);
                log.debug("Loaded compressor plugin: " + compressorPlugin.getName() + " for new resource type "
                        + type.toString());
                plugins.put(type, newListOfPlugins);
            }
        }
    }

    public ResourceCompressorPlugin getCompressorPlugin(ResourceType type, String name) {
        List<ResourceCompressorPlugin> sameResourceTypePlugins = plugins.get(type);

        if (sameResourceTypePlugins != null) {
            for (ResourceCompressorPlugin plugin : sameResourceTypePlugins) {
                if (plugin.getName().equals(name)) {
                    return plugin;
                }
            }
        }
        return null;
    }

    @Override
    public final boolean isSupported(ResourceType resourceType) {
        return (getHighestPriorityCompressorPlugin(resourceType) != null);
    }

    @Override
    public final void compress(Reader input, Writer output, ResourceType resourceType) throws ResourceCompressorException,
            IOException {
        ResourceCompressorPlugin plugin = getHighestPriorityCompressorPlugin(resourceType);
        if (plugin != null) {
            plugin.compress(input, output);
        } else {
            throw new ResourceCompressorException("There is no compressor for " + resourceType + " type");
        }
    }

    @Override
    public String compress(String input, ResourceType resourceType) throws ResourceCompressorException, IOException {
        StringReader reader = new StringReader(input);
        StringWriter writer = new StringWriter();
        compress(reader, writer, resourceType);
        return writer.toString();
    }

    public ResourceCompressorPlugin getHighestPriorityCompressorPlugin(ResourceType resourceType) {
        List<ResourceCompressorPlugin> candidates = plugins.get(resourceType);
        if (candidates == null || candidates.size() == 0) {
            return null;
        }

        // Loop the list instead of invoking sort method
        int highestPriorityIndex = 0;
        int maxPriority = -1;

        for (int i = 0; i < candidates.size(); i++) {
            int currentPriority = candidates.get(i).getPriority();
            if (currentPriority > maxPriority) {
                highestPriorityIndex = i;
                maxPriority = currentPriority;
            }
        }

        return candidates.get(highestPriorityIndex);
    }

    public void setContext(ManagementContext context) {
        if (context == null) {
            return;
        }
        //
        for (Map.Entry<ResourceType, List<ResourceCompressorPlugin>> entry : plugins.entrySet()) {
            for (ResourceCompressorPlugin plugin : entry.getValue()) {
                context.register(plugin);
            }
        }
    }
}
