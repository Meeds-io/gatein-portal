package org.exoplatform.services.organization.idm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import org.exoplatform.commons.api.persistence.ExoEntityProcessor;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class HibernateConfigurationImpl extends Configuration {
  private static final Log           LOG          = ExoLogger.getLogger(HibernateConfigurationImpl.class);

  public HibernateConfigurationImpl() throws HibernateException {
    super();
  }

  public SessionFactory buildSessionFactory() throws HibernateException {
    ServiceRegistry servReg = getStandardServiceRegistryBuilder().applySettings(getProperties())
                                                                 .build();

    try {
      Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(ExoEntityProcessor.ENTITIES_IDX_PATH);
      while (urls.hasMoreElements()) {
        try (InputStream stream = urls.nextElement().openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));) {
          String entityClassName;
          while ((entityClassName = reader.readLine()) != null) {
            try {
              LOG.info("Adding JPA Entity {}", entityClassName);
              addAnnotatedClass(Class.forName(entityClassName));
            } catch (ClassNotFoundException e) {
              LOG.error("Error while trying to register entity [{}] in Persistence Unit", entityClassName, e);
            }
          }
        }
      }
    } catch (IOException e) {
      LOG.error("Error while scanning Annotated classes", e);
    }

    return buildSessionFactory(servReg);
  }
}
