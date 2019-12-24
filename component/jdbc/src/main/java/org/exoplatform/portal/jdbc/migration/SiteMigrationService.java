package org.exoplatform.portal.jdbc.migration;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.pom.config.POMDataStorage;
import org.exoplatform.portal.pom.data.*;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;

public class SiteMigrationService extends AbstractMigrationService {

  public static final String EVENT_LISTENER_KEY = "PORTAL_SITES_MIGRATION";

  public SiteMigrationService(InitParams initParams,
                              POMDataStorage pomStorage,
                              ModelDataStorage modelStorage,
                              ListenerService listenerService,
                              RepositoryService repoService,
                              SettingService settingService) {
    super(initParams, pomStorage, modelStorage, listenerService, repoService, settingService);
  }

  @Override
  public void doMigrate(PortalKey siteToMigrateKey) throws Exception {
    PortalData toMigrateSite = pomStorage.getPortalConfig(siteToMigrateKey);
    ContainerData portalLayoutContainer = this.migrateContainer(toMigrateSite.getPortalLayout());

    PortalData created = modelStorage.getPortalConfig(siteToMigrateKey);
    if (created == null) {
      PortalData migrate = new PortalData(null,
                                          toMigrateSite.getName(),
                                          toMigrateSite.getType(),
                                          toMigrateSite.getLocale(),
                                          toMigrateSite.getLabel(),
                                          toMigrateSite.getDescription(),
                                          toMigrateSite.getAccessPermissions(),
                                          toMigrateSite.getEditPermission(),
                                          toMigrateSite.getProperties(),
                                          toMigrateSite.getSkin(),
                                          portalLayoutContainer,
                                          toMigrateSite.getRedirects());
      modelStorage.create(migrate);
    } else {
      PortalData migrate = new PortalData(created.getStorageId(),
                                          created.getName(),
                                          created.getType(),
                                          created.getLocale(),
                                          created.getLabel(),
                                          created.getDescription(),
                                          created.getAccessPermissions(),
                                          created.getEditPermission(),
                                          created.getProperties(),
                                          created.getSkin(),
                                          portalLayoutContainer,
                                          created.getRedirects());
      modelStorage.save(migrate);
    }

    created = modelStorage.getPortalConfig(siteToMigrateKey);
    broadcastListener(created, created.getKey().toString());
  }

  @Override
  public void doRemove(PortalKey siteToMigrateKey) throws Exception {
    PortalData data = pomStorage.getPortalConfig(siteToMigrateKey);
    if (data != null) {
      pomStorage.remove(data);
    }
    pomStorage.save();
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
