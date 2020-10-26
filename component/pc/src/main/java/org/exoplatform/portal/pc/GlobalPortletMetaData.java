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
package org.exoplatform.portal.pc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gatein.pc.portlet.impl.deployment.staxnav.PortletApplicationMetaDataBuilder;
import org.gatein.pc.portlet.impl.metadata.PortletApplication10MetaData;
import org.gatein.pc.portlet.impl.metadata.PortletApplication20MetaData;
import org.gatein.pc.portlet.impl.metadata.filter.FilterMappingMetaData;
import org.gatein.pc.portlet.impl.metadata.filter.FilterMetaData;

/**
 * @author <a href="hoang281283@gmail.com">Minh Hoang TO</a>
 */
public class GlobalPortletMetaData {
    private PortletApplication10MetaData wrappedMetaData;

    public GlobalPortletMetaData(PortletApplication10MetaData _wrappedMetaData) {
        this.wrappedMetaData = _wrappedMetaData;
    }

    public PortletApplication10MetaData mergeTo(PortletApplication10MetaData tobeMergedMetaData) {
        if (wrappedMetaData.getCustomPortletModes() != null) {
            tobeMergedMetaData.getCustomPortletModes().putAll(wrappedMetaData.getCustomPortletModes());
        }

        if (wrappedMetaData.getCustomWindowStates() != null) {
            tobeMergedMetaData.getCustomWindowStates().putAll(wrappedMetaData.getCustomWindowStates());
        }

        if (tobeMergedMetaData instanceof PortletApplication20MetaData
                && wrappedMetaData instanceof PortletApplication20MetaData) {
            return merge20MetData((PortletApplication20MetaData) wrappedMetaData,
                    (PortletApplication20MetaData) tobeMergedMetaData);
        }

        return tobeMergedMetaData;

    }

    public PortletApplication10MetaData merge20MetData(PortletApplication20MetaData globalMetaData,
            PortletApplication20MetaData tobeMergedMetaData) {
        mergeFilterMetaData(globalMetaData, tobeMergedMetaData);
        mergeFilterMapping(globalMetaData, tobeMergedMetaData);
        mergePublicRenderParameters(globalMetaData, tobeMergedMetaData);

        return tobeMergedMetaData;
    }

    private void mergeFilterMetaData(PortletApplication20MetaData globalMetaData,
            PortletApplication20MetaData tobeMergedMetaData) {
        Map<String, FilterMetaData> globalFilters = globalMetaData.getFilters();
        Map<String, FilterMetaData> applicationFilters = tobeMergedMetaData.getFilters();

        if (globalFilters != null) {
            if (applicationFilters == null) {
                tobeMergedMetaData.setFilters(globalFilters);
                return;
            }

            applicationFilters.putAll(globalFilters);
            tobeMergedMetaData.setFilters(applicationFilters);
        }
    }

    private void mergeFilterMapping(PortletApplication20MetaData globalMetaData, PortletApplication20MetaData tobeMergedMetaData) {
        List<FilterMappingMetaData> applicationFilterMappings = tobeMergedMetaData.getFilterMapping();
        if (applicationFilterMappings == null) {
            applicationFilterMappings = new ArrayList<FilterMappingMetaData>(3);
        }

        Map<String, FilterMetaData> globalFilters = globalMetaData.getFilters();
        if (globalFilters == null) {
            return;
        } else {
            // TODO: Ensure there is no duplicated filter mapping
            for (String filterName : globalFilters.keySet()) {
                FilterMappingMetaData filterMapping = new FilterMappingMetaData();
                filterMapping.setName(filterName);

                // TODO: Use this list, examine if there is a bug in PC on the instantiation of filter mappings
                // List<String> portletsApplyingThisGlobalFilter =
                // findPortletsApplyingGlobalFilter(globalFilters.get(filterName), tobeMergedMetaData);

                List<String> portletsApplyingThisGlobalFilter = new ArrayList<String>(3);
                portletsApplyingThisGlobalFilter.add("*");
                filterMapping.setPortletNames(portletsApplyingThisGlobalFilter);
                applicationFilterMappings.add(filterMapping);
            }

            tobeMergedMetaData.setFilterMapping(applicationFilterMappings);
        }

    }

    private void mergePublicRenderParameters(PortletApplication20MetaData globalMetaData,
            PortletApplication20MetaData tobeMergedMetaData) {
        // TODO: Wait for the spec of merging public render parameters
    }

    /** . */
    private static final PortletApplicationMetaDataBuilder builder = new PortletApplicationMetaDataBuilder();

    public static GlobalPortletMetaData unmarshalling(InputStream in) throws Exception {
        PortletApplication10MetaData application10MetaData = builder.build(in);
        return new GlobalPortletMetaData(application10MetaData);
    }
}
