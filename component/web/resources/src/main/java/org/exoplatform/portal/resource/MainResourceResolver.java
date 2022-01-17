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

package org.exoplatform.portal.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletContext;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class MainResourceResolver implements ResourceResolver {

    /** . */
    final Map<String, SimpleResourceContext> contexts;

    /** . */
    final CopyOnWriteArrayList<ResourceResolver> resolvers;

    /** . */
    private final Log                          log = ExoLogger.getLogger(MainResourceResolver.class);

    public MainResourceResolver() {
        this.contexts = new HashMap<String, SimpleResourceContext>();
        this.resolvers = new CopyOnWriteArrayList<ResourceResolver>();
    }

    /**
     * Register a servlet request context
     * <p>
     * Append a servlet context to the map of contexts if servlet context name is not existing
     *
     * @param servletContext servlet context which want to append
     * @return
     */
    SimpleResourceContext registerContext(ServletContext servletContext) {
        String key = "/" + servletContext.getServletContextName();
        SimpleResourceContext ctx = contexts.get(key);
        if (ctx == null) {
            ctx = new SimpleResourceContext(key, servletContext);
            contexts.put(ctx.getContextPath(), ctx);
        }
        return ctx;
    }

    /**
     * Remove a servlet context from map of contexts
     *
     * @param servletContext
     */
    public void removeServletContext(ServletContext servletContext) {
        String key = "/" + servletContext.getServletContextName();
        SimpleResourceContext ctx = contexts.get(key);
        if (ctx != null) {
            contexts.remove(ctx.getContextPath());
        } else {
            log.warn("Cannot find servlet context module");
            return;
        }
    }

    public Resource resolve(String path) {
        if (path == null) {
            throw new NullPointerException("No null path is accepted");
        }

        //
        for (ResourceResolver resolver : resolvers) {
            Resource res = resolver.resolve(path);
            if (res != null) {
                return res;
            }
        }

        //
        int i1 = path.indexOf("/", 2);
        if (i1 == -1) {
            throw new AssertionError();
        }
        String targetedContextPath = path.substring(0, i1);
        SimpleResourceContext context = contexts.get(targetedContextPath);

        //
        if (context == null) {
            log.warn("Could not resolve " + targetedContextPath + " resource for path " + path);
            return null;
        } else {
            return context.getResource(path.substring(i1));
        }

    }
}
