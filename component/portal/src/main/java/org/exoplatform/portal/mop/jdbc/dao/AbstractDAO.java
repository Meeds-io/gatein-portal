package org.exoplatform.portal.mop.jdbc.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.portal.mop.jdbc.entity.ComponentEntity;

public class AbstractDAO<T extends ComponentEntity> extends GenericDAOJPAImpl<T, Long> {
}
