package org.exoplatform.commons.persistence.impl;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * Startable service to initialize all the data with Liquibase.
 * Changelog files are added by external plugins.
 */
public class LiquibaseDataInitializer implements Startable, DataInitializer {
  private static final Log LOG = ExoLogger.getLogger(LiquibaseDataInitializer.class);

  public static final String LIQUIBASE_DATASOURCE_PARAM_NAME = "liquibase.datasource";
  public static final String LIQUIBASE_CONTEXTS_PARAM_NAME = "liquibase.contexts";
  public static final String LIQUIBASE_DEFAULT_CONTEXTS = "production";

  private String datasourceName;

  private String liquibaseContexts;

  private List<ChangeLogsPlugin> changeLogsPlugins = new ArrayList<>();

  public LiquibaseDataInitializer(InitialContextInitializer initialContextInitializer, InitParams initParams) {
    this(initParams);
  }

  public LiquibaseDataInitializer(InitParams initParams) {
    if (initParams == null) {
      throw new IllegalArgumentException("No InitParams found for LiquibaseDataInitializer service. The datasource name ("
          + LIQUIBASE_DATASOURCE_PARAM_NAME + ") should be defined at least.");
    }

    ValueParam liquibaseDatasourceNameParam = initParams.getValueParam(LIQUIBASE_DATASOURCE_PARAM_NAME);
    if (liquibaseDatasourceNameParam != null && liquibaseDatasourceNameParam.getValue() != null) {
      datasourceName = liquibaseDatasourceNameParam.getValue();
    } else {
      throw new IllegalArgumentException("Datasource name for LiquibaseDataInitializer must be defined in the init params ("
          + LIQUIBASE_DATASOURCE_PARAM_NAME + ")");
    }

    ValueParam liquibaseContextsParam = initParams.getValueParam(LIQUIBASE_CONTEXTS_PARAM_NAME);
    if (liquibaseContextsParam != null && liquibaseContextsParam.getValue() != null) {
      liquibaseContexts = liquibaseContextsParam.getValue();
    } else {
      liquibaseContexts = LIQUIBASE_DEFAULT_CONTEXTS;
    }

    LOG.info("LiquibaseDataInitializer created with : datasourceName={}, contexts={}", datasourceName, liquibaseContexts);
  }

  public String getDatasourceName() {
    return datasourceName;
  }

  public void setDatasourceName(String datasourceName) {
    this.datasourceName = datasourceName;
  }

  public String getContexts() {
    return liquibaseContexts;
  }

  public void setContexts(String liquibaseContexts) {
    this.liquibaseContexts = liquibaseContexts;
  }

  /**
   * Add a changelogs plugin
   * @param changeLogsPlugin Changelogs plugin to add
   */
  public void addChangeLogsPlugin(ChangeLogsPlugin changeLogsPlugin) {
    this.changeLogsPlugins.add(changeLogsPlugin);
  }


  @Override
  public void start() {
    initData();
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  /**
   * Initialize the data with Liquibase with the default datasource.
   */
  @Override
  public void initData() {
    initData(this.datasourceName);
  }

  /**
   * Initialize the data with Liquibase with the given datasource.
   * Iterates over all the changelogs injected by the change logs plugins and executes them.
   */
  @Override
  public void initData(String datasourceName) {
    if (changeLogsPlugins.isEmpty()) {
      LOG.info("No data to initialize with Liquibase");
    } else {
      LOG.info("Starting data initialization with Liquibase with datasource {0}", datasourceName);

      DataSource datasource = getDatasource(datasourceName);
      for (ChangeLogsPlugin changeLogsPlugin : this.changeLogsPlugins) {
        LOG.info("Processing changelogs of " + changeLogsPlugin.getName());
        String changelogDatasourceName = changeLogsPlugin.getDatasourceName();
        DataSource changelogDatasource;
        if (StringUtils.isBlank(changelogDatasourceName)) {
          changelogDatasource = datasource;
          if (changelogDatasource == null) {
            LOG.error("Data initialization of '{}' aborted because the datasource {} has not been found.",
                      changeLogsPlugin.getName(),
                      datasourceName);
            return;
          }
        } else {
          changelogDatasource = getDatasource(changelogDatasourceName);
          if (changelogDatasource == null) {
            LOG.error("Data initialization of '{}' aborted because the datasource {} has not been found.",
                      changeLogsPlugin.getName(),
                      changelogDatasourceName);
            return;
          }
        }
        for (String changelogsPath : changeLogsPlugin.getChangelogPaths()) {
          LOG.info("  * processing changelog " + changelogsPath);
          applyChangeLog(changelogDatasource, changelogsPath);
        }
      }
    }
  }

  /**
   * Apply changelogs with Liquibase
   * @param datasource
   * @param changelogsPath
   */
  protected void applyChangeLog(DataSource datasource, String changelogsPath) {
    Database database = null;
    try {
      database = DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(datasource.getConnection()));
      // Adding "RANK" in data base reserved words as a workaround until Liquibase metadata reserved keywords update.
      database.addReservedWords(Arrays.asList("RANK"));
      Liquibase liquibase = new Liquibase(changelogsPath, new ClassLoaderResourceAccessor(), database);
      liquibase.update(liquibaseContexts);
    } catch (SQLException e) {
      LOG.error("Error while getting a JDBC connection from datasource " + datasourceName + " - Cause : " + e.getMessage(), e);
    } catch (LiquibaseException e) {
      LOG.error("Error while applying liquibase changelogs " + changelogsPath + " - Cause : " + e.getMessage(), e);
    } finally {
      if(database != null) {
        try {
          database.close();
        } catch (DatabaseException e) {
          LOG.error("Error while closing database connection - Cause : " + e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Lookup for a datasource with the given name
   * @param datasourceName Name of the datasource to retrieve
   * @return The datasource with the given name
   */
  protected DataSource getDatasource(String datasourceName) {
    DataSource dataSource = null;
    try {
      Context initCtx = new InitialContext();

      // Look up our data source
      dataSource = (DataSource) initCtx.lookup(datasourceName);
    } catch (NamingException e) {
      LOG.error("Cannot find datasource " + datasourceName + " - Cause : " + e.getMessage(), e);
    }

    return dataSource;
  }
}
