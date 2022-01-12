package org.exoplatform.services.organization.idm;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.picketlink.idm.impl.model.hibernate.*;

public class IdmHibernateConfiguration extends Configuration {
  public SessionFactory buildSessionFactory() throws HibernateException {
    ServiceRegistry servReg = getStandardServiceRegistryBuilder().applySettings(getProperties())
                                                                 .build();
    addAnnotatedClass(HibernateIdentityObject.class);
    addAnnotatedClass(HibernateIdentityObjectAttribute.class);
    addAnnotatedClass(HibernateIdentityObjectAttributeBinaryValue.class);
    addAnnotatedClass(HibernateIdentityObjectCredential.class);
    addAnnotatedClass(HibernateIdentityObjectCredentialBinaryValue.class);
    addAnnotatedClass(HibernateIdentityObjectCredentialType.class);
    addAnnotatedClass(HibernateIdentityObjectRelationship.class);
    addAnnotatedClass(HibernateIdentityObjectRelationshipName.class);
    addAnnotatedClass(HibernateIdentityObjectRelationshipType.class);
    addAnnotatedClass(HibernateIdentityObjectType.class);
    addAnnotatedClass(HibernateRealm.class);
    return super.buildSessionFactory(servReg);
  }
}
