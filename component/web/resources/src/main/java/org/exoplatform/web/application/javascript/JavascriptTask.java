/*
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
package org.exoplatform.web.application.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.ServletContext;

import org.gatein.portal.controller.resource.script.ScriptResource;

/**
 * @author <a href="mailto:hoang281283@gmail.com">Minh Hoang TO</a>
 * @version $Id$
 *
 */
public class JavascriptTask {

    private List<ScriptResourceDescriptor> descriptors;

    public JavascriptTask() {
        descriptors = new ArrayList<ScriptResourceDescriptor>();
    }

    public void execute(JavascriptConfigService service, ServletContext scontext) {
        for (ScriptResourceDescriptor desc : descriptors) {
            String contextPath = null;
            if (desc.modules.size() > 0) {
                contextPath = desc.modules.get(0).getContextPath();
            }

            ScriptResource resource = service.scripts.addResource(desc.id, desc.fetchMode, desc.alias, desc.group, contextPath);
            if (resource != null) {
                for (Javascript module : desc.modules) {
                    module.addModuleTo(resource);
                }
                for (Locale locale : desc.getSupportedLocales()) {
                    resource.addSupportedLocale(locale);
                }
                for (DependencyDescriptor dependency : desc.dependencies) {
                    resource.addDependency(dependency.getResourceId(), dependency.getAlias(), dependency.getPluginResource());
                }
            }
        }
    }

    public void addDescriptor(ScriptResourceDescriptor desc) {
        descriptors.add(desc);
    }
}
