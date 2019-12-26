package org.exoplatform.portal.jdbc.dao;

import org.exoplatform.application.registry.entity.ComponentEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

public class AbstractDAO<T extends ComponentEntity> extends GenericDAOJPAImpl<T, Long> {
}
