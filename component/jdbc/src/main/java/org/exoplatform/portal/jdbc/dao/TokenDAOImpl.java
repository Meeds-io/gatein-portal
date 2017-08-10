package org.exoplatform.portal.jdbc.dao;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.shindig.gadgets.oauth.BasicOAuthStoreTokenIndex;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.jdbc.entity.GadgetTokenEntity;

public class TokenDAOImpl extends GenericDAOJPAImpl<GadgetTokenEntity, Long> implements TokenDAO {

  @Override
  public GadgetTokenEntity findByKey(BasicOAuthStoreTokenIndex key) {
    TypedQuery<GadgetTokenEntity> query =
                                        getEntityManager().createNamedQuery("GadgetTokenEntity.findByKey",
                                                                            GadgetTokenEntity.class);

    query.setParameter("userId", key.getUserId());
    query.setParameter("gadgetUri", key.getGadgetUri());
    query.setParameter("moduleId", key.getModuleId());
    query.setParameter("tokenName", key.getTokenName());
    query.setParameter("serviceName", key.getServiceName());
    try {
      return query.getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  @ExoTransactional
  @Override
  public void deleteByKey(BasicOAuthStoreTokenIndex key) {
    Query query = getEntityManager().createNamedQuery("GadgetTokenEntity.deleteByKey");

    query.setParameter("userId", key.getUserId());
    query.setParameter("gadgetUri", key.getGadgetUri());
    query.setParameter("moduleId", key.getModuleId());
    query.setParameter("tokenName", key.getTokenName());
    query.setParameter("serviceName", key.getServiceName());
    query.executeUpdate();
  }
}
