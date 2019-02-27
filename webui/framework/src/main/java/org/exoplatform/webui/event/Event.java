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

package org.exoplatform.webui.event;

import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.security.csrf.CSRFTokenUtil;
import org.exoplatform.webui.application.WebuiRequestContext;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

public class Event<T> {

    private String name_;

    private T source_;

    private Phase executionPhase_ = Phase.PROCESS;

    private WebuiRequestContext context_;

    private List<EventListener> listeners_;

    private boolean csrfCheck;

    private static final Log LOG = ExoLogger.getLogger(Event.class);

    public Event(T source, String name, WebuiRequestContext context) {
        name_ = name;
        source_ = source;
        context_ = context;
    }

    public String getName() {
        return name_;
    }

    public T getSource() {
        return source_;
    }

    public Phase getExecutionPhase() {
        return executionPhase_;
    }

    public void setExecutionPhase(Phase phase) {
        executionPhase_ = phase;
    }

    public WebuiRequestContext getRequestContext() {
        return context_;
    }

    public void setRequestContext(WebuiRequestContext context) {
        context_ = context;
    }

    public List<EventListener> getEventListeners() {
        return listeners_;
    }

    public void setEventListeners(List<EventListener> listeners) {
        listeners_ = listeners;
    }

    public boolean isCsrfCheck() {
        return csrfCheck;
    }

    public void setCsrfCheck(boolean csrfCheck) {
        this.csrfCheck = csrfCheck;
    }

    public final void broadcast() throws Exception {
        if (isCsrfCheck() && !CSRFTokenUtil.check(getRequest())) {
            getRequestContext().setResponseComplete(true);
            LOG.warn("CSRF token is lost or this is an CSRF attack (event={})", this.getName());
        } else {
            for (EventListener<T> listener : listeners_) {
                listener.execute(this);
            }
        }
    }

    private static HttpServletRequest getRequest() {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        if (context != null && context.getRequest() instanceof PortletRequest) {
            context = (WebuiRequestContext) context.getParentAppRequestContext();
        }

        if (context != null) {
            return context.getRequest();
        } else {
            LOG.warn("Can't find portal context");
            return null;
        }
    }

    public static enum Phase {
        ANY, DECODE, PROCESS, RENDER
    }

}
