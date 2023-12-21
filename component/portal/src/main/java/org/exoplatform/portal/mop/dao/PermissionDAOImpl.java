package org.exoplatform.portal.mop.dao;

import java.util.*;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.PermissionEntity;
import org.exoplatform.portal.jdbc.entity.PermissionEntity.TYPE;

public class PermissionDAOImpl extends GenericDAOJPAImpl<PermissionEntity, Long> implements PermissionDAO {

  @Override
  public List<PermissionEntity> getPermissions(String refType, Long refId, TYPE type) {
    if (refId == null || type == null) {
      return Collections.emptyList();
    }
    
    TypedQuery<PermissionEntity> query = getEntityManager().createNamedQuery("PermissionEntity.getPermissions", PermissionEntity.class);
    query.setParameter("refType", refType);
    query.setParameter("refId", refId);
    query.setParameter("type", type);
    
    return query.getResultList();
  }

  @ExoTransactional
  @Override
  public int deletePermissions(String refType, Long refId) {
    if (refId == null) {
      return 0;
    }
    Query query = getEntityManager().createNamedQuery("PermissionEntity.deleteByRefId");
    query.setParameter("refType", refType);
    query.setParameter("refId", refId);
    return query.executeUpdate();
  }

  @Override
  public List<PermissionEntity> savePermissions(String refType, Long refId, TYPE type, List<String> permissions) {
    if (refId == null || type == null || refType == null) {
      throw new IllegalArgumentException("refType, refId , type must not be null");
    }

    List<PermissionEntity> oldPers = getPermissions(refType, refId, type);

    List<PermissionEntity> results = new LinkedList<>();
    if (permissions != null) {
      for (String permission : permissions) {
        PermissionEntity entity = new PermissionEntity(refType, refId, permission, type);
        
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
    
    deleteAll(new ArrayList<>(oldPers));
    return results;
  }

}
