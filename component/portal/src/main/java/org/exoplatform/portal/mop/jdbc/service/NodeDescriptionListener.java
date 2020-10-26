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
package org.exoplatform.portal.mop.jdbc.service;

import org.exoplatform.portal.jdbc.entity.NodeEntity;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class NodeDescriptionListener extends Listener<NodeEntity, String> {

  private DescriptionService service;
  
  public NodeDescriptionListener(DescriptionService service) {
    this.service = service;
  }

  @Override
  public void onEvent(Event<NodeEntity, String> event) throws Exception {
    service.setDescription(event.getData(), null);
  }

}
