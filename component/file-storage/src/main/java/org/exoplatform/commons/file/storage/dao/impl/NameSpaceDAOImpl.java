/*
 * Copyright (C) 2016 eXo Platform SAS.
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
package org.exoplatform.commons.file.storage.dao.impl;

import org.exoplatform.commons.file.storage.dao.NameSpaceDAO;
import org.exoplatform.commons.file.storage.entity.NameSpaceEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 */
public class NameSpaceDAOImpl extends GenericDAOJPAImpl<NameSpaceEntity, Long> implements NameSpaceDAO {

  private static final Log LOG = ExoLogger.getLogger(NameSpaceDAOImpl.class);

  public NameSpaceEntity getNameSpaceByName(String name) {
    TypedQuery<NameSpaceEntity> query = getEntityManager().createNamedQuery("nameSpace.getNameSpaceByName", NameSpaceEntity.class)
                                                          .setParameter("name", name);
    List<NameSpaceEntity> list = query.getResultList();
    if (list == null || list.isEmpty()) {
      return null;
    } else if (list.size() > 1) {
      LOG.warn("More than one namespace with name '{}' was found, return first one.", name);
    }
    return list.get(0);
  }
}
