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
package org.exoplatform.portal.jdbc.dao;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.NavigationEntity;
import org.exoplatform.portal.mop.SiteType;

public class NavigationDAOImpl extends GenericDAOJPAImpl<NavigationEntity, String>implements NavigationDAO {

    @Override
    public NavigationEntity findByOwner(SiteType type, String name) {
        TypedQuery<NavigationEntity> query = getEntityManager().createNamedQuery("NavigationEntity.findByOwner",
                NavigationEntity.class);

        query.setParameter("ownerType", type);
        query.setParameter("ownerId", name);
        try {
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public void deleteByOwner(SiteType siteType, String name) {
      NavigationEntity entity = findByOwner(siteType, name);
      if (entity != null) {
        delete(entity);
      }
    }

}
