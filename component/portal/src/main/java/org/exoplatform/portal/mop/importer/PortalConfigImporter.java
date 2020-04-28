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

package org.exoplatform.portal.mop.importer;

import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="trongtt@gmail.com">Trong Tran</a>
 * @version $Revision$
 */
public class PortalConfigImporter {
    /** . */
    private static final Log LOG = ExoLogger.getLogger(PortalConfigImporter.class);

    /** . */
    private final PortalConfig src;

    /** . */
    private final DataStorage service;

    /** . */
    private final ImportMode mode;

    public PortalConfigImporter(ImportMode importMode, PortalConfig portal, DataStorage dataStorage_) {
        this.mode = importMode;
        this.src = portal;
        this.service = dataStorage_;
    }

    public void perform() throws Exception {
        PortalConfig existingPortalConfig = service.getPortalConfig(src.getType(), src.getName());
        PortalConfig dst = null;

        //
        boolean portalNotExists = existingPortalConfig == null;
        switch (mode) {
            case CONSERVE:
            case INSERT:
                if (portalNotExists) {
                  dst = src;
                } else {
                  dst = null;
                }
                break;
            case MERGE:
            case OVERWRITE:
                dst = src;
                break;
            default:
                throw new AssertionError();
        }

        if (dst != null) {
            LOG.info("Importing Portal config of site of type '{}' with name '{}', import mode '{}', already exists = {}",
                     src.getType(),
                     src.getName(),
                     mode,
                     portalNotExists);
            if (portalNotExists) {
                service.create(dst);
            } else {
                service.save(dst);
            }
        }
    }
}
