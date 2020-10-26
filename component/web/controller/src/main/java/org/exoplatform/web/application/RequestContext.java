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

package org.exoplatform.web.application;

import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.web.url.PortalURL;
import org.exoplatform.web.url.ResourceType;
import org.exoplatform.web.url.URLFactory;

/**
 * Created by The eXo Platform SAS May 7, 2006
 *
 * This abstract class is a wrapper on top of the request information such as the Locale in use, the application (for instance
 * PortalApplication, PortletApplication...), an access to the JavascriptManager as well as a reference to the URLBuilder in
 * use.
 *
 * It also contains a ThreadLocal object for an easy access.
 *
 * Context can be nested and hence a getParentAppRequestContext() is also available
 *
 */
public abstract class RequestContext {

    public static final String ACTION = "op";

    private static ThreadLocal<RequestContext> tlocal_ = new ThreadLocal<RequestContext>();

    private Application app_;

    protected final RequestContext parentAppRequestContext_;

    private Map<String, Object> attributes;

    public RequestContext(Application app) {
        this.app_ = app;
        this.parentAppRequestContext_ = null;
    }

    protected RequestContext(RequestContext parentAppRequestContext, Application app_) {
        this.parentAppRequestContext_ = parentAppRequestContext;
        this.app_ = app_;
    }

    public Application getApplication() {
        return app_;
    }

    public Locale getLocale() {
        return parentAppRequestContext_.getLocale();
    }

    /**
     * Returns the url factory associated with this context.
     *
     * @return the url factory
     */
    public abstract URLFactory getURLFactory();

    public abstract <R, U extends PortalURL<R, U>> U newURL(ResourceType<R, U> resourceType, URLFactory urlFactory);

    public final <R, U extends PortalURL<R, U>> U createURL(ResourceType<R, U> resourceType, R resource) {
        U url = createURL(resourceType);

        // Set the resource on the URL
        url.setResource(resource);

        //
        return url;
    }

    public final <R, L extends PortalURL<R, L>> L createURL(ResourceType<R, L> resourceType) {
        // Get the provider
        URLFactory provider = getURLFactory();

        // Create an URL from the factory
        return newURL(resourceType, provider);
    }

    /**
     * Returns the orientation for the current request.
     *
     * @return the orientation
     */
    public abstract Orientation getOrientation();

    public ResourceBundle getApplicationResourceBundle() {
        return null;
    }

    public abstract String getRequestParameter(String name);

    public abstract String[] getRequestParameterValues(String name);

    public abstract URLBuilder<?> getURLBuilder();

    public String getRemoteUser() {
        return parentAppRequestContext_.getRemoteUser();
    }

    public boolean isUserInRole(String roleUser) {
        return parentAppRequestContext_.isUserInRole(roleUser);
    }

    public abstract boolean useAjax();

    public boolean getFullRender() {
        return true;
    }

    public ApplicationSession getApplicationSession() {
        throw new RuntimeException("This method is not supported");
    }

    public Writer getWriter() throws Exception {
        return parentAppRequestContext_.getWriter();
    }

    public void setWriter(Writer writer) {
        parentAppRequestContext_.setWriter(writer);
    }

    public final Object getAttribute(String name) {
        if (attributes == null)
            return null;
        return attributes.get(name);
    }

    public final void setAttribute(String name, Object value) {
        if (attributes == null)
            attributes = new HashMap<String, Object>();
        attributes.put(name, value);
    }

    public final Object getAttribute(Class type) {
        return getAttribute(type.getName());
    }

    public final void setAttribute(Class type, Object value) {
        setAttribute(type.getName(), value);
    }

    public RequestContext getParentAppRequestContext() {
        return parentAppRequestContext_;
    }

    public abstract UserPortal getUserPortal();

    @SuppressWarnings("unchecked")
    public static <T extends RequestContext> T getCurrentInstance() {
        return (T) tlocal_.get();
    }

    public static void setCurrentInstance(RequestContext ctx) {
        tlocal_.set(ctx);
    }

}
