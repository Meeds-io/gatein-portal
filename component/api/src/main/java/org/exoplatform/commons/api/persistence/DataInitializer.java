package org.exoplatform.commons.api.persistence;

import javax.sql.DataSource;

/**
 * Interface for data initialization
 */
public interface DataInitializer {

  void initData();

  void initData(String datasourceName);

  default DataSource getDatasource() {
    throw new UnsupportedOperationException();
  }

}
