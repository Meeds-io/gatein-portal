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
package org.exoplatform.portal.application;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.filter.ActionFilter;
import javax.portlet.filter.EventFilter;
import javax.portlet.filter.FilterChain;
import javax.portlet.filter.FilterConfig;
import javax.portlet.filter.RenderFilter;
import javax.portlet.filter.ResourceFilter;

import org.exoplatform.container.PortalContainer;

/**
 * @author <a href="hoang281283@gmail.com">Minh Hoang TO</a>
 */
public class ApplicationMonitoringFilter implements ActionFilter, RenderFilter, EventFilter, ResourceFilter {

    public void init(FilterConfig cfg) throws PortletException {
    }

    public void destroy() {
    }

    public void doFilter(ActionRequest req, ActionResponse resp, FilterChain chain) throws IOException, PortletException {
        ApplicationStatistic stat = get(req);
        if (stat != null) {
            long t = -System.currentTimeMillis();
            chain.doFilter(req, resp);
            t += System.currentTimeMillis();
            stat.logTime(t);
        } else {
            chain.doFilter(req, resp);
        }
    }

    public void doFilter(EventRequest req, EventResponse resp, FilterChain chain) throws IOException, PortletException {
        ApplicationStatistic stat = get(req);
        if (stat != null) {
            long t = -System.currentTimeMillis();
            chain.doFilter(req, resp);
            t += System.currentTimeMillis();
            stat.logTime(t);
        } else {
            chain.doFilter(req, resp);
        }
    }

    public void doFilter(RenderRequest req, RenderResponse resp, FilterChain chain) throws IOException, PortletException {
        ApplicationStatistic stat = get(req);
        if (stat != null) {
            long t = -System.currentTimeMillis();
            chain.doFilter(req, resp);
            t += System.currentTimeMillis();
            stat.logTime(t);
        } else {
            chain.doFilter(req, resp);
        }
    }

    public void doFilter(ResourceRequest req, ResourceResponse resp, FilterChain chain) throws IOException, PortletException {
        ApplicationStatistic stat = get(req);
        if (stat != null) {
            long t = -System.currentTimeMillis();
            chain.doFilter(req, resp);
            t += System.currentTimeMillis();
            stat.logTime(t);
        } else {
            chain.doFilter(req, resp);
        }
    }

    private ApplicationStatistic get(PortletRequest req) {
        PortalContainer container = PortalContainer.getInstance();
        ApplicationStatisticService service = (ApplicationStatisticService) container
                .getComponentInstance(ApplicationStatisticService.class);
        if (service != null) {
            PortletConfig portletConfig = (PortletConfig) req.getAttribute("javax.portlet.config");
            String portletName = portletConfig.getPortletName();
            String phase = (String) req.getAttribute(PortletRequest.LIFECYCLE_PHASE);
            String applicationId = portletName + "/" + phase;
            return service.getApplicationStatistic(applicationId);
        } else {
            return null;
        }
    }
}
