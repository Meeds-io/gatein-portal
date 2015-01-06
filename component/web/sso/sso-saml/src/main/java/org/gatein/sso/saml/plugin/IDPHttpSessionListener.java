package org.gatein.sso.saml.plugin;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.sso.integration.SSOUtils;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Created by tuyennt on 12/31/14.
 */
public class IDPHttpSessionListener extends Listener<PortalContainer, HttpSessionEvent> {
    private static final Logger log = LoggerFactory.getLogger(IDPHttpSessionListener.class);

    private static final String PROPERTY_IDP_ENABLED = "gatein.sso.idp.listener.enabled";

    private volatile HttpSessionListener delegate;

    @Override
    public void onEvent(Event<PortalContainer, HttpSessionEvent> event) throws Exception {
        if ("true".equals(SSOUtils.getSystemProperty(PROPERTY_IDP_ENABLED, "false"))) {
            HttpSessionEvent se = event.getData();
            HttpSessionListener delegate = getOrInitDelegate();
            delegate.sessionDestroyed(se);
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Portal is not acting as SAML2 IDP. Ignore this listener");
            }
        }
    }

    private HttpSessionListener getOrInitDelegate() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = new org.picketlink.identity.federation.web.listeners.IDPHttpSessionListener();
                }
            }
        }
        return delegate;
    }
}
