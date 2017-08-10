package org.exoplatform.portal.jdbc.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE;

public class PermissionDAOImpl extends GenericDAOJPAImpl<PermissionEntity, Long> implements PermissionDAO {

  @Override
  public List<PermissionEntity> getPermissions(String refId, TYPE type) {
    if (refId == null || type == null) {
      return Collections.emptyList();
    }
    
    TypedQuery<PermissionEntity> query = getEntityManager().createNamedQuery("PermissionEntity.getPermissions", PermissionEntity.class);
    query.setParameter("refId", refId);
    query.setParameter("type", type);
    
    return query.getResultList();
  }

  @ExoTransactional
  @Override
  public int deletePermissions(String refId) {
    if (refId == null) {
      return 0;
    }
    Query query = getEntityManager().createNamedQuery("PermissionEntity.deleteByRefId");
    query.setParameter("refId", refId);
    return query.executeUpdate();
  }

  @Override
  public List<PermissionEntity> savePermissions(String refId, TYPE type, List<String> permissions) {
    if (refId == null || type == null) {
      throw new IllegalArgumentException("refId , type must not be null");
    }

    List<PermissionEntity> oldPers = getPermissions(refId, type);

    List<PermissionEntity> results = new LinkedList<PermissionEntity>();
    if (permissions != null) {
      for (String permission : permissions) {
        PermissionEntity entity = new PermissionEntity(refId, permission, type);
        
        int idx = oldPers.indexOf(entity);
        if (idx != -1) {
          results.add(oldPers.get(idx));
          oldPers.remove(entity);
        } else {
          create(entity);
          results.add(entity);
        }        
      }      
    }
    
    deleteAll(new ArrayList<PermissionEntity>(oldPers));
    return results;
  }

}
