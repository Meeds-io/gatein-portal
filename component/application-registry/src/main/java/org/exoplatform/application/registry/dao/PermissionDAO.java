/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.application.registry.dao;

import java.util.List;

import org.exoplatform.application.registry.entity.PermissionEntity;
import org.exoplatform.application.registry.entity.PermissionEntity.TYPE;
import org.exoplatform.commons.api.persistence.GenericDAO;

public interface PermissionDAO extends GenericDAO<PermissionEntity, Long> {
  public List<PermissionEntity> getPermissions(String refType, Long refId, TYPE type);

  public int deletePermissions(String refType, Long refId);

  public List<PermissionEntity> savePermissions(String refType, Long refId, TYPE type, List<String> permissions);
}
