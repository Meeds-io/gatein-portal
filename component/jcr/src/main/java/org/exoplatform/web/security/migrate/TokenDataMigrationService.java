package org.exoplatform.web.security.migrate;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.security.GateInTokenStore;
import org.exoplatform.web.security.JCRGateInTokenStorage;
import org.exoplatform.web.security.jpa.JPAGateInTokenStorage;
import org.exoplatform.web.security.security.TokenExistsException;
import org.picocontainer.Startable;

import java.util.List;

public class TokenDataMigrationService implements Startable {
    private final String SETTING_KEY_TOKEN_MIGRATED = "GATEIN_TOKEN_MIGRATED";
    private final String SETTING_KEY_TOKEN_CLEANUP = "GATEIN_TOKEN_CLEANUP";
    private final int LIMIT_THRESHOLD = 100;

    protected Log LOG = ExoLogger.getLogger(TokenDataMigrationService.class);;

    private final JPAGateInTokenStorage jpaStore;
    private final JCRGateInTokenStorage jcrStore;
    private final SettingService settingService;

    private boolean forkStop;
    private boolean isMigrated;
    private boolean isCleanDone;

    public TokenDataMigrationService(JPAGateInTokenStorage jpaStore, JCRGateInTokenStorage jcrStore, SettingService settingService) {
        this.jpaStore = jpaStore;
        this.jcrStore = jcrStore;
        this.settingService = settingService;

        this.forkStop = false;
        this.isMigrated = false;
        this.isCleanDone = false;
    }

    @Override
    public void start() {
        forkStop = false;
        try {
            RequestLifeCycle.begin(PortalContainer.getInstance());
            beforeMigration();

            //
            if (!this.isMigrated) {
                doMigration();
            }

            if (this.isMigrated && !this.isCleanDone) {
                doRemove();
            }

            //
            afterMigration();
        } catch (Exception e) {
            LOG.error("Failed to run migration data from JCR to Mysql.", e);
        } finally {
            RequestLifeCycle.end();
        }
    }

    @Override
    public void stop() {
        this.forkStop = true;
    }

    private void beforeMigration() {
        this.isMigrated = getSetting(SETTING_KEY_TOKEN_MIGRATED);
        this.isCleanDone = getSetting(SETTING_KEY_TOKEN_CLEANUP);
    }

    private void afterMigration() {
        if (!forkStop) {
            this.saveSetting(SETTING_KEY_TOKEN_CLEANUP, this.isCleanDone);
            this.saveSetting(SETTING_KEY_TOKEN_MIGRATED, this.isMigrated);
        }
    }

    private boolean getSetting(String key) {
        SettingValue<String> setting = (SettingValue<String>)settingService.get(Context.GLOBAL, Scope.GLOBAL.id(null), SETTING_KEY_TOKEN_MIGRATED);
        if (setting != null) {
            return Boolean.parseBoolean(setting.getValue());
        } else {
            return false;
        }
    }
    private void saveSetting(String key, boolean value) {
        SettingValue<String> setting = SettingValue.create(String.valueOf(value));
        settingService.set(Context.GLOBAL, Scope.GLOBAL.id(null), SETTING_KEY_TOKEN_MIGRATED, setting);
    }

    private void doMigration() {
        long offset = 0;
        long limit = LIMIT_THRESHOLD;

        boolean next = true;
        boolean success = true;
        while (next) {
            List<GateInTokenStore.TokenData> tokens = jcrStore.getAll(offset, limit);
            boolean hasData = tokens != null && !tokens.isEmpty();
            for (GateInTokenStore.TokenData token : tokens) {
                try {
                    jpaStore.saveToken(token);
                } catch (TokenExistsException ex) {
                    // ignore this exception
                } catch (Exception ex) {
                    LOG.error("There is exception while migrate token data with id: " + token.tokenId, ex);
                    success = false;
                }
            }

            RequestLifeCycle.end();
            RequestLifeCycle.begin(PortalContainer.getInstance());

            offset += limit;
            next = hasData && !forkStop;
        }
        this.isMigrated = success && !forkStop;
    }

    private void doRemove() {
        try {
            jcrStore.deleteAll();
            this.isCleanDone = true;
        } catch (Exception ex) {
            LOG.error("Error when clean up token", ex);
        }
    }
}
