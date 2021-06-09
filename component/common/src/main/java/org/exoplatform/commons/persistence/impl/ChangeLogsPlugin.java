package org.exoplatform.commons.persistence.impl;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Changelog plugin to add Liquibase changelog path during the data initialization
 */
public class ChangeLogsPlugin extends BaseComponentPlugin {

  public static final String CHANGELOGS_PARAM_NAME = "changelogs";

  public static final String DATASOURCE_PARAM_NAME = "datasource";

  private List<String>       changelogPaths        = new ArrayList<String>();

  private String             datasourceName        = null;

  public ChangeLogsPlugin(InitParams initParams) {
    if(initParams != null) {
      ValuesParam changelogs = initParams.getValuesParam(CHANGELOGS_PARAM_NAME);
      if (changelogs != null) {
        changelogPaths.addAll(changelogs.getValues());
      }
      ValueParam datasourceNameValue = initParams.getValueParam(DATASOURCE_PARAM_NAME);
      if (datasourceNameValue != null) {
        this.datasourceName = datasourceNameValue.getValue();
      }
    }
  }

  public List<String> getChangelogPaths() {
    return changelogPaths;
  }

  public void setChangelogPaths(List<String> changelogPaths) {
    this.changelogPaths = changelogPaths;
  }

  public String getDatasourceName() {
    return datasourceName;
  }
}
